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
package net.imglib2;

/**
 * An iterator over pairs of types.
 *  
 * @author "Johannes Schindelin"
 *
 * @param <T>
 */
public interface PairIterator<T> {

	/**
	 * Returns whether there are pairs left.
	 * 
	 * @return true if there are pairs left.
	 */
	boolean hasNext();

	/**
	 * Resets the iterator to just before the first element.
	 */
	void reset();

	/**
	 * Go to the next pair.
	 */
	void fwd();

	/**
	 * Return the first value of the pair.
	 * 
	 * @return the first value of the pair
	 */
	T getFirst();

	/**
	 * Return the second value of the pair.
	 * 
	 * @return the second value of the pair
	 */
	T getSecond();

}
