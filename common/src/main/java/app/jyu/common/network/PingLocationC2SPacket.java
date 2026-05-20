package app.jyu.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import app.jyu.common.core.PingType;

import java.util.UUID;

import static app.jyu.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

public record PingLocationC2SPacket(String channel, Vec3 pos, UUID entity, int sequence, int dimension, PingType type) implements IPacket {

	public static final Identifier PACKET_ID = Identifier.fromNamespaceAndPath(app.jyu.common.Global.MOD_ID, "ping_location_c2s");

	public PingLocationC2SPacket() {
		this(null, null, null, 0, 0, null);
	}

	public PingLocationC2SPacket(FriendlyByteBuf buf) {
		this(
			buf.readUtf(MAX_CHANNEL_LENGTH),
			new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
			buf.readBoolean() ? buf.readUUID() : null,
			buf.readInt(),
			buf.readInt(),
			PingType.fromWireId(buf.readVarInt())
		);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(channel, MAX_CHANNEL_LENGTH);
		buf.writeDouble(pos.x);
		buf.writeDouble(pos.y);
		buf.writeDouble(pos.z);
		buf.writeBoolean(entity != null);

		if (entity != null) {
			buf.writeUUID(entity);
		}

		buf.writeInt(sequence);
		buf.writeInt(dimension);
		buf.writeVarInt(type.getWireId());
	}

	public boolean isCorrupt() {
		return channel == null || pos == null || type == null;
	}

	public Identifier getId() {
		return PACKET_ID;
	}

	public static PingLocationC2SPacket readSafe(FriendlyByteBuf buf) {
		return PacketHandler.readSafe(buf, PingLocationC2SPacket.class);
	}
}
