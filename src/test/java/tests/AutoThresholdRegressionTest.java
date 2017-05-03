/*-
 * #%L
 * Fiji's plugin for colocalization analysis.
 * %%
 * Copyright (C) 2009 - 2017 Fiji developers.
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
package tests;

import static org.junit.Assert.assertEquals;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.Test;

import algorithms.AutoThresholdRegression;
import algorithms.AutoThresholdRegression.Implementation;
import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;
import gadgets.DataContainer;


public class AutoThresholdRegressionTest extends ColocalisationTest {

	@Test
	public void clampHelperTest() throws MissingPreconditionException {
		assertEquals(4, AutoThresholdRegression.clamp(5, 1, 4), 0.00001);
		assertEquals(1, AutoThresholdRegression.clamp(-2, 1, 4), 0.00001);
		assertEquals(1, AutoThresholdRegression.clamp(5, 1, 1), 0.00001);
		assertEquals(2, AutoThresholdRegression.clamp(2, -1, 3), 0.00001);
	}

	/**
	 * This test makes sure the test images A and B lead to the same thresholds,
	 * regardless whether they are added in the order A, B or B, A to the data
	 * container.
	 *
	 * @throws MissingPreconditionException
	 */
	@Test
	public void cummutativityTest() throws MissingPreconditionException {
		_cummutativityTest(Implementation.Costes);
		_cummutativityTest(Implementation.Bisection);
	}

	protected void _cummutativityTest(Implementation atrImplementation)
			throws MissingPreconditionException {
		PearsonsCorrelation<UnsignedByteType> pc1 =
				new PearsonsCorrelation<UnsignedByteType>(
						PearsonsCorrelation.Implementation.Fast);
		PearsonsCorrelation<UnsignedByteType> pc2 =
				new PearsonsCorrelation<UnsignedByteType>(
						PearsonsCorrelation.Implementation.Fast);

		AutoThresholdRegression<UnsignedByteType> atr1 =
				new AutoThresholdRegression<UnsignedByteType>(pc1, atrImplementation);
		AutoThresholdRegression<UnsignedByteType> atr2 =
				new AutoThresholdRegression<UnsignedByteType>(pc2, atrImplementation);

		RandomAccessibleInterval<UnsignedByteType> img1 = syntheticNegativeCorrelationImageCh1;
		RandomAccessibleInterval<UnsignedByteType> img2 = syntheticNegativeCorrelationImageCh2;

		DataContainer<UnsignedByteType> container1 =
				new DataContainer<UnsignedByteType>(img1, img2, 1, 1,
						"Channel 1", "Channel 2");

		DataContainer<UnsignedByteType> container2 =
				new DataContainer<UnsignedByteType>(img2, img1, 1, 1,
						"Channel 2", "Channel 1");

		atr1.execute(container1);
		atr2.execute(container2);

		assertEquals(atr1.getCh1MinThreshold(), atr2.getCh2MinThreshold());
		assertEquals(atr1.getCh1MaxThreshold(), atr2.getCh2MaxThreshold());
		assertEquals(atr1.getCh2MinThreshold(), atr2.getCh1MinThreshold());
		assertEquals(atr1.getCh2MaxThreshold(), atr2.getCh1MaxThreshold());
	}
}
