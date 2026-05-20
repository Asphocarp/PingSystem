package app.jyu.forge.platform;

import app.jyu.common.platform.IPlatformContextService;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

import static app.jyu.common.Global.MOD_ID;

public class PlatformContextServiceImpl implements IPlatformContextService {
	public static BusGroup modBus;

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
		if (modBus != null) {
			RegisterKeyMappingsEvent.getBus(modBus).addListener(event -> event.register(keyMapping));
		}
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}
}
