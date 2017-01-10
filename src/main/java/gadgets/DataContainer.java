package gadgets;

import algorithms.Algorithm;
import algorithms.AutoThresholdRegression;
import algorithms.InputCheck;
import algorithms.MissingPreconditionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.math.ImageStatistics;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

/**
 * The DataContainer keeps all the source data, jobName, pre-processing results
 * and algorithm results that have been computed. It allows a
 * ResultsHandler implementation to get most of its content
 * and makes the source image and channel information available
 * to a client.
 *
 * @param <T>
 */
public class DataContainer<T extends RealType< T >> {
	// enumeration of different mask types, include labels
	public enum MaskType {
		Regular("ROI"),
		Irregular("mask image"),
		None("none");

		private final String label;

		MaskType(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}
	}
	// some general image statistics
	private double meanCh1, meanCh2, minCh1, maxCh1, minCh2, maxCh2, integralCh1, integralCh2;
	// The source images that the results are based on
	private RandomAccessibleInterval<T> sourceImage1, sourceImage2;
	// The names of the two source images
	private String sourceImage1Name, sourceImage2Name;
	// The name of the colocalisation run job
	public String jobName;
	// The mask for the images
	private RandomAccessibleInterval<BitType> mask;
	// Type of the used mask
	protected MaskType maskType;
	// the hash code integer of the mask object
	private int maskHash;
	// The channels of the source images that the result relate to
	private int ch1, ch2;
	// The mask's bounding box
	protected long[] maskBBSize = null;
	protected long[] maskBBOffset = null;

	InputCheck<T> inputCheck = null;
	AutoThresholdRegression<T> autoThreshold = null;

	// a list that contains all added algorithms
	List< Algorithm<T> > algorithms = new ArrayList< Algorithm<T> >();

	/**
	 * Creates a new {@link DataContainer} for a specific image channel
	 * combination. We create default thresholds here that are the max and min of
	 * the data type of the source image channels.
	 *
	 * @param src1 The channel one image source
	 * @param src2 The channel two image source
	 * @param ch1 The channel one image channel
	 * @param ch2 The channel two image channel
	 */
	public DataContainer(RandomAccessibleInterval<T> src1,
			RandomAccessibleInterval<T> src2, int ch1, int ch2,
			String name1, String name2) {
		sourceImage1 = src1;
		sourceImage2 = src2;
		sourceImage1Name = name1;
		sourceImage2Name = name2;

		// create a mask that is true at all pixels.
		final long[] dims = new long[src1.numDimensions()];
		src1.dimensions(dims);
		mask = MaskFactory.createMask(dims, true);
		this.ch1 = ch1;
		this.ch2 = ch2;
		// fill mask dimension information, here the whole image
		maskBBOffset = new long[mask.numDimensions()];
		Arrays.fill(maskBBOffset, 0);
		maskBBSize = new long[mask.numDimensions()];
		mask.dimensions(maskBBSize);
		// indicated that there is actually no mask
		maskType = MaskType.None;

		maskHash = mask.hashCode();
		// create a jobName so ResultHandler instances can all use the same object
		// for the job name.
		jobName = "Colocalization_of_" + sourceImage1Name + "_versus_" + sourceImage2Name + "_" + maskHash;

		calculateStatistics();
	}

