package app.jyu.fabric;

import app.jyu.common.SophisticatedPingClientCommon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SophisticatedPingClientCommon.init();

        ClientPlayNetworking.registerGlobalReceiver(FabricPayloads.PingS2C.TYPE, (payload, context) ->
                SophisticatedPingClientCommon.receivePing(payload.point()));

        ClientPlayNetworking.registerGlobalReceiver(FabricPayloads.RemovePingS2C.TYPE, (payload, context) ->
                SophisticatedPingClientCommon.receiveRemovePing(payload.point()));
    }
}
