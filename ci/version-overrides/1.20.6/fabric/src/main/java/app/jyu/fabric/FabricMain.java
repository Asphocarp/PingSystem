package app.jyu.fabric;

import app.jyu.common.CommonServer;
import app.jyu.common.command.ServerCommandBuilder;
import app.jyu.common.resource.LanguageUtils;
import app.jyu.fabric.payload.FabricPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class FabricMain implements ModInitializer {
	@Override
	public void onInitialize() {
		CommonServer.INSTANCE.onInit();

		PayloadTypeRegistry.playC2S().register(FabricPayloads.PingLocationC2S.TYPE, FabricPayloads.PingLocationC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(FabricPayloads.UpdateChannelC2S.TYPE, FabricPayloads.UpdateChannelC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(FabricPayloads.PingLocationS2C.TYPE, FabricPayloads.PingLocationS2C.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(
			FabricPayloads.PingLocationC2S.TYPE,
			(payload, context) -> CommonServer.INSTANCE.onPingLocationPacket(
				context.player().getServer(),
				context.player(),
				payload.packet()
			)
		);
		ServerPlayNetworking.registerGlobalReceiver(
			FabricPayloads.UpdateChannelC2S.TYPE,
			(payload, context) -> CommonServer.INSTANCE.onChannelUpdatePacket(
				context.player().getServer(),
				context.player(),
				payload.packet()
			)
		);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(ServerCommandBuilder.build((context, success, response) -> {
			if (success) {
				context.getSource().sendSuccess(() -> LanguageUtils.withModPrefix(response), false);
			} else {
				context.getSource().sendFailure(LanguageUtils.withModPrefix(response));
			}
		})));
	}
}
