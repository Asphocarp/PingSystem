package app.jyu.common.core;

import lombok.Getter;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import app.jyu.common.config.ClientConfig;
import app.jyu.common.integration.ModContext;
import app.jyu.common.math.Raycast;
import app.jyu.common.network.PingLocationC2SPacket;
import app.jyu.common.platform.IPlatformNetworkService;

import java.util.UUID;

import static app.jyu.common.CommonClient.Game;
import static app.jyu.common.config.ClientConfig.MAX_CORRECTION_PERIOD;
import static app.jyu.common.config.ClientConfig.TPS;

public class PingController {
	private PingController() {}

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();

	@Getter
	private static boolean pingQueued = false;
	private static PingType queuedType = PingType.LOCATION;
	private static int pingSequence = 0;
	private static int lastPing = 0;

	public static void revokePingAction() {
		pingQueued = false;
		queuedType = PingType.LOCATION;
	}

	public static void queuePingAction(PingType type) {
		pingQueued = true;
		queuedType = type == null ? PingType.LOCATION : type;
	}

	public static void pollPingAction(float tickDelta) {
		if (!pingQueued || Game.level == null) {
			return;
		}

		var time = (int)Game.level.getGameTime();

		if (CLIENT_CONFIG.getCorrectionPeriod() < MAX_CORRECTION_PERIOD && time - lastPing > CLIENT_CONFIG.getCorrectionPeriod() * TPS) {
			++pingSequence;
		}

		lastPing = time;
		pingQueued = false;
		performPingAction(queuedType, tickDelta);
		queuedType = PingType.LOCATION;
	}

	private static void performPingAction(PingType type, float tickDelta) {
		var cameraEntity = Game.cameraEntity;

		if (cameraEntity == null || Game.level == null) {
			return;
		}

		var cameraDirection = cameraEntity.getViewVector(tickDelta);
		var hitResult = Raycast.traceDirectional(
			cameraDirection,
			tickDelta,
			Math.min(CLIENT_CONFIG.getRaycastDistance(), CLIENT_CONFIG.getPingDistance()),
			cameraEntity.isCrouching());

		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			if (ModContext.HasDistantHorizons) {
				Raycast.traceDistantAsync(cameraDirection, tickDelta, (distantHitResult) -> {
					IPlatformNetworkService.INSTANCE.sendToServer(new PingLocationC2SPacket(CLIENT_CONFIG.getChannel(), distantHitResult.getLocation(), null, pingSequence, GameContext.getDimension(), type));
				});
			}

			return;
		}

		UUID uuid = null;

		if (hitResult.getType() == HitResult.Type.ENTITY) {
			uuid = ((EntityHitResult)hitResult).getEntity().getUUID();
		}

		IPlatformNetworkService.INSTANCE.sendToServer(new PingLocationC2SPacket(CLIENT_CONFIG.getChannel(), hitResult.getLocation(), uuid, pingSequence, GameContext.getDimension(), type));
	}
}
