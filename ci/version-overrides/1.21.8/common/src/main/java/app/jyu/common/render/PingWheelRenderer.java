package app.jyu.common.render;

import app.jyu.common.resource.LanguageUtils;
import app.jyu.common.core.PingType;
import app.jyu.common.core.PingWheelController;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;
import org.jetbrains.annotations.Nullable;

import static app.jyu.common.CommonClient.Game;

public class PingWheelRenderer {
	private PingWheelRenderer() {}

	private static final int CENTER_TEXT_COLOR = 0xFFD8D8D8;
	private static final int ICON_SELECTION_COLOR = 0xCCFFFFFF;
	private static final float ICON_RING_RADIUS = 92f;
	private static final float ICON_SCALE = 2.2f;
	private static final int CENTER_TEXT_Y_OFFSET = -22;

	public static void draw(GuiGraphics guiGraphics) {
		if (!PingWheelController.isOpen()) {
			return;
		}

		final var window = Game.getWindow();
		final var width = window.getGuiScaledWidth();
		final var height = window.getGuiScaledHeight();
		final var centerX = width / 2;
		final var centerY = height / 2;
		final var selected = PingWheelController.getSelectedType();

		drawTypes(guiGraphics, centerX, centerY, selected);
		drawCenteredText(guiGraphics, LanguageUtils.of("ping_wheel", "center").get().getString(), centerX, centerY + CENTER_TEXT_Y_OFFSET, CENTER_TEXT_COLOR);
	}

	private static void drawTypes(GuiGraphics guiGraphics, int centerX, int centerY, @Nullable PingType selected) {
		for (var type : PingType.values()) {
			final var angle = type.getRadialSlot() * (Math.PI / 4.0);
			final var iconX = centerX + Math.cos(angle) * ICON_RING_RADIUS;
			final var iconY = centerY + Math.sin(angle) * ICON_RING_RADIUS;

			drawIcon(guiGraphics, type, (int)iconX, (int)iconY, type == selected);
		}
	}

	private static void drawCenteredText(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
		guiGraphics.drawString(Game.font, text, centerX - Game.font.width(text) / 2, y, color, true);
	}

	private static void drawIcon(GuiGraphics guiGraphics, PingType type, int centerX, int centerY, boolean selected) {
		if (selected) {
			drawSelectionCorners(guiGraphics, centerX, centerY);
		}

		drawScaledCenteredText(guiGraphics, type.getIcon(), centerX, centerY, ICON_SCALE, type.getColor());
	}

	private static void drawScaledCenteredText(GuiGraphics guiGraphics, String text, int centerX, int centerY, float scale, int color) {
		final var width = Game.font.width(text);
		final Matrix3x2fStack matrices = guiGraphics.pose();
		matrices.pushMatrix();
		matrices.translate(centerX, centerY);
		matrices.scale(scale, scale);
		guiGraphics.drawString(Game.font, text, (int)(-width / 2f), -4, color, true);
		matrices.popMatrix();
	}

	private static void drawSelectionCorners(GuiGraphics guiGraphics, int centerX, int centerY) {
		final var left = centerX - 12;
		final var right = centerX + 12;
		final var top = centerY - 12;
		final var bottom = centerY + 12;

		guiGraphics.fill(left, top, left + 7, top + 2, ICON_SELECTION_COLOR);
		guiGraphics.fill(left, top, left + 2, top + 7, ICON_SELECTION_COLOR);
		guiGraphics.fill(right - 7, top, right, top + 2, ICON_SELECTION_COLOR);
		guiGraphics.fill(right - 2, top, right, top + 7, ICON_SELECTION_COLOR);
		guiGraphics.fill(left, bottom - 2, left + 7, bottom, ICON_SELECTION_COLOR);
		guiGraphics.fill(left, bottom - 7, left + 2, bottom, ICON_SELECTION_COLOR);
		guiGraphics.fill(right - 7, bottom - 2, right, bottom, ICON_SELECTION_COLOR);
		guiGraphics.fill(right - 2, bottom - 7, right, bottom, ICON_SELECTION_COLOR);
	}

}
