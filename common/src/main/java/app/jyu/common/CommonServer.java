package app.jyu.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import app.jyu.common.config.ServerConfig;
import app.jyu.common.core.ServerCore;
import app.jyu.common.integration.ModContext;
import app.jyu.common.network.PingLocationC2SPacket;
import app.jyu.common.network.UpdateChannelC2SPacket;
import app.jyu.common.platform.IPlatformServerEventService;
import app.jyu.common.platform.IPlatformSoundService;

import java.util.List;

import static app.jyu.common.Global.LOGGER;

public class CommonServer {

	public static final CommonServer INSTANCE = new CommonServer();
	private CommonServer() {}

	public void onInit() {
		LOGGER.info("Init");

		IPlatformSoundService.INSTANCE.registerSounds(List.of("ping"));
		ServerConfig.HANDLER.load();

		ModContext.indexMods();
		ServerCore.init();

		IPlatformServerEventService.INSTANCE.registerPlayerLogoutEvent(this::onPlayerDisconnect);
	}

	public void onPingLocationPacket(MinecraftServer server, ServerPlayer player, PingLocationC2SPacket packet) {
		ServerCore.onPingLocation(server, player, packet);
	}

	public void onChannelUpdatePacket(MinecraftServer server, ServerPlayer player, UpdateChannelC2SPacket packet) {
		ServerCore.onChannelUpdate(player, packet);
	}

	public void onPlayerDisconnect(ServerPlayer player) {
		ServerCore.onPlayerDisconnect(player);
	}
}
