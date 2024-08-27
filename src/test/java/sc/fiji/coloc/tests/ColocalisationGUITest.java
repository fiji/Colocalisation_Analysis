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
package sc.fiji.coloc.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Window;
import java.awt.event.WindowEvent;

import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.ChannelSplitter;
import sc.fiji.coloc.Colocalisation_Test;

public class ColocalisationGUITest extends ColocalisationTest {
	@Test
	public void testThatWindowRefIsCleanedUpIfClosed() {		
		ImagePlus img = IJ.openImage("http://imagej.net/images/FluorescentCells.zip");
		ImagePlus[] splitImg = ChannelSplitter.split(img);
		Colocalisation_Test colocalisationTest = new Colocalisation_Test();
		
		// run module once, it'll open results textWindow
		colocalisationTest.correlate(splitImg[0], splitImg[1], splitImg[2]);
		
		// check textWindow is open, and close it
		Window resultsWindow = WindowManager.getWindow("Results");
		assertTrue(resultsWindow.isVisible());
		resultsWindow.dispatchEvent(new WindowEvent(resultsWindow, WindowEvent.WINDOW_CLOSING));
		assertFalse(resultsWindow.isVisible());
		
		// run module again
		colocalisationTest.correlate(splitImg[0], splitImg[1], splitImg[2]);
		
		// check textWindow is open, .correlate didnt crash!
		resultsWindow = WindowManager.getWindow("Results");
		assertTrue(resultsWindow.isVisible());
		
	} 
}
