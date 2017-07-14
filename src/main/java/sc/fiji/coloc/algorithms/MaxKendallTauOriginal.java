package sc.fiji.coloc.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.PairIterator;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.roi.RectangleRegionOfInterest;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import sc.fiji.coloc.gadgets.DataContainer;
import sc.fiji.coloc.gadgets.DataContainer.MaskType;
import sc.fiji.coloc.results.ResultHandler;


public class MaxKendallTauOriginal <T extends RealType< T >& NativeType<T>> extends Algorithm<T> {
	
	int nrRandomizations;
	
	public double maxtau;
	public double pvalue;
	public int thresholdRank1;
	public int thresholdRank2;
	public double[] sampleDistribution;
	
	public MaxKendallTauOriginal() {
		super("Maximum of Kendall's Tau Rank Correlation");
		this.nrRandomizations = 10;
	}
	
	@Override
	public void execute(DataContainer<T> container)
		throws MissingPreconditionException
	{
		final RandomAccessible<T> img1 = container.getSourceImage1();
		final RandomAccessible<T> img2 = container.getSourceImage2();
		RandomAccessibleInterval<BitType> mask = container.getMask();

		TwinCursor<T> cursor = new TwinCursor<T>(img1.randomAccess(),
				img2.randomAccess(), Views.iterable(mask).localizingCursor());		
		
		maxtau = calculateMaxTauIndex(cursor);
		
		List<IterableInterval<T>> blockIntervals = generateBlocks(container.getSourceImage1(), container);
		Img<T> shuffledImage;
		sampleDistribution = new double[nrRandomizations];
		
		for (int i=0; i < nrRandomizations; i++) {
			shuffledImage = shuffleBlocks(blockIntervals, img1, container);
			cursor = new TwinCursor<T>(shuffledImage.randomAccess(),
					img2.randomAccess(), Views.iterable(mask).localizingCursor());
			sampleDistribution[i] = calculateMaxTauIndex(cursor);
		}
		
		pvalue = calculatePvalue(maxtau, sampleDistribution);
		
	}
	
	protected double calculatePvalue (double value, double[] distribution) {
		double count = 0;
		for (int i = 0; i <  distribution.length; i++) {
			if (distribution[i] > value)
			{
				count++;
			}
		}
		
		double pvalue1 = count / distribution.length;
		
		return pvalue1;
	}
	
	protected <T extends RealType<T>& NativeType<T>> Img<T> shuffleBlocks(List<IterableInterval<T>> blockIntervals, RandomAccessible<T> img, DataContainer<T> container) {
		int nrBlocksPerImage = blockIntervals.size();
		List<Cursor<T>> inputBlocks = new ArrayList<Cursor<T>>(nrBlocksPerImage);
		List<Cursor<T>> outputBlocks = new ArrayList<Cursor<T>>(nrBlocksPerImage);
		for (IterableInterval<T> roiIt : blockIntervals) {
			inputBlocks.add(roiIt.localizingCursor());
			outputBlocks.add(roiIt.localizingCursor());
		}
		
		final T zero = img.randomAccess().get().createVariable();
		zero.setZero();

		final long[] dims = new long[img.numDimensions()];
		ImgFactory<T> factory = new ArrayImgFactory<T>();
		Img<T> shuffledImage = factory.create(dims, img.randomAccess().get().createVariable());
		RandomAccessible<T> infiniteShuffledImage = Views.extendValue(shuffledImage, zero );
		
		Collections.shuffle( inputBlocks );
		RandomAccess<T> output = infiniteShuffledImage.randomAccess();
		
		if (container.getMaskType() == MaskType.Irregular) {
			Cursor<T> siCursor = shuffledImage.cursor();
			// black the whole intermediate image, just in case we have irr. masks
			while (siCursor.hasNext()) {
				siCursor.fwd();
				output.setPosition(siCursor);
				output.get().setZero();
			}
		}
		
		for (int j = 0; j < inputBlocks.size(); j++) {
			Cursor<T> inputCursor = inputBlocks.get(j);
			Cursor<T> outputCursor = outputBlocks.get(j);
			while (inputCursor.hasNext() && outputCursor.hasNext()) {
				inputCursor.fwd();
				outputCursor.fwd();
				output.setPosition(outputCursor);
				output.get().set( inputCursor.get() );
				inputCursor.reset();
				outputCursor.reset();
			}
		}
		
		return shuffledImage;
	}
	
