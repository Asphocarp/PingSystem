package app.jyu.forge.platform;

import app.jyu.common.Constants;
import app.jyu.common.platform.IPlatformSoundService;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlatformSoundServiceImpl implements IPlatformSoundService {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Constants.FORGE_MOD_ID);
    private static final Map<String, RegistryObject<SoundEvent>> REGISTERED_SOUNDS = new LinkedHashMap<>();
    private static BusGroup modBusGroup;
    private static boolean registeredBus;

    public static void registerModBusGroup(BusGroup busGroup) {
        modBusGroup = busGroup;
    }

    @Override
    public void registerSounds(List<String> soundNames) {
        if (!REGISTERED_SOUNDS.isEmpty()) {
            return;
        }
        if (modBusGroup == null) {
            throw new IllegalStateException("Forge mod bus group was not configured before sound registration");
        }

        for (String soundName : soundNames) {
            REGISTERED_SOUNDS.put(soundName, SOUND_EVENTS.register(soundName, () -> SoundEvent.createVariableRangeEvent(Constants.id(soundName))));
        }

        if (!registeredBus) {
            SOUND_EVENTS.register(modBusGroup);
            registeredBus = true;
        }
    }

    @Override
    public SoundEvent sound(String soundName) {
        RegistryObject<SoundEvent> soundEvent = REGISTERED_SOUNDS.get(soundName);
        if (soundEvent == null) {
            throw new IllegalArgumentException("Unknown sound: " + soundName);
        }
        return soundEvent.get();
    }
}
