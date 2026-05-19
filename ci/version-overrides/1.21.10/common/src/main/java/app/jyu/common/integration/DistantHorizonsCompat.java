package app.jyu.common.integration;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static app.jyu.common.CommonClient.Game;
import static app.jyu.common.Global.LOGGER;

public final class DistantHorizonsCompat {
	private static IDhApiTerrainDataCache terrainCache = null;
	private static Instant lastCacheLoad = Instant.EPOCH;

	private DistantHorizonsCompat() {}

	public static void logApiVersion() {
		LOGGER.info("Distant Horizons API Version: %s.%s.%s".formatted(DhApi.getApiMajorVersion(), DhApi.getApiMinorVersion(), DhApi.getApiPatchVersion()));
	}

	public static void traceDistantAsync(Vec3 direction, float tickDelta, Consumer<BlockHitResult> callback) {
		final var cameraEntity = Game.getCameraEntity();

		if (cameraEntity == null || cameraEntity.level() == null) {
			return;
		}

		final var rayStartVec = cameraEntity.getEyePosition(tickDelta);

		final var future = CompletableFuture.supplyAsync(() -> {
			if (DhApi.Delayed.worldProxy == null) {
				return null;
			}

			final var levelWrapper = DhApi.Delayed.worldProxy.getSinglePlayerLevel();

			if (levelWrapper == null) {
				return null;
			}

			if (terrainCache == null || Duration.between(lastCacheLoad, Instant.now()).getSeconds() > 10) {
				terrainCache = DhApi.Delayed.terrainRepo.createSoftCache();
				lastCacheLoad = Instant.now();
			}

			final var rayCastResult = DhApi.Delayed.terrainRepo.raycast(
				levelWrapper,
				rayStartVec.x, rayStartVec.y, rayStartVec.z,
				(float)direction.x, (float)direction.y, (float)direction.z,
				4096,
				terrainCache
			);

			if (!rayCastResult.success || rayCastResult.payload == null) {
				return null;
			}

			final var pos = new Vec3(rayCastResult.payload.pos.x, rayCastResult.payload.pos.y, rayCastResult.payload.pos.z);
			return new BlockHitResult(pos, Direction.UP, new BlockPos((int)pos.x, (int)pos.y, (int)pos.z), true);
		});

		future.thenAccept(result -> Optional.ofNullable(result).ifPresent(callback));
	}
}
