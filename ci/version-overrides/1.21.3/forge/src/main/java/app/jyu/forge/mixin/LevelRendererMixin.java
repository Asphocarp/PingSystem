package app.jyu.forge.mixin;

import app.jyu.common.render.WorldRenderContext;
import app.jyu.forge.event.WorldRenderCallback;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;applyModelViewMatrix()V", ordinal = 0, shift = At.Shift.AFTER))
	private void onStartRenderLevel(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		WorldRenderCallback.fire(WorldRenderContext.of(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), deltaTracker.getGameTimeDeltaTicks(), camera));
	}
}
