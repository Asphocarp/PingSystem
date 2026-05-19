package app.jyu.fabric.platform;

import app.jyu.common.Constants;
import app.jyu.common.platform.IPlatformSoundService;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlatformSoundServiceImpl implements IPlatformSoundService {
    private static final Map<String, SoundEvent> SOUND_EVENTS = new LinkedHashMap<>();

    @Override
    public void registerSounds(List<String> soundNames) {
        if (!SOUND_EVENTS.isEmpty()) {
            return;
        }

        for (String soundName : soundNames) {
            var soundId = Constants.id(soundName);
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
            Registry.register(BuiltInRegistries.SOUND_EVENT, soundId, soundEvent);
            SOUND_EVENTS.put(soundName, soundEvent);
        }
    }

    @Override
    public SoundEvent sound(String soundName) {
        SoundEvent soundEvent = SOUND_EVENTS.get(soundName);
        if (soundEvent == null) {
            throw new IllegalArgumentException("Unknown sound: " + soundName);
        }
        return soundEvent;
    }
}
