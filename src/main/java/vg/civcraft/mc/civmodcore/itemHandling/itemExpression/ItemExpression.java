package vg.civcraft.mc.civmodcore.itemHandling.itemExpression;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.amount.*;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.enchantment.*;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.lore.*;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.material.*;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.name.*;
import vg.civcraft.mc.civmodcore.itemHandling.itemExpression.uuid.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A unified syntax for matching any ItemStack for things like the material, amount, lore contents, and more.
 *
 * While mostly designed to be used in a .yaml config file, this can also be used from java.
 *
 * @author Ameliorate
 */
public class ItemExpression {
	/**
	 * Creates the default ItemExpression.
	 *
	 * By default, it will match any ItemStack.
	 */
	public ItemExpression() {}

	/**
	 * Creates an ItemExpression from a section of bukkit configuration format.
	 * @param configurationSection The subsection of config that should be parsed.
	 */
	public ItemExpression(ConfigurationSection configurationSection) {
		parseConfig(configurationSection);
	}

	/**
	 * Creates an ItemExpression that matches exactly the passed ItemStack, and no other item.
	 *
	 * Note that because of how ItemExpression is implemented, if ItemExpression does not support matching an element
	 * of an item, this will accept any item with that element. For example, if ItemExpression did not support
	 * matching the player on a player skull (it supports it), this constructor would return an ItemExpression
	 * that matched any player head even when passed a player head with a specific name.
	 * @param item The ItemStack that this ItemExpression would exactly match.
	 */
	public ItemExpression(ItemStack item) {
		this(item, false);
	}

	/**
	 * Creates an ItemExpression that matches exactly the passed ItemStack, or acts equilivent to ItemStack.isSimilar().
	 *
	 * See also ItemExpression(ItemStack).
	 * @param item The item that this ItemExpression would match.
	 * @param acceptSimilar If this ItemExpression should act similar to ItemStack.isSimilar().
	 */
	public ItemExpression(ItemStack item, boolean acceptSimilar) {
		setMaterial(new ExactlyMaterial(item.getType()));
		if (acceptSimilar)
			setAmount(new AnyAmount());
		else
			setAmount(new ExactlyAmount(item.getAmount()));
		setDurability(new ExactlyAmount(item.getDurability()));
		if (item.hasItemMeta() && item.getItemMeta().hasLore())
			setLore(new ExactlyLore(item.getItemMeta().getLore()));
		else
			setLore(new ExactlyLore(new ArrayList<>()));
		if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
			setName(new ExactlyName(item.getItemMeta().getDisplayName()));
		else
			setName(new VanillaName());
		if (item.hasItemMeta() && item.getItemMeta().hasEnchants())
			setEnchantmentAll(exactlyEnchantments(item.getEnchantments()));
		else
			setEnchantmentAll(new EnchantmentSetMatcher(Collections.singletonList(new NoEnchantment())));
		if (item.getItemMeta() instanceof EnchantmentStorageMeta)
			setEnchantmentHeldAll(exactlyEnchantments(((EnchantmentStorageMeta) item.getItemMeta()).getStoredEnchants()));
		else
			setEnchantmentHeldAll(new EnchantmentSetMatcher(Collections.singletonList(new NoEnchantment())));
		if (item.getItemMeta() instanceof SkullMeta)
			setSkullMatchers(Collections.singletonList(new ExactlyUUID(((SkullMeta) item.getItemMeta()).getOwningPlayer().getUniqueId())));
		else
			setSkullMatchers(Collections.singletonList(new ExactlyUUID(new UUID(0, 0))));
	}

	private EnchantmentSetMatcher exactlyEnchantments(Map<Enchantment, Integer> enchantments) {
		return new EnchantmentSetMatcher(enchantments.entrySet().stream().map((kv) -> {
			Enchantment enchantment = kv.getKey();
			int level = kv.getValue();
			return new ExactlyEnchantment(enchantment, new ExactlyAmount(level));
		}).collect(Collectors.toList()));
	}

