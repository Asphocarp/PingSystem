package app.jyu.fabric.platform;

import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PlatformClientEventServiceImpl implements IPlatformClientEventService {
    @Override
    public void registerEndClientTick(Runnable callback) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> callback.run());
    }

    @Override
    public void registerRenderWorld(Consumer<WorldRenderContext> callback) {
        // Fabric API 26.x removed the world-render callback used by older branches.
    }

    @Override
    public void registerRenderGui(BiConsumer<GuiGraphicsExtractor, Float> callback) {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, app.jyu.common.Constants.id("ping_hud"), (guiGraphics, tickCounter) ->
                callback.accept(guiGraphics, tickCounter.getGameTimeDeltaPartialTick(true)));
    }
}
