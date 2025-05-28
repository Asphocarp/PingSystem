package app.jyu;

import net.minecraft.util.Identifier;

public class NetworkingConstants {
    public static final Identifier PING_PACKET = new Identifier(PingSystem.MOD_ID, "ping");
    public static final Identifier REMOVE_PING_PACKET = new Identifier(PingSystem.MOD_ID, "remove_ping");
    public static final Identifier ANSWER_PACKET = new Identifier(PingSystem.MOD_ID, "answer");
}
