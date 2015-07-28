package algorithms;

import gadgets.DataContainer;
import gadgets.ThresholdMode;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import results.ResultHandler;

/**
 * A class implementing the automatic finding of a threshold
 * used for Person colocalisation calculation.
 */
public class AutoThresholdRegression<T extends RealType< T >> extends Algorithm<T> {
	/* the threshold for ration of y-intercept : y-mean
	 * to raise a warning about it being to high or low,
	 * meaning far from zero. 
	 * Don't use y-max as before, since this could be
	 * a very high value outlier. Mean is probably more reliable. 
	 */
	final double warnYInterceptToYMeanRatioThreshold = 0.01;
	// the slope and and intercept of the regression line
	double autoThresholdSlope = 0.0, autoThresholdIntercept = 0.0;
	/* The thresholds for both image channels. Pixels below a lower
	 * threshold do NOT include the threshold and pixels above an upper
	 * one will NOT either. Pixels "in between (and including)" thresholds
	 * do include the threshold values.
	 */
	T ch1MinThreshold, ch1MaxThreshold, ch2MinThreshold, ch2MaxThreshold;
	// additional information
	double bToYMeanRatio = 0.0;
	//This is the Pearson's correlation we will use for further calculations
	PearsonsCorrelation<T> pearsonsCorrellation;

