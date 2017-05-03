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

import ij.IJ;
import ij.ImagePlus;
import ij.text.TextWindow;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.math.ImageStatistics;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;

import sc.fiji.coloc.algorithms.Histogram2D;
import sc.fiji.coloc.gadgets.DataContainer;

public class EasyDisplay<T extends RealType<T>> implements ResultHandler<T> {
	// the text window to present value and text results
	protected static TextWindow textWindow;
	/* the data container with general information about the
	 * source images that were processed by the algorithms.
	 */
	protected DataContainer<T> container;

	public EasyDisplay(DataContainer<T> container) {
		final int twWidth = 170;
		final int twHeight = 250;
		//test if the results windows is already there, if so use it.
		if (textWindow == null || !textWindow.isVisible())
			textWindow = new TextWindow("Results",
				"Result\tValue\n", "", twWidth, twHeight);
		else {
			// set dimensions
			textWindow.setSize(twWidth, twHeight);
		}
		// deactivate the window for now
		textWindow.setVisible(false);
		// save a reference to the data container
		this.container = container;
	}

	@Override
	public void handleImage(RandomAccessibleInterval<T> image, String name) {
		ImagePlus imp = ImageJFunctions.wrapFloat( image, name );
		double max = ImageStatistics.getImageMax( image ).getRealDouble();
		showImage( imp, max );
	}

	@Override
	public void handleHistogram(Histogram2D<T> histogram, String name) {
		ImagePlus imp = ImageJFunctions.wrapFloat( histogram.getPlotImage(), name );
		double max = ImageStatistics.getImageMax( histogram.getPlotImage() ).getRealDouble();
		showImage( imp, max );
	}

	protected void showImage(ImagePlus imp, double max) {
		// set the display range
		imp.setDisplayRange(0.0, max);
		imp.show();
	}

	@Override
	public void handleWarning(Warning warning) {
		// no warnings are shown in easy display
	}

	@Override
	public void handleValue(String name, String value) {
		textWindow.getTextPanel().appendLine(name + "\t"
				+ value + "\n");
	}

	@Override
	public void handleValue(String name, double value) {
		handleValue(name, value, 3);
	}

	@Override
	public void handleValue(String name, double value, int decimals) {
		handleValue(name, IJ.d2s(value, decimals));
	}

	protected void printTextStatistics(DataContainer<T> container){
		textWindow.getTextPanel().appendLine("Ch1 Mean\t" + container.getMeanCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Mean\t" + container.getMeanCh2() + "\n");
		textWindow.getTextPanel().appendLine("Ch1 Min\t" + container.getMinCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Min\t" + container.getMinCh2() + "\n");
		textWindow.getTextPanel().appendLine("Ch1 Max\t" + container.getMaxCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Max\t" + container.getMaxCh2() + "\n");
	}

	@Override
	public void process() {
		// print some general information about images
		printTextStatistics(container);
	    // show the results
		textWindow.setVisible(true);
		IJ.selectWindow("Results");
	}
}
