package algorithms;

/**
 * The simple stepper decrements a start threshold with every update() call. It
 * is finished if the update value is not a number, below zero or larger than
 * the last value. It also stops if the decremented threshold falls below one.
 *
 * @author tom
 */
public class SimpleStepper extends Stepper {
	double threshold;
	double lastThreshold;
	double currentValue;
	double lastValue;
	boolean finished = false;

	/**
	 * Initialize the simple sequential stepper with a starting threshold.
	 *
	 * @param threshold The starting threshold.
	 */
	public SimpleStepper(double threshold) {
		this.threshold = threshold;
		this.currentValue = 1.0;
		this.lastValue = Double.MAX_VALUE;
	}

	/**
	 * Decrement the threshold if the stepper is not marked as finished.
	 * Rendering a stepper finished happens if {@value} is not a number,
	 * below or equal zero or bigger than the last update value. The same
	 * thing happens if the internal threshold falls below one.
	 */
	@Override
	public void update(double value) {
		if (!finished) {
			// Remember current value and store new value
			lastValue = this.currentValue;
			currentValue = value;
			// Decrement threshold
			threshold = this.threshold - 1.0;

			// Stop if the threshold was
			finished = Double.NaN == value ||
					   threshold < 1 ||
					   value < 0.0001 ||
					   value > lastValue;
		}
	}

	/**
	 * Get the current threshold.
	 */
	@Override
	public double getValue() {
		return threshold;
	}

	/**
	 * Indicates if the stepper is marked as finished.
	 */
	@Override
	public boolean isFinished() {
		return finished;
	}
}
