package algorithms;

import gadgets.DataContainer;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import results.ResultHandler;

/**
 * This class implements some basic checks for the input image data. For
 * instance: Is the percentage of zero-zero or saturated pixels too high? Also,
 * we get basic image properties / stats from imglib2,
 */
public class InputCheck<T extends RealType< T >> extends Algorithm<T> {
	/* the maximum allowed ratio between zero-zero and
	 * normal pixels
	 */
	protected final double maxZeroZeroRatio = 0.1f;
	/* the maximum allowed ratio between saturated and
	 * normal pixels within a channel
	 */
	protected final double maxSaturatedRatio = 0.1f;
	// the zero-zero pixel ratio
	double zeroZeroPixelRatio;
	// the saturated pixel ratio of channel 1
	double saturatedRatioCh1;
	// the saturated pixel ratio of channel 2
	double saturatedRatioCh2;

	// the coloc job name
	String colocJobName;

	// general image stats/parameters/values
	double ch1Max;
	double ch2Max;
	double ch1Min;
	double ch2Min;
	double ch1Mean;
	double ch2Mean;
	double ch1Integral;
	double ch2Integral;

	public InputCheck() {
		super("input data check");
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		// get the 2 images and the mask
		final RandomAccessibleInterval<T> img1 = container.getSourceImage1();
		final RandomAccessibleInterval<T> img2 = container.getSourceImage2();
		final RandomAccessibleInterval<BitType> mask = container.getMask();

		// get the cursors for iterating through pixels in images
		TwinCursor<T> cursor = new TwinCursor<T>(img1.randomAccess(),
				img2.randomAccess(), Views.iterable(mask).cursor());

		// get various general image properties/stats/values from the DataContainer
		ch1Max = container.getMaxCh1();
		ch2Max = container.getMaxCh2();
		ch1Min = container.getMinCh1();
		ch2Min = container.getMinCh2();
		ch1Mean = container.getMeanCh1();
		ch2Mean = container.getMeanCh2();
		ch1Integral = container.getIntegralCh1();
		ch2Integral = container.getIntegralCh2();

		// the total amount of pixels that have been taken into consideration
		long N = 0;
		// the number of pixels that are zero in both channels
		long Nzero = 0;
		// the number of ch1 pixels with the maximum ch1 value;
		long NsaturatedCh1 = 0;
		// the number of ch2 pixels with the maximum ch2 value;
		long NsaturatedCh2 = 0;

		while (cursor.hasNext()) {
			cursor.fwd();
			double ch1 = cursor.getFirst().getRealDouble();
			double ch2 = cursor.getSecond().getRealDouble();

			// is the current pixels combination a zero-zero pixel?
			if (Math.abs(ch1 + ch2) < 0.00001)
				Nzero++;

			// is the current pixel of channel one saturated?
			if (Math.abs(ch1Max - ch1) < 0.00001)
				NsaturatedCh1++;

			// is the current pixel of channel one saturated?
			if (Math.abs(ch2Max - ch2) < 0.00001)
				NsaturatedCh2++;

			N++;
		}

		// calculate results
		double zeroZeroRatio = (double)Nzero / (double)N;
		// for channel wise ratios we have to use half of the total pixel amount
		double ch1SaturatedRatio = (double)NsaturatedCh1 / ( (double)N *0.5);
		double ch2SaturatedRatio = (double)NsaturatedCh2 / ( (double)N * 0.5);

		/* save results
		 * Percentage results need to be multiplied by 100
		 */
		zeroZeroPixelRatio = zeroZeroRatio * 100.0;
		saturatedRatioCh1 = ch1SaturatedRatio * 100.0;
		saturatedRatioCh2 = ch2SaturatedRatio * 100.0;

		// get job name so the ResultsHandler implementation can have it.
		colocJobName = container.getJobName();

		// add warnings if values are not in tolerance range
		if ( Math.abs(zeroZeroRatio) > maxZeroZeroRatio ) {

			addWarning("zero-zero ratio too high",
				"The ratio between zero-zero pixels and other pixels is larger "
				+ IJ.d2s(zeroZeroRatio, 2) + ". Maybe you should use a ROI.");
		}
		if ( Math.abs(ch1SaturatedRatio) > maxSaturatedRatio ) {
			addWarning("saturated ch1 ratio too high",
				"The ratio between saturated pixels and other pixels in channel one is larger "
				+ IJ.d2s(maxSaturatedRatio, 2) + ". Maybe you should use a ROI.");
		}
		if ( Math.abs(ch1SaturatedRatio) > maxSaturatedRatio ) {
			addWarning("saturated ch2 ratio too high",
				"The ratio between saturated pixels and other pixels in channel two is larger "
				+ IJ.d2s(maxSaturatedRatio, 2) + ". Maybe you should use a ROI.");
		}
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);

		// Here is a good place to have the coloc job name handled by the
		// ResultsHandler implementation in use, since this class is always run.
		// I'm going to abuse a ValueResult for this,
		// even though the jobName has no numerical value...only it's String name...
		// because i want ot keep the jobName close to all the value results
		// so they get shown together by whatever implementation of ResultsHandler.
		handler.handleValue(colocJobName, -1.0, 0);

		// Make the ResultsHander implementation deal with the results.
		handler.handleValue("% zero-zero pixels", zeroZeroPixelRatio, 2);
		handler.handleValue("% saturated ch1 pixels", saturatedRatioCh1, 2);
		handler.handleValue("% saturated ch2 pixels", saturatedRatioCh2, 2);

		// Make the ResultsHander implementation deal with the images'
		// stats/parameters/values
		handler.handleValue("Channel 1 Max", ch1Max, 3);
		handler.handleValue("Channel 2 Max", ch2Max, 3);
		handler.handleValue("Channel 1 Min", ch1Min, 3);
		handler.handleValue("Channel 2 Min", ch2Min, 3);
		handler.handleValue("Channel 1 Mean", ch1Mean, 3);
		handler.handleValue("Channel 2 Mean", ch2Mean, 3);
		handler.handleValue("Channel 1 Integrated (Sum) Intensity", ch1Integral, 3);
		handler.handleValue("Channel 2 Integrated (Sum) Intensity", ch2Integral, 3);
	}
}