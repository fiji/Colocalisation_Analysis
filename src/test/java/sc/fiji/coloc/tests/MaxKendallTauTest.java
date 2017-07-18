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

package sc.fiji.coloc.tests;

import net.imglib2.TwinCursor;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

import org.junit.Test;

import sc.fiji.coloc.algorithms.MaxKendallTau;

/**
 * This class contains test cases for the calculation of Max Kendall Tau
 * correlation values.
 *
 * @author Ellen T Arena
 */
public class MaxKendallTauTest extends ColocalisationTest {

	/**
	 * Checks Max Kendall Tau's output for positive correlation images
	 */
	@Test
	public void positiveOutputTest() {
		final TwinCursor<UnsignedByteType> cursor =
			new TwinCursor<UnsignedByteType>(positiveCorrelationImageCh1
				.randomAccess(), positiveCorrelationImageCh2.randomAccess(), Views
					.iterable(positiveCorrelationAlwaysTrueMask).localizingCursor());
		// calculate Li's ICQ value
//		double maxKTauMAX = 0;
//		double maxKTauMIN = 0;
//		for (int i = 0; i < 10; i++) {
			double tempMaxKTau = MaxKendallTau.calculateMaxTauIndex(cursor);
//			System.out.println("Max K Tau = " + tempMaxKTau);
//			cursor.reset();
//			if (i == 0) { // first time set tempMaxKTau to actually mean something for
//										// this sample
//				maxKTauMAX = tempMaxKTau;
//				maxKTauMIN = tempMaxKTau;
//			}
//			if (tempMaxKTau > maxKTauMAX) {
//				maxKTauMAX = tempMaxKTau;
//			}
//			if (tempMaxKTau < maxKTauMIN) {
//				maxKTauMIN = tempMaxKTau;
//			}
//		}
		//System.out.println("Ranges from " + maxKTauMIN + " to " + maxKTauMAX + ", difference = " + (maxKTauMAX - maxKTauMIN));
		System.out.println("Max K Tau - positive = " + tempMaxKTau);
		// assertTrue(icq > 0.34 && icq < 0.35);

	}
	
	/**
	 * Checks MaxKendallTau value for zero correlation images
	 */
	@Test
	public void zeroOutputTest() {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				zeroCorrelationImageCh1.randomAccess(),
				zeroCorrelationImageCh2.randomAccess(),
				Views.iterable(zeroCorrelationAlwaysTrueMask).localizingCursor());
		// calculate Li's ICQ value
		double maxKTau = MaxKendallTau.calculateMaxTauIndex(cursor);
		System.out.println("Max K Tau - negative = " + maxKTau);
		//assertTrue(Math.abs(icq) < 0.01);
	}
}
