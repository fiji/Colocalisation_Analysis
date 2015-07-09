
package tests;

import static org.junit.Assert.assertEquals;
import algorithms.MissingPreconditionException;
import algorithms.PercentOverlapThresholdedPixelsAndIntensity;
import gadgets.MaskFactory;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;


/**
 * These are JUnit 4 test cases for the algorithm that calculates Percent Overlap
 * in terms of pixels and intensity above threshold 
 * (from the point of view of each channel with respect to the other)
 * for both of the two channels.  
 * 
 * @author Dan White, GE Healthcare
 */


public class PercentOverlapThresholdedPixelsAndIntensityTest extends ColocalisationTest {

	/**
	 * Set of tests for if the % overlap of pixels and intensity 
	 * (from the point of view of each channel with respect to the other)
	 * for the Manders test images are close enough to correct, 
	 * compared to what the depreciated Colocalisation Threshold plugin calculates 
	 */
	
	@Test
	public void test() throws MissingPreconditionException {
		fail("Not yet implemented");
	}

}
