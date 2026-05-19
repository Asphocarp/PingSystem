package app.jyu.common;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import app.jyu.common.compat.LegacyMigrationHandler;
import app.jyu.common.config.ClientConfig;
import app.jyu.common.core.GameContext;
import app.jyu.common.core.PingController;
import app.jyu.common.core.PingManager;
import app.jyu.common.network.PingLocationS2CPacket;
import app.jyu.common.network.UpdateChannelC2SPacket;
import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.platform.IPlatformContextService;
import app.jyu.common.platform.IPlatformNetworkService;
import app.jyu.common.render.OverlayRenderer;
import app.jyu.common.render.WorldRenderContext;
import app.jyu.common.screen.SettingsScreen;
import app.jyu.common.util.InputUtils;

import static app.jyu.common.util.InputUtils.KEY_BINDING_PING;
import static app.jyu.common.util.InputUtils.KEY_BINDING_SETTINGS;

public class CommonClient {

	public static final CommonClient INSTANCE = new CommonClient();
	public static Minecraft Game = null;

	private CommonClient() {}

	public void onInit() {
		ClientConfig.HANDLER.load();

		IPlatformClientEventService.INSTANCE.registerTickStartEvent(this::onTickStart);
		IPlatformClientEventService.INSTANCE.registerJoinServerEvent(this::onJoinServer);
		IPlatformClientEventService.INSTANCE.registerLeaveServerEvent(this::onLeaveServer);
		IPlatformClientEventService.INSTANCE.registerRenderWorldEvent(this::onRenderWorld);
		IPlatformClientEventService.INSTANCE.registerRenderGUIEvent(this::onRenderGUI);

		IPlatformContextService.INSTANCE.registerKeyMapping(KEY_BINDING_PING);
		IPlatformContextService.INSTANCE.registerKeyMapping(KEY_BINDING_SETTINGS);

		LegacyMigrationHandler.migrateKeyMappings();
	}

	public void onJoinServer() {
		IPlatformNetworkService.INSTANCE.sendToServer(new UpdateChannelC2SPacket(ClientConfig.HANDLER.getConfig().getChannel()));
	}

	public void onLeaveServer() {
		PingManager.clearPings();
	}

	public void onTickStart() {
		Game = Minecraft.getInstance();
		GameContext.updateDimension();

		LegacyMigrationHandler.onTick();

		if (InputUtils.consumePingHotkey()) {
			PingController.queuePingAction();
		}

		if (KEY_BINDING_SETTINGS.consumeClick()) {
			Game.setScreen(new SettingsScreen());
		}
	}

	public void onRenderWorld(WorldRenderContext ctx) {
		PingManager.updatePings(ctx);
		PingController.pollPingAction(ctx.tickDelta);
	}

	public void onRenderGUI(PoseStack poseStack, float tickDelta) {
		OverlayRenderer.draw(poseStack, tickDelta);
	}

	public void onPingLocationPacket(PingLocationS2CPacket packet) {
		PingManager.acceptPingPacket(packet);
	}
}
