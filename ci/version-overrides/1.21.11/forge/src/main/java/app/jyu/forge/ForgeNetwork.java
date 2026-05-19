package app.jyu.forge;

import app.jyu.common.Constants;
import app.jyu.common.PingPoint;
import app.jyu.common.SophisticatedPingClientCommon;
import app.jyu.common.SophisticatedPingCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public final class ForgeNetwork {
    private static final int PROTOCOL_VERSION = 1;
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Constants.id("main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .acceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .simpleChannel();

    private ForgeNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(PingC2S.class, id++).encoder(PingC2S::encode).decoder(PingC2S::decode).consumerMainThread(PingC2S::handle).add();
        CHANNEL.messageBuilder(RemovePingC2S.class, id++).encoder(RemovePingC2S::encode).decoder(RemovePingC2S::decode).consumerMainThread(RemovePingC2S::handle).add();
        CHANNEL.messageBuilder(PingS2C.class, id++).encoder(PingS2C::encode).decoder(PingS2C::decode).consumerMainThread(PingS2C::handle).add();
        CHANNEL.messageBuilder(RemovePingS2C.class, id).encoder(RemovePingS2C::encode).decoder(RemovePingS2C::decode).consumerMainThread(RemovePingS2C::handle).add();
    }

    public record PingC2S(PingPoint point) {
        static void encode(PingC2S packet, FriendlyByteBuf buf) {
            packet.point.write(buf);
        }

        static PingC2S decode(FriendlyByteBuf buf) {
            return new PingC2S(PingPoint.read(buf));
        }

        static void handle(PingC2S packet, CustomPayloadEvent.Context ctx) {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                ctx.enqueueWork(() -> SophisticatedPingCommon.onPingPacket(((net.minecraft.server.level.ServerLevel) sender.level()).getServer(), sender, packet.point));
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

        static void handle(RemovePingC2S packet, CustomPayloadEvent.Context ctx) {
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

        static void handle(PingS2C packet, CustomPayloadEvent.Context ctx) {
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

        static void handle(RemovePingS2C packet, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> SophisticatedPingClientCommon.receiveRemovePing(packet.point));
            ctx.setPacketHandled(true);
        }
    }
}
