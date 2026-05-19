package app.jyu.common.platform;

import app.jyu.common.render.WorldRenderContext;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface IPlatformClientEventService {
    IPlatformClientEventService INSTANCE = ServiceLoader.load(IPlatformClientEventService.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No IPlatformClientEventService implementation found"));

    void registerEndClientTick(Runnable callback);

    void registerRenderWorld(Consumer<WorldRenderContext> callback);

    void registerRenderGui(BiConsumer<GuiGraphics, Float> callback);
}
