package app.jyu.neoforge.platform;

import app.jyu.common.platform.IPlatformContextService;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import java.nio.file.Path;

import static app.jyu.common.Global.MOD_ID;

public final class PlatformContextServiceImpl implements IPlatformContextService {
	public static IEventBus modBus;

	@Override
	public String getSelfModVersion() {
		return ModList.get().getModContainerById(MOD_ID)
			.map(container -> container.getModInfo().getVersion().toString())
			.orElse("Unknown");
	}

	@Override
	public Path resolveGameDir(String path) {
		return FMLPaths.GAMEDIR.get().resolve(path);
	}

	@Override
	public Path resolveConfigDir(String path) {
		return FMLPaths.CONFIGDIR.get().resolve(path);
	}

	@Override
	public void registerKeyMapping(KeyMapping keyMapping) {
		if (modBus == null) {
			return;
		}

		modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(keyMapping));
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}
}
