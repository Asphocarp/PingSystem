package app.jyu.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.event.EventNetworkChannel;
import app.jyu.common.CommonClient;
import app.jyu.common.command.ClientCommandBuilder;
import app.jyu.common.network.PingLocationS2CPacket;
import app.jyu.common.resource.LanguageUtils;
import app.jyu.common.resource.ResourceReloadListener;
import app.jyu.common.screen.SettingsScreen;

import java.util.function.Consumer;
import java.util.function.Function;

import static app.jyu.forge.ForgeMain.PING_LOCATION_CHANNEL_S2C;

public class ForgeClient {

	public ForgeClient() {
		CommonClient.INSTANCE.onInit();

		MinecraftForge.EVENT_BUS.register(this);

		// packets
		registerPacketHandler(PING_LOCATION_CHANNEL_S2C, PingLocationS2CPacket::readSafe, CommonClient.INSTANCE::onPingLocationPacket);

		// resource reload
		FMLJavaModLoadingContext
			.get()
			.getModEventBus()
			.addListener((RegisterClientReloadListenersEvent event) -> event.registerReloadListener(new ResourceReloadListener()));

		// config screen
		ModLoadingContext.get().registerExtensionPoint(
			ConfigScreenHandler.ConfigScreenFactory.class,
			() -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> new SettingsScreen(parent))
		);
	}

	public static <T> void registerPacketHandler(EventNetworkChannel channel, Function<FriendlyByteBuf, T> packetReader, Consumer<T> packetHandler) {
		channel.addListener((event) -> {
			var ctx = event.getSource().get();
			var payload = event.getPayload();

			if (payload != null) {
				var packet = packetReader.apply(payload);
				ctx.enqueueWork(() -> packetHandler.accept(packet));
			}

			ctx.setPacketHandled(true);
		});
	}

	@SubscribeEvent
	public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
		event.getDispatcher().register(ClientCommandBuilder.build((context, success, response) -> {
			if (success) {
				context.getSource().sendSuccess(() -> LanguageUtils.withModPrefix(response), false);
			} else {
				context.getSource().sendFailure(LanguageUtils.withModPrefix(response));
			}
		}));
	}
}
