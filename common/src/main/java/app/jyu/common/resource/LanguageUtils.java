package app.jyu.common.resource;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import app.jyu.common.compat.Component;

import static app.jyu.common.Global.MOD_ID;
import static app.jyu.common.Global.MOD_PREFIX;

public class LanguageUtils {

	public static final MutableComponent VALUE_INFINITE = LanguageUtils.of("value", "infinite").get();
	public static final MutableComponent VALUE_HIDDEN = LanguageUtils.of("value", "hidden").get();
	public static final MutableComponent NEWLINE = Component.literal("\n");
	public static final LanguageUtils UNIT_SECONDS = LanguageUtils.of("unit", "seconds");
	public static final LanguageUtils UNIT_METERS = LanguageUtils.of("unit", "meters");
	public static final LanguageUtils UNIT_PERCENT = LanguageUtils.of("unit", "percent");

	public static LanguageUtils settings(String key) {
		return LanguageUtils.of("settings", key);
	}

	public static LanguageUtils command(String key) {
		return LanguageUtils.of("command", key);
	}

	public static String keyOf(String category, String... path) {
		return LanguageUtils.of(category, path).getKey();
	}

	@Getter
	private String key;

	public static LanguageUtils of(String category, String... path) {
		return new LanguageUtils(String.join(".", category, MOD_ID, String.join(".", path)));
	}

	private LanguageUtils(String key) {
		this.key = key;
	}

	public LanguageUtils path(String... path) {
		return new LanguageUtils(String.join(".", this.key, String.join(".", path)));
	}

	public MutableComponent get(Object... args) {
		return Component.translatable(this.key, args);
	}

	public MutableComponent wrapped() {
		return Component.empty()
			.append(Component.literal("("))
			.append(this.get())
			.append(Component.literal(")"));
	}

	/* Component modifiers */

	public static MutableComponent join(MutableComponent... components) {
		var output = Component.empty();

		for (int i = 0; i < components.length; i++) {
			var component = components[i];

			if (i > 0) {
				output.append(NEWLINE);
			}

			output.append(component);
		}

		return output;
	}

	public static MutableComponent withModPrefix(MutableComponent component) {
		return Component.empty()
			.append(Component.literal(MOD_PREFIX).withStyle(ChatFormatting.DARK_GRAY))
			.append(component);
	}

	public static MutableComponent from(Object any) {
		return Component.literal(String.format("%s", any));
	}
}
