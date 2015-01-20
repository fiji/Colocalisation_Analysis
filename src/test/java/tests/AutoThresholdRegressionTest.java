package tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import algorithms.AutoThresholdRegression;
import algorithms.MissingPreconditionException;


public class AutoThresholdRegressionTest extends ColocalisationTest {

	@Test
	public void clampHelperTest() throws MissingPreconditionException {
		assertEquals(4, AutoThresholdRegression.clamp(5, 1, 4), 0.00001);
		assertEquals(1, AutoThresholdRegression.clamp(-2, 1, 4), 0.00001);
		assertEquals(1, AutoThresholdRegression.clamp(5, 1, 1), 0.00001);
		assertEquals(2, AutoThresholdRegression.clamp(2, -1, 3), 0.00001);
	}
}