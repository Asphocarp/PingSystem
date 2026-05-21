package app.jyu.common.integration;

import io.icker.factions.api.persistents.User;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public class FactionsWrapper {
	private FactionsWrapper() {}

	public static Optional<UUID> getFactionId(Player player) {
		if (!ModContext.HasFactions) return Optional.empty();

		final var user = User.get(player.getUUID());
		if (!user.isInFaction()) return Optional.empty();

		final var faction = user.getFaction();
		if (faction == null) return Optional.empty();

		return Optional.of(faction.getID());
	}
}
