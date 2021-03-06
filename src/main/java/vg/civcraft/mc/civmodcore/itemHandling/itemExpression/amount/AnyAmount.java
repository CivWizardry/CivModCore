package vg.civcraft.mc.civmodcore.itemHandling.itemExpression.amount;

/**
 * Accepts any amount.
 *
 * @author Ameliorate
 */
public class AnyAmount implements AmountMatcher {
	@Override
	public boolean matches(Double amount) {
		return true;
	}

	@Override
	public Double solve(Double defaultValue) throws NotSolvableException {
		return defaultValue;
	}
}
