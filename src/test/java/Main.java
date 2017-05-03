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

		// open the FluorescentCells sample (to test single slice images)
		ImagePlus fluorCellImage = IJ.openImage("http://imagej.net/images/FluorescentCells.zip");
		ImagePlus[] fluorCellchannels = ChannelSplitter.split(fluorCellImage);
		fluorCellchannels[0].show();
		fluorCellchannels[1].show();
		// run the plugin, Coloc 2
		IJ.run("Coloc 2","channel_1=C1-FluorescentCells.tif channel_2=C2-FluorescentCells.tif roi_or_mask=<None> threshold_regression=Costes display_images_in_result li_histogram_channel_1 li_histogram_channel_2 li_icq spearman's_rank_correlation manders'_correlation kendall's_tau_rank_correlation 2d_instensity_histogram costes'_significance_test psf=3 costes_randomisations=10");

//		// open the Confocal Series sample (to test z-stacks)
//		ImagePlus confocalImage = IJ.openImage("http://imagej.net/images/confocal-series.zip");
//		ImagePlus[] confocalchannels = ChannelSplitter.split(confocalImage);
//		confocalchannels[0].show();
//		confocalchannels[1].show();
//		// run the plugin, Coloc 2
//		IJ.run("Coloc 2", "channel_1=C1-confocal-series.tif channel_2=C2-confocal-series.tif roi_or_mask=<None> threshold_regression=Costes display_images_in_result li_histogram_channel_1 li_histogram_channel_2 li_icq spearman's_rank_correlation manders'_correlation kendall's_tau_rank_correlation 2d_instensity_histogram costes'_significance_test psf=3 costes_randomisations=10");
	}
}
