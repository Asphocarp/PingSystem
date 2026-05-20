package app.jyu.common.integration;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public class FTBTeamsWrapper {
	private FTBTeamsWrapper() {}

	public static Optional<UUID> getTeamId(Player player) {
		if (!ModContext.HasFTBTeams || !FTBTeamsAPI.isManagerLoaded()) return Optional.empty();

		final var teamManager = FTBTeamsAPI.getManager();
		final var ftbTeam = teamManager.getPlayerTeam(player.getUUID());
		if (ftbTeam == null || ftbTeam.getType().isPlayer()) return Optional.empty();

		return Optional.of(ftbTeam.getId());
	}

	public static Optional<UUID> getSelfTeamId() {
		if (!ModContext.HasFTBTeams || !FTBTeamsAPI.isClientManagerLoaded()) return Optional.empty();

		final var teamManager = FTBTeamsAPI.getClientManager();
		final var ftbTeam = teamManager.selfTeam;
		if (ftbTeam == null || ftbTeam.getType().isPlayer()) return Optional.empty();

		return Optional.of(ftbTeam.getId());
	}
}
