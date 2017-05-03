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
package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.imglib2.TwinCursor;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

import org.junit.Test;

import algorithms.MissingPreconditionException;
import algorithms.SpearmanRankCorrelation;

/**
 * This class contains JUnit 4 test cases for the calculation of
 * Spearman's Rank Correlation (rho).
 *
 * @author Leonardo Guizzetti
 */
public class SpearmanRankTest extends ColocalisationTest {

	/**
	 * Checks Spearman's Rank Correlation rho for positive correlated images.
	 */
	@Test
	public void spearmanPositiveCorrTest() throws MissingPreconditionException {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				positiveCorrelationImageCh1.randomAccess(),
				positiveCorrelationImageCh2.randomAccess(),
				Views.iterable(positiveCorrelationAlwaysTrueMask).localizingCursor());
		// calculate Spearman's Rank rho value
		double rho = new SpearmanRankCorrelation().calculateSpearmanRank(cursor);
		// Rho value = 0.5463...
		assertTrue(rho > 0.546 && rho < 0.547);
	}

	/**
	 * Checks Spearman's Rank Correlation value for zero correlated images. The rho value
	 * should be about zero.
	 */
	@Test
	public void spearmanZeroCorrTest() throws MissingPreconditionException {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				zeroCorrelationImageCh1.randomAccess(),
				zeroCorrelationImageCh2.randomAccess(),
				Views.iterable(zeroCorrelationAlwaysTrueMask).localizingCursor());
		// calculate Spearman's Rank rho value
		double rho = new SpearmanRankCorrelation().calculateSpearmanRank(cursor);
		// Rho value = -0.11...
		assertTrue(Math.abs(rho) < 0.012);
	}
	
	/**
	 * Checks Spearman's Rank Correlation value for slightly negative correlated synthetic data.
	 * 
	 */
	@Test
	public void statisticsTest() throws MissingPreconditionException {
		
		double[][] data = new double[][] {
			{1,113}, 
			{2,43},
			{3,11},
			{6,86},
			{5,59},
			{8,47},
			{4,92},
			{0,152},
			{6,23},
			{4,9},
			{7,33},
			{3,69},
			{2,75},
			{9,135},
			{3,30}
		 };
		int n = data.length;
		
		final SpearmanRankCorrelation src = new SpearmanRankCorrelation();

		/*
		 * Check the arithmetic for the rho calculation.
		 * Rho is exactly -0.1743 (to 4 decimal points) using the 
		 * exact calculation for Spearman's rho as implemented here.
		 */
		double rho = src.calculateSpearmanRank(data);
		assertEquals(-0.1743, rho, 0.001);
		
		// check the degrees of freedom calculation ( df = n - 2 )
		int df = 0;
		df = src.getSpearmanDF(n);
		assertEquals(df, n - 2);
		
		// check the t-statistic calculation ( t = rho * sqrt( df / (1-rho^2) ) )
		// The t-stat = -0.6382
		double tstat = 0.0;
		tstat = src.getTStatistic(rho, n);
		assertEquals(-0.6382, tstat, 0.001);
	}
	
	/**
	 * Checks Spearman's Rank Correlation value for synthetic test image.
	 * This tests the same dataset as the statisticsTest() but tests reading in image
	 * data, the rank transform, and the calling of the statistics calculation methods.
	 */
	@Test
	public void spearmanSyntheticNegCorrTest() throws MissingPreconditionException {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				syntheticNegativeCorrelationImageCh1.randomAccess(),
				syntheticNegativeCorrelationImageCh2.randomAccess(),
				Views.iterable(syntheticNegativeCorrelationAlwaysTrueMask).localizingCursor());
		
		// calculate Spearman's Rank rho value
		double rho = new SpearmanRankCorrelation().calculateSpearmanRank(cursor);
		assertTrue((rho > -0.178) && (rho < -0.173));
	}
	
}
