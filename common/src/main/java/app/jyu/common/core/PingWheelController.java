package app.jyu.common.core;

import app.jyu.common.config.ClientConfig;
import app.jyu.common.util.InputUtils;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import static app.jyu.common.CommonClient.Game;

public class PingWheelController {
	private PingWheelController() {}

	private static boolean wasDown = false;
	private static long pressStartedAt = 0L;

	@Getter
	private static boolean open = false;
	@Getter
	private static @Nullable PingType selectedType = null;

	public static void tick() {
		if (Game == null || Game.screen != null || Game.player == null || Game.level == null) {
			cancel();
			wasDown = false;
			return;
		}

		final var isDown = InputUtils.isPingKeyDown();
		final var now = System.currentTimeMillis();
		final var config = config();

		if (isDown && !wasDown) {
			pressStartedAt = now;
			selectedType = null;
			PingWheelMouseCapture.open();
		}

		if (isDown) {
			var mouseSelectedType = selectTypeFromMouse();
			if (!open && shouldOpenWheel(now - pressStartedAt, config.getWheelHoldMillis(), mouseSelectedType != null)) {
				open = true;
			}

			if (open) {
				selectedType = mouseSelectedType;
			}
		}

		if (!isDown && wasDown) {
			var type = open && selectedType != null ? selectedType : PingType.LOCATION;
			cancel(true);
			PingController.queuePingAction(type);
		}

		wasDown = isDown;
	}

	public static void cancel() {
		cancel(true);
	}

	public static void cancelForScreenOpen() {
		cancel(false);
	}

	private static void cancel(boolean allowRegrab) {
		PingWheelMouseCapture.close(allowRegrab);
		open = false;
		selectedType = null;
		pressStartedAt = 0L;
	}

	public static boolean isHoldElapsed(long heldMillis, long holdMillis) {
		return heldMillis >= Math.max(0L, holdMillis);
	}

	public static boolean shouldOpenWheel(long heldMillis, long holdMillis, boolean outsideDeadZone) {
		return outsideDeadZone || isHoldElapsed(heldMillis, holdMillis);
	}

	public static @Nullable PingType selectType(double mouseX, double mouseY, int width, int height, double deadZone) {
		final var centerX = width * 0.5;
		final var centerY = height * 0.5;
		final var dx = mouseX - centerX;
		final var dy = mouseY - centerY;
		final var distance = Math.sqrt(dx * dx + dy * dy);

		if (distance < deadZone) {
			return null;
		}

		final var angle = Math.atan2(dy, dx);
		final var normalized = (angle + Math.PI * 2.0 + Math.PI / 8.0) % (Math.PI * 2.0);
		final var slot = (int)Math.floor(normalized / (Math.PI / 4.0));

		return PingType.fromRadialSlot(slot);
	}

	public static double toGuiCoordinate(double cursorPosition, int mouseCoordinateSpaceSize, int guiScaledSize) {
		if (mouseCoordinateSpaceSize <= 0) {
			return cursorPosition;
		}

		return cursorPosition * guiScaledSize / mouseCoordinateSpaceSize;
	}

	private static @Nullable PingType selectTypeFromMouse() {
		final var config = config();
		final var window = Game.getWindow();
		final var mouseX = toGuiCoordinate(Game.mouseHandler.xpos(), window.getScreenWidth(), window.getGuiScaledWidth());
		final var mouseY = toGuiCoordinate(Game.mouseHandler.ypos(), window.getScreenHeight(), window.getGuiScaledHeight());

		return selectType(
			mouseX,
			mouseY,
			window.getGuiScaledWidth(),
			window.getGuiScaledHeight(),
			config.getWheelDeadZone() / config.getWheelMouseSensitivity()
		);
	}

	private static ClientConfig config() {
		return ClientConfig.HANDLER.getConfig();
	}
}
