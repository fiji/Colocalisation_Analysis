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
package net.imglib2;

import java.util.NoSuchElementException;

import net.imglib2.predicate.Predicate;
import net.imglib2.type.Type;

/**
 * The PredicateCursor traverses a whole image but only returns
 * those pixels for which the Predicate returns true. There is
 * little sense to make this less than a LocalizableCursor
 */
public class PredicateCursor<T extends Type<T>> implements Cursor<T> {
	// the condition on which a position is valid
	final protected Predicate<T> predicate;
	// the cursor driven by the evaluation of the predicate
	final protected Cursor<T> cursor;
	// indicate if the next element has already been looked up
	protected boolean lookedForNext = false;
	// true if a next element was found after a look-up
	protected boolean hasNext = false;

	public PredicateCursor(final Cursor<T> cursor,
			final Predicate<T> predicate) {
		this.cursor = cursor;
		this.predicate = predicate;
	}

	/**
	 * Walks to the next valid elements and stores them in member
	 * variables cachedType1 and cachedType2.
	 *
	 * @return true if a next element was found, false otherwise
	 */
	protected boolean findNext() {
		boolean found = false;
		while( cursor.hasNext() ) {
			cursor.fwd();
			if ( predicate.test(cursor) ) {
				found = true;
				break;
			}
		}
		hasNext = found;
		return found;
	}

	@Override
	public boolean hasNext() {
		// did we already check for a next element without doing a fwd()?
		if ( lookedForNext )
			return hasNext;

		// indicate that we will already move the cursor to the next element
		lookedForNext = true;

		return findNext();
	}

	@Override
	public void fwd() {
		/* If we did not check for a next valid element before,
		 * walk to the next element now (if there is any).
		 */
		if ( ! lookedForNext )
			findNext();

		/* Since we have manually forwarded the cursor, the cached
		 * information is not valid any more
		 */
		lookedForNext = false;
	}

	@Override
	public void jumpFwd(long num) {
		while (num > 0) {
			fwd();
		}
	}

	@Override
	public T next() {
		if ( hasNext() ) {
			fwd();
			return get();
		} else {
			throw new NoSuchElementException();
		}
	}
	
	@Override
	public void remove() {
		cursor.remove();
	}

	@Override
	public void reset() {
		cursor.reset();
		lookedForNext = false;
	}

	@Override
	public double getDoublePosition(int arg0) {
		return cursor.getDoublePosition(arg0);
	}

	@Override
	public float getFloatPosition(int arg0) {
		return cursor.getFloatPosition(arg0);
	}

	@Override
	public void localize(float[] arg0) {
		cursor.localize(arg0);
	}

	@Override
	public void localize(double[] arg0) {
		cursor.localize(arg0);
	}

	@Override
	public int numDimensions() {
		return cursor.numDimensions();
	}

	@Override
	public Sampler<T> copy() {
		return cursor.copy();
	}

	@Override
	public T get() {
		return cursor.get();
	}

	@Override
	public int getIntPosition(int arg0) {
		return cursor.getIntPosition(arg0);
	}

	@Override
	public long getLongPosition(int arg0) {
		return cursor.getLongPosition(arg0);
	}

	@Override
	public void localize(int[] arg0) {
		cursor.localize(arg0);
	}

	@Override
	public void localize(long[] arg0) {
		cursor.localize(arg0);
	}

	@Override
	public Cursor<T> copyCursor() {
		return new PredicateCursor<T>( cursor.copyCursor(), predicate );
	}
}
