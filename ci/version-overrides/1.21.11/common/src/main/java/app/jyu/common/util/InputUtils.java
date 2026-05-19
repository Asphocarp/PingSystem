package app.jyu.common.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.phys.HitResult;
import app.jyu.common.resource.LanguageUtils;
import org.lwjgl.glfw.GLFW;

import static app.jyu.common.CommonClient.Game;
import static app.jyu.common.Global.MOD_ID;

public class InputUtils {
	InputUtils() {}

	private static final KeyMapping.Category SETTINGS_CATEGORY = KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "settings"));
	public static final KeyMapping KEY_BINDING_PING = new KeyMapping(LanguageUtils.keyOf("key", "ping_location"), InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, SETTINGS_CATEGORY);
	public static final KeyMapping KEY_BINDING_SETTINGS = new KeyMapping(LanguageUtils.keyOf("key", "open_settings"), InputConstants.Type.KEYSYM, -1, SETTINGS_CATEGORY);

	public static boolean consumePingHotkey() {
		if (!KEY_BINDING_PING.same(Game.options.keyPickItem)) {
			return KEY_BINDING_PING.consumeClick();
		}

		if (Game.player == null || Game.hitResult == null) {
			return false;
		}

		var isMiss = Game.hitResult.getType() == HitResult.Type.MISS || (!Game.player.isCreative() && Game.hitResult.getType() == HitResult.Type.ENTITY);

		return isMiss && Game.options.keyPickItem.consumeClick();
	}

	public static boolean isPingKeyDown() {
		if (!KEY_BINDING_PING.same(Game.options.keyPickItem)) {
			return KEY_BINDING_PING.isDown();
		}

		if (Game.player == null || Game.hitResult == null) {
			return false;
		}

		var isMiss = Game.hitResult.getType() == HitResult.Type.MISS || (!Game.player.isCreative() && Game.hitResult.getType() == HitResult.Type.ENTITY);

		return isMiss && Game.options.keyPickItem.isDown();
	}
}
