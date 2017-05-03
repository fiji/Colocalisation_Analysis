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
package sc.fiji.coloc.algorithms;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import sc.fiji.coloc.gadgets.DataContainer;
import sc.fiji.coloc.results.ResultHandler;

/**
 * This algorithm calculates Li et al.'s ICQ (intensity
 * correlation quotient).
 *
 * @param <T>
 */
public class LiICQ<T extends RealType< T >> extends Algorithm<T> {
	// the resulting ICQ value
	double icqValue;

	public LiICQ() {
		super("Li ICQ calculation");
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		double mean1 = container.getMeanCh1();
		double mean2 = container.getMeanCh2();

		// get the 2 images for the calculation of Li's ICQ
		RandomAccessible<T> img1 = container.getSourceImage1();
		RandomAccessible<T> img2 = container.getSourceImage2();
		RandomAccessibleInterval<BitType> mask = container.getMask();

		TwinCursor<T> cursor = new TwinCursor<T>(img1.randomAccess(),
				img2.randomAccess(), Views.iterable(mask).localizingCursor());
		// calculate ICQ value
		icqValue = calculateLisICQ(cursor, mean1, mean2);
	}

	/**
	 * Calculates Li et al.'s intensity correlation quotient (ICQ) for
	 * two images.
	 *
	 * @param cursor A TwinCursor that iterates over two images
	 * @param mean1 The first images mean
	 * @param mean2 The second images mean
	 * @return Li et al.'s ICQ value
	 */
	public static <T extends RealType<T>> double calculateLisICQ(TwinCursor<T> cursor, double mean1, double mean2) {
		/* variables to count the positive and negative results
		 * of Li's product of the difference of means.
		 */
		long numPositiveProducts = 0;
		long numNegativeProducts = 0;
		// iterate over image
		while (cursor.hasNext()) {
			cursor.fwd();
			T type1 = cursor.getFirst();
			T type2 = cursor.getSecond();
			double ch1 = type1.getRealDouble();
			double ch2 = type2.getRealDouble();

			double productOfDifferenceOfMeans = (mean1 - ch1) * (mean2 - ch2);

			// check for positive and negative values
			if (productOfDifferenceOfMeans < 0.0 )
				++numNegativeProducts;
			else
				++numPositiveProducts;
		}

		/* calculate Li's ICQ value by dividing the amount of "positive pixels" to the
		 * total number of pixels. Then shift it in the -0.5,0.5 range.
		 */
		return ( (double) numPositiveProducts / (double) (numNegativeProducts + numPositiveProducts) ) - 0.5;
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);
		handler.handleValue("Li's ICQ value", icqValue);
	}
}
