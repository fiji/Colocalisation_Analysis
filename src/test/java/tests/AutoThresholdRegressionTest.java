package tests;

import static org.junit.Assert.assertEquals;
import gadgets.DataContainer;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.Test;

import com.sun.jdi.FloatType;

import algorithms.AutoThresholdRegression;
import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;


public class AutoThresholdRegressionTest extends ColocalisationTest {

	@Test
	public void clampHelperTest() throws MissingPreconditionException {
		assertEquals(4, AutoThresholdRegression.clamp(5, 1, 4), 0.00001);
		assertEquals(1, AutoThresholdRegression.clamp(-2, 1, 4), 0.00001);
		assertEquals(1, AutoThresholdRegression.clamp(5, 1, 1), 0.00001);
		assertEquals(2, AutoThresholdRegression.clamp(2, -1, 3), 0.00001);
	}

	/**
	 * This test makes sure the test images A and B lead to the same thresholds,
	 * regardless whether they are added in the order A, B or B, A to the data
	 * container.
	 *
	 * @throws MissingPreconditionException
	 */
	@Test
	public void cummutativityTest() throws MissingPreconditionException {
		PearsonsCorrelation<UnsignedByteType> pc1 =
				new PearsonsCorrelation<UnsignedByteType>(
						PearsonsCorrelation.Implementation.Fast);
		PearsonsCorrelation<UnsignedByteType> pc2 =
				new PearsonsCorrelation<UnsignedByteType>(
						PearsonsCorrelation.Implementation.Fast);

		AutoThresholdRegression<UnsignedByteType> atr1 =
				new AutoThresholdRegression<UnsignedByteType>(pc1);
		AutoThresholdRegression<UnsignedByteType> atr2 =
				new AutoThresholdRegression<UnsignedByteType>(pc2);

		RandomAccessibleInterval<UnsignedByteType> img1 = syntheticNegativeCorrelationImageCh1;
		RandomAccessibleInterval<UnsignedByteType> img2 = syntheticNegativeCorrelationImageCh2;

		DataContainer<UnsignedByteType> container1 =
				new DataContainer<UnsignedByteType>(img1, img2, 1, 1,
						"Channel 1", "Channel 2");

		DataContainer<UnsignedByteType> container2 =
				new DataContainer<UnsignedByteType>(img2, img1, 1, 1,
						"Channel 2", "Channel 1");

		atr1.execute(container1);
		atr2.execute(container2);

		assertEquals(atr1.getCh1MinThreshold(), atr2.getCh2MinThreshold());
		assertEquals(atr1.getCh1MaxThreshold(), atr2.getCh2MaxThreshold());
		assertEquals(atr1.getCh2MinThreshold(), atr2.getCh1MinThreshold());
		assertEquals(atr1.getCh2MaxThreshold(), atr2.getCh1MaxThreshold());
	}
}