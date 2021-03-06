package vg.civcraft.mc.civmodcore.itemHandling.itemExpression.uuid;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * @author Ameliorate
 */
public class PlayerNameUUID implements UUIDMatcher {
	public PlayerNameUUID(String name) {
		this.name = name;
	}

	public String name;

	@Override
	public boolean matches(UUID uuid) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
		if (player.getName() != null)
			return player.getName().equals(name);
		else
			return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public UUID solve(UUID startingValue) throws NotSolvableException {
		OfflinePlayer player = Bukkit.getOfflinePlayer(name);

		if (player == null)
			throw new NotSolvableException("can't find player with name " + name);

		return player.getUniqueId();
	}
}
