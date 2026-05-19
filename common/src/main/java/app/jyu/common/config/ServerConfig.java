package app.jyu.common.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import app.jyu.common.core.ServerCore;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ServerConfig implements IConfig {
	ChannelMode defaultChannelMode = ChannelMode.AUTO;
	boolean playerTrackingEnabled = true;
	int msToRegenerate = 1000;
	int rateLimit = 5;

	@Override
	public void validate() {
		if (msToRegenerate < 0) {
			msToRegenerate = 1000;
		}

		if (rateLimit < 0) {
			rateLimit = 0;
		}
	}

	@Override
	public void onUpdate() {
		ServerCore.init();
	}

	public static final ConfigHandler<ServerConfig> HANDLER = ConfigHandler.of(ServerConfig.class, ".server.json");
}
