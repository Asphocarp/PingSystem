package app.jyu.common.platform;

import app.jyu.common.render.WorldRenderContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface IPlatformClientEventService {

	IPlatformClientEventService INSTANCE = ServiceLoader.load(IPlatformClientEventService.class)
		.findFirst()
		.orElseThrow(() -> new IllegalStateException("No IPlatformClientEventService implementation found!"));

	void registerTickStartEvent(Runnable callback);
	void registerJoinServerEvent(Runnable callback);
	void registerLeaveServerEvent(Runnable callback);
	void registerRenderWorldEvent(Consumer<WorldRenderContext> callback);
	void registerRenderGUIEvent(BiConsumer<GuiGraphicsExtractor, Float> callback);
}
