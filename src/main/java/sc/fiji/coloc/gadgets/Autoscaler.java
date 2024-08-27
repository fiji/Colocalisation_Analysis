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
package sc.fiji.coloc.gadgets;

import ij.process.ImageProcessor;

/** Utility class for autoscaling image displays. */
public final class Autoscaler {

	private Autoscaler() { }

	public static void autoscale(ImageProcessor ip) {
		// Find the minimum and maximum bounds of an image,
		// excluding infinities and NaNs.
		float min = Float.POSITIVE_INFINITY;
		float max = Float.NEGATIVE_INFINITY;
		for (int y = 0; y < ip.getHeight(); y++) {
			for (int x = 0; x < ip.getWidth(); x++) {
				float pix = ip.getPixelValue(x, y);
				if (!Float.isFinite(pix)) continue;
				if (pix < min) min = pix;
				if (pix > max) max = pix;
			}
		}
		ip.setMinAndMax(min, max);
	}
}
