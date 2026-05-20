package app.jyu.neoforge;

import app.jyu.common.CommonClient;
import app.jyu.common.command.ClientCommandBuilder;
import app.jyu.common.network.PingLocationS2CPacket;
import app.jyu.common.resource.LanguageUtils;
import app.jyu.common.resource.ResourceReloadListener;
import app.jyu.common.screen.SettingsScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoClient {
	public NeoClient(IEventBus modBus) {
		CommonClient.INSTANCE.onInit();
		NeoForge.EVENT_BUS.register(this);
		modBus.addListener((RegisterClientReloadListenersEvent event) -> event.registerReloadListener(new ResourceReloadListener()));
		ModLoadingContext.get().registerExtensionPoint(
			IConfigScreenFactory.class,
			() -> (container, parent) -> new SettingsScreen(parent)
		);
	}

	public static void receivePing(PingLocationS2CPacket packet) {
		CommonClient.INSTANCE.onPingLocationPacket(packet);
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
