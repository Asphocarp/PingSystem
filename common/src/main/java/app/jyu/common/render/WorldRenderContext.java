package app.jyu.common.render;

import lombok.AllArgsConstructor;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;

@AllArgsConstructor(staticName = "of")
public class WorldRenderContext {
	public final Matrix4f modelViewMatrix;
	public final Matrix4f projectionMatrix;
	public final Float tickDelta;
	public final Camera camera;
}
