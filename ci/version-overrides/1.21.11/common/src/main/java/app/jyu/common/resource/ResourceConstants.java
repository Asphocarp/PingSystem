package app.jyu.common.resource;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import static app.jyu.common.Global.MOD_ID;

public class ResourceConstants {
	private ResourceConstants() {}

	public static final Identifier PING_SOUND_ID = Identifier.fromNamespaceAndPath(MOD_ID, "ping");
	public static final SoundEvent PING_SOUND_EVENT = SoundEvent.createVariableRangeEvent(PING_SOUND_ID);
	public static final Identifier PING_TEXTURE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "textures/ping.png");
	public static final Identifier ARROW_TEXTURE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "textures/arrow.png");
}
