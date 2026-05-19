package app.jyu.forge.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PlatformClientEventServiceImpl implements IPlatformClientEventService {

	@Override
	public void registerTickStartEvent(Runnable callback) {
		MinecraftForge.EVENT_BUS.register(new ClientTickEventEventHandler(callback));
	}
	private record ClientTickEventEventHandler(Runnable callback) {
		@SubscribeEvent
		public void onClientTick(TickEvent.ClientTickEvent event) {
			if (event.phase.equals(TickEvent.Phase.START)) {
				callback.run();
			}
		}
	}

	@Override
	public void registerJoinServerEvent(Runnable callback) {
		MinecraftForge.EVENT_BUS.register(new JoinServerEventHandler(callback));
	}
	private record JoinServerEventHandler(Runnable callback) {
		@SubscribeEvent
		public void onClientConnectedToServer(ClientPlayerNetworkEvent.LoggingIn event) {
			callback.run();
		}
	}

	@Override
	public void registerLeaveServerEvent(Runnable callback) {
		MinecraftForge.EVENT_BUS.register(new LeaveServerEventHandler(callback));
	}
	private record LeaveServerEventHandler(Runnable callback) {
		@SubscribeEvent
		public void onClientDisconnectedFromServer(ClientPlayerNetworkEvent.LoggingOut event) {
			callback.run();
		}
	}

	@Override
	public void registerRenderWorldEvent(Consumer<WorldRenderContext> callback) {
		MinecraftForge.EVENT_BUS.register(new RenderWorldEventEventHandler(callback));
	}
	private record RenderWorldEventEventHandler(Consumer<WorldRenderContext> callback) {
		@SubscribeEvent
		public void onRenderWorld(RenderLevelStageEvent event) {
			if (event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_WEATHER)) {
				callback.accept(WorldRenderContext.of(event.getPoseStack().last().pose(), event.getProjectionMatrix(), event.getPartialTick(), event.getCamera()));
			}
		}
	}

	@Override
	public void registerRenderGUIEvent(BiConsumer<PoseStack, Float> callback) {
		MinecraftForge.EVENT_BUS.register(new RenderGUIEventEventHandler(callback));
	}
	private record RenderGUIEventEventHandler(BiConsumer<PoseStack, Float> callback) {
		@SubscribeEvent
		public void onPreGuiRender(RenderGuiEvent.Pre event) {
			callback.accept(event.getPoseStack(), event.getPartialTick());
		}
	}
}
