package app.jyu.forge.platform;

import app.jyu.common.PingPoint;
import app.jyu.common.platform.IPlatformNetworkService;
import app.jyu.forge.ForgeNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class PlatformNetworkServiceImpl implements IPlatformNetworkService {
    @Override
    public void sendPingToServer(PingPoint point) {
        ForgeNetwork.CHANNEL.sendToServer(new ForgeNetwork.PingC2S(point));
    }

    @Override
    public void sendRemovePingToServer(PingPoint point) {
        ForgeNetwork.CHANNEL.sendToServer(new ForgeNetwork.RemovePingC2S(point));
    }

    @Override
    public void sendPingToClient(ServerPlayer player, PingPoint point) {
        ForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ForgeNetwork.PingS2C(point));
    }

    @Override
    public void sendRemovePingToClient(ServerPlayer player, PingPoint point) {
        ForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ForgeNetwork.RemovePingS2C(point));
    }
}