	protected <T extends RealType< T >& NativeType<T>> List<IterableInterval<T>> generateBlocks(RandomAccessibleInterval<T> img, DataContainer<T> container) {
		long[] dimensions = container.getMaskBBSize();
		int nrDimensions = dimensions.length;
		int nrBlocksPerImage = 1;
		long[] nrBlocksPerDimension = new long[nrDimensions];
		long[] blockSize = new long[nrDimensions];
		
		for (int i = 0; i < nrDimensions; i++) {
			blockSize[i] = (long) Math.floor(Math.sqrt(dimensions[i]));
			nrBlocksPerDimension[i] = dimensions[i] / blockSize[i];
			// if there is the need for a out-of-bounds block, increase count
			if ( dimensions[i] % blockSize[i] != 0 )
				nrBlocksPerDimension[i]++;
			nrBlocksPerImage *= nrBlocksPerDimension[i];
		}
		
		long[] floatOffset = new long[ img.numDimensions() ];
		long[] longOffset = container.getMaskBBOffset();
		for (int i=0; i< longOffset.length; ++i )
			floatOffset[i] = longOffset[i];
		double[] floatDimensions = new double[ nrDimensions ];
		for (int i=0; i< nrDimensions; ++i )
			floatDimensions[i] = dimensions[i];
		List<IterableInterval<T>> blockIntervals;
		blockIntervals = new ArrayList<IterableInterval<T>>( nrBlocksPerImage );
		RandomAccessible< T> infiniteImg = Views.extendMirrorSingle( img );
		generateBlocksXYZ(infiniteImg, blockIntervals, floatOffset, floatDimensions, blockSize);
		
		return blockIntervals;
		
	}
	
	protected <T extends RealType< T >& NativeType<T>> void generateBlocksXYZ(RandomAccessible<T> infiniteImg, List<IterableInterval<T>> blockIntervals,
			long[] offset, double[] size, long[] Blockszie) {
		// get the number of dimensions
		int nrDimensions = infiniteImg.numDimensions();
		if (nrDimensions == 2)
		{ // for a 2D image...
			generateBlocksXY(infiniteImg, blockIntervals, offset, size, Blockszie);
		}
		else if (nrDimensions == 3)
		{ // for a 3D image...
			final double depth = size[2];
			long z;
			long originalZ = offset[2];
			// go through the depth in steps of block depth
			for ( z = Blockszie[2]; z <= depth; z += Blockszie[2] ) {

				offset[2] = originalZ + z - Blockszie[2];
				generateBlocksXY(infiniteImg, blockIntervals, offset, size, Blockszie);
			}
			offset[2] = originalZ;
		}
	}
	
	protected <T extends RealType< T >& NativeType<T>> void generateBlocksXY(RandomAccessible<T> img, List<IterableInterval<T>> blockList,
			long[] offset, double[] size, long[] Blockszie) {
		// potentially masked image height
		double height = size[1];
		final long originalY = offset[1];
		// go through the height in steps of block width
		long y;
		for ( y = Blockszie[1]; y <= height; y += Blockszie[1] ) {
			offset[1] = originalY + y - Blockszie[1];
			generateBlocksX(img, blockList, offset, size, Blockszie);
		}
		// check is we need to add a out of bounds strategy cursor
		offset[1] = originalY;
	}
	
