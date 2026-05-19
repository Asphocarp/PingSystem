package app.jyu.common.compat;

import net.minecraft.network.chat.MutableComponent;

public interface Component {

	static MutableComponent literal(String text) {
		return net.minecraft.network.chat.Component.literal(text);
	}

	static MutableComponent empty() {
		return net.minecraft.network.chat.Component.empty();
	}

	static MutableComponent translatable(String key, Object... args) {
		return net.minecraft.network.chat.Component.translatable(key, args);
	}
}