	/**
	 * Creates a new {@link DataContainer} for a specific set of image and
	 * channel combination. It will give access to the image according to
	 * the mask passed. It is expected that the mask is of the same size
	 * as an image slice. Default thresholds, min, max and mean will be set
	 * according to the mask as well.
	 *
	 * @param src1 The channel one image source
	 * @param src2 The channel two image source
	 * @param ch1 The channel one image channel
	 * @param ch2 The channel two image channel
	 * @param mask The mask to use
	 * @param offset The offset of the ROI in each dimension
	 * @param size The size of the ROI in each dimension
	 * @throws MissingPreconditionException
	 */
	public DataContainer(RandomAccessibleInterval<T> src1,
			RandomAccessibleInterval<T> src2, int ch1, int ch2,
			String name1, String name2, 
			final RandomAccessibleInterval<T> mask,
			final long[] offset, final long[] size)
			throws MissingPreconditionException {
		sourceImage1 = src1;
		sourceImage2 = src2;
		this.ch1 = ch1;
		this.ch2 = ch2;
		sourceImage1Name = name1;
		sourceImage2Name = name2;

		final int numDims = src1.numDimensions();
		maskBBOffset = new long[numDims];
		maskBBSize = new long[numDims];
		final long[] dim = new long[numDims];
		src1.dimensions(dim);
		this.mask = MaskFactory.createMask(dim.clone(), mask);

		// this constructor supports irregular masks
		maskType = MaskType.Irregular;
		adjustRoiOffset(offset, maskBBOffset, dim);
		adjustRoiSize(size, maskBBSize, dim, maskBBOffset);

		maskHash = mask.hashCode();
		// create a jobName so ResultHandler instances can all use the same
		// object for the job name.
		jobName = "Colocalization_of_" + sourceImage1Name + "_versus_" + sourceImage2Name + "_" + maskHash;

		calculateStatistics();
	}

	/**
	 * Creates a new {@link DataContainer} for a specific set of image and
	 * channel combination. It will give access to the image according to
	 * the region of interest (ROI) passed. Default thresholds, min, max and
	 * mean will be set according to the ROI as well.
	 *
	 * @param src1 The channel one image source
	 * @param src2 The channel two image source
	 * @param ch1 The channel one image channel
	 * @param ch2 The channel two image channel
	 * @param offset The offset of the ROI in each dimension
	 * @param size The size of the ROI in each dimension
	 */
	public DataContainer(RandomAccessibleInterval<T> src1,
			RandomAccessibleInterval<T> src2, int ch1, int ch2,
			String name1, String name2,
			final long[] offset, final long size[])
			throws MissingPreconditionException {
		sourceImage1 = src1;
		sourceImage2 = src2;
		sourceImage1Name = name1;
		sourceImage2Name = name2;
		
		final int numDims = src1.numDimensions();
		final long[] dim = new long[numDims];
		src1.dimensions(dim);
		long[] roiOffset = new long[numDims];
		long[] roiSize = new long[numDims];

		adjustRoiOffset(offset, roiOffset, dim);
		adjustRoiSize(size, roiSize, dim, roiOffset);

		// create a mask that is valid everywhere
		mask = MaskFactory.createMask(dim, roiOffset, roiSize);
		maskBBOffset = roiOffset.clone();
		maskBBSize = roiSize.clone();
		// this constructor only supports regular masks
		maskType = MaskType.Regular;

		this.ch1 = ch1;
		this.ch2 = ch2;

		maskHash = mask.hashCode();
		// create a jobName so ResultHandler instances can all use the same
		// object for the job name.
		jobName = "Colocalization_of_" + sourceImage1Name + "_versus_" + sourceImage2Name + "_" + maskHash;

		calculateStatistics();
	}

	protected void calculateStatistics() {
		meanCh1 = ImageStatistics.getImageMean(sourceImage1, mask);
		meanCh2 = ImageStatistics.getImageMean(sourceImage2, mask);
		minCh1 = ImageStatistics.getImageMin(sourceImage1, mask).getRealDouble();
		minCh2 = ImageStatistics.getImageMin(sourceImage2, mask).getRealDouble();
		maxCh1 = ImageStatistics.getImageMax(sourceImage1, mask).getRealDouble();
		maxCh2 = ImageStatistics.getImageMax(sourceImage2, mask).getRealDouble();
		integralCh1 = ImageStatistics.getImageIntegral(sourceImage1, mask);
		integralCh2 = ImageStatistics.getImageIntegral(sourceImage2, mask);
	}

