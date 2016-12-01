
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.ChannelSplitter;

/**
 * Test class for Coloc 2 functionality.
 * 
 * @author Ellen T Arena
 */
public class Main {
	

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Coloc_2.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the FluorescentCells sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/FluorescentCells.zip");
		ImagePlus[] channels = ChannelSplitter.split(image);
		channels[0].show();
		channels[1].show();

		// run the plugin, Coloc 2
		IJ.run("Coloc 2","channel_1=C1-FluorescentCells.tif channel_2=C2-FluorescentCells.tif roi_or_mask=<None> threshold_regression=Costes li_histogram_channel_1 li_histogram_channel_2 li_icq spearman's_rank_correlation manders'_correlation kendall's_tau_rank_correlation 2d_instensity_histogram costes'_significance_test psf=3 costes_randomisations=10");
	}
}
