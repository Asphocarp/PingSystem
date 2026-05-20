package app.jyu.common.integration;

public enum TeamContext {
	NONE, VANILLA_TEAM, FTB_TEAMS, VOICE_CHAT;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
