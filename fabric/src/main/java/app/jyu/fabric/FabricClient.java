package app.jyu.fabric;

import app.jyu.common.Constants;
import app.jyu.common.PingPoint;
import app.jyu.common.SophisticatedPingClientCommon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SophisticatedPingClientCommon.init();

        ClientPlayNetworking.registerGlobalReceiver(Constants.PING_PACKET, (client, handler, buf, responseSender) ->
                SophisticatedPingClientCommon.receivePing(PingPoint.read(buf)));

        ClientPlayNetworking.registerGlobalReceiver(Constants.REMOVE_PING_PACKET, (client, handler, buf, responseSender) ->
                SophisticatedPingClientCommon.receiveRemovePing(PingPoint.read(buf)));
    }
}
