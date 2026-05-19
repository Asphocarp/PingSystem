package app.jyu.common.platform;

import net.minecraft.server.MinecraftServer;

import java.util.ServiceLoader;
import java.util.function.Consumer;

public interface IPlatformServerEventService {
    IPlatformServerEventService INSTANCE = ServiceLoader.load(IPlatformServerEventService.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No IPlatformServerEventService implementation found"));

    void registerEndServerTick(Consumer<MinecraftServer> callback);
}
