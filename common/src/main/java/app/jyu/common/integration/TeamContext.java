package app.jyu.common.integration;

public enum TeamContext {
	NONE, VANILLA_TEAM, FACTIONS, FTB_TEAMS, VOICE_CHAT;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