	public AutoThresholdRegression(PearsonsCorrelation<T> pc) {
		super("auto threshold regression");
		pearsonsCorrellation = pc;
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		// get the 2 images for the calculation of Pearson's
		final RandomAccessibleInterval<T> img1 = container.getSourceImage1();
		final RandomAccessibleInterval<T> img2 = container.getSourceImage2();
		final RandomAccessibleInterval<BitType> mask = container.getMask();

		double ch1Mean = container.getMeanCh1();
		double ch2Mean = container.getMeanCh2();

		double combinedMean = ch1Mean + ch2Mean;

		// get the cursors for iterating through pixels in images
		TwinCursor<T> cursor = new TwinCursor<T>(
				img1.randomAccess(), img2.randomAccess(),
				Views.iterable(mask).localizingCursor());

		// variables for summing up the
		double ch1MeanDiffSum = 0.0, ch2MeanDiffSum = 0.0, combinedMeanDiffSum = 0.0;
		double combinedSum = 0.0;
		int N = 0, NZero = 0;

		// reference image data type
		final T type = cursor.getFirst();

		while (cursor.hasNext()) {
			cursor.fwd();
			double ch1 = cursor.getFirst().getRealDouble();
			double ch2 = cursor.getSecond().getRealDouble();

			combinedSum = ch1 + ch2;

			// TODO: Shouldn't the whole calculation take only pixels
			// into account that are combined above zero? And not just
			// the denominator (like it is done now)?

			// calculate the numerators for the variances
			ch1MeanDiffSum += (ch1 - ch1Mean) * (ch1 - ch1Mean);
			ch2MeanDiffSum += (ch2 - ch2Mean) * (ch2 - ch2Mean);
			combinedMeanDiffSum += (combinedSum - combinedMean) * (combinedSum - combinedMean);

			// count only pixels that are above zero
			if ( (ch1 + ch2) > 0.00001)
				NZero++;

			N++;
		}

		double ch1Variance = ch1MeanDiffSum / (N - 1);
		double ch2Variance = ch2MeanDiffSum / (N - 1);
		double combinedVariance = combinedMeanDiffSum / (N - 1.0);

		//http://mathworld.wolfram.com/Covariance.html
		//?2 = X2?(X)2
		// = E[X2]?(E[X])2
		//var (x+y) = var(x)+var(y)+2(covar(x,y));
		//2(covar(x,y)) = var(x+y) - var(x)-var(y);

		double ch1ch2Covariance = 0.5*(combinedVariance - (ch1Variance + ch2Variance));

		// calculate regression parameters
		double denom = 2*ch1ch2Covariance;
		double num = ch2Variance - ch1Variance
			+ Math.sqrt( (ch2Variance - ch1Variance) * (ch2Variance - ch1Variance)
					+ (4 * ch1ch2Covariance *ch1ch2Covariance) );

		final double m = num/denom;
		final double b = ch2Mean - m*ch1Mean ;

		// initialize some variables relevant for regression
		// indicates whether the threshold has been found or not
		boolean thresholdFound = false;
		// the maximum number of iterations to look for the threshold
		final int maxIterations = 100;
		// the current iteration
		int iteration = 0;
		// current and last working threshold
		double threshold1, threshold2;
		// to map working thresholds to channels
		ChannelMapper mapper;

		// let working threshold walk on channel one if the regression line
		// leans more towards the abscissa (which represents channel one) for
		// positive and negative correlation.
		if (m > -1 && m < 1.0) {
			// Start at the midpoint of channel one
			threshold1 = Math.abs(container.getMaxCh1() +
					container.getMinCh1()) * 0.5;
			threshold2 = container.getMaxCh1();

			// Map working threshold to channel one (because channel one has a
			// larger maximum value.
			mapper = new ChannelMapper() {

				@Override
				public double getCh1Threshold(double t) {
					return t;
				}

				@Override
				public double getCh2Threshold(double t) {
					return (t * m) + b;
				}
			};
		} else {
			// Start at the midpoint of channel two
			threshold1 = Math.abs(container.getMaxCh2() +
					container.getMinCh2()) * 0.5;
			threshold2 = container.getMaxCh2();

			// Map working threshold to channel two (because channel two has a
			// larger maximum value.
			mapper = new ChannelMapper() {

				@Override
				public double getCh1Threshold(double t) {
					return (t - b) / m;
				}

				@Override
				public double getCh2Threshold(double t) {
					return t;
				}
			};
		}

		// Min threshold not yet implemented
		double ch1ThreshMax = container.getMaxCh1();
		double ch2ThreshMax = container.getMaxCh2();

		// define some image type specific threshold variables
		T thresholdCh1 = type.createVariable();
		T thresholdCh2 = type.createVariable();
		// reset the previously created cursor
		cursor.reset();

		/* Get min and max value of image data type. Since type of image
		 * one and two are the same, we dont't need to distinguish them.
		 */
		T dummyT = type.createVariable();
		final double minVal = dummyT.getMinValue();
		final double maxVal = dummyT.getMaxValue();

		// do regression
		while (!thresholdFound && iteration<maxIterations) {
			// round ch1 threshold and compute ch2 threshold
			ch1ThreshMax = Math.round(mapper.getCh1Threshold(threshold1));
			ch2ThreshMax = Math.round(mapper.getCh2Threshold(threshold1));
			/* Make sure we don't get overflow the image type specific threshold variables
			 * if the image data type doesn't support this value.
			 */
			thresholdCh1.setReal(clamp(ch1ThreshMax, minVal, maxVal));
			thresholdCh2.setReal(clamp(ch2ThreshMax, minVal, maxVal));

			// Person's R value
			double currentPersonsR = Double.MAX_VALUE;
			// indicates if we have actually found a real number
			boolean badResult = false;
			try {
				// do persons calculation within the limits
				currentPersonsR = pearsonsCorrellation.calculatePearsons(cursor,
						ch1Mean, ch2Mean, thresholdCh1, thresholdCh2, ThresholdMode.Below);
			} catch (MissingPreconditionException e) {
				/* the exception that could occur is due to numerical
				 * problems within the pearsons calculation.
				 */
				badResult = true;
			}

			/* If the difference between both thresholds is < 1, we consider
			 * that as reasonable close to abort the regression.
			 */
			final double thrDiff = Math.abs(threshold1 - threshold2);
			if (thrDiff < 1.0)
				thresholdFound = true;

			// update working thresholds for next iteration
			threshold2 = threshold1;
			if (badResult || currentPersonsR < 0) {
				// we went too far, increase by the absolute half
				threshold1 = threshold1 + thrDiff * 0.5;
			} else if (currentPersonsR > 0) {
				// as long as r > 0 we go half the way down
				threshold1 = threshold1 - thrDiff * 0.5;
			}

			// reset the cursor to reuse it
			cursor.reset();

			// increment iteration counter
			iteration++;
		}

		/* Store the new results. The lower thresholds are the types
		 * min value for now. For the max threshold we do a clipping
		 * to make it fit into the image type.
		 */
		ch1MinThreshold = type.createVariable();
		ch1MinThreshold.setReal(minVal);

		ch1MaxThreshold = type.createVariable();
		ch1MaxThreshold.setReal(clamp(ch1ThreshMax, minVal, maxVal));

		ch2MinThreshold = type.createVariable();
		ch2MinThreshold.setReal(minVal);

		ch2MaxThreshold = type.createVariable();
		ch2MaxThreshold.setReal(clamp(ch2ThreshMax, minVal, maxVal));

		autoThresholdSlope = m;
		autoThresholdIntercept = b;
		bToYMeanRatio = b / container.getMeanCh2();

		// add warnings if values are not in tolerance range
		if ( Math.abs(bToYMeanRatio) > warnYInterceptToYMeanRatioThreshold ) {
			addWarning("y-intercept far from zero",
				"The ratio of the y-intercept of the auto threshold regression line to the mean value of Channel 2 is high. This means the y-intercept is far from zero, implying a significant positive or negative zero offset in the image data intensities. Maybe you should use a ROI. Maybe do a background subtraction in both channels. Make sure you didn't clip off the low intensities to zero. This might not affect Pearson's correlation values very much, but might harm other results.");
		}

		// add warning if threshold is above the image mean
		if (ch1ThreshMax > ch1Mean) {
			addWarning("Threshold of ch. 1 too high",
					"Too few pixels are taken into account for above-threshold calculations. The threshold is above the channel's mean.");
		}
		if (ch2ThreshMax > ch2Mean) {
			addWarning("Threshold of ch. 2 too high",
					"Too few pixels are taken into account for above-threshold calculations. The threshold is above the channel's mean.");
		}

		// add warnings if values are below lowest pixel value of images
		if ( ch1ThreshMax < container.getMinCh1() || ch2ThreshMax < container.getMinCh2() ) {
			addWarning("thresholds too low",
				"The auto threshold method could not find a positive threshold, so thresholded results are meaningless.");
		}
	}

