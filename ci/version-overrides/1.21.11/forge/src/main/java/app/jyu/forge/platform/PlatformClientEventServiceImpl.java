package app.jyu.forge.platform;

import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PlatformClientEventServiceImpl implements IPlatformClientEventService {
    @Override
    public void registerEndClientTick(Runnable callback) {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> callback.run());
    }

    @Override
    public void registerRenderWorld(Consumer<WorldRenderContext> callback) {
        // Forge 1.21.3+ no longer exposes the legacy world-render event in the binary API.
    }

    @Override
    public void registerRenderGui(BiConsumer<GuiGraphics, Float> callback) {
        CustomizeGuiOverlayEvent.Chat.BUS.addListener(event -> callback.accept(event.getGuiGraphics(), event.getPartialTick()));
    }
}
