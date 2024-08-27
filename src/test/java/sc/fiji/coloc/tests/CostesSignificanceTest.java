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
package sc.fiji.coloc.tests;

import static org.junit.Assert.assertTrue;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;

import sc.fiji.coloc.algorithms.AutoThresholdRegression;
import sc.fiji.coloc.algorithms.MissingPreconditionException;
import sc.fiji.coloc.algorithms.PearsonsCorrelation;
import sc.fiji.coloc.gadgets.DataContainer;

/**
 * This class contains JUnit 4 test cases for the Costes
 * statistical significance test.
 *
 * @author Dan White
 * @author Tom Kazimiers
 */
public class CostesSignificanceTest extends ColocalisationTest {

	/**
	 * This test checks the Costes statistical significance test implementation
	 * by artificially disturbing known colocalisation. It simulates what Costes
	 * describes in figure 3 of section "Simulated data". An image representing
	 * colocalised data is generated. This is put onto two random Perlin noise
	 * images. A smoothing step is applied after the combination. With the two
	 * resulting images the Costes calculation and shuffling is done. These steps
	 * are done multiple times, every time with an increasing percentage of
	 * colocalised data in the images. As stated by Costes, colocalisation data
	 * percentages above three percent should be detected (P value > 0.95. This
	 * is the assertion of this test and checked with every iteration. Percentages
	 * to test are calculated as percentage = 10^x. Five iterations are done,
	 * increasing "x" in steps of 0.5, starting at 0. The test uses circles with
	 * a diameter of 7 as objects (similar to Costes' paper, he uses 7x7 squares).
	 */
	@Test
	public void backgroundNoiseTest() throws MissingPreconditionException {
		final int width = 512;
		final int height = 512;
		final double z = 2.178;
		final double scale = 0.1;
		final int psf = 3;
		final int objectSize = 7;
		final double[] sigma = new double[] {3.0,3.0};

		for (double exp=0; exp < 2.5; exp=exp+0.5) {
			double colocPercentage = Math.pow(10, exp);
			RandomAccessibleInterval<FloatType> ch1 = TestImageAccessor.producePerlinNoiseImage(
				new FloatType(), width, height, z, scale);
			RandomAccessibleInterval<FloatType> ch2 = TestImageAccessor.producePerlinNoiseImage(
				new FloatType(), width, height, z, scale);
			/* calculate the number of colocalised pixels, based on the percentage and the
			 * space one noise point will take (here 9, because we use 3x3 dots)
			 */
			int nrColocPixels = (int) ( ( (width * height / 100.0) * colocPercentage ) / (objectSize * objectSize) );
			// create non-smoothed coloc image. add it to the noise images and smooth them
			RandomAccessibleInterval<FloatType> colocImg = TestImageAccessor.produceNoiseImage(
				width, height, objectSize, nrColocPixels);
			TestImageAccessor.combineImages(ch1, colocImg);
			ch1 = TestImageAccessor.gaussianSmooth(ch1, sigma);
			TestImageAccessor.combineImages(ch2, colocImg);
			ch2 = TestImageAccessor.gaussianSmooth(ch2, sigma);

			DataContainer<FloatType> container
				= new DataContainer<FloatType>(ch1, ch2, 1, 1, "Channel 1", "Channel 2");

			PearsonsCorrelation<FloatType> pc
				= new PearsonsCorrelation<FloatType>(PearsonsCorrelation.Implementation.Fast);
			AutoThresholdRegression<FloatType> atr
				= new AutoThresholdRegression<FloatType>(pc);
			container.setAutoThreshold(atr);
			atr.execute(container);
			try {
				pc.execute(container);
			}
			catch (MissingPreconditionException e) {
				/* this can happen for random noise data in seldom cases,
				 * but we are not after this here. The cases that are
				 * important for Costes work well, but are again sanity
				 * checked here.
				 */
				if (pc.getPearsonsCorrelationValue() == Double.NaN)
					throw e;
			}

			sc.fiji.coloc.algorithms.CostesSignificanceTest<FloatType> costes
				= new sc.fiji.coloc.algorithms.CostesSignificanceTest<FloatType>(pc, psf, 10, false);
			costes.execute(container);

			// check if we can expect a high P
			if (colocPercentage > 3.0) {
				double pVal = costes.getCostesPValue();
				assertTrue("Costes P value was " + pVal, pVal > 0.95);
			}
		}
	}
}
