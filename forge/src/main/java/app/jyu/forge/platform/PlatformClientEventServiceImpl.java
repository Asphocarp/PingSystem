package app.jyu.forge.platform;

import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;
import app.jyu.forge.event.WorldRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
		WorldRenderCallback.register(callback);
	}

	@Override
	public void registerRenderGUIEvent(BiConsumer<GuiGraphics, Float> callback) {
		MinecraftForge.EVENT_BUS.register(new RenderGUIEventEventHandler(callback));
	}
	private record RenderGUIEventEventHandler(BiConsumer<GuiGraphics, Float> callback) {
		@SubscribeEvent
		public void onPreGuiRender(CustomizeGuiOverlayEvent.Chat event) {
			callback.accept(event.getGuiGraphics(), event.getPartialTick());
		}
	}
}
