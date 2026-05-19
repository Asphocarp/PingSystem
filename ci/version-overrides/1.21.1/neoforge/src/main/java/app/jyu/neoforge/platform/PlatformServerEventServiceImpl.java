package app.jyu.neoforge.platform;

import app.jyu.common.platform.IPlatformServerEventService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.function.Consumer;

public final class PlatformServerEventServiceImpl implements IPlatformServerEventService {
	@Override
	public void registerPlayerLogoutEvent(Consumer<ServerPlayer> callback) {
		NeoForge.EVENT_BUS.register(new PlayerLogoutEventHandler(callback));
	}

	private record PlayerLogoutEventHandler(Consumer<ServerPlayer> callback) {
		@SubscribeEvent
		public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
			callback.accept((ServerPlayer)event.getEntity());
		}
	}
}