	protected <T extends RealType< T >& NativeType<T>> void generateBlocksX(RandomAccessible<T> img, List<IterableInterval<T>> blockList,
			long[] offset, double[] size, long[] Blocksize) {
		// potentially masked image width
		double width = size[0];
		final long originalX = offset[0];
		// go through the width in steps of block width
		long x;
		double[] intiBlocksize= new double[Blocksize.length];
		double[] intioffset= new double[offset.length];
		for (int i = 1;i < offset.length; i++ )
		{
			intiBlocksize[i] = Blocksize[i];
			intioffset[i] = offset[i];
		}
		for ( x = Blocksize[0]; x <= width; x += Blocksize[0] ) {
			offset[0] = originalX + x - Blocksize[0];
			RectangleRegionOfInterest roi =
					new RectangleRegionOfInterest(intioffset.clone(), intiBlocksize.clone());
			IterableInterval<T> roiInterval = roi.getIterableIntervalOverROI(img);
			blockList.add(roiInterval);
		}
		offset[0] = originalX;
	}
	
	protected <T extends RealType<T>> double calculateMaxTauIndex(final PairIterator<T> iterator) {
		double[][] values;
		double[][] rank;
		double maxKTau;
		
		int capacity = 0;
		while (iterator.hasNext()) {
			iterator.fwd();
			capacity++;
		}
		iterator.reset();
		
		values = dataPreprocessing(iterator, capacity);
		
		double[] values1 = values[0];
		double[] values2 = values[1];
		
		thresholdRank1 = calculateOtsuThreshold(values1);
		thresholdRank1 = 162930; ////////////////////////////////////////////////////////////////////////////////////////////////////// why override?
		thresholdRank2 = calculateOtsuThreshold(values2);
		thresholdRank2 = 196241; ////////////////////////////////////////////////////////////////////////////////////////////////////// why override?
		rank = rankTransformation(values, thresholdRank1, thresholdRank2, capacity);
		
		maxKTau = calculateMaxKendallTau(rank, thresholdRank1, thresholdRank2, capacity);
		
		return maxKTau;
		
	}
	
	protected <T extends RealType<T>> double[][] dataPreprocessing(final PairIterator<T> iterator, int capacity) {
		double[] values1 = new double[capacity];
		double[] values2 = new double[capacity];
		iterator.reset();
		int count = 0;
		while (iterator.hasNext()) {
			iterator.fwd();
			values1[count] = iterator.getFirst().getRealDouble();
			values2[count] = iterator.getSecond().getRealDouble();
			count++;
		}
		return new double[][] { values1, values2 };	
	}
	
	protected int calculateOtsuThreshold(double[] data) {		
		double[] sortdata = data.clone();
		Arrays.sort(sortdata);
		
		int start = 0;
		int end = 0;
		int bestThre=0;
		double bestVar = -1;
		int L = sortdata.length;
		double lessIns = 0;
		int lessNum = 0;
		double largeIns;
		int largeNum;
		double lessMean;
		double largeMean;
		double diffMean;
		double varBetween;
		int tempNum = 0;
		double totalSum = 0;
		for (int i = 0; i < L; i++)
		{
			sortdata[i] = sortdata[i] / 100;
			totalSum += sortdata[i];
		}
		
		start = 0;
		end = 0; 
		while (end < L - 1)
		{
			while (end < L)
			{
				if (Double.compare(sortdata[start],sortdata[end]) == 0)
					end++;
				else
					break;
			}
			tempNum = end-start;
			lessNum += tempNum;
			largeNum = L - lessNum;
			lessIns += tempNum*sortdata[start];
			largeIns = totalSum - lessIns;
			lessMean = lessIns / lessNum;
			largeMean = largeIns / largeNum;
			diffMean = largeMean - lessMean;
			varBetween = lessNum * largeNum * diffMean * diffMean;
			
			if (varBetween > bestVar)
			{
				bestVar = varBetween;
				bestThre = lessNum;
			}
			start = end; 
		}
		
		if (bestThre < L/2)
			bestThre = L/2;
		
		return bestThre;
	}
	
