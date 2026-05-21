package app.jyu.common.math;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import app.jyu.common.integration.DistantHorizonsCompat;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static app.jyu.common.CommonClient.Game;

public class Raycast {
	private Raycast() {}

	public static void traceDistantAsync(Vec3 direction, float tickDelta, Consumer<BlockHitResult> callback) {
		DistantHorizonsCompat.traceDistantAsync(direction, tickDelta, callback);
	}

	public static HitResult traceDirectional(Vec3 direction,
											 float tickDelta,
											 double maxDistance,
											 boolean hitTranslucent) {
		var cameraEntity = Game.cameraEntity;

		if (cameraEntity == null || cameraEntity.level() == null) {
			return null;
		}

		var rayStartVec = cameraEntity.getEyePosition(tickDelta);
		var rayEndVec = rayStartVec.add(direction.scale(maxDistance));
		var boundingBox = cameraEntity
			.getBoundingBox()
			.expandTowards(cameraEntity.getViewVector(1.f).scale(maxDistance))
			.inflate(1.0, 1.0, 1.0);

		var blockHitResult = cameraEntity.level().clip(
			new ClipContext(
				rayStartVec,
				rayEndVec,
				hitTranslucent ? ClipContext.Block.OUTLINE : ClipContext.Block.VISUAL,
				hitTranslucent ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
				cameraEntity)
		);

		var entityHitResult = traceEntity(
			cameraEntity,
			rayStartVec,
			rayEndVec,
			boundingBox,
			(targetEntity) -> !targetEntity.isSpectator());

		if (entityHitResult == null) {
			return blockHitResult;
		}

		if (rayStartVec.distanceToSqr(blockHitResult.getLocation()) < rayStartVec.distanceToSqr(entityHitResult.getLocation())) {
			return blockHitResult;
		}

		return entityHitResult;
	}

	private static EntityHitResult traceEntity(Entity entity,
											   Vec3 min,
											   Vec3 max,
											   AABB box,
											   Predicate<Entity> predicate) {
		var minDist = min.distanceToSqr(max);
		EntityHitResult minHitResult = null;

		for (var ent : entity.level().getEntities(entity, box, predicate)) {
			var targetBoundingBox = ent.getBoundingBox()
				.inflate(ent.getPickRadius())
				.inflate(0.25);
			var hitPos = targetBoundingBox.clip(min, max);

			if (hitPos.isEmpty()) {
				continue;
			}

			var hitResult = new EntityHitResult(ent, hitPos.get());
			var hitDist = min.distanceToSqr(hitResult.getLocation());

			if (minDist > hitDist) {
				minDist = hitDist;
				minHitResult = hitResult;
			}
		}

		return minHitResult;
	}
}
