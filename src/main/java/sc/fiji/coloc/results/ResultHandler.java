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
package sc.fiji.coloc.results;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

import sc.fiji.coloc.algorithms.Histogram2D;

/**
 * A result handler offers different methods to process results
 * of algorithms. Algorithms get passed such a result handler and
 * can let the handler process whatever information they like.
 *
 * @param <T> The source images value type
 */
public interface ResultHandler<T extends RealType<T>> {

	void handleImage(RandomAccessibleInterval<T> image, String name);

	void handleHistogram(Histogram2D<T> histogram, String name);

	void handleWarning(Warning warning);

	void handleValue(String name, String value);

	void handleValue(String name, double value);

	void handleValue(String name, double value, int decimals);

	/**
	 * The process method should start the processing of the
	 * previously collected results. E.g. it could show some
	 * windows or produce a final zip file.
	 */
	void process();
}
