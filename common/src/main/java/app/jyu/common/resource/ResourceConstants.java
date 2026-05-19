package app.jyu.common.resource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import static app.jyu.common.Global.MOD_ID;

public class ResourceConstants {
	private ResourceConstants() {}

	public static final ResourceLocation PING_SOUND_ID = new ResourceLocation(MOD_ID, "ping");
	public static final SoundEvent PING_SOUND_EVENT = new SoundEvent(PING_SOUND_ID);
	public static final ResourceLocation PING_TEXTURE_ID = new ResourceLocation(MOD_ID, "textures/ping.png");
	public static final ResourceLocation ARROW_TEXTURE_ID = new ResourceLocation(MOD_ID, "textures/arrow.png");
}
