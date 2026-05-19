package app.jyu.forge.platform;

import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PlatformClientEventServiceImpl implements IPlatformClientEventService {
    @Override
    public void registerEndClientTick(Runnable callback) {
        MinecraftForge.EVENT_BUS.register(new ClientTickHandler(callback));
    }

    @Override
    public void registerRenderWorld(Consumer<WorldRenderContext> callback) {
        // Forge 1.21.3+ no longer exposes the legacy world-render event in the binary API.
    }

    @Override
    public void registerRenderGui(BiConsumer<GuiGraphics, Float> callback) {
        MinecraftForge.EVENT_BUS.register(new RenderGuiHandler(callback));
    }

    private record ClientTickHandler(Runnable callback) {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                callback.run();
            }
        }
    }

    private record RenderGuiHandler(BiConsumer<GuiGraphics, Float> callback) {
        @SubscribeEvent
        public void onRenderGui(CustomizeGuiOverlayEvent.Chat event) {
            callback.accept(event.getGuiGraphics(), event.getPartialTick());
        }
    }
}
