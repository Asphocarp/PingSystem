package app.jyu.forge.event;

import app.jyu.common.render.WorldRenderContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class WorldRenderCallback {
	private static final List<Consumer<WorldRenderContext>> CALLBACKS = new CopyOnWriteArrayList<>();

	private WorldRenderCallback() {}

	public static void register(Consumer<WorldRenderContext> callback) {
		CALLBACKS.add(callback);
	}

	public static void fire(WorldRenderContext context) {
		for (var callback : CALLBACKS) {
			callback.accept(context);
		}
	}
}
