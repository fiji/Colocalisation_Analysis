/*-
 * #%L
 * Fiji's plugin for colocalization analysis.
 * %%
 * Copyright (C) 2009 - 2023 Fiji developers.
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

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import sc.fiji.coloc.gadgets.DataContainer;
import sc.fiji.coloc.gadgets.ThresholdMode;
import sc.fiji.coloc.results.ResultHandler;

/**
 * <p>This algorithm calculates Manders et al.'s split colocalization
 * coefficients, M1 and M2. These are independent of signal intensities,
 * but are directly proportional to the amount of
 * fluorescence in the colocalized objects in each colour channel of the
 * image, relative to the total amount of fluorescence in that channel.
 * See "Manders, Verbeek, Aten - Measurement of colocalization
 * of objects in dual-colour confocal images. J. Microscopy, vol. 169
 * pt 3, March 1993, pp 375-382".</p>
 *
 * <p>M1 = sum of Channel 1 intensity in pixels over the channel 2 threshold / total Channel 1 intensity.
 * M2 is vice versa.
 * The threshold may be everything &gt; 0 in the other channel, which we call M1 and M2: without thresholds
 * or everything above some thresholds in the opposite channels 1 or 2, called tM1 and tM2: with thresholds
 * The result is a fraction (range 0-1, but often misrepresented as a %. We wont do that here.</p>
 *
 * <p>TODO: Further, it could/should/will calculate other split colocalization coefficients,
 * such as fraction of pixels (voxels) colocalized,
 * or fraction of intensity colocalized, as described at:
 * <a href=http://www.uhnresearch.ca/facilities/wcif/imagej/colour_analysis.htm>WCIF</a>
 * copy pasted here - credits to Tony Collins.</p>
 *
 * <p>Number of colocalised voxels - Ncoloc
 * This is the number of voxels which have both channel 1 and channel 2 intensities above threshold
 * (i.e., the number of pixels in the yellow area of the scatterplot).</p>
 *
 * <p>%Image volume colocalised - %Volume
 * This is the percentage of voxels which have both channel 1 and channel 2 intensities above threshold,
 * expressed as a percentage of the total number of pixels in the image (including zero-zero pixels);
 * in other words, the number of pixels in the scatterplot's yellow area / total number of pixels in the scatter plot (the Red + Green + Blue + Yellow areas).</p>
 *
 * <p>%Voxels Colocalised - %Ch1 Vol; %Ch2 Vol
 * This generates a value for each channel. This is the number of voxels for each channel
 * which have both channel 1 and channel 2 intensities above threshold,
 * expressed as a percentage of the total number of voxels for each channel
 * above their respective thresholds; in other words, for channel 1 (along the x-axis),
 * this equals the (the number of pixels in the Yellow area) / (the number of pixels in the Blue + Yellow areas).
 * For channel 2 this is calculated as follows:
 * (the number of pixels in the Yellow area) / (the number of pixels in the Red + Yellow areas).</p>
 *
 * <p>%Intensity Colocalised - %Ch1 Int; %Ch2 Int
 * This generates a value for each channel. For channel 1, this value is equal to
 * the sum of the pixel intensities, with intensities above both channel 1 and channel 2 thresholds
 * expressed as a percentage of the sum of all channel 1 intensities;
 * in other words, it is calculated as follows:
 * (the sum of channel 1 pixel intensities in the Yellow area) / (the sum of channel 1 pixels intensities in the Red + Green + Blue + Yellow areas).</p>
 *
 * <p>%Intensities above threshold colocalised - %Ch1 Int &gt; thresh; %Ch2 Int &gt; thresh
 * This generates a value for each channel. For channel 1,
 * this value is equal to the sum of the pixel intensities
 * with intensities above both channel 1 and channel 2 thresholds
 * expressed as a percentage of the sum of all channel 1 intensities above the threshold for channel 1.
 * In other words, it is calculated as follows:
 * (the sum of channel 1 pixel intensities in the Yellow area) / (sum of channel 1 pixels intensities in the Blue + Yellow area)</p>
 *
 * <p>The results are often represented as % values, but to make them consistent with Manders'
 * split coefficients, we will also report them as fractions (range 0-1).
 * Perhaps this helps prevent the confusion in comparisons of results
 * caused by one person's %coloc being a totally
 * different measurement than another person's %coloc.</p>
 *
 * @param <T>
 */
