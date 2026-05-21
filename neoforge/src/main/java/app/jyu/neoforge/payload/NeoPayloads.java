package app.jyu.neoforge.payload;

import app.jyu.common.network.PingLocationC2SPacket;
import app.jyu.common.network.PingLocationS2CPacket;
import app.jyu.common.network.UpdateChannelC2SPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class NeoPayloads {
	private NeoPayloads() {}

	public record PingLocationC2S(PingLocationC2SPacket packet) implements CustomPacketPayload {
		public static final Type<PingLocationC2S> TYPE = new Type<>(PingLocationC2SPacket.PACKET_ID);
		public static final StreamCodec<RegistryFriendlyByteBuf, PingLocationC2S> CODEC = StreamCodec.ofMember(PingLocationC2S::write, PingLocationC2S::read);

		private void write(RegistryFriendlyByteBuf buf) {
			packet.write(buf);
		}

		private static PingLocationC2S read(RegistryFriendlyByteBuf buf) {
			return new PingLocationC2S(new PingLocationC2SPacket(buf));
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record PingLocationS2C(PingLocationS2CPacket packet) implements CustomPacketPayload {
		public static final Type<PingLocationS2C> TYPE = new Type<>(PingLocationS2CPacket.PACKET_ID);
		public static final StreamCodec<RegistryFriendlyByteBuf, PingLocationS2C> CODEC = StreamCodec.ofMember(PingLocationS2C::write, PingLocationS2C::read);

		private void write(RegistryFriendlyByteBuf buf) {
			packet.write(buf);
		}

		private static PingLocationS2C read(RegistryFriendlyByteBuf buf) {
			return new PingLocationS2C(new PingLocationS2CPacket(buf));
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record UpdateChannelC2S(UpdateChannelC2SPacket packet) implements CustomPacketPayload {
		public static final Type<UpdateChannelC2S> TYPE = new Type<>(UpdateChannelC2SPacket.PACKET_ID);
		public static final StreamCodec<RegistryFriendlyByteBuf, UpdateChannelC2S> CODEC = StreamCodec.ofMember(UpdateChannelC2S::write, UpdateChannelC2S::read);

		private void write(RegistryFriendlyByteBuf buf) {
			packet.write(buf);
		}

		private static UpdateChannelC2S read(RegistryFriendlyByteBuf buf) {
			return new UpdateChannelC2S(new UpdateChannelC2SPacket(buf));
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
