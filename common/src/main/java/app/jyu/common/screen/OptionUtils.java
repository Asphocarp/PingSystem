package app.jyu.common.screen;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class OptionUtils {
	private OptionUtils() {}

	public static OptionInstance<Integer> ofInt(String key, int min, int max, int step, Function<Integer, Component> formatter, Supplier<Integer> getter, Consumer<Integer> setter) {
		final var steps = Math.max(1, (max - min) / step);
		final var values = new OptionInstance.IntRange(0, steps)
			.xmap(value -> min + value * step, value -> Math.max(0, Math.min(steps, Math.round((value - min) / (float)step))));

		return new OptionInstance<>(
			key,
			OptionInstance.noTooltip(),
			(caption, value) -> formatter.apply(value),
			values,
			clampToStep(getter.get(), min, max, step),
			value -> setter.accept(clampToStep(value, min, max, step))
		);
	}

	public static OptionInstance<Float> ofFloat(String key, float min, float max, float step, Function<Float, Component> formatter, Supplier<Float> getter, Consumer<Float> setter) {
		final var steps = Math.max(1, Math.round((max - min) / step));
		final var values = new OptionInstance.IntRange(0, steps)
			.xmap(value -> min + value * step, value -> Math.max(0, Math.min(steps, Math.round((value - min) / step))));

		return new OptionInstance<>(
			key,
			OptionInstance.noTooltip(),
			(caption, value) -> formatter.apply(value),
			values,
			clampToStep(getter.get(), min, max, step),
			value -> setter.accept(clampToStep(value, min, max, step))
		);
	}

	public static OptionInstance<Boolean> ofBool(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return OptionInstance.createBoolean(
			key,
			OptionInstance.noTooltip(),
			getter.get(),
			setter
		);
	}

	public static <E extends Enum<E>> OptionInstance<E> ofEnum(String key, Class<E> enumClass, Function<E, Component> formatter, Function<E, List<FormattedCharSequence>> tooltipSupplier, Supplier<E> getter, Consumer<E> setter) {
		final var values = Arrays.asList(enumClass.getEnumConstants());
		final var codec = Codec.STRING.xmap(
			value -> Enum.valueOf(enumClass, value),
			Enum::name
		);

		return new OptionInstance<>(
			key,
			OptionInstance.noTooltip(),
			(caption, value) -> formatter.apply(value),
			new OptionInstance.Enum<>(values, codec),
			getter.get(),
			setter
		);
	}

	private static int clampToStep(int value, int min, int max, int step) {
		final var clamped = Math.max(min, Math.min(max, value));
		return min + Math.round((clamped - min) / (float)step) * step;
	}

	private static float clampToStep(float value, float min, float max, float step) {
		final var clamped = Math.max(min, Math.min(max, value));
		return min + Math.round((clamped - min) / step) * step;
	}
}
