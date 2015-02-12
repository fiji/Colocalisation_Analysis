package results;

/**
 * A small structure to keep decimal places information
 * with numbers along with a name or a simple named text.
 */
public class ValueResult {
	public String name;
	public double number;
	public int decimals;
	public String value;
	public boolean isNumber;

	public ValueResult( String name, double number, int decimals ) {
		this.name = name;
		this.number = number;
		this.decimals = decimals;
		this.isNumber = true;
	}

	public ValueResult( String name, String value) {
		this.name = name;
		this.value = value;
		this.isNumber = false;
	}
}
