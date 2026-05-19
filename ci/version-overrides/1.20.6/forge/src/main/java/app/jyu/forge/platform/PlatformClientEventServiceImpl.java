package app.jyu.forge.platform;

import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.render.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
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
        MinecraftForge.EVENT_BUS.register(new RenderWorldHandler(callback));
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

    private record RenderWorldHandler(Consumer<WorldRenderContext> callback) {
        @SubscribeEvent
        public void onRenderWorld(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
                PoseStack poseStack = new PoseStack();
                poseStack.last().pose().set(event.getPoseStack());
                callback.accept(WorldRenderContext.of(poseStack, event.getProjectionMatrix(), event.getPartialTick(), event.getCamera()));
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
