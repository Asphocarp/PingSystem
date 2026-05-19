package app.jyu.fabric;

import app.jyu.common.Constants;
import app.jyu.common.PingPoint;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FabricPayloads {
    static final StreamCodec<FriendlyByteBuf, PingC2S> PING_C2S_CODEC = StreamCodec.ofMember(PingC2S::write, PingC2S::read);
    static final StreamCodec<FriendlyByteBuf, RemovePingC2S> REMOVE_PING_C2S_CODEC = StreamCodec.ofMember(RemovePingC2S::write, RemovePingC2S::read);
    static final StreamCodec<FriendlyByteBuf, PingS2C> PING_S2C_CODEC = StreamCodec.ofMember(PingS2C::write, PingS2C::read);
    static final StreamCodec<FriendlyByteBuf, RemovePingS2C> REMOVE_PING_S2C_CODEC = StreamCodec.ofMember(RemovePingS2C::write, RemovePingS2C::read);

    private FabricPayloads() {
    }

    static void register() {
        PayloadTypeRegistry.serverboundPlay().register(PingC2S.TYPE, PING_C2S_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RemovePingC2S.TYPE, REMOVE_PING_C2S_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PingS2C.TYPE, PING_S2C_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RemovePingS2C.TYPE, REMOVE_PING_S2C_CODEC);
    }

    public record PingC2S(PingPoint point) implements CustomPacketPayload {
        public static final Type<PingC2S> TYPE = new Type<>(Constants.PING_PACKET);

        private void write(FriendlyByteBuf buf) {
            point.write(buf);
        }

        private static PingC2S read(FriendlyByteBuf buf) {
            return new PingC2S(PingPoint.read(buf));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RemovePingC2S(PingPoint point) implements CustomPacketPayload {
        public static final Type<RemovePingC2S> TYPE = new Type<>(Constants.REMOVE_PING_PACKET);

        private void write(FriendlyByteBuf buf) {
            point.write(buf);
        }

        private static RemovePingC2S read(FriendlyByteBuf buf) {
            return new RemovePingC2S(PingPoint.read(buf));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PingS2C(PingPoint point) implements CustomPacketPayload {
        public static final Type<PingS2C> TYPE = new Type<>(Constants.PING_PACKET);

        private void write(FriendlyByteBuf buf) {
            point.write(buf);
        }

        private static PingS2C read(FriendlyByteBuf buf) {
            return new PingS2C(PingPoint.read(buf));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RemovePingS2C(PingPoint point) implements CustomPacketPayload {
        public static final Type<RemovePingS2C> TYPE = new Type<>(Constants.REMOVE_PING_PACKET);

        private void write(FriendlyByteBuf buf) {
            point.write(buf);
        }

        private static RemovePingS2C read(FriendlyByteBuf buf) {
            return new RemovePingS2C(PingPoint.read(buf));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
