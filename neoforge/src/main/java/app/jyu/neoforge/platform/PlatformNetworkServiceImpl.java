package app.jyu.neoforge.platform;

import app.jyu.common.network.IPacket;
import app.jyu.common.network.PingLocationC2SPacket;
import app.jyu.common.network.PingLocationS2CPacket;
import app.jyu.common.network.UpdateChannelC2SPacket;
import app.jyu.common.platform.IPlatformNetworkService;
import app.jyu.neoforge.payload.NeoPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlatformNetworkServiceImpl implements IPlatformNetworkService {
	@Override
	public void sendToServer(IPacket packet) {
		if (packet instanceof PingLocationC2SPacket pingPacket) {
			PacketDistributor.sendToServer(new NeoPayloads.PingLocationC2S(pingPacket));
			return;
		}

		if (packet instanceof UpdateChannelC2SPacket channelPacket) {
			PacketDistributor.sendToServer(new NeoPayloads.UpdateChannelC2S(channelPacket));
		}
	}

	@Override
	public void sendToClient(IPacket packet, ServerPlayer player) {
		if (packet instanceof PingLocationS2CPacket pingPacket) {
			PacketDistributor.sendToPlayer(player, new NeoPayloads.PingLocationS2C(pingPacket));
		}
	}
}
