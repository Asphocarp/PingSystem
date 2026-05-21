package app.jyu.common.integration;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public class FTBTeamsWrapper {
	private FTBTeamsWrapper() {}

	public static Optional<UUID> getTeamId(Player player) {
		var api = FTBTeamsAPI.api();
		if (!ModContext.HasFTBTeams || api == null || !api.isManagerLoaded()) return Optional.empty();

		final var ftbTeam = api.getManager().getTeamForPlayerID(player.getUUID());
		if (ftbTeam.isEmpty() || ftbTeam.get().isPlayerTeam()) return Optional.empty();

		return Optional.of(ftbTeam.get().getId());
	}

	public static Optional<UUID> getSelfTeamId() {
		var api = FTBTeamsAPI.api();
		if (!ModContext.HasFTBTeams || api == null || !api.isClientManagerLoaded()) return Optional.empty();

		final var ftbTeam = api.getClientManager().selfTeam();
		if (ftbTeam == null || ftbTeam.isPlayerTeam()) return Optional.empty();

		return Optional.of(ftbTeam.getId());
	}
}
