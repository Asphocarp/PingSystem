package app.jyu.forge;

import app.jyu.common.Constants;
import app.jyu.common.PingPoint;
import app.jyu.common.SophisticatedPingClientCommon;
import app.jyu.common.SophisticatedPingCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class ForgeNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Constants.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ForgeNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, PingC2S.class, PingC2S::encode, PingC2S::decode, PingC2S::handle);
        CHANNEL.registerMessage(id++, RemovePingC2S.class, RemovePingC2S::encode, RemovePingC2S::decode, RemovePingC2S::handle);
        CHANNEL.registerMessage(id++, PingS2C.class, PingS2C::encode, PingS2C::decode, PingS2C::handle);
        CHANNEL.registerMessage(id, RemovePingS2C.class, RemovePingS2C::encode, RemovePingS2C::decode, RemovePingS2C::handle);
    }

    public record PingC2S(PingPoint point) {
        static void encode(PingC2S packet, FriendlyByteBuf buf) {
            packet.point.write(buf);
        }

        static PingC2S decode(FriendlyByteBuf buf) {
            return new PingC2S(PingPoint.read(buf));
        }

        static void handle(PingC2S packet, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                ctx.enqueueWork(() -> SophisticatedPingCommon.onPingPacket(sender.server, sender, packet.point));
            }
            ctx.setPacketHandled(true);
        }
    }

    public record RemovePingC2S(PingPoint point) {
        static void encode(RemovePingC2S packet, FriendlyByteBuf buf) {
            packet.point.write(buf);
        }

        static RemovePingC2S decode(FriendlyByteBuf buf) {
            return new RemovePingC2S(PingPoint.read(buf));
        }

        static void handle(RemovePingC2S packet, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                ctx.enqueueWork(() -> SophisticatedPingCommon.onRemovePingPacket(sender, packet.point));
            }
            ctx.setPacketHandled(true);
        }
    }

    public record PingS2C(PingPoint point) {
        static void encode(PingS2C packet, FriendlyByteBuf buf) {
            packet.point.write(buf);
        }

        static PingS2C decode(FriendlyByteBuf buf) {
            return new PingS2C(PingPoint.read(buf));
        }

        static void handle(PingS2C packet, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> SophisticatedPingClientCommon.receivePing(packet.point));
            ctx.setPacketHandled(true);
        }
    }

    public record RemovePingS2C(PingPoint point) {
        static void encode(RemovePingS2C packet, FriendlyByteBuf buf) {
            packet.point.write(buf);
        }

        static RemovePingS2C decode(FriendlyByteBuf buf) {
            return new RemovePingS2C(PingPoint.read(buf));
        }

        static void handle(RemovePingS2C packet, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> SophisticatedPingClientCommon.receiveRemovePing(packet.point));
            ctx.setPacketHandled(true);
        }
    }
}
