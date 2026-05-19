package app.jyu.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.GuiGraphics;

public interface GuiRenderCallback {
	Event<GuiRenderCallback> START = EventFactory.createArrayBacked(GuiRenderCallback.class, (listeners) -> (guiGraphics, delta) -> {
		for (GuiRenderCallback event : listeners) {
			event.onRenderGui(guiGraphics, delta);
		}
	});

	void onRenderGui(GuiGraphics guiGraphics, float tickDelta);
}
