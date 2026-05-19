package app.jyu.common.compat;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class Vector4f {

	public float x;
	public float y;
	public float z;
	public float w;

	public Vector4f(Vec3 vec3d, float w) {
		this.x = (float)vec3d.x;
		this.y = (float)vec3d.y;
		this.z = (float)vec3d.z;
		this.w = w;
	}

	public Vector4f mul(Matrix4f mat) {
		var v = new org.joml.Vector4f(this.x, this.y, this.z, this.w);
		v.mul(mat);

		this.x = v.x();
		this.y = v.y();
		this.z = v.z();
		this.w = v.w();

		return this;
	}

	public Vector4f div(float scalar) {
		float inv = 1f / scalar;
		this.x *= inv;
		this.y *= inv;
		this.z *= inv;
		this.w *= inv;

		return this;
	}
}
