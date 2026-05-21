package app.jyu.forge.platform;

import app.jyu.common.network.IPacket;
import app.jyu.common.platform.IPlatformNetworkService;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.event.EventNetworkChannel;

import java.util.HashMap;
import java.util.Map;

import static app.jyu.common.CommonClient.Game;

public class PlatformNetworkServiceImpl implements IPlatformNetworkService {

	public static final Map<ResourceLocation, EventNetworkChannel> CHANNEL_MAP = new HashMap<>();

	@Override
	public void sendToServer(IPacket packet) {
		var connection = Game.getConnection();

		if (connection == null) {
			return;
		}

		var chan = CHANNEL_MAP.get(packet.getId());

		if (chan == null || !chan.isRemotePresent(connection.getConnection())) {
			return;
		}

		var buf = new FriendlyByteBuf(Unpooled.buffer());
		packet.write(buf);

		connection.send(new ServerboundCustomPayloadPacket(packet.getId(), buf));
	}

	@Override
	public void sendToClient(IPacket packet, ServerPlayer player) {
		var chan = CHANNEL_MAP.get(packet.getId());

		if (chan == null || !chan.isRemotePresent(player.connection.connection)) {
			return;
		}

		var buf = new FriendlyByteBuf(Unpooled.buffer());
		packet.write(buf);

		player.connection.send(new ClientboundCustomPayloadPacket(packet.getId(), buf));
	}
}
