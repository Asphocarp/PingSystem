package app.jyu.forge.platform;

import app.jyu.common.platform.IPlatformServerEventService;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.Consumer;

public final class PlatformServerEventServiceImpl implements IPlatformServerEventService {
    @Override
    public void registerEndServerTick(Consumer<MinecraftServer> callback) {
        MinecraftForge.EVENT_BUS.register(new ServerTickHandler(callback));
    }

    private record ServerTickHandler(Consumer<MinecraftServer> callback) {
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                callback.accept(event.getServer());
            }
        }
    }
}
