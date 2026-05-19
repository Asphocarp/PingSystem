package app.jyu.fabric.platform;

import app.jyu.common.PingPoint;
import app.jyu.fabric.FabricPayloads;
import app.jyu.common.platform.IPlatformNetworkService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class PlatformNetworkServiceImpl implements IPlatformNetworkService {
    @Override
    public void sendPingToServer(PingPoint point) {
        if (!ClientPlayNetworking.canSend(FabricPayloads.PingC2S.TYPE)) {
            return;
        }
        ClientPlayNetworking.send(new FabricPayloads.PingC2S(point));
    }

    @Override
    public void sendRemovePingToServer(PingPoint point) {
        if (!ClientPlayNetworking.canSend(FabricPayloads.RemovePingC2S.TYPE)) {
            return;
        }
        ClientPlayNetworking.send(new FabricPayloads.RemovePingC2S(point));
    }

    @Override
    public void sendPingToClient(ServerPlayer player, PingPoint point) {
        if (!ServerPlayNetworking.canSend(player, FabricPayloads.PingS2C.TYPE)) {
            return;
        }
        ServerPlayNetworking.send(player, new FabricPayloads.PingS2C(point));
    }

    @Override
    public void sendRemovePingToClient(ServerPlayer player, PingPoint point) {
        if (!ServerPlayNetworking.canSend(player, FabricPayloads.RemovePingS2C.TYPE)) {
            return;
        }
        ServerPlayNetworking.send(player, new FabricPayloads.RemovePingS2C(point));
    }
}
