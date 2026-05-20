package app.jyu.common.integration;

import net.minecraft.world.entity.player.Player;

import static app.jyu.common.CommonClient.Game;

public class TeamContextHandler {
	private TeamContextHandler() {}

	public static boolean hasTeam(Player player) {
		if (player == null) return false;

		return getContext(player) != TeamContext.NONE;
	}

	public static TeamContext getContext(Player player) {
		final var voiceChatId = VoiceChatWrapper.getGroupId(player);
		if (voiceChatId.isPresent()) return TeamContext.VOICE_CHAT;

		final var ftbTeamId = FTBTeamsWrapper.getTeamId(player);
		if (ftbTeamId.isPresent()) return TeamContext.FTB_TEAMS;

		if (player.getTeam() != null) return TeamContext.VANILLA_TEAM;

		return TeamContext.NONE;
	}

	public static TeamContext getSelfContext() {
		final var voiceChatId = VoiceChatWrapper.getSelfGroupId();
		if (voiceChatId.isPresent()) return TeamContext.VOICE_CHAT;

		final var ftbTeamId = FTBTeamsWrapper.getSelfTeamId();
		if (ftbTeamId.isPresent()) return TeamContext.FTB_TEAMS;

		if (Game.player != null && Game.player.getTeam() != null) return TeamContext.VANILLA_TEAM;

		return TeamContext.NONE;
	}

	public static boolean inSameContext(Player p1, Player p2) {
		final var p1TeamId = VoiceChatWrapper.getGroupId(p1).orElse(FTBTeamsWrapper.getTeamId(p1).orElse(null));
		final var p2TeamId = VoiceChatWrapper.getGroupId(p2).orElse(FTBTeamsWrapper.getTeamId(p2).orElse(null));
		if (p1TeamId != null) return p1TeamId.equals(p2TeamId);
		if (p2TeamId != null) return false;

		return p1.getTeam() == p2.getTeam();
	}
}
