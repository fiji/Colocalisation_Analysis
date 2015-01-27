package tests;

import static org.junit.Assert.assertEquals;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.junit.Test;

import algorithms.LiICQ;
import algorithms.MandersColocalization;
import algorithms.MandersColocalization.MandersResults;
import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;
import algorithms.SpearmanRankCorrelation;

public class CommutativityTest extends ColocalisationTest {

	/**
	 * This test makes sure the test images A and B lead to the same results,
	 * regardless whether they are added in the order A, B or B, A to the data
	 * container.
	 *
	 * @throws MissingPreconditionException
	 */
	@Test
	public void cummutativityTest() throws MissingPreconditionException {
		assertCommutativity(zeroCorrelationImageCh1, zeroCorrelationImageCh2,
				zeroCorrelationAlwaysTrueMask, zeroCorrelationImageCh1Mean,
				zeroCorrelationImageCh2Mean);
		assertCommutativity(positiveCorrelationImageCh1, positiveCorrelationImageCh1,
				positiveCorrelationAlwaysTrueMask, positiveCorrelationImageCh1Mean,
				positiveCorrelationImageCh2Mean);
		assertCommutativity(syntheticNegativeCorrelationImageCh1,
				syntheticNegativeCorrelationImageCh2,
				syntheticNegativeCorrelationAlwaysTrueMask,
				syntheticNegativeCorrelationImageCh1Mean,
				syntheticNegativeCorrelationImageCh2Mean);
	}
	
	protected static <T extends RealType< T > > void assertCommutativity(
			RandomAccessibleInterval<T> ch1, RandomAccessibleInterval<T> ch2,
			RandomAccessibleInterval<BitType> mask, double mean1, double mean2)
			throws MissingPreconditionException
	{
		// create a twin value range cursor that iterates over all pixels of the input data
		TwinCursor<T> cursor1 = new TwinCursor<T>(ch1.randomAccess(),
				ch2.randomAccess(), Views.iterable(mask).localizingCursor());
		TwinCursor<T> cursor2 = new TwinCursor<T>(ch1.randomAccess(),
				ch2.randomAccess(), Views.iterable(mask).localizingCursor());
		
		// get the Pearson's values
		double pearsonsR1 = PearsonsCorrelation.fastPearsons(cursor1);
		double pearsonsR2 = PearsonsCorrelation.fastPearsons(cursor2);
		// check Pearsons R is the same
		assertEquals(pearsonsR1, pearsonsR2, 0.0001);
		
		// get Li's ICQ values
		double icq1 = LiICQ.calculateLisICQ(cursor1, mean1, mean2);
		double icq2 = LiICQ.calculateLisICQ(cursor2, mean2, mean1);
		// check Li's ICQ is the same
		assertEquals(icq1, icq2, 0.0001);
		
		// get Manders values
		MandersColocalization<T> mc = new MandersColocalization<T>();
		MandersResults manders1 = mc.calculateMandersCorrelation(cursor1,
				ch1.randomAccess().get().createVariable());
		MandersResults manders2 = mc.calculateMandersCorrelation(cursor2,
				ch2.randomAccess().get().createVariable());
		// check Manders m1 and m2 values are the same
		assertEquals(manders1.m1, manders2.m2, 0.0001);
		assertEquals(manders1.m2, manders2.m1, 0.0001);
		
		// calculate Spearman's Rank rho values
		double rho1 = SpearmanRankCorrelation.calculateSpearmanRank(cursor1);
		double rho2 = SpearmanRankCorrelation.calculateSpearmanRank(cursor2);
		// make sure both ranks are the same
		assertEquals(rho1, rho2, 0.0001);
	}
}
