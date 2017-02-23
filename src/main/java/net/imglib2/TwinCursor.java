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
package net.imglib2;

import net.imglib2.predicate.MaskPredicate;
import net.imglib2.predicate.Predicate;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;

/**
 * The TwinCursor moves over two images with respect to a mask. The mask
 * has to be of the same dimensionality as the images. Position information
 * obtained from this class comes from the mask.
 *
 * @author Johannes Schindelin and Tom Kazimiers
 */
public class TwinCursor<T extends Type<T>> implements Cursor<T>, PairIterator<T> {
		final protected PredicateCursor<BitType> mask;
		final protected RandomAccess<T> channel1;
		final protected RandomAccess<T> channel2;
		/*
		 * For performance, we keep one position array (to avoid 
		 * having to create a new array in every single step).
		 */
		final protected long[] position;
		/* To avoid calling next() too often */
		protected boolean gotNext;

		public TwinCursor(final RandomAccess<T> channel1,
				final RandomAccess<T> channel2,
				final Cursor<BitType> mask) {
			final Predicate<BitType> predicate = new MaskPredicate();
			this.mask = new PredicateCursor<BitType>(mask, predicate);
			this.channel1 = channel1;
			this.channel2 = channel2;
			position = new long[mask.numDimensions()];
			mask.localize(position);
		}

		@Override
		final public boolean hasNext() {
			gotNext = false;
			return mask.hasNext();
		}

		final public void getNext() {
			if (gotNext)
				return;
			mask.next();
			mask.localize(position);
			channel1.setPosition(position);
			channel2.setPosition(position);
			gotNext = true;
		}

		@Override
		final public T getFirst() {
			getNext();
			return channel1.get();
		}

		@Override
		final public T getSecond() {
			getNext();
			return channel2.get();
		}

		@Override
		public void reset() {
			gotNext = false;
			mask.reset();
		}

		@Override
		public void fwd() {
			if (hasNext())
				getNext();
		}

		@Override
		public void jumpFwd(long arg0) {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public T next() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public double getDoublePosition(int arg0) {
			return mask.getDoublePosition(arg0);
		}

		@Override
		public float getFloatPosition(int arg0) {
			return mask.getFloatPosition(arg0);
		}

		@Override
		public void localize(float[] arg0) {
			mask.localize(arg0);
		}

		@Override
		public void localize(double[] arg0) {
			mask.localize(arg0);
		}

		@Override
		public int numDimensions() {
			return mask.numDimensions();
		}

		@Override
		public Sampler<T> copy() {
			throw new UnsupportedOperationException("This method has not been implemented, yet."); 
		}

		@Override
		public T get() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public int getIntPosition(int arg0) {
			return mask.getIntPosition(arg0);
		}

		@Override
		public long getLongPosition(int arg0) {
			return mask.getLongPosition(arg0);
		}

		@Override
		public void localize(int[] arg0) {
			mask.localize(arg0);
		}

		@Override
		public void localize(long[] arg0) {
			mask.localize(arg0);
		}

		@Override
		public Cursor<T> copyCursor() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}
	}
