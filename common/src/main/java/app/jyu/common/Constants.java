package app.jyu.common;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Constants {
    public static final String MOD_ID = "sophisticated_ping";
    public static final String FORGE_MOD_ID = "sophisticated_ping";
    public static final String MOD_NAME = "Sophisticated Ping";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier PING_PACKET = id("ping");
    public static final Identifier REMOVE_PING_PACKET = id("remove_ping");

    private Constants() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
