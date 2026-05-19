package app.jyu.common.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import app.jyu.common.core.GameContext;
import app.jyu.common.network.UpdateChannelC2SPacket;
import app.jyu.common.platform.IPlatformNetworkService;

import java.util.HashMap;
import java.util.Map;

import static app.jyu.common.CommonClient.Game;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ClientConfig implements IConfig {
	int pingVolume = 100;
	int pingDuration = 7;
	int pingDistance = 2048;
	float correctionPeriod = 1f;
	boolean itemIconVisible = true;
	boolean directionIndicatorVisible = true;
	PlayerInfoMode playerInfoMode = PlayerInfoMode.HOLD;
	TeamColorMode teamColorMode = TeamColorMode.FULL;
	int pingSize = 100;
	String channel = "";
	Map<String, String> serverChannels = new HashMap<>();

	// hidden from the settings screen
	int removeRadius = 10;
	int raycastDistance = 1024;
	int safeZoneLeft = 5;
	int safeZoneRight = 5;
	int safeZoneTop = 5;
	int safeZoneBottom = 60;
	int wheelHoldMillis = 180;
	int wheelDeadZone = 12;
	double wheelMouseSensitivity = 1.0;

	public static final int TPS = 20;
	public static final int MAX_PING_DURATION = 60;
	public static final int MAX_PING_DISTANCE = 2048;
	public static final float MAX_CORRECTION_PERIOD = 5f;
	public static final int MAX_CHANNEL_LENGTH = 128;

	public String getChannel() {
		final var ip = GameContext.getCurrentServerIp();

		if (ip.isPresent()) return serverChannels.getOrDefault(ip.get(), "");

		return channel;
	}

	public void setChannel(String newChannel) {
		final var ip = GameContext.getCurrentServerIp();

		if (ip.isPresent()) {
			serverChannels.put(ip.get(), newChannel);
		} else {
			channel = newChannel;
		}
	}

	@Override
	public void validate() {
		if (channel.length() > MAX_CHANNEL_LENGTH) {
			channel = channel.substring(0, MAX_CHANNEL_LENGTH);
		}

		for (Map.Entry<String, String> entry : serverChannels.entrySet()) {
			final var channel = entry.getValue();

			if (channel.length() > MAX_CHANNEL_LENGTH) {
				entry.setValue(channel.substring(0, MAX_CHANNEL_LENGTH));
			}
		}

		wheelHoldMillis = Math.max(0, wheelHoldMillis);
		wheelDeadZone = Math.max(0, wheelDeadZone);
		wheelMouseSensitivity = Math.max(0.1, wheelMouseSensitivity);
	}

	@Override
	public void onUpdate() {
		if (Game != null) {
			IPlatformNetworkService.INSTANCE.sendToServer(new UpdateChannelC2SPacket(getChannel()));
		}
	}

	public static final ConfigHandler<ClientConfig> HANDLER = ConfigHandler.of(ClientConfig.class, ".json");
}
