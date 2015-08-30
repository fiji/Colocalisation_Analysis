package algorithms;

import gadgets.DataContainer;
import gadgets.ThresholdMode;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import results.ResultHandler;

/**
 * This algorithm calculates Manders et al.'s split colocalization
 * coefficients, M1 and M2. These are independent of signal intensities,
 * but are directly proportional to the amount of
 * fluorescence in the colocalized objects in each colour channel of the
 * image, relative to the total amount of fluorescence in that channel.
 * See "Manders, Verbeek, Aten - Measurement of colocalization
 * of objects in dual-colour confocal images. J. Microscopy, vol. 169
 * pt 3, March 1993, pp 375-382".
 *
 * M1 = sum of Channel 1 intensity in pixels over the channel 2 threshold / total Channel 1 intensity.
 * M2 is vice versa.
 * The result is a fraction (range 0-1, but often misrepresented as a %. We wont do that here.
 *
 * @param <T>
 */
public class MandersColocalization<T extends RealType< T >> extends Algorithm<T> {
	// Manders M1 and M2 values -
	// fraction of intensity of a channel, in pixels above zero in the other channel.
	double mandersM1, mandersM2;

	// thresholded Manders M1 and M2 values,
	// fraction of intensity of a channel, in pixels above threshold in the other channel.
	double mandersThresholdedM1, mandersThresholdedM2;

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
		// get the two images for the calculation of Manders' values
		RandomAccessible<T> img1 = container.getSourceImage1();
		RandomAccessible<T> img2 = container.getSourceImage2();
		RandomAccessibleInterval<BitType> mask = container.getMask();

		TwinCursor<T> cursor = new TwinCursor<T>(img1.randomAccess(),
				img2.randomAccess(), Views.iterable(mask).localizingCursor());

		// calculate Manders' coefficients without threshold
		MandersResults results = calculateMandersCorrelation(cursor,
				img1.randomAccess().get().createVariable());

		// save the results
		mandersM1 = results.m1;
		mandersM2 = results.m2;

		// calculate the thresholded split Manders' coefficients, if possible
		AutoThresholdRegression<T> autoThreshold = container.getAutoThreshold();
		if (autoThreshold != null ) {
			// calculate thresholded Manders' coefficients
			cursor.reset();
			results = calculateMandersCorrelation(cursor, autoThreshold.getCh1MaxThreshold(),
					autoThreshold.getCh2MaxThreshold(), ThresholdMode.Above);

			// save the results
			mandersThresholdedM1 = results.m1;
			mandersThresholdedM2 = results.m2;
		}
	}

	/**
	 * Calculates Manders' split M1 and M2 coefficients, without a threshold
	 *
	 * @param cursor A TwinCursor that walks over two images
	 * @param type A type instance, its value is not relevant
	 * @return Both Manders' M1 and M2 values
	 */
	public MandersResults calculateMandersCorrelation(TwinCursor<T> cursor, T type) {
		return calculateMandersCorrelation(cursor, type, type, ThresholdMode.None);
	}

	/**
	 * Calculates Manders' split M1 and M2 coefficients, with a threshold
	 *
	 * @param cursor A TwinCursor that walks over two images
	 * @param type A type instance, its value is not relevant
	 * @param thresholdCh1 type T
	 * @param thresholdCh2 type T
	 * @param tmode A ThresholdMode the threshold mode
	 * @return Both Manders' M1 and M2 values
	 */
	public MandersResults calculateMandersCorrelation(TwinCursor<T> cursor,
			final T thresholdCh1, final T thresholdCh2, ThresholdMode tMode) {
		SplitCoeffAccumulator mandersAccum;
		// create a zero-values variable to compare to later on
		final T zero = thresholdCh1.createVariable();
		zero.setZero();

		// iterate over images - set the boolean value for if a pixel is thresholded
		if (tMode == ThresholdMode.None) {
			mandersAccum = new SplitCoeffAccumulator(cursor) {
				final boolean acceptMandersCh1(T type1, T type2) {
					return (type2.compareTo(zero) > 0);
				}
				final boolean acceptMandersCh2(T type1, T type2) {
					return (type1.compareTo(zero) > 0);
				}
			};
		} else if (tMode == ThresholdMode.Below) {
			mandersAccum = new SplitCoeffAccumulator(cursor) {
				final boolean acceptMandersCh1(T type1, T type2) {
					return (type2.compareTo(zero) > 0) &&
						(type2.compareTo(thresholdCh2) <= 0);
				}
				final boolean acceptMandersCh2(T type1, T type2) {
					return (type1.compareTo(zero) > 0) &&
						(type1.compareTo(thresholdCh1) <= 0);
				}
			};
		} else if (tMode == ThresholdMode.Above) {
			mandersAccum = new SplitCoeffAccumulator(cursor) {
				final boolean acceptMandersCh1(T type1, T type2) {
					return (type2.compareTo(zero) > 0) &&
						(type2.compareTo(thresholdCh2) >= 0);
				}
				final boolean acceptMandersCh2(T type1, T type2) {
					return (type1.compareTo(zero) > 0) &&
						(type1.compareTo(thresholdCh1) >= 0);
				}
			};
		} else {
			throw new UnsupportedOperationException();
		}

		MandersResults results = new MandersResults();
		// calculate the results, see description above.
		results.m1 = mandersAccum.mandersSumCh1 / mandersAccum.sumCh1;
		results.m2 = mandersAccum.mandersSumCh2 / mandersAccum.sumCh2;

		return results;
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);
		handler.handleValue( "Manders' M1 (no threshold)", mandersM1 );
		handler.handleValue( "Manders' M2 (no threshold)", mandersM2 );
		handler.handleValue( "Manders' M1 (threshold)", mandersThresholdedM1 );
		handler.handleValue( "Manders' M2 (threshold)", mandersThresholdedM2 );
	}

	/**
	 * A class similar to the Accumulator class, but more specific
	 * to the split Manders and other split channel calculations.
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
