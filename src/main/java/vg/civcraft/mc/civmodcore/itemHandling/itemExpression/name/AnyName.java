package vg.civcraft.mc.civmodcore.itemHandling.itemExpression.name;

/**
 * @author Ameliorate
 */
public class AnyName implements NameMatcher {
	@Override
	public boolean matches(String matched) {
		return true;
	}

	@Override
	public String solve(String defaultValue) throws NotSolvableException {
		return defaultValue;
	}
}
