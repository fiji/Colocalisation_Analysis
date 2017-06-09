package sc.fiji.coloc.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.AbstractAnnotatedSpace;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Iterator;
import net.imglib2.PairIterator;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.roi.RectangleRegionOfInterest;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.IterablePair;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

@Plugin(type = MaxKendallTau.class, headless = true)
public class MaxKendallTau<T extends RealType<T> & NativeType<T>, U> implements Command {

	@Parameter(label = "Img 1")
	private Iterable<T> img1;

	@Parameter(label = "Img 2")
	private Iterable<U> img2;

	@Parameter(label = "BitMask")
	private ImgPlus<BitType> mask;

	@Parameter(label = "Number of Randomizations")
	private final int nrRandomizations;

	@Parameter(type = ItemIO.OUTPUT, label = "Max Tau")
	private double maxtau;

	@Parameter(type = ItemIO.OUTPUT, label = "P-Value")
	private double pvalue;

	// internally used stuff
	private int thresholdRank1;

	private int thresholdRank2;

	private double[] sampleDistribution;

	public MaxKendallTau() {
		this.nrRandomizations = 10;
	}

	@Override
	public void run() {
		Iterable<Pair<T, U>> samples = new IterablePair<>(img1, img2);

		maxtau = calculateMaxTauIndex(samples);

		final List<IterableInterval<T>> blockIntervals = generateBlocks(img1);
		Img<T> shuffledImage;
		sampleDistribution = new double[nrRandomizations];

		for (int i = 0; i < nrRandomizations; i++) {
			shuffledImage = shuffleBlocks(blockIntervals, img1);
			samples = new IterablePair<>(img1, img2);
			sampleDistribution[i] = calculateMaxTauIndex(samples);
		}

		pvalue = calculatePvalue(maxtau, sampleDistribution);

	}

	protected double calculatePvalue(final double value, final double[] distribution) {
		double count = 0;
		for (int i = 0; i < distribution.length; i++) {
			if (distribution[i] > value) {
				count++;
			}
		}

		final double pvalue1 = count / distribution.length;

		return pvalue1;
	}

	protected <T extends RealType<T> & NativeType<T>> Img<T> shuffleBlocks(
			final List<IterableInterval<T>> blockIntervals, final Iterable<T> img12) {
		final int nrBlocksPerImage = blockIntervals.size();
		final List<Cursor<T>> inputBlocks = new ArrayList<>(nrBlocksPerImage);
		final List<Cursor<T>> outputBlocks = new ArrayList<>(nrBlocksPerImage);
		for (final IterableInterval<T> roiIt : blockIntervals) {
			inputBlocks.add(roiIt.localizingCursor());
			outputBlocks.add(roiIt.localizingCursor());
		}

		final T zero = img12.randomAccess().get().createVariable();
		zero.setZero();

		final long[] dims = Intervals.dimensionsAsLongArray(img12);
		final ImgFactory<T> factory = new ArrayImgFactory<>();
		final Img<T> shuffledImage = factory.create(dims, img12.randomAccess().get().createVariable());
		final RandomAccessible<T> infiniteShuffledImage = Views.extendValue(shuffledImage, zero);

		Collections.shuffle(inputBlocks);
		final RandomAccess<T> output = infiniteShuffledImage.randomAccess();

		// FIXME ASSUMPTION FOR NOW: Mask is irregular
		final Cursor<T> siCursor = shuffledImage.cursor();
		// black the whole intermediate image, just in case we have irr. masks
		while (siCursor.hasNext()) {
			siCursor.fwd();
			output.setPosition(siCursor);
			output.get().setZero();
		}

		for (int j = 0; j < inputBlocks.size(); j++) {
			final Cursor<T> inputCursor = inputBlocks.get(j);
			final Cursor<T> outputCursor = outputBlocks.get(j);
			while (inputCursor.hasNext() && outputCursor.hasNext()) {
				inputCursor.fwd();
				outputCursor.fwd();
				output.setPosition(outputCursor);
				output.get().set(inputCursor.get());
				inputCursor.reset();
				outputCursor.reset();
			}
		}

		return shuffledImage;
	}

