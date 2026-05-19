package app.jyu.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import static app.jyu.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

public record UpdateChannelC2SPacket(String channel) implements IPacket {

	public static final Identifier PACKET_ID = Identifier.fromNamespaceAndPath(app.jyu.common.Global.MOD_ID, "update_channel_c2s");

	public UpdateChannelC2SPacket() {
		this((String)null);
	}

	public UpdateChannelC2SPacket(FriendlyByteBuf buf) {
		this(buf.readUtf(MAX_CHANNEL_LENGTH));
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(channel, MAX_CHANNEL_LENGTH);
	}

	public boolean isCorrupt() {
		return channel == null;
	}

	public Identifier getId() {
		return PACKET_ID;
	}

	public static UpdateChannelC2SPacket readSafe(FriendlyByteBuf buf) {
		return PacketHandler.readSafe(buf, UpdateChannelC2SPacket.class);
	}
}
