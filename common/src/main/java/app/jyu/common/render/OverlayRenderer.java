package app.jyu.common.render;

import com.mojang.blaze3d.vertex.PoseStack;
import app.jyu.common.core.GameContext;
import app.jyu.common.core.PingManager;
import app.jyu.common.config.ClientConfig;

import static app.jyu.common.CommonClient.Game;

public class OverlayRenderer {
	private OverlayRenderer() {}

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();

	public static void draw(PoseStack m, float tickDelta) {
		final var pingRepo = PingManager.PING_REPO;

		if (Game.player == null || pingRepo.isEmpty()) {
			return;
		}

		final var ctx = new DrawContext(m);
		final var showDirectionIndicator = CLIENT_CONFIG.isDirectionIndicatorVisible();

		if (showDirectionIndicator) {
			DirectionIndicatorRenderer.prepareSafeZone();
		}

		m.pushPose();
		m.translate(0f, 0f, -pingRepo.size() * 16f);

		for (var ping : pingRepo) {
			final var screenPos = ping.getScreenPos();

			if (screenPos == null || ping.dimension != GameContext.getDimension()) {
				continue;
			}

			final var behindCamera = screenPos.isBehindCamera();

			if (behindCamera && !showDirectionIndicator) {
				continue;
			}

			m.translate(0f, 0f, 16f);

			if (showDirectionIndicator) {
				DirectionIndicatorRenderer.draw(ctx, ping);
			}

			if (!behindCamera) {
				PingLocationRenderer.draw(ctx, ping);
			}
		}

		m.popPose();
	}
}
