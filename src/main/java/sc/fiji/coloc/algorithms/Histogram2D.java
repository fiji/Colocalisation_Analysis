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

import ij.measure.ResultsTable;

import java.util.EnumSet;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.view.Views;

import sc.fiji.coloc.gadgets.DataContainer;
import sc.fiji.coloc.results.ResultHandler;

/**
 * Represents the creation of a 2D histogram between two images.
 * Channel 1 is set out in x direction, while channel 2 in y direction.
 * @param <T> The source images value type
 */
public class Histogram2D<T extends RealType< T >> extends Algorithm<T> {
	// An enumeration of possible drawings
	public enum DrawingFlags { Plot, RegressionLine, Axes }
	// the drawing configuration
	EnumSet<DrawingFlags> drawingSettings;

	// The width of the scatter-plot
	protected int xBins = 256;
	// The height of the scatter-plot
	protected int yBins = 256;
	// The name of the result 2D histogram to pass elsewhere
	protected String title = "";
	// Swap or not swap ch1 and ch2
	protected boolean swapChannels = false;
	// member variables for labeling
	protected String ch1Label = "Channel 1";
	protected String ch2Label = "Channel 2";

	// Result keeping members

	// the generated plot image
	private RandomAccessibleInterval<LongType> plotImage;
	// the bin widths for each channel
	private double xBinWidth = 0.0, yBinWidth = 0.0;
	// labels for the axes
	private String xLabel = "", yLabel = "";
	// ranges for the axes
	private double xMin = 0.0, xMax = 0.0, yMin = 0.0, yMax = 0.0;


	public Histogram2D(){
		this("2D Histogram");
	}

	public Histogram2D(String title){
		this(title, false);
	}

	public Histogram2D(String title, boolean swapChannels){
		this(title, swapChannels, EnumSet.of( DrawingFlags.Plot, DrawingFlags.RegressionLine ));
	}

	public Histogram2D(String title, boolean swapChannels, EnumSet<DrawingFlags> drawingSettings){
		super(title);
		this.title = title;
		this.swapChannels = swapChannels;

		if (swapChannels) {
			int xBins = this.xBins;
			this.xBins = this.yBins;
			this.yBins = xBins;
		}

		this.drawingSettings = drawingSettings;
	}

	/**
	 * Gets the minimum of channel one. Takes channel
	 * swapping into consideration and will return min
	 * of channel two if swapped.
	 *
	 * @return The minimum of what is seen as channel one.
	 */
	protected double getMinCh1(DataContainer<T> container) {
		return swapChannels ? container.getMinCh2() : container.getMinCh1();
	}

	/**
	 * Gets the minimum of channel two. Takes channel
	 * swapping into consideration and will return min
	 * of channel one if swapped.
	 *
	 * @return The minimum of what is seen as channel two.
	 */
	protected double getMinCh2(DataContainer<T> container) {
		return swapChannels ? container.getMinCh1() : container.getMinCh2();
	}

	/**
	 * Gets the maximum of channel one. Takes channel
	 * swapping into consideration and will return max
	 * of channel two if swapped.
	 *
	 * @return The maximum of what is seen as channel one.
	 */
	protected double getMaxCh1(DataContainer<T> container) {
		return swapChannels ? container.getMaxCh2() : container.getMaxCh1();
	}

	/**
	 * Gets the maximum of channel two. Takes channel
	 * swapping into consideration and will return max
	 * of channel one if swapped.
	 *
	 * @return The maximum of what is seen as channel two.
	 */
	protected double getMaxCh2(DataContainer<T> container) {
		return swapChannels ? container.getMaxCh1() : container.getMaxCh2();
	}

	/**
	 * Gets the image of channel one. Takes channel
	 * swapping into consideration and will return image
	 * of channel two if swapped.
	 *
	 * @return The image of what is seen as channel one.
	 */
	protected RandomAccessibleInterval<T> getImageCh1(DataContainer<T> container) {
		return swapChannels ? container.getSourceImage2() : container.getSourceImage1();
	}

