package vg.civcraft.mc.civmodcore.itemHandling.itemExpression.enchantment;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.ItemMatcher;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.misc.ListMatchingMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static vg.civcraft.mc.civmodcore.itemHandling.itemExpression.enchantment.EnchantmentsSource.HELD;

/**
 * @author Ameliorate
 */
public class ItemEnchantmentsMatcher implements ItemMatcher {
	public ItemEnchantmentsMatcher(List<EnchantmentMatcher> enchantmentMatchers, ListMatchingMode mode, EnchantmentsSource source) {
		if (enchantmentMatchers.isEmpty())
			throw new IllegalArgumentException("enchanmentMatchers can not be empty. If an empty enchantmentMatchers " +
					"was allowed, it would cause many subtle logic errors.");
		this.enchantmentMatchers = enchantmentMatchers;
		this.mode = mode;
		this.source = source;
	}

	public List<EnchantmentMatcher> enchantmentMatchers;
	public ListMatchingMode mode;
	public EnchantmentsSource source;

	@Override
	public boolean matches(ItemStack item) {
		switch (source) {
			case ITEM:
				return matches(item.getEnchantments());
			case HELD:
				if (!item.hasItemMeta() || !(item.getItemMeta() instanceof EnchantmentStorageMeta))
					return false;
				return matches(((EnchantmentStorageMeta) item.getItemMeta()).getStoredEnchants());
		}
		throw new AssertionError("not reachable");
	}

	@Override
	public ItemStack solve(ItemStack item) throws NotSolvableException {
		if (source == HELD && !(item.getItemMeta() instanceof EnchantmentStorageMeta))
			item.setType(Material.ENCHANTED_BOOK);

		Map<Enchantment, Integer> defaultEnchantments = item.getEnchantments();
		if (defaultEnchantments.isEmpty()) {
			defaultEnchantments = new HashMap<>(defaultEnchantments); // spigot returns a immutable hashmap
			defaultEnchantments.put(Enchantment.DAMAGE_ALL, 1);
		}

		List<Map.Entry<Enchantment, Integer>> enchantments =
				mode.solve(enchantmentMatchers,
						new ListMatchingMode.LazyFromListEntrySupplier<>(defaultEnchantments));

		Map<Enchantment, Integer> enchantmentMap =
				enchantments.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		source.set(item, enchantmentMap, true);
		return item;
	}

	public boolean matches(Map<Enchantment, Integer> enchantments) {
		return mode.matches(enchantmentMatchers, enchantments.entrySet());
	}
}
