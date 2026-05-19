package app.jyu.neoforge;

import app.jyu.common.Constants;
import app.jyu.common.PingPoint;
import app.jyu.common.SophisticatedPingCommon;
import app.jyu.neoforge.platform.PlatformContextServiceImpl;
import app.jyu.neoforge.platform.PlatformSoundServiceImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Constants.MOD_ID)
public final class NeoMain {
    private static final StreamCodec<FriendlyByteBuf, PingC2S> PING_C2S_CODEC = StreamCodec.ofMember(PingC2S::write, PingC2S::read);
    private static final StreamCodec<FriendlyByteBuf, RemovePingC2S> REMOVE_PING_C2S_CODEC = StreamCodec.ofMember(RemovePingC2S::write, RemovePingC2S::read);
    private static final StreamCodec<FriendlyByteBuf, PingS2C> PING_S2C_CODEC = StreamCodec.ofMember(PingS2C::write, PingS2C::read);
    private static final StreamCodec<FriendlyByteBuf, RemovePingS2C> REMOVE_PING_S2C_CODEC = StreamCodec.ofMember(RemovePingS2C::write, RemovePingS2C::read);

    public NeoMain(IEventBus modBus) {
        PlatformContextServiceImpl.modBus = modBus;
        PlatformSoundServiceImpl.registerModBus(modBus);
        SophisticatedPingCommon.init();
        modBus.addListener(this::registerPayloads);
        if (FMLEnvironment.dist.isClient()) {
            new NeoClient(modBus);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Constants.MOD_ID).optional();
        registrar.playToServer(PingC2S.TYPE, PING_C2S_CODEC, (payload, context) ->
                context.enqueueWork(() -> SophisticatedPingCommon.onPingPacket(context.player().getServer(), (ServerPlayer) context.player(), payload.point())));
        registrar.playToServer(RemovePingC2S.TYPE, REMOVE_PING_C2S_CODEC, (payload, context) ->
                context.enqueueWork(() -> SophisticatedPingCommon.onRemovePingPacket((ServerPlayer) context.player(), payload.point())));
        registrar.playToClient(PingS2C.TYPE, PING_S2C_CODEC, (payload, context) ->
                context.enqueueWork(() -> NeoClient.receivePing(payload.point())));
        registrar.playToClient(RemovePingS2C.TYPE, REMOVE_PING_S2C_CODEC, (payload, context) ->
                context.enqueueWork(() -> NeoClient.receiveRemovePing(payload.point())));
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
