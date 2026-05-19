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
		if (player.getTeam() != null) return TeamContext.VANILLA_TEAM;

		return TeamContext.NONE;
	}

	public static TeamContext getSelfContext() {
		if (Game.player != null && Game.player.getTeam() != null) return TeamContext.VANILLA_TEAM;

		return TeamContext.NONE;
	}

	public static boolean inSameContext(Player p1, Player p2) {
		return p1.getTeam() == p2.getTeam();
	}
}
