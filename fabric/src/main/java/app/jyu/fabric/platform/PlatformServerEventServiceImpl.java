package app.jyu.fabric.platform;

import app.jyu.common.platform.IPlatformServerEventService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

public final class PlatformServerEventServiceImpl implements IPlatformServerEventService {
    @Override
    public void registerEndServerTick(Consumer<MinecraftServer> callback) {
        ServerTickEvents.END_SERVER_TICK.register(callback::accept);
    }
}
