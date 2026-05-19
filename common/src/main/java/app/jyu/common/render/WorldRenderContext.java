package app.jyu.common.render;

import com.mojang.math.Matrix4f;
import lombok.AllArgsConstructor;
import net.minecraft.client.Camera;

@AllArgsConstructor(staticName = "of")
public class WorldRenderContext {
	public final Matrix4f modelViewMatrix;
	public final Matrix4f projectionMatrix;
	public final Float tickDelta;
	public final Camera camera;
}
