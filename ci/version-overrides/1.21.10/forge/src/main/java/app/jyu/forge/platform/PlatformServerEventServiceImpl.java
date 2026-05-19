package app.jyu.forge.platform;

import app.jyu.common.platform.IPlatformServerEventService;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;

import java.util.function.Consumer;

public final class PlatformServerEventServiceImpl implements IPlatformServerEventService {
    @Override
    public void registerEndServerTick(Consumer<MinecraftServer> callback) {
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> callback.accept(event.server()));
    }
}
