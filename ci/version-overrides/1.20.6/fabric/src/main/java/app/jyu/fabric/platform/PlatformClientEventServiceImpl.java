package app.jyu.fabric.platform;

import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PlatformClientEventServiceImpl implements IPlatformClientEventService {
    @Override
    public void registerEndClientTick(Runnable callback) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> callback.run());
    }

    @Override
    public void registerRenderWorld(Consumer<WorldRenderContext> callback) {
        WorldRenderEvents.LAST.register(context ->
                callback.accept(WorldRenderContext.of(context.matrixStack(), context.projectionMatrix(), context.tickDelta(), context.camera())));
    }

    @Override
    public void registerRenderGui(BiConsumer<GuiGraphics, Float> callback) {
        HudRenderCallback.EVENT.register(callback::accept);
    }
}
