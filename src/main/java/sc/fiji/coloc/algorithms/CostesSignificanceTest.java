/*-
 * #%L
 * Fiji's plugin for colocalization analysis.
 * %%
 * Copyright (C) 2009 - 2017 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package sc.fiji.coloc.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss.Gauss;
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
import sc.fiji.coloc.gadgets.Statistics;
import sc.fiji.coloc.results.ResultHandler;

public class CostesSignificanceTest<T extends RealType<T> & NativeType<T>>
	extends Algorithm<T>
{

	// radius of the PSF in pixels, its size *must* for now be three
	protected double[] psfRadius = new double[3];
	// indicates if the shuffled images should be shown as a result
	boolean showShuffledImages = false;
	// the number of randomization tests
	int nrRandomizations;
	// the shuffled image last worked on
	Img<T> smoothedShuffledImage;
	// the Pearson's algorithm (that should have been run before)
	PearsonsCorrelation<T> pearsonsCorrelation;
	// a list of resulting Pearsons values from the randomized images
	List<Double> shuffledPearsonsResults;
	/* the amount of Pearson's values with shuffled data
	 * that has the value of the original one or is larger.
	 */
	int shuffledPearsonsNotLessOriginal = 0;
	// The mean of the shuffled Pearson values
	double shuffledMean = 0.0;
	// The standard derivation of the shuffled Pearson values
	double shuffledStdDerivation = 0.0;
	/* The Costes P-Value which is the probability that
	 * Pearsons r is different from the mean of the randomized
	 * r values.
	 */
	double costesPValue;
	// the maximum retries in case of Pearson numerical errors
	protected final int maxErrorRetries = 3;

	/**
	 * Creates a new Costes significance test object by using a cube block with
	 * the given edge length.
	 *
	 * @param psfRadiusInPixels The edge width of the 3D cube block.
	 */
	public CostesSignificanceTest(final PearsonsCorrelation<T> pc,
		final int psfRadiusInPixels, final int nrRandomizations,
		final boolean showShuffledImages)
	{
		super("Costes significance test");
		this.pearsonsCorrelation = pc;
		Arrays.fill(psfRadius, psfRadiusInPixels);
		this.nrRandomizations = nrRandomizations;
		this.showShuffledImages = showShuffledImages;
	}

	/**
	 * Builds a list of blocks that represent the images. To do so we create a
	 * list image ROI cursors. If a block does not fit into the image it will get
	 * a out-of-bounds strategy.
	 */
	@Override
	public void execute(final DataContainer<T> container)
		throws MissingPreconditionException
	{
		final RandomAccessibleInterval<T> img1 = container.getSourceImage1();
		final RandomAccessibleInterval<T> img2 = container.getSourceImage2();
		final RandomAccessibleInterval<BitType> mask = container.getMask();

		/* To determine the number of needed blocks, we need
		 * the effective dimensions of the image. Since the
		 * mask is responsible for this, we ask for its size.
		 */
		final long[] dimensions = container.getMaskBBSize();
		final int nrDimensions = dimensions.length;

		// calculate the needed number of blocks per image
		int nrBlocksPerImage = 1;
		final long[] nrBlocksPerDimension = new long[3];
		for (int i = 0; i < nrDimensions; i++) {
			// add the amount of full fitting blocks to the counter
			nrBlocksPerDimension[i] = (long) (dimensions[i] / psfRadius[i]);
			// if there is the need for a out-of-bounds block, increase count
			if (dimensions[i] % psfRadius[i] != 0) nrBlocksPerDimension[i]++;
			// increase total count
			nrBlocksPerImage *= nrBlocksPerDimension[i];
		}

		/* For creating the input and output blocks we need
		 * offset and size as floating point array.
		 */
		final double[] floatOffset = new double[img1.numDimensions()];
		final long[] longOffset = container.getMaskBBOffset();
		for (int i = 0; i < longOffset.length; ++i)
			floatOffset[i] = longOffset[i];
		final double[] floatDimensions = new double[nrDimensions];
		for (int i = 0; i < nrDimensions; ++i)
			floatDimensions[i] = dimensions[i];

		/* Create the ROI blocks. The image dimensions might not be
		 * divided cleanly by the block size. Therefore we need to
		 * have an out of bounds strategy -- a mirror.
		 */
		List<IterableInterval<T>> blockIntervals;
		blockIntervals = new ArrayList<>(nrBlocksPerImage);
		final RandomAccessible<T> infiniteImg = Views.extendMirrorSingle(img1);
		generateBlocks(infiniteImg, blockIntervals, floatOffset, floatDimensions);

		// create input and output cursors and store them along their offset
		final List<Cursor<T>> inputBlocks = new ArrayList<>(
			nrBlocksPerImage);
		final List<Cursor<T>> outputBlocks = new ArrayList<>(
			nrBlocksPerImage);
		for (final IterableInterval<T> roiIt : blockIntervals) {
			inputBlocks.add(roiIt.localizingCursor());
			outputBlocks.add(roiIt.localizingCursor());
		}

		// we will need a zero variable
		final T zero = img1.randomAccess().get().createVariable();
		zero.setZero();

		/* Create a new image to contain the shuffled data and with
		 * same dimensions as the original data.
		 */
		final long[] dims = new long[img1.numDimensions()];
		img1.dimensions(dims);
		final ImgFactory<T> factory = new ArrayImgFactory<>();
		final Img<T> shuffledImage = factory.create(dims, img1.randomAccess().get()
			.createVariable());
		final RandomAccessible<T> infiniteShuffledImage = Views.extendValue(
			shuffledImage, zero);

		// create a double version of the PSF for the smoothing
		final double[] smoothingPsfRadius = new double[nrDimensions];
		for (int i = 0; i < nrDimensions; i++) {
			smoothingPsfRadius[i] = psfRadius[i];
		}

		// the retry count for error cases
		int retries = 0;

		shuffledPearsonsResults = new ArrayList<>();
		for (int i = 0; i < nrRandomizations; i++) {
			// shuffle the list
			Collections.shuffle(inputBlocks);
			// get an output random access
			final RandomAccess<T> output = infiniteShuffledImage.randomAccess();

			// check if a mask is in use and further actions are needed
			if (container.getMaskType() == MaskType.Irregular) {
				final Cursor<T> siCursor = shuffledImage.cursor();
				// black the whole intermediate image, just in case we have irr. masks
				while (siCursor.hasNext()) {
					siCursor.fwd();
					output.setPosition(siCursor);
					output.get().setZero();
				}
			}

			// write out the shuffled input blocks into the output blocks
			for (int j = 0; j < inputBlocks.size(); ++j) {
				final Cursor<T> inputCursor = inputBlocks.get(j);
				final Cursor<T> outputCursor = outputBlocks.get(j);
				/* Iterate over both blocks. Theoretically the iteration
				 * order could be different. Because we are dealing with
				 * randomized data anyway, this is not a problem here.
				 */
				while (inputCursor.hasNext() && outputCursor.hasNext()) {
					inputCursor.fwd();
					outputCursor.fwd();
					output.setPosition(outputCursor);
					// write the data
					output.get().set(inputCursor.get());
				}

				/* Reset both cursors. If we wouldn't do that, the
				 * image contents would not change on the next pass.
				 */
				inputCursor.reset();
				outputCursor.reset();
			}

			smoothedShuffledImage = Gauss.inFloat(smoothingPsfRadius, shuffledImage);

			try {
				// calculate correlation value...
				final double pValue = pearsonsCorrelation.calculatePearsons(
					smoothedShuffledImage, img2, mask);
				// ...and add it to the results list
				shuffledPearsonsResults.add(pValue);
			}
			catch (final MissingPreconditionException e) {
				/* if the randomized input data does not suit due to numerical
				 * problems, try it three times again and then fail.
				 */
				if (retries < maxErrorRetries) {
					// increase retry count and the number of randomizations
					retries++;
					nrRandomizations++;
				}
				else {
					throw new MissingPreconditionException(
						"Maximum retries have been made (" + +retries +
							"), but errors keep on coming: " + e.getMessage(), e);
				}
			}
		}

		// calculate statistics on the randomized values and the original one
		final double originalVal = pearsonsCorrelation
			.getPearsonsCorrelationValue();
		calculateStatistics(shuffledPearsonsResults, originalVal);
	}

	/**
	 * This method drives the creation of RegionOfInterest-Cursors on the given
	 * image. It does not matter if those generated blocks are used for reading
	 * and/or writing. The resulting blocks are put into the given list and are in
	 * the responsibility of the caller, i.e. he or she must make sure the cursors
	 * get closed on some point in time.
	 *
	 * @param img The image to create cursors on.
	 * @param blockList The list to put newly created cursors into
	 * @param offset
	 * @param size
	 */
	protected void generateBlocks(final RandomAccessible<T> img,
		final List<IterableInterval<T>> blockList, final double[] offset,
		final double[] size) throws MissingPreconditionException
	{
		// get the number of dimensions
		final int nrDimensions = img.numDimensions();
		if (nrDimensions == 2) { // for a 2D image...
			generateBlocksXY(img, blockList, offset, size);
		}
		else if (nrDimensions == 3) { // for a 3D image...
			final double depth = size[2];
			double z;
			final double originalZ = offset[2];
			// go through the depth in steps of block depth
			for (z = psfRadius[2]; z <= depth; z += psfRadius[2]) {

				offset[2] = originalZ + z - psfRadius[2];
				generateBlocksXY(img, blockList, offset, size);
			}
			// check is we need to add a out of bounds strategy cursor
			if (z > depth) {
				offset[2] = originalZ + z - psfRadius[2];
				generateBlocksXY(img, blockList, offset, size);
			}
			offset[2] = originalZ;
		}
		else throw new MissingPreconditionException(
			"Currently only 2D and 3D images are supported.");
	}

	/**
	 * Goes stepwise through the y-dimensions of the image data and adds cursors
	 * for each row to the given list. The method does not check if there is a
	 * y-dimensions, so this should be made sure before. you can enforce to create
	 * all cursors as out-of-bounds one.
	 *
	 * @param img The image to get the data and cursors from.
	 * @param blockList The list to put the blocks into.
	 * @param offset The current offset configuration. Only [0] and [1] will be
	 *          changed.
	 * @param size
	 */
	protected void generateBlocksXY(final RandomAccessible<T> img,
		final List<IterableInterval<T>> blockList, final double[] offset,
		final double[] size)
	{
		// potentially masked image height
		final double height = size[1];
		final double originalY = offset[1];
		// go through the height in steps of block width
		double y;
		for (y = psfRadius[1]; y <= height; y += psfRadius[1]) {
			offset[1] = originalY + y - psfRadius[1];
			generateBlocksX(img, blockList, offset, size);
		}
		// check is we need to add a out of bounds strategy cursor
		if (y > height) {
			offset[1] = originalY + y - psfRadius[1];
			generateBlocksX(img, blockList, offset, size);
		}
		offset[1] = originalY;
	}

	/**
	 * Goes stepwise through a row of image data and adds cursors to the given
	 * list. If there is not enough image data for a whole block, an out-of-bounds
	 * cursor is generated. The creation of out-of-bound cursors could be enforced
	 * as well.
	 *
	 * @param img The image to get the data and cursors from.
	 * @param blockList The list to put the blocks into.
	 * @param offset The current offset configuration. Only [0] of it will be
	 *          changed.
	 * @param size
	 */
	protected void generateBlocksX(final RandomAccessible<T> img,
		final List<IterableInterval<T>> blockList, final double[] offset,
		final double[] size)
	{
		// potentially masked image width
		final double width = size[0];
		final double originalX = offset[0];
		// go through the width in steps of block width
		double x;
		for (x = psfRadius[0]; x <= width; x += psfRadius[0]) {
			offset[0] = originalX + x - psfRadius[0];
			final RectangleRegionOfInterest roi = new RectangleRegionOfInterest(offset
				.clone(), psfRadius.clone());
			final IterableInterval<T> roiInterval = roi.getIterableIntervalOverROI(
				img);
			blockList.add(roiInterval);
		}
		// check is we need to add a out of bounds strategy cursor
		if (x > width) {
			offset[0] = originalX + x - psfRadius[0];
			final RectangleRegionOfInterest roi = new RectangleRegionOfInterest(offset
				.clone(), psfRadius.clone());
			final IterableInterval<T> roiInterval = roi.getIterableIntervalOverROI(
				img);
			blockList.add(roiInterval);
		}
		offset[0] = originalX;
	}

	protected void calculateStatistics(final List<Double> compareValues,
		final double originalVal)
	{
		shuffledPearsonsNotLessOriginal = 0;
		final int iterations = shuffledPearsonsResults.size();
		double compareSum = 0.0;

		for (final Double shuffledVal : shuffledPearsonsResults) {
			final double diff = shuffledVal - originalVal;
			/* check if the randomized Pearsons value is equal
			 * or larger than the original one.
			 */
			if (diff > -0.00001) {
				shuffledPearsonsNotLessOriginal++;
			}
			compareSum += shuffledVal;
		}

		shuffledMean = compareSum / iterations;
		shuffledStdDerivation = Statistics.stdDeviation(compareValues);

		// get the quantile of the original value in the shuffle distribution
		costesPValue = Statistics.phi(originalVal, shuffledMean,
			shuffledStdDerivation);

		if (costesPValue > 1.0) costesPValue = 1.0;
		else if (costesPValue < 0.0) costesPValue = 0.0;
	}

	@Override
	public void processResults(final ResultHandler<T> handler) {
		super.processResults(handler);

		// if desired, show the last shuffled image available
		if (showShuffledImages) {
			handler.handleImage(smoothedShuffledImage,
				"Smoothed & shuffled channel 1");
		}

		handler.handleValue("Costes P-Value", costesPValue, 2);
		handler.handleValue("Costes Shuffled Mean", shuffledMean, 2);
		handler.handleValue("Costes Shuffled Std.D.", shuffledStdDerivation, 2);

		/* give the ratio of results at least as large as the
		 * original value.
		 */
		double ratio = 0.0;
		if (shuffledPearsonsNotLessOriginal > 0) {
			ratio = (double) shuffledPearsonsResults.size() /
				(double) shuffledPearsonsNotLessOriginal;
		}
		handler.handleValue("Ratio of rand. Pearsons >= actual Pearsons value ",
			ratio, 2);
	}

	public double getCostesPValue() {
		return costesPValue;
	}

	public double getShuffledMean() {
		return shuffledMean;
	}

	public double getShuffledStdDerivation() {
		return shuffledStdDerivation;
	}

	public double getShuffledPearsonsNotLessOriginal() {
		return shuffledPearsonsNotLessOriginal;
	}
}
