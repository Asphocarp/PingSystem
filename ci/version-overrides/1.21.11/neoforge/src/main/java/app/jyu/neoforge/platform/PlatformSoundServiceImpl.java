package app.jyu.neoforge.platform;

import app.jyu.common.Constants;
import app.jyu.common.platform.IPlatformSoundService;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlatformSoundServiceImpl implements IPlatformSoundService {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> REGISTERED_SOUNDS = new LinkedHashMap<>();
    private static IEventBus modBus;
    private static boolean registeredBus;

    public static void registerModBus(IEventBus eventBus) {
        modBus = eventBus;
    }

    @Override
    public void registerSounds(List<String> soundNames) {
        if (!REGISTERED_SOUNDS.isEmpty()) {
            return;
        }
        if (modBus == null) {
            throw new IllegalStateException("NeoForge mod event bus was not configured before sound registration");
        }

        for (String soundName : soundNames) {
            REGISTERED_SOUNDS.put(soundName, SOUND_EVENTS.register(soundName, () -> SoundEvent.createVariableRangeEvent(Constants.id(soundName))));
        }

        if (!registeredBus) {
            SOUND_EVENTS.register(modBus);
            registeredBus = true;
        }
    }

    @Override
    public SoundEvent sound(String soundName) {
        DeferredHolder<SoundEvent, SoundEvent> soundEvent = REGISTERED_SOUNDS.get(soundName);
        if (soundEvent == null) {
            throw new IllegalArgumentException("Unknown sound: " + soundName);
        }
        return soundEvent.get();
    }
}
