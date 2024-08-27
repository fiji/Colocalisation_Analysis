/*-
 * #%L
 * Fiji's plugin for colocalization analysis.
 * %%
 * Copyright (C) 2009 - 2024 Fiji developers.
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

import ij.IJ;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import sc.fiji.coloc.gadgets.DataContainer;
import sc.fiji.coloc.gadgets.DataContainer.MaskType;
import sc.fiji.coloc.results.ResultHandler;

/**
 * This class implements some basic checks for the input image data. For
 * instance: Is the percentage of zero-zero or saturated pixels too high? Also,
 * we get basic image properties / stats from imglib2, and also the
 * colocalization job name from the DataContainer and allow implementations of
 * ResultHandler to report them.
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

	// Mask infos
	MaskType maskType;
	double maskID;

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

		// get the info about the mask/ROI being used or not.
		maskType = container.getMaskType();
		maskID = (double)container.getMaskID();

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

		// add warnings if images contain negative values
		if (ch1Min < 0 || ch2Min < 0) {
			addWarning("Negative minimum pixel value found.",
					"The minimum pixel value in at least one of the channels is negative. Negative values might break the logic of some analysis methods by breaking a basic basic assumption: The pixel value is assumed to be proportional to the number of photons detected in a pixel. Negative photon counts make no physical sense. Set negative pixel values to zero, or shift pixel intensities higher so there are no negative pixel values.");
		}

		// add warnings if values are not in tolerance range
		if ( Math.abs(zeroZeroRatio) > maxZeroZeroRatio ) {

			addWarning("Zero-zero ratio too high",
				"The ratio between zero-zero pixels and other pixels is large: "
				+ IJ.d2s(zeroZeroRatio, 2) + ". Maybe you should use a ROI.");
		}
		if ( Math.abs(ch1SaturatedRatio) > maxSaturatedRatio ) {
			addWarning("Saturated ch1 ratio too high",
				"The ratio between saturated pixels and other pixels in channel one is large: "
				+ IJ.d2s(maxSaturatedRatio, 2) + ". Maybe you should use a ROI.");
		}
		if ( Math.abs(ch1SaturatedRatio) > maxSaturatedRatio ) {
			addWarning("Saturated ch2 ratio too high",
				"The ratio between saturated pixels and other pixels in channel two is large: "
				+ IJ.d2s(maxSaturatedRatio, 2) + ". Maybe you should use a ROI.");
		}
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);

		// Let us have a ValueResult for the colocalisation analysis job name: 
		// A ValueResult can be two Strings (or a string and a numerical value)
		// We want to keep the jobName close to all the value results
		// so they get shown together by whatever implementation of ResultsHandler.
		handler.handleValue("Coloc_Job_Name", colocJobName);

		// Make the ResultsHander implementation deal with the input check results.
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

		// Make the ResultsHandler implementation deal with the images'
		// ROI or mask or lack thereof, so the user knows what they used.
		handler.handleValue("Mask Type Used", maskType.label());
		handler.handleValue("Mask ID Used", maskID, 0);
	}
}
