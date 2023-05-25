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
