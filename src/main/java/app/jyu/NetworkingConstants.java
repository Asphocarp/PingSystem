package app.jyu;

import net.minecraft.util.Identifier;

public class NetworkingConstants {
    public static final Identifier PING_PACKET = new Identifier(QuizCraft.MOD_ID, "ping");
    public static final Identifier REMOVE_PING_PACKET = new Identifier(QuizCraft.MOD_ID, "remove_ping");
    public static final Identifier ANSWER_PACKET = new Identifier(QuizCraft.MOD_ID, "answer");
    
    // Server config packets
    public static final Identifier REQUEST_CONFIG_PACKET = new Identifier(QuizCraft.MOD_ID, "request_config");
    public static final Identifier SYNC_CONFIG_PACKET = new Identifier(QuizCraft.MOD_ID, "sync_config");
    public static final Identifier UPDATE_CONFIG_PACKET = new Identifier(QuizCraft.MOD_ID, "update_config");
    public static final Identifier OPEN_CONFIG_GUI_PACKET = new Identifier(QuizCraft.MOD_ID, "open_config_gui");
}
