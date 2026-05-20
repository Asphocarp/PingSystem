package app.jyu.fabric.mixin;

import app.jyu.common.render.WorldRenderContext;
import app.jyu.fabric.event.WorldRenderCallback;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static app.jyu.common.CommonClient.Game;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Inject(method = "renderLevel", at = @At(value = "TAIL"))
	private void onStartRenderLevel(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean renderBlockOutline, CameraRenderState cameraRenderState, Matrix4fc frustumMatrix, GpuBufferSlice fog, org.joml.Vector4f fogColor, boolean renderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
		WorldRenderCallback.START.invoker().onRenderWorld(WorldRenderContext.of(RenderSystem.getModelViewMatrix(), cameraRenderState.projectionMatrix, deltaTracker.getGameTimeDeltaTicks(), Game.gameRenderer.getMainCamera()));
	}
}
