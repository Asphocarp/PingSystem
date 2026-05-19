package app.jyu.fabric.platform;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import app.jyu.common.platform.IPlatformContextService;

import java.nio.file.Path;

import static app.jyu.common.Global.MOD_ID;

public class PlatformContextServiceImpl implements IPlatformContextService {

	@Override
	public String getSelfModVersion() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("Unknown");
	}

	@Override
	public Path resolveGameDir(String path) {
		return FabricLoader.getInstance().getGameDir().resolve(path);
	}

	@Override
	public Path resolveConfigDir(String path) {
		return FabricLoader.getInstance().getConfigDir().resolve(path);
	}

	@Override
	public void registerKeyMapping(KeyMapping keyMapping) {
		KeyBindingHelper.registerKeyBinding(keyMapping);
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}
}
