package app.jyu.fabric;

import app.jyu.common.SophisticatedPingCommon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FabricMain implements ModInitializer {
    @Override
    public void onInitialize() {
        SophisticatedPingCommon.init();
        FabricPayloads.register();

        ServerPlayNetworking.registerGlobalReceiver(FabricPayloads.PingC2S.TYPE, (payload, context) ->
                SophisticatedPingCommon.onPingPacket(context.player().getServer(), context.player(), payload.point()));

        ServerPlayNetworking.registerGlobalReceiver(FabricPayloads.RemovePingC2S.TYPE, (payload, context) ->
                SophisticatedPingCommon.onRemovePingPacket(context.player(), payload.point()));
    }
}
