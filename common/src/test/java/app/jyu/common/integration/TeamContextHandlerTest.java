package app.jyu.common.integration;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeamContextHandlerTest {
	@Test
	void sameContextTypeAndIdMatches() {
		final var teamId = UUID.randomUUID();
		final var p1 = new TeamContextHandler.ResolvedContext(TeamContext.FACTIONS, teamId);
		final var p2 = new TeamContextHandler.ResolvedContext(TeamContext.FACTIONS, teamId);

		assertTrue(TeamContextHandler.inSameContext(p1, p2));
	}

	@Test
	void sameIdInDifferentContextTypesDoesNotMatch() {
		final var sharedId = UUID.randomUUID();
		final var voice = new TeamContextHandler.ResolvedContext(TeamContext.VOICE_CHAT, sharedId);
		final var factions = new TeamContextHandler.ResolvedContext(TeamContext.FACTIONS, sharedId);

		assertFalse(TeamContextHandler.inSameContext(voice, factions));
	}

	@Test
	void bothPlayersWithoutAnyContextStillMatchExistingNullTeamBehavior() {
		final var p1 = new TeamContextHandler.ResolvedContext(TeamContext.NONE, null);
		final var p2 = new TeamContextHandler.ResolvedContext(TeamContext.NONE, null);

		assertTrue(TeamContextHandler.inSameContext(p1, p2));
	}

	@Test
	void factionContextHasPriorityAfterFtbAndBeforeVanilla() {
		final var ftbId = UUID.randomUUID();
		final var factionId = UUID.randomUUID();
		final var vanillaTeam = new Object();

		final var withFtb = TeamContextHandler.selectContext(
			Optional.empty(),
			Optional.of(ftbId),
			Optional.of(factionId),
			vanillaTeam
		);
		final var withFaction = TeamContextHandler.selectContext(
			Optional.empty(),
			Optional.empty(),
			Optional.of(factionId),
			vanillaTeam
		);

		assertEquals(TeamContext.FTB_TEAMS, withFtb.context());
		assertEquals(ftbId, withFtb.id());
		assertEquals(TeamContext.FACTIONS, withFaction.context());
		assertEquals(factionId, withFaction.id());
	}
}