public class MandersColocalization<T extends RealType< T >> extends Algorithm<T> {
	// Manders' split coefficients, M1 and M2: without thresholds
	// fraction of intensity of a channel, in pixels above zero in the other channel.
	private double mandersM1, mandersM2;


	// thresholded Manders M1 and M2 values,
	// Manders' split coefficients, tM1 and tM2: with thresholds
	// fraction of intensity of a channel, in pixels above threshold in the other channel.
	private double mandersThresholdedM1, mandersThresholdedM2;

	// Number of colocalized voxels (pixels) - Ncoloc
	private long numberOfPixelsAboveBothThresholds;

	// Fraction of Image volume colocalized - Fraction of Volume
	private double fractionOfPixelsAboveBothThresholds;

	// Fraction Voxels (pixels) Colocalized - Fraction of Ch1 Vol; Fraction of Ch2 Vol
	private double fractionOfColocCh1Pixels,  fractionOfColocCh2Pixels;

	// Fraction Intensity Colocalized - Fraction of Ch1 Int; Fraction of Ch2 Int
	private double fractionOfColocCh1Intensity,  fractionOfColocCh2Intensity;

	// Fraction of Intensities above threshold, colocalized -
	// Fraction of Ch1 Int > thresh; Fraction of Ch2 Int > thresh
	private double fractionOfColocCh1IntensityAboveCh1Thresh, fractionOfColocCh2IntensityAboveCh2Thresh;

	/**
	 * A result container for Manders' calculations.
	 */
	public static class MandersResults {
		public double m1;
		public double m2;
	}

	public MandersColocalization() {
		super("Manders correlation");
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		// get the two images for the calculation of Manders' split coefficients
		RandomAccessible<T> img1 = container.getSourceImage1();
		RandomAccessible<T> img2 = container.getSourceImage2();
		RandomAccessibleInterval<BitType> mask = container.getMask();

		TwinCursor<T> cursor = new TwinCursor<T>(img1.randomAccess(),
				img2.randomAccess(), Views.iterable(mask).localizingCursor());

		// calculate Manders' split coefficients without threshold, M1 and M2.
		MandersResults results = calculateMandersCorrelation(cursor,
				img1.randomAccess().get().createVariable());

		// save the results
		mandersM1 = results.m1;
		mandersM2 = results.m2;

		// calculate the thresholded Manders' split coefficients, tM1 and tM2, if possible
		AutoThresholdRegression<T> autoThreshold = container.getAutoThreshold();
		if (autoThreshold != null ) {
			// thresholded Manders' split coefficients, tM1 and tM2
			cursor.reset();
			results = calculateMandersCorrelation(cursor, autoThreshold.getCh1MaxThreshold(),
					autoThreshold.getCh2MaxThreshold(), ThresholdMode.Above);

			// save the results
			mandersThresholdedM1 = results.m1;
			mandersThresholdedM2 = results.m2;
		}
	}

	/**
	 * Calculates Manders' split coefficients, M1 and M2: without thresholds
	 *
	 * @param cursor A TwinCursor that walks over two images
	 * @param type A type instance, its value is not relevant
	 * @return Both Manders' split coefficients, M1 and M2.
	 */
	public MandersResults calculateMandersCorrelation(TwinCursor<T> cursor, T type) {
		return calculateMandersCorrelation(cursor, type, type, ThresholdMode.None);
	}