	/**
	 * Clamp a value to a min or max value. If the value is below min, min is
	 * returned. Accordingly, max is returned if the value is larger. If it is
	 * neither, the value itself is returned.
	 */
	public static double clamp(double val, double min, double max) {
		return min > val ? min : max < val ? max : val;
	}

	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);

		handler.handleValue( "m (slope)", autoThresholdSlope , 2 );
		handler.handleValue( "b (y-intercept)", autoThresholdIntercept, 2 );
		handler.handleValue( "b to y-mean ratio", bToYMeanRatio, 2 );
		handler.handleValue( "Ch1 Max Threshold", ch1MaxThreshold.getRealDouble(), 2);
		handler.handleValue( "Ch2 Max Threshold", ch2MaxThreshold.getRealDouble(), 2);
	}

	public double getBToYMeanRatio() {
		return bToYMeanRatio;
	}

	public double getWarnYInterceptToYMaxRatioThreshold() {
		return warnYInterceptToYMeanRatioThreshold;
	}

	public double getAutoThresholdSlope() {
		return autoThresholdSlope;
	}

	public double getAutoThresholdIntercept() {
		return autoThresholdIntercept;
	}

	public T getCh1MinThreshold() {
		return ch1MinThreshold;
	}

	public T getCh1MaxThreshold() {
		return ch1MaxThreshold;
	}

	public T getCh2MinThreshold() {
		return ch2MinThreshold;
	}

	public T getCh2MaxThreshold() {
		return ch2MaxThreshold;
	}
}