	protected <T extends RealType<T> & NativeType<T>> List<IterableInterval<T>> generateBlocks(
			final Iterable<T> img12) {
		final long[] dimensions = Intervals.dimensionsAsLongArray(mask);
		final int nrDimensions = dimensions.length;
		int nrBlocksPerImage = 1;
		final long[] nrBlocksPerDimension = new long[nrDimensions];
		final long[] blockSize = new long[nrDimensions];

		for (int i = 0; i < nrDimensions; i++) {
			blockSize[i] = (long) Math.floor(Math.sqrt(dimensions[i]));
			nrBlocksPerDimension[i] = dimensions[i] / blockSize[i];
			// if there is the need for a out-of-bounds block, increase count
			if (dimensions[i] % blockSize[i] != 0)
				nrBlocksPerDimension[i]++;
			nrBlocksPerImage *= nrBlocksPerDimension[i];
		}

		final long[] floatOffset = new long[((AbstractAnnotatedSpace<CalibratedAxis>) img12).numDimensions()];
		final long[] longOffset = Intervals.minAsLongArray(mask);
		for (int i = 0; i < longOffset.length; ++i)
			floatOffset[i] = longOffset[i];
		final double[] floatDimensions = new double[nrDimensions];
		for (int i = 0; i < nrDimensions; ++i)
			floatDimensions[i] = dimensions[i];
		List<IterableInterval<T>> blockIntervals;
		blockIntervals = new ArrayList<>(nrBlocksPerImage);
		final RandomAccessible<T> infiniteImg = Views.extendMirrorSingle(img12);
		generateBlocksXYZ(infiniteImg, blockIntervals, floatOffset, floatDimensions, blockSize);

		return blockIntervals;

	}

	protected <T extends RealType<T> & NativeType<T>> void generateBlocksXYZ(final RandomAccessible<T> infiniteImg,
			final List<IterableInterval<T>> blockIntervals, final long[] offset, final double[] size,
			final long[] Blockszie) {
		// get the number of dimensions
		final int nrDimensions = infiniteImg.numDimensions();
		if (nrDimensions == 2) { // for a 2D image...
			generateBlocksXY(infiniteImg, blockIntervals, offset, size, Blockszie);
		} else if (nrDimensions == 3) { // for a 3D image...
			final double depth = size[2];
			long z;
			final long originalZ = offset[2];
			// go through the depth in steps of block depth
			for (z = Blockszie[2]; z <= depth; z += Blockszie[2]) {

				offset[2] = originalZ + z - Blockszie[2];
				generateBlocksXY(infiniteImg, blockIntervals, offset, size, Blockszie);
			}
			offset[2] = originalZ;
		}
	}

	protected <T extends RealType<T> & NativeType<T>> void generateBlocksXY(final RandomAccessible<T> img,
			final List<IterableInterval<T>> blockList, final long[] offset, final double[] size,
			final long[] Blockszie) {
		// potentially masked image height
		final double height = size[1];
		final long originalY = offset[1];
		// go through the height in steps of block width
		long y;
		for (y = Blockszie[1]; y <= height; y += Blockszie[1]) {
			offset[1] = originalY + y - Blockszie[1];
			generateBlocksX(img, blockList, offset, size, Blockszie);
		}
		// check is we need to add a out of bounds strategy cursor
		offset[1] = originalY;
	}

	protected <T extends RealType<T> & NativeType<T>> void generateBlocksX(final RandomAccessible<T> img,
			final List<IterableInterval<T>> blockList, final long[] offset, final double[] size,
			final long[] Blockszie) {
		// potentially masked image width
		final double width = size[0];
		final long originalX = offset[0];
		// go through the width in steps of block width
		long x;
		final double[] intiBlocksize = new double[Blockszie.length];
		final double[] intioffset = new double[offset.length];
		for (int i = 1; i < offset.length; i++) {
			intiBlocksize[i] = Blockszie[i];
			intioffset[i] = offset[i];
		}
		for (x = Blockszie[0]; x <= width; x += Blockszie[0]) {
			offset[0] = originalX + x - Blockszie[0];
			final RectangleRegionOfInterest roi = new RectangleRegionOfInterest(intioffset.clone(), intiBlocksize.clone());
			final IterableInterval<T> roiInterval = roi.getIterableIntervalOverROI(img);
			blockList.add(roiInterval);
		}
		offset[0] = originalX;
	}

