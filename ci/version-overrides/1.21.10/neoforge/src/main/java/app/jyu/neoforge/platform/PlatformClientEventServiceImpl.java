package app.jyu.neoforge.platform;

import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static app.jyu.common.CommonClient.Game;

public final class PlatformClientEventServiceImpl implements IPlatformClientEventService {
	@Override
	public void registerTickStartEvent(Runnable callback) {
		NeoForge.EVENT_BUS.register(new ClientTickHandler(callback));
	}

	@Override
	public void registerJoinServerEvent(Runnable callback) {
		NeoForge.EVENT_BUS.register(new JoinServerEventHandler(callback));
	}

	@Override
	public void registerLeaveServerEvent(Runnable callback) {
		NeoForge.EVENT_BUS.register(new LeaveServerEventHandler(callback));
	}

	@Override
	public void registerRenderWorldEvent(Consumer<WorldRenderContext> callback) {
		NeoForge.EVENT_BUS.register(new RenderWorldHandler(callback));
	}

	@Override
	public void registerRenderGUIEvent(BiConsumer<GuiGraphics, Float> callback) {
		NeoForge.EVENT_BUS.register(new RenderGuiHandler(callback));
	}

	private record ClientTickHandler(Runnable callback) {
		@SubscribeEvent
		public void onClientTick(ClientTickEvent.Pre event) {
			callback.run();
		}
	}

	private record JoinServerEventHandler(Runnable callback) {
		@SubscribeEvent
		public void onClientConnectedToServer(ClientPlayerNetworkEvent.LoggingIn event) {
			callback.run();
		}
	}

	private record LeaveServerEventHandler(Runnable callback) {
		@SubscribeEvent
		public void onClientDisconnectedFromServer(ClientPlayerNetworkEvent.LoggingOut event) {
			callback.run();
		}
	}

	private record RenderWorldHandler(Consumer<WorldRenderContext> callback) {
		@SubscribeEvent
		public void onRenderWorld(RenderLevelStageEvent event) {
			if (event instanceof RenderLevelStageEvent.AfterWeather) {
				float tickDelta = event.getPartialTick().getGameTimeDeltaPartialTick(false);
				callback.accept(WorldRenderContext.of(
					event.getModelViewMatrix(),
					Game.gameRenderer.getProjectionMatrix(tickDelta),
					tickDelta,
					Game.gameRenderer.getMainCamera()
				));
			}
		}
	}

	private record RenderGuiHandler(BiConsumer<GuiGraphics, Float> callback) {
		@SubscribeEvent
		public void onGuiRender(RenderGuiEvent.Post event) {
			callback.accept(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaTicks());
		}
	}
}