	/**
	 * Gets the image of channel two. Takes channel
	 * swapping into consideration and will return image
	 * of channel one if swapped.
	 *
	 * @return The image of what is seen as channel two.
	 */
	protected RandomAccessibleInterval<T> getImageCh2(DataContainer<T> container) {
		return swapChannels ? container.getSourceImage1() : container.getSourceImage2();
	}

	/**
	 * Gets the label of channel one. Takes channel
	 * swapping into consideration and will return label
	 * of channel two if swapped.
	 *
	 * @return The label of what is seen as channel one.
	 */
	protected String getLabelCh1() {
		return swapChannels ? ch2Label : ch1Label;
	}

	/**
	 * Gets the label of channel two. Takes channel
	 * swapping into consideration and will return label
	 * of channel one if swapped.
	 *
	 * @return The label of what is seen as channel two.
	 */
	protected String getLabelCh2() {
		return swapChannels ? ch1Label : ch2Label;
	}

	@Override
	public void execute(DataContainer<T> container) throws MissingPreconditionException {
		generateHistogramData(container);
	}

	protected void generateHistogramData(DataContainer<T> container) {
		double ch1BinWidth = getXBinWidth(container);
		double ch2BinWidth = getYBinWidth(container);

		// get the 2 images for the calculation of Pearson's
		final RandomAccessibleInterval<T> img1 = getImageCh1(container);
		final RandomAccessibleInterval<T> img2 = getImageCh2(container);
		final RandomAccessibleInterval<BitType> mask = container.getMask();

		// get the cursors for iterating through pixels in images
		TwinCursor<T> cursor = new TwinCursor<T>(img1.randomAccess(),
				img2.randomAccess(), Views.iterable(mask).localizingCursor());

		// create new image to put the scatter-plot in
		final ImgFactory<LongType> scatterFactory = new ArrayImgFactory< LongType >();
		plotImage = scatterFactory.create(new int[] {xBins, yBins}, new LongType() );

		// create access cursors
		final RandomAccess<LongType> histogram2DCursor =
			plotImage.randomAccess();

		long ignoredPixelCount = 0;

		// iterate over images
		long[] pos = new long[ plotImage.numDimensions() ];
		while (cursor.hasNext()) {
			cursor.fwd();
			double ch1 = cursor.getFirst().getRealDouble();
			double ch2 = cursor.getSecond().getRealDouble();
			/* Scale values for both channels to fit in the range.
			 * Moreover mirror the y value on the x axis.
			 */
			pos[0] = getXValue(ch1, ch1BinWidth, ch2, ch2BinWidth);
			pos[1] = getYValue(ch1, ch1BinWidth, ch2, ch2BinWidth);

			if (pos[0] >= 0 && pos[1] >=0 && pos[0] < xBins && pos[1] < yBins) {
				// set position of input/output cursor
				histogram2DCursor.setPosition( pos );
				// get current value at position and increment it
				long count = histogram2DCursor.get().getIntegerLong();
				count++;

				histogram2DCursor.get().set(count);
			} else {
				ignoredPixelCount ++;
			}
		}

		if (ignoredPixelCount > 0) {
			addWarning("Ignored pixels while generating histogram.",
					"" + ignoredPixelCount + " pixels were ignored while generating the 2D histogram \"" + title +
							"\" because the grey values were out of range." +
							"This may happen, if an image contains negative pixel values.");
		}
		xBinWidth = ch1BinWidth;
		yBinWidth = ch2BinWidth;
		xLabel = getLabelCh1();
		yLabel = getLabelCh2();
		xMin = getXMin(container);
		xMax = getXMax(container);
		yMin = getYMin(container);
		yMax = getYMax(container);
	}

