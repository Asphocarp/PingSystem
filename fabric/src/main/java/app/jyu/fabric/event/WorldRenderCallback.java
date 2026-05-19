package app.jyu.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import app.jyu.common.render.WorldRenderContext;

public interface WorldRenderCallback {

	Event<WorldRenderCallback> START = EventFactory.createArrayBacked(WorldRenderCallback.class, (listeners) -> (worldRenderContext) -> {
		for (WorldRenderCallback event : listeners) {
			event.onRenderWorld(worldRenderContext);
		}
	});

	void onRenderWorld(WorldRenderContext worldRenderContext);
}
