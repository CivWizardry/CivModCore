package vg.civcraft.mc.civmodcore.itemHandling.itemExpression.map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.ItemMatcher;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.name.NameMatcher;

/**
 * @author Ameliorate
 */
public class ItemMapLocationMatcher implements ItemMatcher {
	public ItemMapLocationMatcher(NameMatcher locationName) {
		this.locationName = locationName;
	}

	public NameMatcher locationName;

	@Override
	public boolean matches(ItemStack item) {
		if (!item.hasItemMeta() || !(item.getItemMeta() instanceof MapMeta) ||
				!((MapMeta) item.getItemMeta()).hasLocationName())
			return false;

		String location = ((MapMeta) item.getItemMeta()).getLocationName();
		return locationName.matches(location);
	}

	@Override
	public ItemStack solve(ItemStack item) throws NotSolvableException {
		if (!item.hasItemMeta() || !(item.getItemMeta() instanceof MapMeta))
			item.setType(Material.MAP);

		MapMeta meta = (MapMeta) item.getItemMeta();
		meta.setLocationName(locationName.solve(""));

		item.setItemMeta(meta);

		return item;
	}
}
