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
package sc.fiji.coloc.tests;

import static org.junit.Assert.assertEquals;

import net.imglib2.Cursor;
import net.imglib2.TwinCursor;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

import org.junit.Test;

import sc.fiji.coloc.algorithms.MandersColocalization;
import sc.fiji.coloc.algorithms.MandersColocalization.MandersResults;
import sc.fiji.coloc.algorithms.MissingPreconditionException;
import sc.fiji.coloc.gadgets.ThresholdMode;

/**
 * This class contains JUnit 4 test cases for the calculation
 * of Manders' split colocalization coefficients
 *
 * @author Dan White
 * @author Tom Kazimiers
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
		// this cast is bad, so use Views.iterable instead. 
		//Cursor<BitType> mask = Converters.convert((IterableInterval<UnsignedByteType>) positiveCorrelationMaskImage,
		Cursor<BitType> mask = Converters.convert(Views.iterable(positiveCorrelationMaskImage),
                new Converter<UnsignedByteType, BitType>() {

                    @Override
                    public void convert(UnsignedByteType arg0, BitType arg1) {
                        arg1.set(arg0.get() > 0);
                    }
                }, new BitType()).cursor();
		
		TwinCursor<UnsignedByteType> twinCursor;
		MandersResults r;
		// Manually set the thresholds for ch1 and ch2 with the results from a
		// Costes Autothreshold using bisection implementation of regression, of the images used
		UnsignedByteType thresholdCh1 = new UnsignedByteType();
		thresholdCh1.setInteger(70);
		UnsignedByteType thresholdCh2 = new UnsignedByteType();
		thresholdCh2.setInteger(53);
		//Set the threshold mode
		ThresholdMode tMode;
		tMode = ThresholdMode.Above;
		// Set the TwinCursor to have the mask image channel, and 2 images.
		twinCursor = new TwinCursor<UnsignedByteType>(
				positiveCorrelationImageCh1.randomAccess(),
				positiveCorrelationImageCh2.randomAccess(),
				mask);

		// Use the constructor that takes ch1 and ch2 autothresholds and threshold mode.
		r = mrnc.calculateMandersCorrelation(twinCursor, thresholdCh1, thresholdCh2, tMode);

		assertEquals(0.705665d, r.m1, 0.000001);
		assertEquals(0.724752d, r.m2, 0.000001);
	}
}
