
package results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;

import algorithms.Histogram2D;

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
