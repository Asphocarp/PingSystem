package app.jyu.forge.platform;

import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;
import app.jyu.forge.event.WorldRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PlatformClientEventServiceImpl implements IPlatformClientEventService {

	@Override
	public void registerTickStartEvent(Runnable callback) {
		TickEvent.ClientTickEvent.Pre.BUS.addListener(event -> callback.run());
	}

	@Override
	public void registerJoinServerEvent(Runnable callback) {
		ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> callback.run());
	}

	@Override
	public void registerLeaveServerEvent(Runnable callback) {
		ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> callback.run());
	}

	@Override
	public void registerRenderWorldEvent(Consumer<WorldRenderContext> callback) {
		WorldRenderCallback.register(callback);
	}

	@Override
	public void registerRenderGUIEvent(BiConsumer<GuiGraphics, Float> callback) {
		Consumer<CustomizeGuiOverlayEvent.Chat> listener = event -> callback.accept(event.getGuiGraphics(), event.getPartialTick());
		CustomizeGuiOverlayEvent.Chat.BUS.addListener(listener);
	}
}
