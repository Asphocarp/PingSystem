package app.jyu.common.integration;

public enum TeamContext {
	NONE, VANILLA_TEAM;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