	/**
	 * 	Make sure that the ROI offset has the same dimensionality
	 *	as the image. The method fills it up with zeros if needed.
	 *
	 * @param oldOffset The offset with the original dimensionality
	 * @param newOffset The output array with the new dimensionality
	 * @param dimensions An array of the dimensions
	 * @throws MissingPreconditionException
	 */
	protected void adjustRoiOffset(long[] oldOffset, long[] newOffset, long[] dimensions)
			throws MissingPreconditionException {
		for (int i=0; i<newOffset.length; ++i) {
			if (i < oldOffset.length) {
				if (oldOffset[i] > dimensions[i])
					throw new MissingPreconditionException("Dimension " + i + " of ROI offset is larger than image dimension.");
				newOffset[i] = oldOffset[i];
			} else {
				newOffset[i] = 0;
			}
		}
	}

	/**
	 * Transforms a ROI size array to a dimensionality. The method
	 * fill up with image (dimension - offset in that dimension) if
	 * needed.
	 *
	 * @param oldSize Size array of old dimensionality
	 * @param newSize Output size array of new dimensionality
	 * @param dimensions Dimensions representing the new dimensionality
	 * @param offset Offset of the new dimensionality
	 * @throws MissingPreconditionException
	 */
	protected void adjustRoiSize(long[] oldSize, long[] newSize, long[] dimensions, long[] offset)
			throws MissingPreconditionException {
		for (int i=0; i<newSize.length; ++i) {
			if (i < oldSize.length) {
				if (oldSize[i] > (dimensions[i] - offset[i]))
					throw new MissingPreconditionException("Dimension " + i + " of ROI size is larger than what fits in.");
				newSize[i] = oldSize[i];
			} else {
				newSize[i] = dimensions[i] - offset[i];
			}
		}
	}

	public RandomAccessibleInterval<T> getSourceImage1() {
		return sourceImage1;
	}

	public RandomAccessibleInterval<T> getSourceImage2() {
		return sourceImage2;
	}

	public String getSourceImage1Name() {
		return "Ch1_" + sourceImage1Name;
	}

	public String getSourceImage2Name() {
		return "Ch2_" + sourceImage2Name;
	}

	public String getSourceCh1Name() {
		return sourceImage1Name;
	}

	public String getSourceCh2Name() {
		return sourceImage2Name;
	}

	public String getJobName() {
		return jobName;
	}

	public RandomAccessibleInterval<BitType> getMask() {
		return mask;
	}

	public long[] getMaskBBOffset() {
		return maskBBOffset.clone();
	}

	public long[] getMaskBBSize() {
		return maskBBSize.clone();
	}

	public MaskType getMaskType(){
		return maskType;
	}

	public int getMaskID(){
		return maskHash;
	}

	public int getCh1() {
		return ch1;
	}

	public int getCh2() {
		return ch2;
	}
	public double getMeanCh1() {
		return meanCh1;
	}

	public double getMeanCh2() {
		return meanCh2;
	}

	public double getMinCh1() {
		return minCh1;
	}

	public double getMaxCh1() {
		return maxCh1;
	}

	public double getMinCh2() {
		return minCh2;
	}

	public double getMaxCh2() {
		return maxCh2;
	}

	public double getIntegralCh1() {
		return integralCh1;
	}

	public double getIntegralCh2() {
		return integralCh2;
	}

	public InputCheck<T> getInputCheck() {
		return inputCheck;
	}

	public Algorithm<T> setInputCheck(InputCheck<T> inputCheck) {
		this.inputCheck = inputCheck;
		return inputCheck;
	}

	public AutoThresholdRegression<T> getAutoThreshold() {
		return autoThreshold;
	}

	public Algorithm<T> setAutoThreshold(AutoThresholdRegression<T> autoThreshold) {
		this.autoThreshold = autoThreshold;
		return autoThreshold;
	}
}
