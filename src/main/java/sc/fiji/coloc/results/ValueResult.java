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
package sc.fiji.coloc.results;

/**
 * A small structure to keep decimal places information
 * with numbers along with a name or a simple named text.
 */
public class ValueResult {
	public String name;
	public double number;
	public int decimals;
	public String value;
	public boolean isNumber;

	public ValueResult( String name, double number, int decimals ) {
		this.name = name;
		this.number = number;
		this.decimals = decimals;
		this.isNumber = true;
	}

	public ValueResult( String name, String value) {
		this.name = name;
		this.value = value;
		this.isNumber = false;
	}
}
