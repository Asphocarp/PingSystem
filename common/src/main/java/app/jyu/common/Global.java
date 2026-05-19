package app.jyu.common;

import app.jyu.common.platform.IPlatformContextService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessageFactory;
import org.apache.logging.log4j.message.Message;

public class Global {
	private Global() {}

	public static final String MOD_VERSION = IPlatformContextService.INSTANCE.getSelfModVersion();
	public static final String MOD_ID = "sophisticated_ping";
	public static final String MOD_PREFIX = "[Sophisticated Ping] ";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID,
		new FormattedMessageFactory() {
			@Override
			public Message newMessage(String message) {
				return super.newMessage(MOD_PREFIX + message);
			}
		});
}
