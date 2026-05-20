package app.jyu.neoforge;

import app.jyu.common.CommonServer;
import app.jyu.common.Global;
import app.jyu.common.command.ServerCommandBuilder;
import app.jyu.common.resource.LanguageUtils;
import app.jyu.neoforge.platform.PlatformContextServiceImpl;
import app.jyu.neoforge.platform.PlatformSoundServiceImpl;
import app.jyu.neoforge.payload.NeoPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Global.MOD_ID)
public final class NeoMain {
	public NeoMain(IEventBus modBus) {
		PlatformContextServiceImpl.modBus = modBus;
		PlatformSoundServiceImpl.registerModBus(modBus);
		modBus.addListener(this::registerPayloads);

		CommonServer.INSTANCE.onInit();
		NeoForge.EVENT_BUS.register(this);

		if (FMLEnvironment.getDist().isClient()) {
			new NeoClient(modBus);
		}
	}

	private void registerPayloads(RegisterPayloadHandlersEvent event) {
		var registrar = event.registrar(Global.MOD_ID).optional();
		registrar.playToServer(NeoPayloads.PingLocationC2S.TYPE, NeoPayloads.PingLocationC2S.CODEC, (payload, context) -> context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer serverPlayer) {
				CommonServer.INSTANCE.onPingLocationPacket(serverPlayer.level().getServer(), serverPlayer, payload.packet());
			}
		}));
		registrar.playToServer(NeoPayloads.UpdateChannelC2S.TYPE, NeoPayloads.UpdateChannelC2S.CODEC, (payload, context) -> context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer serverPlayer) {
				CommonServer.INSTANCE.onChannelUpdatePacket(serverPlayer.level().getServer(), serverPlayer, payload.packet());
			}
		}));
		registrar.playToClient(NeoPayloads.PingLocationS2C.TYPE, NeoPayloads.PingLocationS2C.CODEC, (payload, context) ->
			context.enqueueWork(() -> NeoClient.receivePing(payload.packet()))
		);
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(ServerCommandBuilder.build((context, success, response) -> {
			if (success) {
				context.getSource().sendSuccess(() -> LanguageUtils.withModPrefix(response), false);
			} else {
				context.getSource().sendFailure(LanguageUtils.withModPrefix(response));
			}
		}));
	}
}
