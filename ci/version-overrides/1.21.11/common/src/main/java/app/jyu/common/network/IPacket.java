package app.jyu.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public interface IPacket {
	void write(FriendlyByteBuf buf);
	boolean isCorrupt();
	Identifier getId();
}