	protected <T extends RealType<T>> double calculateMaxTauIndex(final Iterable<Pair<T, U>> samples) {
		double[][] values;
		double[][] rank;
		double maxtau1;

		int capacity = 0;
		while (((Iterator) samples).hasNext()) {
			((Iterator) samples).fwd();
			capacity++;
		}
		((Iterator) samples).reset();

		values = dataPreprocessing(samples, capacity);

		final double[] values1 = new double[capacity];
		final double[] values2 = new double[capacity];
		for (int i = 0; i < capacity; i++) {
			values1[i] = values[i][0];
			values2[i] = values[i][1];
		}

		thresholdRank1 = calculateOtsuThreshold(values1);
		// thresholdRank1 = 162930;
		thresholdRank2 = calculateOtsuThreshold(values2);
		// thresholdRank2 = 196241;
		rank = rankTransformation(values, thresholdRank1, thresholdRank2, capacity);

		maxtau1 = calculateMaxKendallTau(rank, thresholdRank1, thresholdRank2, capacity);

		return maxtau1;

	}

protected <T extends RealType<T>> double[][] dataPreprocessing(final Iterable<Pair<T, U>> samples, final int capacity) {
		final double[][] values = new double[capacity][2];
		((Iterator) samples).reset();
		int count = 0;
		for (Pair<T, U> sample : samples) {
			values[count][0] = sample.getA().getRealDouble();
			values[count][1] = sample.getB().getRealDouble();
			count++;
		}

		return values;
	}

	protected int calculateOtsuThreshold(final double[] data) {
		final double[] sortdata = data.clone();
		Arrays.sort(sortdata);

		int start = 0;
		int end = 0;
		int bestThre = 0;
		double bestVar = -1;
		final int L = sortdata.length;
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
		for (int i = 0; i < L; i++) {
			sortdata[i] = sortdata[i] / 100;
			totalSum += sortdata[i];
		}

		start = 0;
		end = 0;
		while (end < L - 1) {
			while (end < L) {
				if (Double.compare(sortdata[start], sortdata[end]) == 0)
					end++;
				else
					break;
			}
			tempNum = end - start;
			lessNum += tempNum;
			largeNum = L - lessNum;
			lessIns += tempNum * sortdata[start];
			largeIns = totalSum - lessIns;
			lessMean = lessIns / lessNum;
			largeMean = largeIns / largeNum;
			diffMean = largeMean - lessMean;
			varBetween = lessNum * largeNum * diffMean * diffMean;

			if (varBetween > bestVar) {
				bestVar = varBetween;
				bestThre = lessNum;
			}
			start = end;
		}

		if (bestThre < L / 2)
			bestThre = L / 2;

		return bestThre;
	}

	protected double[][] rankTransformation(final double[][] values, final double threshRank1, final double threshRank2, final int n) {
		final double[][] tempRank = new double[n][2];
		for (int i = 0; i < n; i++) {
			tempRank[i][0] = values[i][0];
			tempRank[i][1] = values[i][1];
		}

		Arrays.sort(tempRank, new Comparator<double[]>() {
			@Override
			public int compare(final double[] row1, final double[] row2) {
				return Double.compare(row1[1], row2[1]);
			}
		});

		int start = 0;
		int end = 0;
		int rank = 0;
		while (end < n - 1) {
			while (Double.compare(tempRank[start][1], tempRank[end][1]) == 0) {
				end++;
				if (end >= n)
					break;
			}
			for (int i = start; i < end; i++) {
				tempRank[i][1] = rank + Math.random();
			}
			rank++;
			start = end;
		}

		Arrays.sort(tempRank, new Comparator<double[]>() {
			@Override
			public int compare(final double[] row1, final double[] row2) {
				return Double.compare(row1[1], row2[1]);
			}
		});

		for (int i = 0; i < n; i++) {
			tempRank[i][1] = i + 1;
		}

		// second
		Arrays.sort(tempRank, new Comparator<double[]>() {
			@Override
			public int compare(final double[] row1, final double[] row2) {
				return Double.compare(row1[0], row2[0]);
			}
		});

		start = 0;
		end = 0;
		rank = 0;
		while (end < n - 1) {
			while (Double.compare(tempRank[start][0], tempRank[end][0]) == 0) {
				end++;
				if (end >= n)
					break;
			}
			for (int i = start; i < end; i++) {
				tempRank[i][0] = rank + Math.random();
			}
			rank++;
			start = end;
		}

		Arrays.sort(tempRank, new Comparator<double[]>() {
			@Override
			public int compare(final double[] row1, final double[] row2) {
				return Double.compare(row1[0], row2[0]);
			}
		});

		for (int i = 0; i < n; i++) {
			tempRank[i][0] = i + 1;
		}

		final List<Integer> validIndex = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			if (tempRank[i][0] >= threshRank1 && tempRank[i][1] >= thresholdRank2) {
				validIndex.add(i);
			}
		}

