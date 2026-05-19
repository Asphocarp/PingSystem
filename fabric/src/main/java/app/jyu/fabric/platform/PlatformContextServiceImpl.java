package app.jyu.fabric.platform;

import app.jyu.common.platform.IPlatformContextService;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

import java.nio.file.Path;

public final class PlatformContextServiceImpl implements IPlatformContextService {
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
