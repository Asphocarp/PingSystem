package app.jyu.forge.platform;

import app.jyu.common.platform.IPlatformServerEventService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.function.Consumer;

public class PlatformServerEventServiceImpl implements IPlatformServerEventService {

	@Override
	public void registerPlayerLogoutEvent(Consumer<ServerPlayer> callback) {
		PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> callback.accept((ServerPlayer)event.getEntity()));
	}
}
