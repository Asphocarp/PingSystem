package app.jyu.fabric.platform;

import app.jyu.common.network.IPacket;
import app.jyu.common.network.PingLocationC2SPacket;
import app.jyu.common.network.PingLocationS2CPacket;
import app.jyu.common.network.UpdateChannelC2SPacket;
import app.jyu.common.platform.IPlatformNetworkService;
import app.jyu.fabric.payload.FabricPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class PlatformNetworkServiceImpl implements IPlatformNetworkService {
	@Override
	public void sendToServer(IPacket packet) {
		if (packet instanceof PingLocationC2SPacket pingPacket) {
			if (ClientPlayNetworking.canSend(FabricPayloads.PingLocationC2S.TYPE)) {
				ClientPlayNetworking.send(new FabricPayloads.PingLocationC2S(pingPacket));
			}
			return;
		}

		if (packet instanceof UpdateChannelC2SPacket channelPacket && ClientPlayNetworking.canSend(FabricPayloads.UpdateChannelC2S.TYPE)) {
			ClientPlayNetworking.send(new FabricPayloads.UpdateChannelC2S(channelPacket));
		}
	}

	@Override
	public void sendToClient(IPacket packet, ServerPlayer player) {
		if (!(packet instanceof PingLocationS2CPacket pingPacket)) {
			return;
		}

		if (!ServerPlayNetworking.canSend(player, FabricPayloads.PingLocationS2C.TYPE)) {
			return;
		}

		ServerPlayNetworking.send(player, new FabricPayloads.PingLocationS2C(pingPacket));
	}
}
