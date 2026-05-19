package app.jyu.common.platform;

import net.minecraft.sounds.SoundEvent;

import java.util.List;
import java.util.ServiceLoader;

public interface IPlatformSoundService {
    IPlatformSoundService INSTANCE = ServiceLoader.load(IPlatformSoundService.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No platform sound service found"));

    void registerSounds(List<String> soundNames);

    SoundEvent sound(String soundName);
}
