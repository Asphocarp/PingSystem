package app.jyu.common.render;

import app.jyu.common.core.PingType;
import app.jyu.common.core.PingWheelController;
import app.jyu.common.resource.LanguageUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;

import static app.jyu.common.CommonClient.Game;

public class PingWheelRenderer {
	private PingWheelRenderer() {}

	private static final int OVERLAY_COLOR = 0xAA000000;
	private static final int LINE_COLOR = 0xB0FFFFFF;
	private static final int MUTED_LINE_COLOR = 0x70FFFFFF;
	private static final int SELECTED_BACKDROP = 0x44FFFFFF;
	private static final int CENTER_TEXT_COLOR = 0xFFD8D8D8;
	private static final float RADIUS = 116f;
	private static final float INNER_RADIUS = 34f;

	public static void draw(PoseStack matrices) {
		if (!PingWheelController.isOpen()) {
			return;
		}

		final var window = Game.getWindow();
		final var width = window.getGuiScaledWidth();
		final var height = window.getGuiScaledHeight();
		final var centerX = width / 2;
		final var centerY = height / 2;
		final var selected = PingWheelController.getSelectedType();

		RenderSystem.enableBlend();
		GuiComponent.fill(matrices, 0, 0, width, height, OVERLAY_COLOR);

		drawLines(matrices, centerX, centerY);
		drawTypes(matrices, centerX, centerY, selected);
		drawCenteredText(matrices, LanguageUtils.of("ping_wheel", "center").get().getString(), centerX, centerY - 4, CENTER_TEXT_COLOR);
		RenderSystem.disableBlend();
	}

	private static void drawLines(PoseStack matrices, int centerX, int centerY) {
		GuiComponent.fill(matrices, centerX - 1, centerY - (int)RADIUS, centerX + 1, centerY - (int)INNER_RADIUS, LINE_COLOR);
		GuiComponent.fill(matrices, centerX - 1, centerY + (int)INNER_RADIUS, centerX + 1, centerY + (int)RADIUS, LINE_COLOR);
		GuiComponent.fill(matrices, centerX - (int)RADIUS, centerY - 1, centerX - (int)INNER_RADIUS, centerY + 1, LINE_COLOR);
		GuiComponent.fill(matrices, centerX + (int)INNER_RADIUS, centerY - 1, centerX + (int)RADIUS, centerY + 1, LINE_COLOR);

		drawDiagonalLine(matrices, centerX, centerY, -1, -1);
		drawDiagonalLine(matrices, centerX, centerY, 1, -1);
		drawDiagonalLine(matrices, centerX, centerY, -1, 1);
		drawDiagonalLine(matrices, centerX, centerY, 1, 1);
	}

	private static void drawDiagonalLine(PoseStack matrices, int centerX, int centerY, int xSign, int ySign) {
		for (int i = (int)INNER_RADIUS; i < RADIUS; i += 2) {
			final var x = centerX + xSign * i;
			final var y = centerY + ySign * i;
			GuiComponent.fill(matrices, x, y, x + 2, y + 2, MUTED_LINE_COLOR);
		}
	}

	private static void drawTypes(PoseStack matrices, int centerX, int centerY, PingType selected) {
		for (var type : PingType.values()) {
			final var angle = type.getRadialSlot() * (Math.PI / 4.0);
			final var labelX = centerX + Math.cos(angle) * RADIUS;
			final var labelY = centerY + Math.sin(angle) * RADIUS * 0.74;
			final var markerX = centerX + Math.cos(angle) * (RADIUS - 36);
			final var markerY = centerY + Math.sin(angle) * (RADIUS - 36) * 0.74;

			if (type == selected) {
				GuiComponent.fill(matrices, (int)labelX - 42, (int)labelY - 11, (int)labelX + 42, (int)labelY + 21, SELECTED_BACKDROP);
			}

			drawCenteredText(matrices, type.getLabel().getString(), (int)labelX, (int)labelY - 8, type.getColor());
			GuiComponent.fill(matrices, (int)markerX - 3, (int)markerY - 3, (int)markerX + 3, (int)markerY + 3, type.getColor());
		}
	}

	private static void drawCenteredText(PoseStack matrices, String text, int centerX, int y, int color) {
		Game.font.drawShadow(matrices, text, centerX - Game.font.width(text) / 2f, y, color);
	}
}
