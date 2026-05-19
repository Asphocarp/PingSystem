package app.jyu.fabric;

import app.jyu.common.Constants;
import app.jyu.common.PingPoint;
import app.jyu.common.SophisticatedPingCommon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FabricMain implements ModInitializer {
    @Override
    public void onInitialize() {
        SophisticatedPingCommon.init();

        ServerPlayNetworking.registerGlobalReceiver(Constants.PING_PACKET, (server, player, handler, buf, responseSender) ->
                SophisticatedPingCommon.onPingPacket(server, player, PingPoint.read(buf)));

        ServerPlayNetworking.registerGlobalReceiver(Constants.REMOVE_PING_PACKET, (server, player, handler, buf, responseSender) ->
                SophisticatedPingCommon.onRemovePingPacket(player, PingPoint.read(buf)));
    }
}
