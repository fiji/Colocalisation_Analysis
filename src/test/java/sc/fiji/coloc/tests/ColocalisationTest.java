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
package sc.fiji.coloc.tests;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.math.ImageStatistics;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.After;
import org.junit.Before;

import sc.fiji.coloc.gadgets.MaskFactory;


public abstract class ColocalisationTest {

	// images and meta data for zero correlation
	RandomAccessibleInterval<UnsignedByteType> zeroCorrelationImageCh1;
	RandomAccessibleInterval<UnsignedByteType> zeroCorrelationImageCh2;
	RandomAccessibleInterval<BitType> zeroCorrelationAlwaysTrueMask;
	double zeroCorrelationImageCh1Mean;
	double zeroCorrelationImageCh2Mean;

	// images and meta data for positive correlation test
	// and real noisy image Manders' coeff with mask test
	RandomAccessibleInterval<UnsignedByteType> positiveCorrelationImageCh1;
	RandomAccessibleInterval<UnsignedByteType> positiveCorrelationImageCh2;
	//  open mask image as a bit type cursor
	Img<UnsignedByteType> positiveCorrelationMaskImage;
	RandomAccessibleInterval<BitType> positiveCorrelationAlwaysTrueMask;
	double positiveCorrelationImageCh1Mean;
	double positiveCorrelationImageCh2Mean;

	// images and meta data for a synthetic negative correlation dataset
	RandomAccessibleInterval<UnsignedByteType> syntheticNegativeCorrelationImageCh1;
	RandomAccessibleInterval<UnsignedByteType> syntheticNegativeCorrelationImageCh2;
	RandomAccessibleInterval<BitType> syntheticNegativeCorrelationAlwaysTrueMask;
	double syntheticNegativeCorrelationImageCh1Mean;
	double syntheticNegativeCorrelationImageCh2Mean;
	
	// images like in the Manders paper
	RandomAccessibleInterval<UnsignedByteType> mandersA, mandersB, mandersC, mandersD,
		mandersE, mandersF, mandersG, mandersH, mandersI;
	RandomAccessibleInterval<BitType> mandersAlwaysTrueMask;

	/**
	 * This method is run before every single test is run and is meant to set up
	 * the images and meta data needed for testing image colocalisation.
	 */
	@Before
	public void setup() {
		zeroCorrelationImageCh1 = TestImageAccessor.loadTiffFromJar("/greenZstack.tif");
		zeroCorrelationImageCh1Mean = ImageStatistics.getImageMean(zeroCorrelationImageCh1);

		zeroCorrelationImageCh2 = TestImageAccessor.loadTiffFromJar("/redZstack.tif");
		zeroCorrelationImageCh2Mean = ImageStatistics.getImageMean(zeroCorrelationImageCh2);

		final long[] dimZeroCorrCh1 = new long[ zeroCorrelationImageCh1.numDimensions() ];
		zeroCorrelationImageCh1.dimensions(dimZeroCorrCh1);
		zeroCorrelationAlwaysTrueMask = MaskFactory.createMask(dimZeroCorrCh1, true);

		positiveCorrelationImageCh1 = TestImageAccessor.loadTiffFromJar("/colocsample1b-green.tif");
		positiveCorrelationImageCh1Mean = ImageStatistics.getImageMean(positiveCorrelationImageCh1);

		positiveCorrelationImageCh2 = TestImageAccessor.loadTiffFromJar("/colocsample1b-red.tif");
		positiveCorrelationImageCh2Mean = ImageStatistics.getImageMean(positiveCorrelationImageCh2);
		
		positiveCorrelationMaskImage = TestImageAccessor.loadTiffFromJarAsImg("/colocsample1b-mask.tif");

		final long[] dimPosCorrCh1 = new long[ positiveCorrelationImageCh1.numDimensions() ];
		positiveCorrelationImageCh1.dimensions(dimPosCorrCh1);
		positiveCorrelationAlwaysTrueMask = MaskFactory.createMask(dimPosCorrCh1, true);

		syntheticNegativeCorrelationImageCh1 = TestImageAccessor.loadTiffFromJar("/syntheticNegCh1.tif");
		syntheticNegativeCorrelationImageCh1Mean = ImageStatistics.getImageMean(syntheticNegativeCorrelationImageCh1);

		syntheticNegativeCorrelationImageCh2 = TestImageAccessor.loadTiffFromJar("/syntheticNegCh2.tif");
		syntheticNegativeCorrelationImageCh2Mean = ImageStatistics.getImageMean(syntheticNegativeCorrelationImageCh2);

		final long[] dimSynthNegCorrCh1 = new long[ syntheticNegativeCorrelationImageCh1.numDimensions() ];
		syntheticNegativeCorrelationImageCh1.dimensions(dimSynthNegCorrCh1);
		syntheticNegativeCorrelationAlwaysTrueMask = MaskFactory.createMask(dimSynthNegCorrCh1, true);
		
		mandersA = TestImageAccessor.loadTiffFromJar("/mandersA.tiff");
		mandersB = TestImageAccessor.loadTiffFromJar("/mandersB.tiff");
		mandersC = TestImageAccessor.loadTiffFromJar("/mandersC.tiff");
		mandersD = TestImageAccessor.loadTiffFromJar("/mandersD.tiff");
		mandersE = TestImageAccessor.loadTiffFromJar("/mandersE.tiff");
		mandersF = TestImageAccessor.loadTiffFromJar("/mandersF.tiff");
		mandersG = TestImageAccessor.loadTiffFromJar("/mandersG.tiff");
		mandersH = TestImageAccessor.loadTiffFromJar("/mandersH.tiff");
		mandersI = TestImageAccessor.loadTiffFromJar("/mandersI.tiff");

		final long[] dimMandersA = new long[ mandersA.numDimensions() ];
		mandersA.dimensions(dimMandersA);
		mandersAlwaysTrueMask = MaskFactory.createMask(dimMandersA, true);
	}

	/**
	 * This method is run after every single test and is meant to clean up.
	 */
	@After
	public void cleanup() {
		// nothing to do
	}

	/**
	 * Creates a ROI offset array with a distance of 1/4 to the origin
	 * in each dimension.
	 */
	protected <T extends RealType<T>> long[] createRoiOffset(RandomAccessibleInterval<T> img) {
		final long[] offset = new long[ img.numDimensions() ];
		img.dimensions(offset);
		for (int i=0; i<offset.length; i++) {
			offset[i] = Math.max(1, img.dimension(i) / 4);
		}
		return offset;
	}

	/**
	 * Creates a ROI size array with a size of 1/2 of each
	 * dimension.
	 */
	protected <T extends RealType<T>> long[] createRoiSize(RandomAccessibleInterval<T> img) {
		final long[] size = new long[ img.numDimensions() ];
		img.dimensions(size);
		for (int i=0; i<size.length; i++) {
			size[i] = Math.max(1, img.dimension(i) / 2);
		}
		return size;
	}
}
