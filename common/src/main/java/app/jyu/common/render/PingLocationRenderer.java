package app.jyu.common.render;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import app.jyu.common.compat.Component;
import app.jyu.common.config.ClientConfig;
import app.jyu.common.config.PlayerInfoMode;
import app.jyu.common.core.PingType;
import app.jyu.common.core.PingView;
import app.jyu.common.resource.LanguageUtils;

import static app.jyu.common.CommonClient.Game;

public class PingLocationRenderer {
	private PingLocationRenderer() {}

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();
	public static void draw(DrawContext ctx, PingView ping) {
		final var screenPos = ping.getScreenPos();

		if (screenPos == null) {
			return;
		}

		final var m = ctx.getMatrices();
		final var pingScale = ping.getScale();

		m.pushMatrix();
		m.translate(screenPos.x, screenPos.y);
		m.scale(pingScale, pingScale);

		final var typeColor = ping.type.getColor();

		final var author = ping.getPlayerInfo();
		final var compactPlayerInfo = CLIENT_CONFIG.getPlayerInfoMode() == PlayerInfoMode.COMPACT ? author : null;

		final var distanceText = LanguageUtils.UNIT_METERS.get("%,.1f".formatted(ping.getDistance()));
		ctx.renderLabel(distanceText, -1.5f, compactPlayerInfo, typeColor);
		ctx.renderPing(ping.getItemStack(), CLIENT_CONFIG.isItemIconVisible(), typeColor);

		final var isPlayerListHeld = CLIENT_CONFIG.getPlayerInfoMode() == PlayerInfoMode.HOLD && Game.options.keyPlayerList.isDown();
		final var showVerbosePlayerInfo = CLIENT_CONFIG.getPlayerInfoMode() == PlayerInfoMode.ALWAYS || isPlayerListHeld;
		MutableComponent label = ping.type == PingType.LOCATION ? Component.literal("") : ping.type.getLabel();

		if (showVerbosePlayerInfo && author != null) {
			var displayName = PlayerTeam.formatNameForTeam(author.getTeam(), Component.literal(author.getProfile().getName()));
			displayName = displayName.withStyle(ChatFormatting.RESET);
			label = ping.type == PingType.LOCATION
				? displayName
				: Component.literal(label.getString() + " - ").append(displayName);
		}

		if (ping.type != PingType.LOCATION || showVerbosePlayerInfo) {
			ctx.renderLabel(label, 1.75f, showVerbosePlayerInfo ? author : null, typeColor);
		}

		m.popMatrix();
	}
}