	protected double[][] rankTransformation(final double[][] values, double thresRank1, double thresRank2, int n) {	
		
		double[][] tempRank = values;

		Arrays.sort(tempRank, new Comparator<double[]>() {
			@Override
			public int compare(double[] row1, double[] row2) {
				return Double.compare(row1[1], row2[1]);
			}
		});
		
		double[] tempRank1 = tempRank[1];
		double[] tempRank2 = tempRank[0];
		int start = 0;
		int end = 0;
		int rank=0;
		while (end < n-1)
		{
			while (Double.compare(tempRank1[start],tempRank1[end]) == 0) 
			{
				end++;
				if(end >= n)
					break;
			}
			for (int i = start; i < end; i++){
				tempRank1[i]=rank+Math.random(); 
			}
			rank++;
			start=end;
		}
		// reset tempRank with new tempRank1
		tempRank = new double[][] {tempRank2, tempRank1};
		
		Arrays.sort(tempRank, new Comparator<double[]>() { 
			@Override
			public int compare(double[] row1, double[] row2) {
				return Double.compare(row1[1], row2[1]);
			}
		});
		
		for (int i = 0; i < n; i++) {
			tempRank1[i] = i + 1; 
		}
		// reset tempRank again with new tempRank1
		tempRank = new double[][] {tempRank2, tempRank1};
		
		//second 
		Arrays.sort(tempRank, new Comparator<double[]>() {
			@Override
			public int compare(double[] row1, double[] row2) {
				return Double.compare(row1[0], row2[0]);
			}
		});
		
		// reset tempRank1 and tempRank2
		tempRank1 = tempRank[1];
		tempRank2 = tempRank[0];
		start = 0;
		end = 0;
		rank=0;
		while (end < n-1)
		{
			while (Double.compare(tempRank2[start],tempRank2[end]) == 0)
			{
				end++;
				if(end >= n)
					break;
			}
			for (int i = start; i < end; i++){
				tempRank2[i]=rank+Math.random();
			}
			rank++;
			start=end;
		}
		// reset tempRank with new tempRank2
		tempRank = new double[][] {tempRank2, tempRank1};
		
		Arrays.sort(tempRank, new Comparator<double[]>() {
			@Override
			public int compare(double[] row1, double[] row2) {
				return Double.compare(row1[0], row2[0]);
			}
		});
		
		for (int i = 0; i < n; i++) {
			tempRank2[i] = i + 1;
		}
		// reset tempRank again with new tempRank2
		tempRank = new double[][] {tempRank2, tempRank1};
		// reset tempRank1 and tempRank2
		tempRank1 = tempRank[1];
		tempRank2 = tempRank[0];
		
		List<Integer> validIndex = new ArrayList<Integer>();
		for (int i = 0; i < n; i++)
		{
			if(tempRank2[i] >= thresRank1 && tempRank1[i] >= thresRank2) 
			{
				validIndex.add(i);
			}
		}
		
		int rn = validIndex.size();
		double[] finalRank1 = new double[rn];  
		double[] finalRank2 = new double[rn];
		int index = 0;
		for( Integer i : validIndex ) {
			finalRank2[index] = tempRank2[i];
			finalRank1[index] = tempRank1[i];
			index++;
		}
		
		return new double[][]{finalRank1, finalRank2};
	}
	
