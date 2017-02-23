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
// This macro uses the Process>Math>Macro command
// to create a synthetic image.
// The formula that generates the image comes from
// MathMacroDemo macro from imageJ example macros page
// http://rsbweb.nih.gov/ij/macros/examples/MathMacroDemo.txt
// The image is a sin wave in x and a cos wave in y,
// giving a regular grid of blobs that look a bit like
// diffraction limited images of sub resolution objects

// get a new image to write into - it is 32 bit for high precision
run("Hyperstack...", "title=sine type=32-bit display=Grayscale width=1000 height=1000 channels=1 slices=1 frames=1");

// Macro... is the Process>Math>Macro command
// for making images from expresions.
// Here is the string expression for making the image
eqn = "v = (254/2) + ( (254/2) * (sin(0.4*(x+1)) * sin(0.4*(y+1)) ) )";
// why cant i just do:
//"code=[Eqn]"
//string concatenation or something i guess?

//Now run the Process>Math>Macro command with the expression
run("Macro...", "code=["+eqn+"]");

//
setMinAndMax(0.0, 255.0);
run("8-bit");
