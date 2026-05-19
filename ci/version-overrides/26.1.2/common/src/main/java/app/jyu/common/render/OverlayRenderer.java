package app.jyu.common.render;

import app.jyu.common.core.GameContext;
import app.jyu.common.core.PingManager;
import app.jyu.common.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static app.jyu.common.CommonClient.Game;

public class OverlayRenderer {
	private OverlayRenderer() {}

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();

	public static void draw(GuiGraphicsExtractor guiGraphics, float tickDelta) {
		PingWheelRenderer.draw(guiGraphics);

		final var pingRepo = PingManager.PING_REPO;

		if (Game.player == null || pingRepo.isEmpty()) {
			return;
		}

		final var ctx = new DrawContext(guiGraphics);
		final var showDirectionIndicator = CLIENT_CONFIG.isDirectionIndicatorVisible();
		final var m = guiGraphics.pose();

		if (showDirectionIndicator) {
			DirectionIndicatorRenderer.prepareSafeZone();
		}

		m.pushMatrix();

		for (var ping : pingRepo) {
			final var screenPos = ping.getScreenPos();

			if (screenPos == null || ping.dimension != GameContext.getDimension()) {
				continue;
			}

			final var behindCamera = screenPos.isBehindCamera();

			if (behindCamera && !showDirectionIndicator) {
				continue;
			}

			guiGraphics.nextStratum();

			if (showDirectionIndicator) {
				DirectionIndicatorRenderer.draw(ctx, ping);
			}

			if (!behindCamera) {
				PingLocationRenderer.draw(ctx, ping);
			}
		}

		m.popMatrix();
	}
}
