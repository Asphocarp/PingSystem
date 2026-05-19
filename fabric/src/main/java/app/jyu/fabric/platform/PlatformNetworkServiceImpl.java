package app.jyu.fabric.platform;

import app.jyu.common.Constants;
import app.jyu.common.PingPoint;
import app.jyu.common.platform.IPlatformNetworkService;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class PlatformNetworkServiceImpl implements IPlatformNetworkService {
    @Override
    public void sendPingToServer(PingPoint point) {
        sendToServer(Constants.PING_PACKET, point);
    }

    @Override
    public void sendRemovePingToServer(PingPoint point) {
        sendToServer(Constants.REMOVE_PING_PACKET, point);
    }

    @Override
    public void sendPingToClient(ServerPlayer player, PingPoint point) {
        sendToClient(player, Constants.PING_PACKET, point);
    }

    @Override
    public void sendRemovePingToClient(ServerPlayer player, PingPoint point) {
        sendToClient(player, Constants.REMOVE_PING_PACKET, point);
    }

    private static void sendToServer(net.minecraft.resources.ResourceLocation id, PingPoint point) {
        if (!ClientPlayNetworking.canSend(id)) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        point.write(buf);
        ClientPlayNetworking.send(id, buf);
    }

    private static void sendToClient(ServerPlayer player, net.minecraft.resources.ResourceLocation id, PingPoint point) {
        if (!ServerPlayNetworking.canSend(player, id)) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        point.write(buf);
        ServerPlayNetworking.send(player, id, buf);
    }
}