	protected double calculateMaxKendallTau(final double[][] rank, double thresRank1, double thresRank2, int n) { ////////////////////////////////////////////////////////////////// 2D array issues cont...
		int rn = rank.length;
		int an;
		double step = 1+1.0/Math.log(Math.log(n));
		double tempOff1=1;
		double tempOff2;
		List<Integer> activeIndex;
		double sdTau;
		double kendallTau;
		double normalTau;
		double maxNormalTau = Double.MIN_VALUE;
		
		while (tempOff1*step+thresRank1<n) {
			tempOff1 *= step;
			tempOff2 = 1;
			while (tempOff2*step+thresRank2<n) {
				tempOff2 *= step;
				
				activeIndex = new ArrayList<Integer>();
				for (int i = 0; i < rn; i++)
				{
					if(rank[i][0] >= n - tempOff1 && rank[i][1] >= n - tempOff2)
					{
						activeIndex.add(i);
					}
				}
				an = activeIndex.size();
				if (an > 1)
				{
					kendallTau = calculateKendallTau(rank, activeIndex);
					sdTau = Math.sqrt(2.0 * (2 * an + 5) / 9 / an / (an - 1));
					normalTau = kendallTau / sdTau;
				}
				else
				{
					normalTau = Double.MIN_VALUE;
				}
				if (normalTau > maxNormalTau)
					maxNormalTau = normalTau;
			}
		}
		
		return maxNormalTau;
	}
	
	protected double calculateKendallTau(final double[][] rank, List<Integer> activeIndex) { ////////////////////////////////////////////////////////////////// 2D array issues cont...
		int an = activeIndex.size();
		double[][] partRank = new double[2][an];
		int indicatr = 0;
		for( Integer i : activeIndex ) {
			partRank[0][indicatr] = rank[i][0];
			partRank[1][indicatr] = rank[i][1];
			indicatr++;
		}
		final double[] partRank1 = partRank[0];
		final double[] partRank2 = partRank[1];
		
		int[] index = new int[an];
		for (int i = 0; i < an; i++) {
			index[i] = i;
		}
		
		IntArraySorter.sort(index, new IntComparator() {
			@Override
			public int compare(int a, int b) {
				double xa = partRank1[a];
				double xb = partRank1[b];
				return Double.compare(xa, xb);
			}
		});
		
		final MergeSort mergeSort = new MergeSort(index, new IntComparator() {

			@Override
			public int compare(int a, int b) {
				double ya = partRank2[a];
				double yb = partRank2[b];
				return Double.compare(ya, yb);
			}
		});
		
		long n0 = an * (long)(an - 1) / 2;
		long S = mergeSort.sort();
		
		return (n0 - 2 * S) / (double)n0;
		
	}
	
	private final static class MergeSort {

		private int[] index;
		private final IntComparator comparator;

		public MergeSort(int[] index, IntComparator comparator) {
			this.index = index;
			this.comparator = comparator;
		}

//		public int[] getSorted() {
//			return index;
//		}

		/**
		 * Sorts the {@link #index} array.
		 * <p>
		 * This implements a non-recursive merge sort.
		 * </p>
		 * @return the equivalent number of BubbleSort swaps
		 */
		public long sort() {
			long swaps = 0;
			int n = index.length;
			// There are merge sorts which perform in-place, but their runtime is worse than O(n log n)
			int[] index2 = new int[n];
			for (int step = 1; step < n; step <<= 1) {
				int begin = 0, k = 0;
				for (;;) {
					int begin2 = begin + step, end = begin2 + step;
					if (end >= n) {
						if (begin2 >= n) {
							break;
						}
						end = n;
					}

					// calculate the equivalent number of BubbleSort swaps
					// and perform merge, too
					int i = begin, j = begin2;
					while (i < begin2 && j < end) {
						int compare = comparator.compare(index[i], index[j]);
						if (compare > 0) {
							swaps += (begin2 - i);
							index2[k++] = index[j++];
						} else {
							index2[k++] = index[i++];
						}
					}
					if (i < begin2) {
						do {
							index2[k++] = index[i++];
						} while (i < begin2);
					} else {
						while (j < end) {
							index2[k++] = index[j++];
						}
					}
					begin = end;
				}
				if (k < n) {
					System.arraycopy(index, k, index2, k, n - k);
				}
				int[] swapIndex = index2;
				index2 = index;
				index = swapIndex;
			}

			return swaps;
		}

	}
	
	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);
		handler.handleValue("Max Kendall Tau correlation value", maxtau, 4);
		handler.handleValue("Max Kendall Tau P-value", pvalue, 4);
	}

}