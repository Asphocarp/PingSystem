package app.jyu.common.integration;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

import static app.jyu.common.CommonClient.Game;

public class TeamContextHandler {
	private TeamContextHandler() {}

	record ResolvedContext(TeamContext context, Object id) {}

	public static boolean hasTeam(Player player) {
		if (player == null) return false;

		return getContext(player) != TeamContext.NONE;
	}

	public static TeamContext getContext(Player player) {
		return resolveContext(player).context();
	}

	public static TeamContext getSelfContext() {
		final var voiceChatId = VoiceChatWrapper.getSelfGroupId();
		if (voiceChatId.isPresent()) return TeamContext.VOICE_CHAT;

		final var ftbTeamId = FTBTeamsWrapper.getSelfTeamId();
		if (ftbTeamId.isPresent()) return TeamContext.FTB_TEAMS;

		if (Game.player == null) return TeamContext.NONE;

		return resolveContext(Game.player).context();
	}

	public static boolean inSameContext(Player p1, Player p2) {
		return inSameContext(resolveContext(p1), resolveContext(p2));
	}

	static boolean inSameContext(ResolvedContext p1, ResolvedContext p2) {
		if (p1.context() != p2.context()) return false;

		return p1.id() == p2.id() || (p1.id() != null && p1.id().equals(p2.id()));
	}

	static ResolvedContext resolveContext(Player player) {
		if (player == null) return new ResolvedContext(TeamContext.NONE, null);

		return selectContext(
			VoiceChatWrapper.getGroupId(player),
			FTBTeamsWrapper.getTeamId(player),
			FactionsWrapper.getFactionId(player),
			player.getTeam()
		);
	}

	static ResolvedContext selectContext(Optional<?> voiceChatId, Optional<?> ftbTeamId, Optional<?> factionId, Object vanillaTeam) {
		if (voiceChatId.isPresent()) return new ResolvedContext(TeamContext.VOICE_CHAT, voiceChatId.get());

		if (ftbTeamId.isPresent()) return new ResolvedContext(TeamContext.FTB_TEAMS, ftbTeamId.get());

		if (factionId.isPresent()) return new ResolvedContext(TeamContext.FACTIONS, factionId.get());

		if (vanillaTeam != null) return new ResolvedContext(TeamContext.VANILLA_TEAM, vanillaTeam);

		return new ResolvedContext(TeamContext.NONE, null);
	}
}
