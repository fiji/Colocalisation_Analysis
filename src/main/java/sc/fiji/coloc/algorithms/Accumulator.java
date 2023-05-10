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

import net.imglib2.TwinCursor;
import net.imglib2.type.numeric.RealType;

/**
 * A class allowing an easy accumulation of values visited by a
 * TwinCursor. After instantiation the sum of channel one,
 * channel two, products with them self and a product of both of
 * them will be available. It additionally provides the possibility
 * to subtract values from the data before the adding them to the
 * sum.
 * 
 * @author Johannes Schindelin and Tom Kazimiers
 */
public abstract class Accumulator<T extends RealType< T >> {
	protected double x, y, xx, xy, yy;
	protected int count;

	/**
	 * The two values x and y from each cursor iteration to get
	 * summed up as single values and their combinations.
	 */
	public Accumulator(final TwinCursor<T> cursor) {
		this(cursor, false, 0.0d, 0.0d);
	}

	/**
	 * The two values (x - xDiff) and (y - yDiff) from each cursor
	 * iteration to get summed up as single values and their combinations.
	 */
	public Accumulator(final TwinCursor<T> cursor, double xDiff, double yDiff) {
		this(cursor, true, xDiff, yDiff);
	}

	protected Accumulator(final TwinCursor<T> cursor, boolean substract, double xDiff, double yDiff) {
		while (cursor.hasNext()) {
			cursor.fwd();

			T type1 = cursor.getFirst();
			T type2 = cursor.getSecond();

			if (!accept(type1, type2))
				continue;

			double value1 = type1.getRealDouble();
			double value2 = type2.getRealDouble();

			if (substract) {
				value1 -= xDiff;
				value2 -= yDiff;
			}

			x += value1;
			y += value2;
			xx += value1 * value1;
			xy += value1 * value2;
			yy += value2 * value2;
			count++;
		}
	}

	public abstract boolean accept(T type1, T type2);

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getXX() {
		return xx;
	}

	public double getXY() {
		return xy;
	}

	public double getYY() {
		return yy;
	}

	public int getCount() {
		return count;
	}
}
