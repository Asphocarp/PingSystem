package app.jyu.fabric.platform;

import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;
import app.jyu.fabric.event.GuiRenderCallback;
import app.jyu.fabric.event.WorldRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PlatformClientEventServiceImpl implements IPlatformClientEventService {
	@Override
	public void registerTickStartEvent(Runnable callback) {
		ClientTickEvents.START_CLIENT_TICK.register(client -> callback.run());
	}

	@Override
	public void registerJoinServerEvent(Runnable callback) {
		ClientPlayConnectionEvents.JOIN.register((a, b, c) -> callback.run());
	}

	@Override
	public void registerLeaveServerEvent(Runnable callback) {
		ClientPlayConnectionEvents.DISCONNECT.register((a, b) -> callback.run());
	}

	@Override
	public void registerRenderWorldEvent(Consumer<WorldRenderContext> callback) {
		WorldRenderCallback.START.register(callback::accept);
	}

	@Override
	public void registerRenderGUIEvent(BiConsumer<GuiGraphics, Float> callback) {
		GuiRenderCallback.START.register(callback::accept);
	}
}