		final int rn = validIndex.size();
		final double[][] finalrank = new double[rn][2];
		int index = 0;
		for (final Integer i : validIndex) {
			finalrank[index][0] = tempRank[i][0];
			finalrank[index][1] = tempRank[i][1];
			index++;
		}

		return finalrank;
	}

	protected double calculateMaxKendallTau(final double[][] rank, final double threshRank1, final double threshRank21, final int n) {
		final int rn = rank.length;
		int an;
		final double step = 1 + 1.0 / Math.log(Math.log(n));
		double tempOff1 = 1;
		double tempOff2;
		List<Integer> activeIndex;
		double sdTau;
		double kendallTau;
		double normalTau;
		double maxNormalTau = Double.MIN_VALUE;

		while (tempOff1 * step + threshRank1 < n) {
			tempOff1 *= step;
			tempOff2 = 1;
			while (tempOff2 * step + threshRank21 < n) {
				tempOff2 *= step;

				activeIndex = new ArrayList<>();
				for (int i = 0; i < rn; i++) {
					if (rank[i][0] >= n - tempOff1 && rank[i][1] >= n - tempOff2) {
						activeIndex.add(i);
					}
				}
				an = activeIndex.size();
				if (an > 1) {
					kendallTau = calculateKendallTau(rank, activeIndex);
					sdTau = Math.sqrt(2.0 * (2 * an + 5) / 9 / an / (an - 1));
					normalTau = kendallTau / sdTau;
				} else {
					normalTau = Double.MIN_VALUE;
				}
				if (normalTau > maxNormalTau)
					maxNormalTau = normalTau;
			}
		}

		return maxNormalTau;
	}

	protected double calculateKendallTau(final double[][] rank, final List<Integer> activeIndex) {
		final int an = activeIndex.size();
		final double[][] partRank = new double[2][an];
		int indicatr = 0;
		for (final Integer i : activeIndex) {
			partRank[0][indicatr] = rank[i][0];
			partRank[1][indicatr] = rank[i][1];
			indicatr++;
		}
		final double[] partRank1 = partRank[0];
		final double[] partRank2 = partRank[1];

		final int[] index = new int[an];
		for (int i = 0; i < an; i++) {
			index[i] = i;
		}

		IntArraySorter.sort(index, new IntComparator() {
			@Override
			public int compare(final int a, final int b) {
				final double xa = partRank1[a];
				final double xb = partRank1[b];
				return Double.compare(xa, xb);
			}
		});

		final MergeSort mergeSort = new MergeSort(index, new IntComparator() {

			@Override
			public int compare(final int a, final int b) {
				final double ya = partRank2[a];
				final double yb = partRank2[b];
				return Double.compare(ya, yb);
			}
		});

		final long n0 = an * (long) (an - 1) / 2;
		final long S = mergeSort.sort();

		return (n0 - 2 * S) / (double) n0;

	}

	private final static class MergeSort {

		private int[] index;
		private final IntComparator comparator;

		public MergeSort(final int[] index, final IntComparator comparator) {
			this.index = index;
			this.comparator = comparator;
		}

		/**
		 * Sorts the {@link #index} array.
		 * <p>
		 * This implements a non-recursive merge sort.
		 * </p>
		 *
		 * @return the equivalent number of BubbleSort swaps
		 */
		public long sort() {
			long swaps = 0;
			final int n = index.length;
			// There are merge sorts which perform in-place, but their runtime
			// final is worse than O(n log n)
			int[] index2 = new int[n];
			for (int step = 1; step < n; step <<= 1) {
				int begin = 0, k = 0;
				for (;;) {
					final int begin2 = begin + step;
					int end = begin2 + step;
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
						final int compare = comparator.compare(index[i], index[j]);
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
				final int[] swapIndex = index2;
				index2 = index;
				index = swapIndex;
			}

			return swaps;
		}
	}
}