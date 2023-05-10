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

/**
 * A class representing a warning, combining a short and
 * a long message. Typically Algorithms can produce such
 * warnings if they find problems with the input data.
 */
public class Warning
{
	private String shortMessage;
	private String longMessage;

	public Warning(String shortMessage, String longMessage)
	{
		this.shortMessage = shortMessage;
		this.longMessage = longMessage;
	}

	public String getShortMessage() {
		return shortMessage;
	}

	public String getLongMessage() {
		return longMessage;
	}

}
