package app.jyu.neoforge.platform;

import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PlatformClientEventServiceImpl implements IPlatformClientEventService {
    @Override
    public void registerEndClientTick(Runnable callback) {
        NeoForge.EVENT_BUS.register(new ClientTickHandler(callback));
    }

    @Override
    public void registerRenderWorld(Consumer<WorldRenderContext> callback) {
        NeoForge.EVENT_BUS.register(new RenderWorldHandler(callback));
    }

    @Override
    public void registerRenderGui(BiConsumer<GuiGraphicsExtractor, Float> callback) {
        NeoForge.EVENT_BUS.register(new RenderGuiHandler(callback));
    }

    private record ClientTickHandler(Runnable callback) {
        @SubscribeEvent
        public void onClientTick(ClientTickEvent.Post event) {
            callback.run();
        }
    }

    private record RenderWorldHandler(Consumer<WorldRenderContext> callback) {
        @SubscribeEvent
        public void onRenderWorld(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
                callback.accept(WorldRenderContext.of(event.getPoseStack(), event.getProjectionMatrix(), event.getPartialTick().getGameTimeDeltaPartialTick(true), event.getCamera()));
            }
        }
    }

    private record RenderGuiHandler(BiConsumer<GuiGraphicsExtractor, Float> callback) {
        @SubscribeEvent
        public void onRenderGui(RenderGuiEvent.Post event) {
            callback.accept(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
        }
    }
}
