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
// This macro draws randomly positioned dots on the active
// image in the current foreground color, then gaussian smooths them.

dotSize = 3;
numberOfDots = 5000;
  width = getWidth();
  height = getHeight();
  for (i=0; i<numberOfDots; i++) {
      x = random()*width-dotSize/2;
      y = random()*height-dotSize/2;
      makeOval(x, y, dotSize, dotSize);
      run("Fill");
      run("Select None");
   }
   run("Gaussian Blur...", "sigma=1");
