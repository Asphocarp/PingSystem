package app.jyu.common.integration;

import app.jyu.common.platform.IPlatformContextService;

import static app.jyu.common.Global.LOGGER;

public class ModContext {
	private ModContext() {}

	public static boolean HasDistantHorizons = false;
	public static boolean HasVoiceChat = false;
	public static boolean HasFTBTeams = false;
	public static boolean HasFactions = false;
	public static boolean HasSable = false;

	public static void indexMods() {
		HasDistantHorizons = IPlatformContextService.INSTANCE.isModLoaded("distanthorizons");
		HasVoiceChat = IPlatformContextService.INSTANCE.isModLoaded("voicechat");
		HasFTBTeams = IPlatformContextService.INSTANCE.isModLoaded("ftbteams");
		HasFactions = IPlatformContextService.INSTANCE.isModLoaded("factions");
		HasSable = IPlatformContextService.INSTANCE.isModLoaded("sable");

		if (HasDistantHorizons) {
			DistantHorizonsCompat.logApiVersion();
		}
	}
}
