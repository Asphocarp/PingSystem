package app.jyu.forge.platform;

import app.jyu.common.Global;
import app.jyu.common.platform.IPlatformSoundService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlatformSoundServiceImpl implements IPlatformSoundService {
	private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Global.MOD_ID);
	private static final Map<String, RegistryObject<SoundEvent>> REGISTERED_SOUNDS = new LinkedHashMap<>();
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
			throw new IllegalStateException("Forge mod event bus was not configured before sound registration");
		}

		for (String soundName : soundNames) {
			REGISTERED_SOUNDS.put(soundName, SOUND_EVENTS.register(
				soundName,
				() -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Global.MOD_ID, soundName))
			));
		}

		if (!registeredBus) {
			SOUND_EVENTS.register(modBus);
			registeredBus = true;
		}
	}

	@Override
	public SoundEvent sound(String soundName) {
		var soundEvent = REGISTERED_SOUNDS.get(soundName);
		if (soundEvent == null) {
			throw new IllegalArgumentException("Unknown sound: " + soundName);
		}
		return soundEvent.get();
	}
}
