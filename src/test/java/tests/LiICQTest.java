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

import static org.junit.Assert.assertTrue;
import algorithms.LiICQ;
import net.imglib2.TwinCursor;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

import org.junit.Test;

/**
 * This class contains JUnit 4 test cases for the calculation of Li's
 * ICQ value.
 *
 * @author Dan White & Tom Kazimiers
 */
public class LiICQTest extends ColocalisationTest {

	/**
	 * Checks Li's ICQ value for positive correlated images.
	 */
	@Test
	public void liPositiveCorrTest() {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				positiveCorrelationImageCh1.randomAccess(),
				positiveCorrelationImageCh2.randomAccess(),
				Views.iterable(positiveCorrelationAlwaysTrueMask).localizingCursor());
		// calculate Li's ICQ value
		double icq = LiICQ.calculateLisICQ(cursor, positiveCorrelationImageCh1Mean,
					positiveCorrelationImageCh2Mean);
		assertTrue(icq > 0.34 && icq < 0.35);
	}

	/**
	 * Checks Li's ICQ value for zero correlated images. The ICQ value
	 * should be about zero.
	 */
	@Test
	public void liZeroCorrTest() {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				zeroCorrelationImageCh1.randomAccess(),
				zeroCorrelationImageCh2.randomAccess(),
				Views.iterable(zeroCorrelationAlwaysTrueMask).localizingCursor());
		// calculate Li's ICQ value
		double icq = LiICQ.calculateLisICQ(cursor, zeroCorrelationImageCh1Mean,
					zeroCorrelationImageCh2Mean);
		assertTrue(Math.abs(icq) < 0.01);
	}
}
