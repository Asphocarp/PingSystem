package app.jyu.forge.platform;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import app.jyu.common.platform.IPlatformServerEventService;

import java.util.function.Consumer;

public class PlatformServerEventServiceImpl implements IPlatformServerEventService {

	@Override
	public void registerPlayerLogoutEvent(Consumer<ServerPlayer> callback) {
		MinecraftForge.EVENT_BUS.register(new PlayerLogoutEventHandler(callback));
	}
	private record PlayerLogoutEventHandler(Consumer<ServerPlayer> callback) {
		@SubscribeEvent
		public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
			callback.accept((ServerPlayer)event.getEntity());
		}
	}
}