	/**
	 * Mutate this ItemExpression, overriding the existing options set for this with the options given in the
	 * ConfigurationSection.
	 * @param config The config that options will be taken from.
	 */
	public void parseConfig(ConfigurationSection config) {
		setMaterial(parseMaterial(config, "material"));
		setAmount(parseAmount(config, "amount"));
		setDurability(parseAmount(config, "durability"));
		setLore(parseLore(config, "lore"));
		setName(parseName(config, "name"));
		setEnchantmentAny(parseEnchantment(config, "enchantmentsAny"));
		setEnchantmentAll(parseEnchantment(config, "enchantmentsAll"));
		setEnchantmentNone(parseEnchantment(config, "enchantmentsNone"));
		setEnchantmentHeldAny(parseEnchantment(config, "enchantmentsHeldAny"));
		setEnchantmentHeldAll(parseEnchantment(config, "enchantmentsHeldAll"));
		setEnchantmentHeldNone(parseEnchantment(config, "enchantmentsHeldNone"));
		setSkullMatchers(parseSkull(config, "skull"));

		if (config.contains("unbreakable"))
			unbreakable = config.getBoolean("unbreakable");
		else
			unbreakable = null;
	}

	/**
	 * Gets a ItemExpression from the given path in the config
	 * @param configurationSection The config to get the ItemExpression from
	 * @param path The path to the ItemExpression
	 * @return The ItemExpression in the config that path points to, or null if there was not an ItemExpression at path.
	 */
	public static ItemExpression getItemExpression(ConfigurationSection configurationSection, String path) {
		if (configurationSection == null)
			return null;
		if (!configurationSection.contains(path))
			return null;
		return new ItemExpression(configurationSection.getConfigurationSection(path));
	}

	private MaterialMatcher parseMaterial(ConfigurationSection config, String path) {
		if (config.contains(path + ".regex"))
			return(new RegexMaterial(Pattern.compile(config.getString(path + ".regex"))));
		else if (config.contains(path))
			return(new ExactlyMaterial(Material.getMaterial(config.getString(path))));
		return null;
	}

	private AmountMatcher parseAmount(ConfigurationSection config, String path) {
		if (config.contains(path + ".range"))
			return(new RangeAmount(
					config.getInt(path + ".range.low", 0),
					config.getInt(path + ".range.high"),
					config.getBoolean(path + ".range.inclusiveLow", true),
					config.getBoolean(path + ".range.inclusiveHigh", true)));
		else if ("any".equals(config.getString(path)))
			return(new AnyAmount());
		else if (config.contains(path))
			return(new ExactlyAmount(config.getInt(path)));
		return null;
	}

	private LoreMatcher parseLore(ConfigurationSection config, String path) {
		if (config.contains(path + ".regex")) {
			String patternStr = config.getString(path + ".regex");
			boolean multiline = config.getBoolean(path + ".regexMultiline", true);
			Pattern pattern = Pattern.compile(patternStr, multiline ? Pattern.MULTILINE : 0);

			return(new RegexLore(pattern));
		} else if (config.contains(path))
			return(new ExactlyLore(config.getStringList(path)));
		return null;
	}

	private NameMatcher parseName(ConfigurationSection config, String path) {
		if (config.contains(path + ".regex"))
			return(new RegexName(Pattern.compile(config.getString(path + ".regex"))));
		else if ("vanilla".equals(config.getString(path)))
			return(new VanillaName());
		else if (config.contains(path))
			return(new ExactlyName(config.getString(path)));
		return null;
	}

