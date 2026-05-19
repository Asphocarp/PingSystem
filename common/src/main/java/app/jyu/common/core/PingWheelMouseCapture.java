package app.jyu.common.core;

import static app.jyu.common.CommonClient.Game;

public class PingWheelMouseCapture {
	private PingWheelMouseCapture() {}

	private static boolean releasedByWheel = false;

	public static void open() {
		if (Game == null || Game.mouseHandler == null || !Game.mouseHandler.isMouseGrabbed()) {
			return;
		}

		Game.mouseHandler.releaseMouse();
		releasedByWheel = true;
	}

	public static void close(boolean allowRegrab) {
		final var shouldRegrab = shouldRegrabMouse(
			releasedByWheel,
			allowRegrab,
			Game != null && Game.screen == null,
			Game != null && Game.isWindowActive()
		);

		releasedByWheel = false;

		if (!shouldRegrab || Game == null || Game.mouseHandler == null) {
			return;
		}

		Game.mouseHandler.grabMouse();
		Game.mouseHandler.setIgnoreFirstMove();
	}

	public static boolean shouldRegrabMouse(boolean released, boolean allowRegrab, boolean screenAbsent, boolean windowActive) {
		return released && allowRegrab && screenAbsent && windowActive;
	}
}
