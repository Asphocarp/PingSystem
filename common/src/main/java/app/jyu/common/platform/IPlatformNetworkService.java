package app.jyu.common.platform;

import app.jyu.common.PingPoint;
import net.minecraft.server.level.ServerPlayer;

import java.util.ServiceLoader;

public interface IPlatformNetworkService {
    IPlatformNetworkService INSTANCE = ServiceLoader.load(IPlatformNetworkService.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No IPlatformNetworkService implementation found"));

    void sendPingToServer(PingPoint point);

    void sendRemovePingToServer(PingPoint point);

    void sendPingToClient(ServerPlayer player, PingPoint point);

    void sendRemovePingToClient(ServerPlayer player, PingPoint point);
}