	private EnchantmentSetMatcher parseEnchantment(ConfigurationSection config, String path) {
		ConfigurationSection enchantments = config.getConfigurationSection(path);
		if (enchantments == null)
			return null;

		ArrayList<EnchantmentMatcher> enchantmentMatcher = new ArrayList<>();
		for (String enchantName : enchantments.getKeys(false)) {
			enchantmentMatcher.add(
					new ExactlyEnchantment(Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase())),
							parseAmount(config, path + "." + enchantName)));
		}

		return new EnchantmentSetMatcher(enchantmentMatcher);
	}

	private List<UUIDMatcher> parseSkull(ConfigurationSection config, String path) {
		List<UUIDMatcher> matchers = new ArrayList<>();
		ConfigurationSection skull = config.getConfigurationSection(path);
		if (skull == null)
			return Collections.emptyList();

		for (String name : skull.getStringList("names")) {
			matchers.add(new PlayerNameUUID(name));
		}

		for (String uuid : skull.getStringList("uuids")) {
			matchers.add(new ExactlyUUID(UUID.fromString(uuid)));
		}

		if (skull.contains("name"))
			matchers.add(new PlayerNameUUID(skull.getString("name")));
		if (skull.contains("uuid"))
			matchers.add(new ExactlyUUID(UUID.fromString(skull.getString("name"))));

		return matchers;
	}

	/**
	 * Runs this ItemExpression on a given ItemStack.
	 *
	 * This will not mutate the ItemStack nor this ItemExpression.
	 * @param item The ItemStack to be matched upon.
	 * @return If the given item matches.
	 */
	public boolean matches(ItemStack item) {
		ItemMeta meta;
		if (item.hasItemMeta()) {
			meta = item.getItemMeta();
		} else {
			meta = new ItemStack(Material.IRON_AXE, 1).getItemMeta(); // clever hack
		}

		if (!materialMatcher.matches(item.getType()))
			return false;
		else if (!amountMatcher.matches(item.getAmount()))
			return false;
		else if (!durabilityMatcher.matches(item.getDurability()))
			return false;
		else if (!loreMatcher.matches(meta.getLore()))
			return false;
		else if (!nameMatcher.matches(meta.getDisplayName()))
			return false;
		else if (!enchantmentMatcherAny.matches(item.getEnchantments(), true))
			return false;
		else if (!enchantmentMatcherAll.matches(item.getEnchantments(), false))
			return false;
		else if (enchantmentMatcherNone.matches(item.getEnchantments(), false))
			return false;
		else if (unbreakable != null && meta.isUnbreakable() != unbreakable)
			return false;
		else if (!enchantmentMatcherHeldAny.matches(castOrNull(meta), true))
			return false;
		else if (!enchantmentMatcherHeldAll.matches(castOrNull(meta), false))
			return false;
		else if (enchantmentMatcherHeldNone.matches(castOrNull(meta), false))
			return false;
		else if (!skullMatches(meta))
			return false;
		return true;
	}

	private Map<Enchantment, Integer> castOrNull(ItemMeta itemMeta) {
		Map<Enchantment, Integer> result = (itemMeta instanceof EnchantmentStorageMeta) ?
				((EnchantmentStorageMeta) itemMeta).getStoredEnchants() : null;
		if (result == null)
			result = new HashMap<>();
		return result;
	}

	private boolean skullMatches(ItemMeta meta) {
		UUID uuid;
		if (!(meta instanceof SkullMeta) || !((SkullMeta) meta).hasOwner())
			uuid = new UUID(0, 0);
		else
			uuid = ((SkullMeta) meta).getOwningPlayer().getUniqueId();

		return skullMatchers.stream().anyMatch((matcher) -> matcher.matches(uuid));
	}

	/**
	 * Removes amount items that match this ItemExpression from tne inventory.
	 *
	 * This function correctly handles situations where the inventory has two or more ItemStacks that do not satisfy
	 * .isSimilar() but do match this ItemExpression.
	 * @param inventory The inventory items will be removed from.
	 * @param amount The number of items to remove. If this is -1, all items that match will be removed.
	 * @return If there were enough items to remove. If this is false, no items have been removed from the inventory.
	 */
	public boolean removeFromInventory(Inventory inventory, int amount) {
		// store the amount matcher, because it'll mess with things later
		// for exacple, what happens when amount=1 was passed into this function but amount: 64 is in the config?
		AmountMatcher amountMatcher1 = getAmount();
		setAmount(new AnyAmount());

		try {
			int runningAmount = amount;
			boolean infinite = false;
			if (runningAmount == -1) {
				runningAmount = Integer.MAX_VALUE;
				infinite = true;
			}

			ItemStack[] contents = inventory.getStorageContents();
			contents = Arrays.stream(contents).map(item -> item != null ? item.clone() : null).toArray(ItemStack[]::new);
			for (ItemStack item : contents) {
				if (item == null)
					continue;
				if (item.getType() == Material.AIR)
					continue;

				if (matches(item)) {
					if (item.getAmount() >= runningAmount) {
						int itemOldAmount = item.getAmount();
						item.setAmount(item.getAmount() - runningAmount);
						runningAmount -= itemOldAmount - item.getAmount();
						break;
					} else if (item.getAmount() < runningAmount) {
						runningAmount -= item.getAmount();
						item.setAmount(0);
					}
				}
			}

			if (runningAmount == 0 || infinite) {
				inventory.setStorageContents(contents);
				return true;
			} else if (runningAmount < 0) {
				// big trouble, this isn't supposed to happen
				throw new AssertionError("runningAmount is less than 0, there's a bug somewhere. runningAmount: " + runningAmount);
			} else {
				// items remaining
				return false;
			}
		} finally {
			// restore the amount matcher now we're done
			setAmount(amountMatcher1);
		}
	}

	/**
	 * Removes the items that match this ItemExpression. The amount to remove is infered from the amount of this
	 * ItemExpression.
	 *
	 * If amount is `any`, all items that match this ItemExpression will be removed.
	 * If amount is a range and random is true, a random number of items within the range will be removed.
	 * If amount is a range and random is false, the lower bound of the range will be used.
	 * @param inventory The inventory items will be removed from.
	 * @param random To select a random number within amount. This only applies if amount is a range.
	 * @return If there were enough items to remove. If this is false, no items have been removed from the inventory.
	 */
	public boolean removeFromInventory(Inventory inventory, boolean random) {
		int amount;
		if (amountMatcher instanceof ExactlyAmount) {
			amount = ((ExactlyAmount) amountMatcher).amount;
		} else if (amountMatcher instanceof AnyAmount) {
			amount = -1;
		} else if (amountMatcher instanceof RangeAmount && !random) {
			RangeAmount rangeAmount = (RangeAmount) amountMatcher;
			amount = rangeAmount.getLow() + (rangeAmount.lowInclusive ? 0 : 1);
		} else if (amountMatcher instanceof RangeAmount && random) {
			RangeAmount rangeAmount = (RangeAmount) amountMatcher;
			amount = ThreadLocalRandom.current()
					.nextInt(rangeAmount.getLow() + (rangeAmount.lowInclusive ? 0 : -1),
							rangeAmount.getHigh() + (rangeAmount.highInclusive ? 1 : 0));
		} else {
			throw new IllegalArgumentException("removeFromInventory(Inventory, boolean) does not work with custom AmountMatchers");
		}

		return removeFromInventory(inventory, amount);
	}

	/**
	 * null if matches unbreakable == true and unbreakable == false
	 */
	public Boolean unbreakable = null;

	private MaterialMatcher materialMatcher = new AnyMaterial();

	public MaterialMatcher getMaterial() {
		return materialMatcher;
	}

	public void setMaterial(MaterialMatcher materialMatcher) {
		if (materialMatcher == null)
			return;
		this.materialMatcher = materialMatcher;
	}

	private AmountMatcher amountMatcher = new AnyAmount();

	public AmountMatcher getAmount() {
		return amountMatcher;
	}

	public void setAmount(AmountMatcher amountMatcher) {
		if (amountMatcher == null)
            return;
		this.amountMatcher = amountMatcher;
	}

	private LoreMatcher loreMatcher = new AnyLore();

	public LoreMatcher getLore() {
		return loreMatcher;
	}

	public void setLore(LoreMatcher loreMatcher) {
		if (loreMatcher == null)
            return;
		this.loreMatcher = loreMatcher;
	}

	private NameMatcher nameMatcher = new AnyName();

	public NameMatcher getName() {
		return nameMatcher;
	}

	public void setName(NameMatcher nameMatcher) {
		if (nameMatcher == null)
            return;
		this.nameMatcher = nameMatcher;
	}

	private EnchantmentSetMatcher enchantmentMatcherAny =
			new EnchantmentSetMatcher(Collections.singletonList(new AnyEnchantment()));

	public EnchantmentSetMatcher getEnchantmentAny() {
		return enchantmentMatcherAny;
	}

	public void setEnchantmentAny(EnchantmentSetMatcher enchantmentMatcher) {
		if (enchantmentMatcher == null)
			return;
		this.enchantmentMatcherAny = enchantmentMatcher;
	}

	private EnchantmentSetMatcher enchantmentMatcherAll =
			new EnchantmentSetMatcher(Collections.singletonList(new AnyEnchantment()));

	public EnchantmentSetMatcher getEnchantmentAll() {
		return enchantmentMatcherAll;
	}

	public void setEnchantmentAll(EnchantmentSetMatcher enchantmentMatcherAll) {
		if (enchantmentMatcherAll == null)
			return;
		this.enchantmentMatcherAll = enchantmentMatcherAll;
	}

	private EnchantmentSetMatcher enchantmentMatcherNone =
			new EnchantmentSetMatcher(Collections.singletonList(new NoEnchantment()));

	public EnchantmentSetMatcher getEnchantmentNone() {
		return enchantmentMatcherNone;
	}

	public void setEnchantmentNone(EnchantmentSetMatcher getEnchantmentMatcherNone) {
		if (getEnchantmentMatcherNone == null)
			return;
		this.enchantmentMatcherNone = getEnchantmentMatcherNone;
	}

	public EnchantmentSetMatcher getEnchantmentHeldAny() {
		return enchantmentMatcherHeldAny;
	}

	public void setEnchantmentHeldAny(EnchantmentSetMatcher enchantmentMatcherHeldAny) {
		if (enchantmentMatcherHeldAny == null)
			return;
		this.enchantmentMatcherHeldAny = enchantmentMatcherHeldAny;
	}

	public EnchantmentSetMatcher getEnchantmentHeldAll() {
		return enchantmentMatcherHeldAll;
	}

	public void setEnchantmentHeldAll(EnchantmentSetMatcher enchantmentMatcherHeldAll) {
		if (enchantmentMatcherHeldAll == null)
			return;
		this.enchantmentMatcherHeldAll = enchantmentMatcherHeldAll;
	}

	public EnchantmentSetMatcher getEnchantmentHeldNone() {
		return enchantmentMatcherHeldNone;
	}

	public void setEnchantmentHeldNone(EnchantmentSetMatcher enchantmentMatcherHeldNone) {
		if (enchantmentMatcherHeldNone == null)
			return;
		this.enchantmentMatcherHeldNone = enchantmentMatcherHeldNone;
	}

	private EnchantmentSetMatcher enchantmentMatcherHeldAny =
			new EnchantmentSetMatcher(Collections.singletonList(new AnyEnchantment()));

	private EnchantmentSetMatcher enchantmentMatcherHeldAll =
			new EnchantmentSetMatcher(Collections.singletonList(new AnyEnchantment()));

	private EnchantmentSetMatcher enchantmentMatcherHeldNone =
			new EnchantmentSetMatcher(Collections.singletonList(new NoEnchantment()));

	private AmountMatcher durabilityMatcher = new AnyAmount();

	public AmountMatcher getDurability() {
		return durabilityMatcher;
	}

	public void setDurability(AmountMatcher durabilityMatcher) {
		if (durabilityMatcher == null)
			return;
		this.durabilityMatcher = durabilityMatcher;
	}

	public List<UUIDMatcher> skullMatchers = Collections.singletonList(new AnyUUID());

	public void setSkullMatchers(List<UUIDMatcher> matchers) {
		if (matchers == null || matchers.isEmpty())
			return;
		skullMatchers = matchers;
	}

	public class EnchantmentSetMatcher {
		public EnchantmentSetMatcher(List<EnchantmentMatcher> enchantmentMatchers) {
			this.enchantmentMatchers = enchantmentMatchers;
		}

		public List<EnchantmentMatcher> enchantmentMatchers;

		public boolean matches(Map<Enchantment, Integer> enchantments, boolean isAny) {
			if (matchesAny() && enchantments.size() == 0)
				return true;

			for (EnchantmentMatcher matcher : enchantmentMatchers) {
				boolean matchedOne = false;

				for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
					Enchantment enchantment = entry.getKey();
					int level = entry.getValue();

					if (matcher.matches(enchantment, level)) {
						matchedOne = true;
						if (isAny)
							return true;
					}
				}

				if (!isAny && !matchedOne)
					return false;
			}

			if (!isAny)
				return true;
			else
				return false;
		}

		public boolean matchesAny() {
			return enchantmentMatchers.size() == 1 && enchantmentMatchers.get(0) instanceof AnyEnchantment;
		}
	}
}
