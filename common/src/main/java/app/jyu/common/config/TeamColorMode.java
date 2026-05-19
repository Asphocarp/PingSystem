package app.jyu.common.config;

public enum TeamColorMode {
	FULL, DISABLED, PING_ONLY, LABELS_ONLY;

	public static TeamColorMode get(String name) {
		for (TeamColorMode mode : TeamColorMode.values()) {
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
