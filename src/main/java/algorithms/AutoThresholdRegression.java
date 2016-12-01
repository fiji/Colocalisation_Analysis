package algorithms;

import gadgets.DataContainer;
import gadgets.ThresholdMode;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import results.ResultHandler;

/**
 * A class implementing the automatic finding of a threshold
 * used for Pearson colocalisation calculation.
 */
public class AutoThresholdRegression<T extends RealType< T >> extends Algorithm<T> {
	// Identifiers for choosing which implementation to use
	public enum Implementation {Costes, Bisection};
	Implementation implementation = Implementation.Bisection;
	/* The threshold for ratio of y-intercept : y-mean to raise a warning about
	 * it being to high or low, meaning far from zero. Don't use y-max as before,
	 * since this could be a very high value outlier. Mean is probably more
	 * reliable.
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
		this(pc, Implementation.Costes);
	}

	public AutoThresholdRegression(PearsonsCorrelation<T> pc, Implementation impl) {
		super("auto threshold regression");
		pearsonsCorrellation = pc;
		implementation = impl;
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

		// A stepper that walks thresholds
		Stepper stepper;
		// to map working thresholds to channels
		ChannelMapper mapper;

		// let working threshold walk on channel one if the regression line
		// leans more towards the abscissa (which represents channel one) for
		// positive and negative correlation.
		if (m > -1 && m < 1.0) {
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
			// Select a stepper
			if (implementation == Implementation.Bisection) {
				// Start at the midpoint of channel one
				stepper = new BisectionStepper(
					Math.abs(container.getMaxCh1() + container.getMinCh1()) * 0.5,
					container.getMaxCh1());
			} else {
				stepper = new SimpleStepper(container.getMaxCh1());
			}

		} else {
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
			// Select a stepper
			if (implementation == Implementation.Bisection) {
				// Start at the midpoint of channel two
				stepper = new BisectionStepper(
					Math.abs(container.getMaxCh2() + container.getMinCh2()) * 0.5,
					container.getMaxCh2());
			} else {
				stepper = new SimpleStepper(container.getMaxCh2());
			}
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
		while (!stepper.isFinished()) {
			// round ch1 threshold and compute ch2 threshold
			ch1ThreshMax = Math.round(mapper.getCh1Threshold(stepper.getValue()));
			ch2ThreshMax = Math.round(mapper.getCh2Threshold(stepper.getValue()));
			/* Make sure we don't get overflow the image type specific threshold variables
			 * if the image data type doesn't support this value.
			 */
			thresholdCh1.setReal(clamp(ch1ThreshMax, minVal, maxVal));
			thresholdCh2.setReal(clamp(ch2ThreshMax, minVal, maxVal));

			try {
				// do persons calculation within the limits
				final double currentPersonsR =
						pearsonsCorrellation.calculatePearsons(cursor,
						ch1Mean, ch2Mean, thresholdCh1, thresholdCh2,
						ThresholdMode.Below);
				stepper.update(currentPersonsR);
			} catch (MissingPreconditionException e) {
				/* the exception that could occur is due to numerical
				 * problems within the Pearsons calculation. */
				stepper.update(Double.NaN);
			}

			// reset the cursor to reuse it
			cursor.reset();
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
				"The ratio of the y-intercept of the auto threshold regression " +
				"line to the mean value of Channel 2 is high. This means the " +
				"y-intercept is far from zero, implying a significant positive " +
				"or negative zero offset in the image data intensities. Maybe " +
				"you should use a ROI. Maybe do a background subtraction in " +
				"both channels. Make sure you didn't clip off the low " +
				"intensities to zero. This might not affect Pearson's " +
				"correlation values very much, but might harm other results.");
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
			String msg = "The auto threshold method could not find a positive " +
				"threshold, so thresholded results are meaningless.";
			msg += implementation == Implementation.Costes ? "" : " Maybe you should try classic thresholding.";
			addWarning("thresholds too low", msg);
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

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);

		handler.handleValue( "m (slope)", autoThresholdSlope , 2 );
		handler.handleValue( "b (y-intercept)", autoThresholdIntercept, 2 );
		handler.handleValue( "b to y-mean ratio", bToYMeanRatio, 2 );
		handler.handleValue( "Ch1 Max Threshold", ch1MaxThreshold.getRealDouble(), 2);
		handler.handleValue( "Ch2 Max Threshold", ch2MaxThreshold.getRealDouble(), 2);
		handler.handleValue( "Threshold regression", implementation.toString());
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
