package app.jyu.neoforge.platform;

import app.jyu.common.PingPoint;
import app.jyu.common.platform.IPlatformNetworkService;
import app.jyu.neoforge.NeoMain;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlatformNetworkServiceImpl implements IPlatformNetworkService {
    @Override
    public void sendPingToServer(PingPoint point) {
        PacketDistributor.sendToServer(new NeoMain.PingC2S(point));
    }

    @Override
    public void sendRemovePingToServer(PingPoint point) {
        PacketDistributor.sendToServer(new NeoMain.RemovePingC2S(point));
    }

    @Override
    public void sendPingToClient(ServerPlayer player, PingPoint point) {
        PacketDistributor.sendToPlayer(player, new NeoMain.PingS2C(point));
    }

    @Override
    public void sendRemovePingToClient(ServerPlayer player, PingPoint point) {
        PacketDistributor.sendToPlayer(player, new NeoMain.RemovePingS2C(point));
    }
}
