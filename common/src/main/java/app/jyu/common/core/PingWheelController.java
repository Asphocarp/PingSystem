package app.jyu.common.core;

import app.jyu.common.config.ClientConfig;
import app.jyu.common.util.InputUtils;
import lombok.Getter;

import static app.jyu.common.CommonClient.Game;

public class PingWheelController {
	private PingWheelController() {}

	private static boolean wasDown = false;
	private static long pressStartedAt = 0L;

	@Getter
	private static boolean open = false;
	@Getter
	private static PingType selectedType = PingType.LOCATION;

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
			selectedType = PingType.LOCATION;
		}

		if (isDown && !open && isHoldElapsed(now - pressStartedAt, config.getWheelHoldMillis())) {
			open = true;
		}

		if (isDown && open) {
			selectedType = selectTypeFromMouse(selectedType);
		}

		if (!isDown && wasDown) {
			var type = open ? selectedType : PingType.LOCATION;
			cancel();
			PingController.queuePingAction(type);
		}

		wasDown = isDown;
	}

	public static void cancel() {
		open = false;
		selectedType = PingType.LOCATION;
		pressStartedAt = 0L;
	}

	public static boolean isHoldElapsed(long heldMillis, long holdMillis) {
		return heldMillis >= Math.max(0L, holdMillis);
	}

	public static PingType selectType(double mouseX, double mouseY, int width, int height, double deadZone, PingType current) {
		final var centerX = width * 0.5;
		final var centerY = height * 0.5;
		final var dx = mouseX - centerX;
		final var dy = mouseY - centerY;
		final var distance = Math.sqrt(dx * dx + dy * dy);

		if (distance < deadZone) {
			return current;
		}

		final var angle = Math.atan2(dy, dx);
		final var normalized = (angle + Math.PI * 2.0 + Math.PI / 8.0) % (Math.PI * 2.0);
		final var slot = (int)Math.floor(normalized / (Math.PI / 4.0));

		return PingType.fromRadialSlot(slot);
	}

	private static PingType selectTypeFromMouse(PingType current) {
		final var config = config();
		final var window = Game.getWindow();
		final var scale = window.getGuiScale();
		final var mouseX = Game.mouseHandler.xpos() / scale;
		final var mouseY = Game.mouseHandler.ypos() / scale;

		return selectType(
			mouseX,
			mouseY,
			window.getGuiScaledWidth(),
			window.getGuiScaledHeight(),
			config.getWheelDeadZone() / config.getWheelMouseSensitivity(),
			current
		);
	}

	private static ClientConfig config() {
		return ClientConfig.HANDLER.getConfig();
	}
}
