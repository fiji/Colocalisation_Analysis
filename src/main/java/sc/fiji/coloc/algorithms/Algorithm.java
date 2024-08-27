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
package sc.fiji.coloc.algorithms;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.type.numeric.RealType;

import sc.fiji.coloc.gadgets.DataContainer;
import sc.fiji.coloc.results.ResultHandler;
import sc.fiji.coloc.results.Warning;

/**
 * An algorithm is an abstraction of techniques like the
 * calculation of the Persons coefficient or Li'S ICQ. It
 * allows to separate initialization and execution of
 * such an algorithm.
 */
public abstract class Algorithm<T extends RealType< T >> {
	// a name for the algorithm
	protected String name;
	/* a list of warnings that can be filled by the
	 *  execute method
	 */
	List<Warning> warnings = new ArrayList<Warning>();

	public Algorithm(String name) {
		this.name = name;
	}

	/**
	 * Executes the previously initialized {@link Algorithm}.
	 */
	public abstract void execute(DataContainer<T> container) throws MissingPreconditionException;

	public String getName() {
		return name;
	}

	/**
	 * A method to give the algorithm the opportunity to let
	 * its results being processed by the passed handler.
	 * By default this methods passes the collected warnings to
	 * the handler and sub-classes should make use of this by
	 * adding custom behavior and call the super class.
	 *
	 * @param handler The ResultHandler to process the results.
	 */
	public void processResults(ResultHandler<T> handler) {
		for (Warning w : warnings)
			handler.handleWarning( w );
	}

	/**
	 * Gets a reference to the warnings.
	 *
	 * @return A reference to the warnings list
	 */
	public List<Warning> getWarnings() {
		return warnings;
	}

	/**
	 * Adds a warning to the list of warnings.
	 *
	 * @param shortMsg A short descriptive message
	 * @param longMsg A long message
	 */
	protected void addWarning(String shortMsg, String longMsg) {
		warnings.add( new Warning(shortMsg, longMsg) );
	}
}