	/**
	 * A table of x-values, y-values and the counts is generated and
	 * returned as a string. The single fields in one row (X Y Count)
	 * are separated by tabs.
	 *
	 * @return A String representation of the histogram data.
	 */
	public String getData() {
		StringBuffer sb = new StringBuffer();

		double xBinWidth = 1.0 / getXBinWidth();
		double yBinWidth = 1.0 / getYBinWidth();
		double xMin = getXMin();
		double yMin = getYMin();
		// check if we have bins of size one or other ones
		boolean xBinWidthIsOne = Math.abs(xBinWidth - 1.0) < 0.00001;
		boolean yBinWidthIsOne = Math.abs(yBinWidth - 1.0) < 0.00001;
		// configure decimal places accordingly
		int xDecimalPlaces = xBinWidthIsOne ? 0 : 3;
		int yDecimalPlaces = yBinWidthIsOne ? 0 : 3;
		// create a cursor to access the histogram data
		RandomAccess<LongType> cursor = plotImage.randomAccess();
		// loop over 2D histogram
		for (int i=0; i < plotImage.dimension(0); ++i) {
			for (int j=0; j < plotImage.dimension(1); ++j) {
				cursor.setPosition(i, 0);
				cursor.setPosition(j, 1);
				sb.append(
						ResultsTable.d2s(xMin + (i * xBinWidth), xDecimalPlaces) + "\t" +
						ResultsTable.d2s(yMin + (j * yBinWidth), yDecimalPlaces) + "\t" +
						ResultsTable.d2s(cursor.get().getRealDouble(), 0) + "\n");
			}
		}

		return sb.toString();
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);

		handler.handleHistogram( this, title );
	}

	/**
	 * Calculates the bin width of one bin in x/ch1 direction.
	 * @param container The container with images to work on
	 * @return The width of one bin in x direction
	 */
	protected double getXBinWidth(DataContainer<T> container) {
		double ch1Max = getMaxCh1(container);
		if (ch1Max < yBins) {
			// bin widths must not exceed 1
			return 1;
		}
		// we need (ch1Max * width + 0.5) < yBins, but just so, i.e.
		// ch1Max * width + 0.5 == yBins - eps
		// width = (yBins - 0.5 - eps) / ch1Max
		return (yBins - 0.50001) / ch1Max;
	}

	/**
	 * Calculates the bin width of one bin in y/ch2 direction.
	 * @param container The container with images to work on
	 * @return The width of one bin in y direction
	 */
	protected double getYBinWidth(DataContainer<T> container) {
		double ch2Max = getMaxCh2(container);
		if (ch2Max < yBins) {
			// bin widths must not exceed 1
			return 1;
		}
		return (yBins - 0.50001) / ch2Max;
	}

	/**
	 * Calculates the locations x value.
	 * @param ch1Val The intensity of channel one
	 * @param ch1BinWidth The bin width for channel one
	 * @return The x value of the data point location
	 */
	protected int getXValue(double ch1Val, double ch1BinWidth, double ch2Val, double ch2BinWidth) {
		return (int)(ch1Val * ch1BinWidth + 0.5);
	}

	/**
	 * Calculates the locations y value.
	 * @param ch2Val The intensity of channel one
	 * @param ch2BinWidth The bin width for channel one
	 * @return The x value of the data point location
	 */
	protected int getYValue(double ch1Val, double ch1BinWidth, double ch2Val, double ch2BinWidth) {
		return (yBins - 1) - (int)(ch2Val * ch2BinWidth + 0.5);
	}

	protected double getXMin(DataContainer<T> container) {
		return 0;
	}

	protected double getXMax(DataContainer<T> container) {
		return swapChannels ? getMaxCh2(container) : getMaxCh1(container);
	}

	protected double getYMin(DataContainer<T> container) {
		return 0;
	}

	protected double getYMax(DataContainer<T> container) {
		return swapChannels ? getMaxCh1(container) : getMaxCh2(container);
	}

	// Result access methods

	public RandomAccessibleInterval<LongType> getPlotImage() {
		return plotImage;
	}

	public double getXBinWidth() {
		return xBinWidth;
	}

	public double getYBinWidth() {
		return yBinWidth;
	}

	public String getXLabel() {
		return xLabel;
	}

	public String getYLabel() {
		return yLabel;
	}

	public double getXMin() {
		return xMin;
	}

	public double getXMax() {
		return xMax;
	}

	public double getYMin() {
		return yMin;
	}

	public double getYMax() {
		return yMax;
	}

	public String getTitle() {
		return title;
	}

	public EnumSet<DrawingFlags> getDrawingSettings() {
		return drawingSettings;
	}
}
