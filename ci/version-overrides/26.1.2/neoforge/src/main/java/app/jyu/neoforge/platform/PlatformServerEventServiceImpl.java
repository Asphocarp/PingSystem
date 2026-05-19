package app.jyu.neoforge.platform;

import app.jyu.common.platform.IPlatformServerEventService;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.function.Consumer;

public final class PlatformServerEventServiceImpl implements IPlatformServerEventService {
    @Override
    public void registerEndServerTick(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.register(new ServerTickHandler(callback));
    }

    private record ServerTickHandler(Consumer<MinecraftServer> callback) {
        @SubscribeEvent
        public void onServerTick(ServerTickEvent.Post event) {
            callback.accept(event.getServer());
        }
    }
}
