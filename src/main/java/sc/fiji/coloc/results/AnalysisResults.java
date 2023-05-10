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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;

import sc.fiji.coloc.algorithms.Histogram2D;

/**
 * Data structure housing all colocalisation results. Intended for programmatic
 * access via API calls.
 *
 * @author Curtis Rueden
 */
public class AnalysisResults<T extends RealType<T>> implements
	ResultHandler<T>
{

	/** Result images, no matter what specific kinds. */
	private final List<NamedContainer<RandomAccessibleInterval<? extends RealType<?>>>> listOfImages =
		new ArrayList<>();

	/** Histogram results. */
	private final Map<RandomAccessibleInterval<LongType>, Histogram2D<T>> mapOf2DHistograms =
		new HashMap<>();

	/** Warnings produced during analysis. */
	private final List<Warning> warnings = new ArrayList<>();

	/** Named values, collected from algorithms. */
	private final List<ValueResult> valueResults = new ArrayList<>();

	/**
	 * Images and corresponding LUTs. When an image is not in there no LUT should
	 * be applied.
	 */
	private final Map<Object, String> listOfLUTs = new HashMap<>();

	// -- AllTheData methods --

	public List<NamedContainer<RandomAccessibleInterval<? extends RealType<?>>>>
		images()
	{
		return listOfImages;
	}

	public Map<RandomAccessibleInterval<LongType>, Histogram2D<T>> histograms() {
		return mapOf2DHistograms;
	}

	public List<Warning> warnings() {
		return warnings;
	}

	public List<ValueResult> values() {
		return valueResults;
	}

	// -- ResultHandler methods --

	@Override
	public void handleImage(final RandomAccessibleInterval<T> image,
		final String name)
	{
		listOfImages.add(
			new NamedContainer<RandomAccessibleInterval<? extends RealType<?>>>(image,
				name));
	}

	@Override
	public void handleHistogram(final Histogram2D<T> histogram,
		final String name)
	{
		listOfImages.add(
			new NamedContainer<RandomAccessibleInterval<? extends RealType<?>>>(
				histogram.getPlotImage(), name));
		mapOf2DHistograms.put(histogram.getPlotImage(), histogram);
		// link the histogram to a LUT
		listOfLUTs.put(histogram.getPlotImage(), "Fire");
	}

	@Override
	public void handleWarning(final Warning warning) {
		warnings.add(warning);
	}

	@Override
	public void handleValue(final String name, final String value) {
		valueResults.add(new ValueResult(name, value));
	}

	@Override
	public void handleValue(final String name, final double value) {
		handleValue(name, value, 3);
	}

	@Override
	public void handleValue(final String name, final double value,
		final int decimals)
	{
		valueResults.add(new ValueResult(name, value, decimals));
	}

	@Override
	public void process() {
		// NB: No action needed.
	}
}
