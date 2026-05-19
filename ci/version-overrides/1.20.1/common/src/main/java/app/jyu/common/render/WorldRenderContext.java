package app.jyu.common.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;

public final class WorldRenderContext {
    public final PoseStack poseStack;
    public final Matrix4f projectionMatrix;
    public final float tickDelta;
    public final Camera camera;

    private WorldRenderContext(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, Camera camera) {
        this.poseStack = poseStack;
        this.projectionMatrix = projectionMatrix;
        this.tickDelta = tickDelta;
        this.camera = camera;
    }

    public static WorldRenderContext of(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, Camera camera) {
        return new WorldRenderContext(poseStack, projectionMatrix, tickDelta, camera);
    }
}
