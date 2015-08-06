package tests;

import static org.junit.Assert.assertEquals;
import algorithms.MandersColocalization;
import algorithms.MandersColocalization.MandersResults;
import algorithms.MissingPreconditionException;
import net.imglib2.Cursor;
import net.imglib2.TwinCursor;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

import gadgets.ThresholdMode;

import org.junit.Test;

/**
 * This class contains JUnit 4 test cases for the calculation
 * of Manders' split colocalization coefficients
 *
 * @author Dan White & Tom Kazimiers
 */
public class MandersColocalizationTest extends ColocalisationTest {

	/**
	 * This method tests artificial test images as detailed in 
	 * the Manders et al. paper, using above zero threshold (none).
	 * Note: It is not sensitive to choosing the wrong channel to test for
	 * above threshold, because threshold is same for both channels: above zero,
	 * and also that the blobs overlap perfectly or not at all.
	 */
	@Test
	public void mandersPaperImagesTest() throws MissingPreconditionException {
		MandersColocalization<UnsignedByteType> mc =
				new MandersColocalization<UnsignedByteType>();

		TwinCursor<UnsignedByteType> cursor;
		MandersResults r;
		// test A-A combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersA.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.randomAccess().get().createVariable());

		assertEquals(1.0d, r.m1, 0.0001);
		assertEquals(1.0d, r.m2, 0.0001);

		// test A-B combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersB.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.randomAccess().get());

		assertEquals(0.75d, r.m1, 0.0001);
		assertEquals(0.75d, r.m2, 0.0001);

		// test A-C combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersC.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());
		
		r = mc.calculateMandersCorrelation(cursor,
				mandersA.randomAccess().get());
		
		assertEquals(0.5d, r.m1, 0.0001);
		assertEquals(0.5d, r.m2, 0.0001);

		// test A-D combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersD.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.randomAccess().get());

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(0.25d, r.m2, 0.0001);

		// test A-E combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersE.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.randomAccess().get());

		assertEquals(0.0d, r.m1, 0.0001);
		assertEquals(0.0d, r.m2, 0.0001);

		// test A-F combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersF.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.randomAccess().get());

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(0.3333d, r.m2, 0.0001);

		// test A-G combination.firstElement(
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersG.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.randomAccess().get());

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(0.50d, r.m2, 0.0001);

		// test A-H combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersH.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.randomAccess().get());

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(1.00d, r.m2, 0.0001);

		// test A-I combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersI.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.randomAccess().get());

		assertEquals(0.083d, r.m1, 0.001);
		assertEquals(0.75d, r.m2, 0.0001);
	}
	
	/**
	 * This method tests real experimental noisy but 
	 * biologically perfectly colocalized test images, 
	 * using previously calculated autothresholds (.above mode)
	 * Amongst other things, hopefully it is sensitive to
	 * choosing the wrong channel to test for above threshold
	 */
	@Test
	public void mandersRealNoisyImagesTest() throws MissingPreconditionException {
		
		MandersColocalization<UnsignedByteType> mrnc = 
				new MandersColocalization<UnsignedByteType>();

		// test biologically perfect but noisy image coloc combination
		Cursor<BitType> mask;
		mask = positiveCorrelationMaskImage.cursor();
		TwinCursor<UnsignedByteType> twinCursor;
		MandersResults r;
		// Manually set the thresholds for ch1 and ch2 with the results from a
		// Costes Autothreshold using bisection implementation of regression, of the images used
		UnsignedByteType thresholdCh1 = new UnsignedByteType();
		thresholdCh1.setReal(14.0);
		UnsignedByteType thresholdCh2 = new UnsignedByteType();
		thresholdCh2.setReal(12.0);
		//Set the threshold mode
		ThresholdMode tMode;
		tMode = ThresholdMode.Above;

		twinCursor = new TwinCursor<UnsignedByteType>(
				positiveCorrelationImageCh1.randomAccess(),
				positiveCorrelationImageCh2.randomAccess(),
				mask);


		// should use the constructor that takes autothresholds and mask channel, not this one?
		// thresholds of ch1=14 and ch2=12 can be used,
		// that's what autothresholds (bisection) calculates.
		r = mrnc.calculateMandersCorrelation(twinCursor, thresholdCh1, thresholdCh2, tMode);

		assertEquals(0.795d, r.m1, 0.0001);
		assertEquals(0.773d, r.m2, 0.0001);
	}
}