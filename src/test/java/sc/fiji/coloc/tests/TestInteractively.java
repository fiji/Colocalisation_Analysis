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

package sc.fiji.coloc.tests;

import fiji.Debug;

public class TestInteractively {

	public static void main(String[] args) {
		// dummy call: initialize debug session
		Debug.run("Close All", "");
		final String image1 = "colocsample1b-green.tif";
		final String image2 = "colocsample1b-red.tif";
		String testsDataDir = System.getProperty("plugins.dir") + "/../src/test/resources/";
		Debug.run("Open...", "open=" + testsDataDir + image1);
		Debug.run("Open...", "open=" + testsDataDir + image2);
		Debug.run("Coloc 2",
			"channel_1=" + image1 + " channel_2=" + image2 + " roi_or_mask=<None> "
			+ "li_histogram_channel_1 "
			+ "li_histogram_channel_2 "
			+ "li_icq "
			+ "spearman's_rank_correlation "
			+ "manders'_correlation "
			+ "kendall's_tau_rank_correlation "
			+ "2d_instensity_histogram "
			+ "costes'_significance_test "
			+ "psf=3 "
			+ "costes_randomisations=10"
			);
		}
}
