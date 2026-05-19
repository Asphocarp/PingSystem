package app.jyu.common.core;

import app.jyu.common.resource.LanguageUtils;
import net.minecraft.network.chat.MutableComponent;

public enum PingType {
	LOCATION(0, 0, 0xFFBFD7FF, "location"),
	ATTACK(1, 1, 0xFFFF4A2F, "attack"),
	DANGER(2, 2, 0xFFFF7A2F, "danger"),
	HELP(3, 3, 0xFF36F55F, "help"),
	GATHER(4, 4, 0xFFFFFFFF, "gather"),
	DEFEND(5, 5, 0xFFB32CFF, "defend"),
	LOOT(6, 6, 0xFFFFE84D, "loot"),
	CONFIRM(7, 7, 0xFF6DFF6D, "confirm");

	private final int wireId;
	private final int radialSlot;
	private final int color;
	private final String key;

	PingType(int wireId, int radialSlot, int color, String key) {
		this.wireId = wireId;
		this.radialSlot = radialSlot;
		this.color = color;
		this.key = key;
	}

	public int getWireId() {
		return wireId;
	}

	public int getRadialSlot() {
		return radialSlot;
	}

	public int getColor() {
		return color;
	}

	public MutableComponent getLabel() {
		return LanguageUtils.of("ping_type", key).get();
	}

	public static PingType fromWireId(int wireId) {
		for (var type : values()) {
			if (type.wireId == wireId) {
				return type;
			}
		}

		return null;
	}

	public static PingType fromRadialSlot(int slot) {
		var normalizedSlot = Math.floorMod(slot, values().length);

		for (var type : values()) {
			if (type.radialSlot == normalizedSlot) {
				return type;
			}
		}

		return LOCATION;
	}
}
