package vg.civcraft.mc.civmodcore.itemHandling.itemExpression.tropicalbucket;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.ItemMatcher;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.enummatcher.EnumMatcher;

import java.util.Optional;

/**
 * @author Ameliorate
 */
public class ItemTropicFishBBodyColorMatcher implements ItemMatcher {
	public ItemTropicFishBBodyColorMatcher(EnumMatcher<DyeColor> color) {
		this.color = color;
	}

	public static ItemTropicFishBBodyColorMatcher construct(Optional<EnumMatcher<DyeColor>> color) {
		return color.map(ItemTropicFishBBodyColorMatcher::new).orElse(null);
	}

	public EnumMatcher<DyeColor> color;

	@Override
	public boolean matches(ItemStack item) {
		if (!item.hasItemMeta() || !(item.getItemMeta() instanceof TropicalFishBucketMeta) ||
				!((TropicalFishBucketMeta) item.getItemMeta()).hasVariant())
			return false;

		return color.matches(((TropicalFishBucketMeta) item.getItemMeta()).getBodyColor());
	}

	@Override
	public ItemStack solve(ItemStack item) throws NotSolvableException {
		if (!item.hasItemMeta() || !(item.getItemMeta() instanceof TropicalFishBucketMeta))
			item.setType(Material.TROPICAL_FISH_BUCKET);

		TropicalFishBucketMeta meta = (TropicalFishBucketMeta) item.getItemMeta();

		meta.setBodyColor(color.solve(meta.getBodyColor()));
		item.setItemMeta(meta);
		return item;
	}
}
