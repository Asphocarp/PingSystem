package app.jyu.common.integration;

import app.jyu.common.platform.IPlatformContextService;

import static app.jyu.common.Global.LOGGER;

public class ModContext {
	private ModContext() {}

	public static boolean HasDistantHorizons = false;

	public static void indexMods() {
		HasDistantHorizons = IPlatformContextService.INSTANCE.isModLoaded("distanthorizons");

		if (HasDistantHorizons) {
			DistantHorizonsCompat.logApiVersion();
		}
	}
}