	/**
	 * Calculates Manders' split coefficients, tM1 and tM2: with thresholds
	 *
	 * @param cursor A TwinCursor that walks over two images
	 * @param thresholdCh1 type T
	 * @param thresholdCh2 type T
	 * @param tMode A ThresholdMode the threshold mode
	 * @return Both thresholded Manders' split coefficients, tM1 and tM2.
	 */
	public MandersResults calculateMandersCorrelation(TwinCursor<T> cursor,
			final T thresholdCh1, final T thresholdCh2, ThresholdMode tMode) {
		SplitCoeffAccumulator mandersAccum;
		// create a zero-values variable to compare to later on
		final T zero = thresholdCh1.createVariable();
		zero.setZero();

		// iterate over images - set the boolean value for if a pixel is thresholded

		// without thresholds: M1 and M1
		if (tMode == ThresholdMode.None) {
			mandersAccum = new SplitCoeffAccumulator(cursor) {
				@Override
				final boolean acceptMandersCh1(T type1, T type2) {
					return (type2.compareTo(zero) > 0);
				}
				@Override
				final boolean acceptMandersCh2(T type1, T type2) {
					return (type1.compareTo(zero) > 0);
				}
			};
		// with thresholds - below thresholds
		} else if (tMode == ThresholdMode.Below) {
			mandersAccum = new SplitCoeffAccumulator(cursor) {
				@Override
				final boolean acceptMandersCh1(T type1, T type2) {
					return (type2.compareTo(zero) > 0) &&
						(type2.compareTo(thresholdCh2) <= 0);
				}
				@Override
				final boolean acceptMandersCh2(T type1, T type2) {
					return (type1.compareTo(zero) > 0) &&
						(type1.compareTo(thresholdCh1) <= 0);
				}
			};
		// with thresholds - above thresholds: tM1 and tM2
		} else if (tMode == ThresholdMode.Above) {
			mandersAccum = new SplitCoeffAccumulator(cursor) {
				@Override
				final boolean acceptMandersCh1(T type1, T type2) {
					return (type2.compareTo(zero) > 0) &&
						(type2.compareTo(thresholdCh2) >= 0);
				}
				@Override
				final boolean acceptMandersCh2(T type1, T type2) {
					return (type1.compareTo(zero) > 0) &&
						(type1.compareTo(thresholdCh1) >= 0);
				}
			};
		} else {
			throw new UnsupportedOperationException();
		}

		MandersResults results = new MandersResults();
		// calculate the results, see description above, as a fraction.
		results.m1 = mandersAccum.mandersSumCh1 / mandersAccum.sumCh1;
		results.m2 = mandersAccum.mandersSumCh2 / mandersAccum.sumCh2;

		return results;
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);
		handler.handleValue( "Manders' M1 (Above zero intensity of Ch2)", mandersM1 );
		handler.handleValue( "Manders' M2 (Above zero intensity of Ch1)", mandersM2 );
		handler.handleValue( "Manders' tM1 (Above autothreshold of Ch2)", mandersThresholdedM1 );
		handler.handleValue( "Manders' tM2 (Above autothreshold of Ch1)", mandersThresholdedM2 );
	}

	/**
	 * A class similar to the Accumulator class, but more specific
	 * to the Manders' split and other split channel coefficient calculations.
	 */
	protected abstract class SplitCoeffAccumulator {
		double sumCh1, sumCh2, mandersSumCh1, mandersSumCh2;

		public SplitCoeffAccumulator(TwinCursor<T> cursor) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getFirst();
				T type2 = cursor.getSecond();
				double ch1 = type1.getRealDouble();
				double ch2 = type2.getRealDouble();

				// boolean logics for adding or not adding to the different value counters for a pixel.
				if (acceptMandersCh1(type1, type2))
					mandersSumCh1 += ch1;
				if (acceptMandersCh2(type1, type2))
					mandersSumCh2 += ch2;

				// add this pixel's two intensity values to the ch1 and ch2 sum counters
				sumCh1 += ch1;
				sumCh2 += ch2;
			}
		}
		abstract boolean acceptMandersCh1(T type1, T type2);
		abstract boolean acceptMandersCh2(T type1, T type2);
	}
}
