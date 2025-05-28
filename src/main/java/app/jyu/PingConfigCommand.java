package app.jyu;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static app.jyu.NetworkingConstants.OPEN_CONFIG_GUI_PACKET;

public class PingConfigCommand {
    
    public static void register() {
        CommandRegistrationCallback.EVENT.register(PingConfigCommand::registerCommands);
    }
    
    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("quizconfig")
            .requires(source -> source.hasPermissionLevel(2)) // OP level 2 or higher
            .executes(PingConfigCommand::openConfigGui));
    }
    
    private static int openConfigGui(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            // Check if source is a player
            ServerPlayerEntity player = source.getPlayerOrThrow();
            
            // Check permissions (should be redundant due to requires(), but double-check)
            if (!ServerConfig.hasConfigPermission(player)) {
                source.sendError(Text.literal("You do not have permission to use this command!")
                    .formatted(Formatting.RED));
                return 0;
            }
            
            // Send packet to client to open GUI
            ServerPlayNetworking.send(player, OPEN_CONFIG_GUI_PACKET, PacketByteBufs.create());
            
            source.sendFeedback(() -> Text.literal("Opening QuizCraft configuration GUI...")
                .formatted(Formatting.GREEN), false);
            
            QuizCraft.LOGGER.info("[QuizCraft] Player {} opened config GUI via command", player.getEntityName());
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to open configuration GUI: " + e.getMessage())
                .formatted(Formatting.RED));
            QuizCraft.LOGGER.error("[QuizCraft] Failed to open config GUI via command", e);
            return 0;
        }
    }
} 