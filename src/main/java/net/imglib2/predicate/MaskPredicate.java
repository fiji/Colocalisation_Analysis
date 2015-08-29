package net.imglib2.predicate;

import net.imglib2.Cursor;
import net.imglib2.type.logic.BitType;

public class MaskPredicate implements Predicate<BitType> {
	
	@Override
	public boolean test(final Cursor<BitType> cursor) {
		return cursor.get().get();
	}
}
