package app.jyu.forge;

import app.jyu.common.CommonServer;
import app.jyu.common.command.ServerCommandBuilder;
import app.jyu.common.network.PingLocationC2SPacket;
import app.jyu.common.network.PingLocationS2CPacket;
import app.jyu.common.network.UpdateChannelC2SPacket;
import app.jyu.common.resource.LanguageUtils;
import app.jyu.forge.platform.PlatformContextServiceImpl;
import app.jyu.forge.platform.PlatformNetworkServiceImpl;
import app.jyu.forge.platform.PlatformSoundServiceImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.Function;

import static app.jyu.common.Global.MOD_ID;

@Mod(MOD_ID)
public class ForgeMain {

	private static final String PROTOCOL_VERSION = "1";
	public static final EventNetworkChannel PING_LOCATION_CHANNEL_C2S = eventChannel(PingLocationC2SPacket.PACKET_ID);
	public static final EventNetworkChannel PING_LOCATION_CHANNEL_S2C = eventChannel(PingLocationS2CPacket.PACKET_ID);
	public static final EventNetworkChannel UPDATE_CHANNEL_C2S = eventChannel(UpdateChannelC2SPacket.PACKET_ID);

	@SuppressWarnings({"java:S1118", "the public constructor is required by forge"})
	public ForgeMain(FMLJavaModLoadingContext context) {
		BusGroup modBus = context.getModBusGroup();
		PlatformSoundServiceImpl.registerModBus(modBus);
		PlatformContextServiceImpl.modBus = modBus;
		CommonServer.INSTANCE.onInit();

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ForgeClient::new);

		PlatformNetworkServiceImpl.CHANNEL_MAP.put(PingLocationC2SPacket.PACKET_ID, PING_LOCATION_CHANNEL_C2S);
		PlatformNetworkServiceImpl.CHANNEL_MAP.put(PingLocationS2CPacket.PACKET_ID, PING_LOCATION_CHANNEL_S2C);
		PlatformNetworkServiceImpl.CHANNEL_MAP.put(UpdateChannelC2SPacket.PACKET_ID, UPDATE_CHANNEL_C2S);
		registerPacketHandler(PING_LOCATION_CHANNEL_C2S, PingLocationC2SPacket::readSafe, CommonServer.INSTANCE::onPingLocationPacket);
		registerPacketHandler(UPDATE_CHANNEL_C2S, UpdateChannelC2SPacket::readSafe, CommonServer.INSTANCE::onChannelUpdatePacket);
		RegisterCommandsEvent.BUS.addListener(ForgeMain::onRegisterCommands);
	}

	public static <T> void registerPacketHandler(EventNetworkChannel channel, Function<FriendlyByteBuf, T> packetReader, TriConsumer<MinecraftServer, ServerPlayer, T> packetHandler) {
		channel.addListener((event) -> {
			var ctx = event.getSource();
			var payload = event.getPayload();
			var sender = ctx.getSender();

			if (payload != null && sender != null) {
				var packet = packetReader.apply(payload);
				ctx.enqueueWork(() -> packetHandler.accept(sender.getServer(), sender, packet));
			}

			ctx.setPacketHandled(true);
		});
	}

	private static EventNetworkChannel eventChannel(net.minecraft.resources.ResourceLocation id) {
		return ChannelBuilder
			.named(id)
			.networkProtocolVersion(Integer.parseInt(PROTOCOL_VERSION))
			.optional()
			.eventNetworkChannel();
	}

	private static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(ServerCommandBuilder.build((context, success, response) -> {
			if (success) {
				context.getSource().sendSuccess(() -> LanguageUtils.withModPrefix(response), false);
			} else {
				context.getSource().sendFailure(LanguageUtils.withModPrefix(response));
			}
		}));
	}
}
