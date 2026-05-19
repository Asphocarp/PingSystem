package app.jyu.common.config;

public enum PlayerInfoMode {
	HOLD, DISABLED, ALWAYS, COMPACT;

	public static PlayerInfoMode get(String name) {
		for (PlayerInfoMode mode : PlayerInfoMode.values()) {
			if (mode.name().equalsIgnoreCase(name)) {
				return mode;
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
