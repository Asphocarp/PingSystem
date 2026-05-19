package app.jyu.forge.platform;

import app.jyu.common.PingPoint;
import app.jyu.common.platform.IPlatformNetworkService;
import app.jyu.forge.ForgeNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class PlatformNetworkServiceImpl implements IPlatformNetworkService {
    @Override
    public void sendPingToServer(PingPoint point) {
        ForgeNetwork.CHANNEL.send(new ForgeNetwork.PingC2S(point), PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendRemovePingToServer(PingPoint point) {
        ForgeNetwork.CHANNEL.send(new ForgeNetwork.RemovePingC2S(point), PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendPingToClient(ServerPlayer player, PingPoint point) {
        ForgeNetwork.CHANNEL.send(new ForgeNetwork.PingS2C(point), PacketDistributor.PLAYER.with(player));
    }

    @Override
    public void sendRemovePingToClient(ServerPlayer player, PingPoint point) {
        ForgeNetwork.CHANNEL.send(new ForgeNetwork.RemovePingS2C(point), PacketDistributor.PLAYER.with(player));
    }
}
