package app.jyu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import app.jyu.PingSystemClient;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;
import static app.jyu.PingSystem.LOGGER;
import static app.jyu.NetworkingConstants.PING_PACKET;
import static app.jyu.NetworkingConstants.REMOVE_PING_PACKET;
import static app.jyu.NetworkingConstants.ANSWER_PACKET;
import static app.jyu.NetworkingConstants.SYNC_CONFIG_PACKET;
import static app.jyu.NetworkingConstants.OPEN_CONFIG_GUI_PACKET;

import net.minecraft.entity.projectile.ProjectileUtil;

import java.util.HashMap;
import java.util.Map;

public class PingSystemClient implements ClientModInitializer {
    public static final double MAX_REACH = 512.0D;
    public static KeyBinding pingKeyBinding;
    public static KeyBinding answerKey1;
    public static KeyBinding answerKey2;
    public static KeyBinding answerKey3;
    public static KeyBinding answerKey4;

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        pingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ping_system.ping", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_C, // The keycode of the key
                "category.ping_system.ping_system" // The translation key of the keybinding's category.
        ));

        // Register answer key bindings for 1/2/3/4
        answerKey1 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ping_system.answer1",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_1,
                "category.ping_system.ping_system"
        ));

        answerKey2 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ping_system.answer2",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_2,
                "category.ping_system.ping_system"
        ));

        answerKey3 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ping_system.answer3",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_3,
                "category.ping_system.ping_system"
        ));

        answerKey4 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ping_system.answer4",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_4,
                "category.ping_system.ping_system"
        ));

        // Register Fabric events
        // Tick handler for key presses
        ClientTickEvents.END_CLIENT_TICK.register(PingSystemClient::checkKeyPress);
        // Tick handler for updating RenderHandler data (previously in ClientTickHandler)
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickHandler.getInstance()::onClientTick);

        // Rendering handlers
        WorldRenderEvents.LAST.register(RenderHandler.getInstance()::onRenderWorldLast);
        HudRenderCallback.EVENT.register(RenderHandler.getInstance()::onRenderGameOverlayPost);

        ClientPlayNetworking.registerGlobalReceiver(PING_PACKET, (client, handler, buf, responseSender) -> {
            // Everything in this lambda is run on the render thread
            pingReceiver(buf);
        });
        ClientPlayNetworking.registerGlobalReceiver(REMOVE_PING_PACKET, (client, handler, buf, responseSender) -> {
            removePingReceiver(buf);
        });

        // Register server config sync receiver
        ClientPlayNetworking.registerGlobalReceiver(SYNC_CONFIG_PACKET, (client, handler, buf, responseSender) -> {
            syncConfigReceiver(buf);
        });

        // Register config GUI open receiver
        ClientPlayNetworking.registerGlobalReceiver(OPEN_CONFIG_GUI_PACKET, (client, handler, buf, responseSender) -> {
            // Open the GUI on client side
            client.execute(() -> openServerConfigGui());
        });

        ModConfig.loadConfig(ModConfig.CFG_FILE);
    }

    private static void checkKeyPress(MinecraftClient client) {
        while (pingKeyBinding.wasPressed()) {
            assert client.player != null;
            var player = client.player;
            assert client.cameraEntity != null;
            handlePingAction(client, player, ModConfig.includeFluids);
        }

        // Check answer keys 1/2/3/4
        RenderHandler renderer = RenderHandler.getInstance();
        if (renderer.isOnPing()) {
            assert client.player != null;
            var player = client.player;
            PingPoint currentPing = renderer.getOnPing();

            while (answerKey1.wasPressed()) {
                handleAnswerKey(currentPing, 0, player.getGameProfile().getName());
            }
            while (answerKey2.wasPressed()) {
                handleAnswerKey(currentPing, 1, player.getGameProfile().getName());
            }
            while (answerKey3.wasPressed()) {
                handleAnswerKey(currentPing, 2, player.getGameProfile().getName());
            }
            while (answerKey4.wasPressed()) {
                handleAnswerKey(currentPing, 3, player.getGameProfile().getName());
            }
        }
    }

    private static void handleAnswerKey(PingPoint ping, int answerIndex, String playerName) {
        if (ping != null && ping.quiz != null) {
            try {
                AnswerPacket answerPacket = new AnswerPacket(ping.id, answerIndex, playerName);
                PacketByteBuf buf = answerPacket.toPacketByteBuf();
                ClientPlayNetworking.send(ANSWER_PACKET, buf);
                LOGGER.info("Sent answer {} for ping {}", answerIndex, ping.id);
            } catch (IOException e) {
                LOGGER.error("Failed to send answer packet", e);
            }
        }
    }

    // Renamed from pingDirection to handlePingAction
    private static void handlePingAction(MinecraftClient client, ClientPlayerEntity player,
                                         boolean includeFluids) {
        RenderHandler renderer = RenderHandler.getInstance();
        if (renderer.isOnPing()) {
            renderer.removeOnPing();
            sendRemovePingToServer(renderer.getOnPing());
            renderer.resetOnPing(); 
        }
    }

    private static void addPointToRenderer(PingPoint p) {
        RenderHandler.getInstance().addPing(p);
    }

    private void removePointAtRenderer(PingPoint p) {
        RenderHandler.getInstance().removePing(p);
    }

    private static HitResult raycast( Entity cameraEntity, double maxDistance, float tickDelta, boolean includeFluids) {
        Vec3d cameraPos = cameraEntity.getCameraPosVec(tickDelta);
        Vec3d rotationVec = cameraEntity.getRotationVec(tickDelta);
        Vec3d endVec = cameraPos.add(rotationVec.multiply(maxDistance));
        Box searchBox = cameraEntity.getBoundingBox().stretch(rotationVec.multiply(maxDistance)).expand(1.0D, 1.0D, 1.0D);

        // 1. Raycast for Blocks
        BlockHitResult blockHitResult = cameraEntity.getWorld().raycast(new RaycastContext(
                cameraPos,
                endVec,
                RaycastContext.ShapeType.OUTLINE,
                includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                cameraEntity
        ));

        // 2. Raycast for Entities
        double currentMaxDistSq = endVec.squaredDistanceTo(cameraPos);
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            currentMaxDistSq = blockHitResult.getPos().squaredDistanceTo(cameraPos);
        }
        
        // Predicate to filter which entities can be targeted
        Predicate<Entity> entityPredicate = entity -> !entity.isSpectator() && entity.canHit();

        // Use ProjectileUtil.raycast which is commonly used for this purpose
        EntityHitResult entityHitResult = ProjectileUtil.raycast(
                cameraEntity, 
                cameraPos, 
                endVec, 
                searchBox, 
                entityPredicate, 
                currentMaxDistSq
        );

        // 3. Compare Results
        if (entityHitResult != null) {
            double entityDistSq = entityHitResult.getPos().squaredDistanceTo(cameraPos);
            // If entity is closer than block (or if block was a miss), return entity hit
            if (entityDistSq < currentMaxDistSq || blockHitResult.getType() == HitResult.Type.MISS) {
                 LOGGER.debug("Raycast hit entity: " + entityHitResult.getEntity().getName().getString());
                return entityHitResult;
            }
        }
        
        // Otherwise, return the block hit (or miss if both missed)
        LOGGER.debug("Raycast hit block: " + (blockHitResult.getType() != HitResult.Type.MISS ? blockHitResult.getBlockPos().toString() : "MISS"));
        return blockHitResult;
    }

    public static void sendPingToServer(PingPoint p) {
        try {
            PacketByteBuf buf = p.toPacketByteBuf();
            ClientPlayNetworking.send(PING_PACKET, buf);
        } catch (IOException e) {
            LOGGER.error("Fail to send ping packet to server", e);
        }
    }

    private static void sendRemovePingToServer(PingPoint p) {
        try {
            PacketByteBuf buf = p.toPacketByteBuf();
            ClientPlayNetworking.send(REMOVE_PING_PACKET, buf);
        } catch (IOException e) {
            LOGGER.error("Fail to send remove ping packet to server", e);
        }
    }

    public void pingReceiver(PacketByteBuf buf) {
        try {
            var p = PingPoint.fromPacketByteBuf(buf);
            addPointToRenderer(p);
            LOGGER.debug("Received ping at " + p.pos.toString());
        } catch (Exception e) {
            LOGGER.error("Fail to deserialize the ping packet received", e);
        }
    }

    private void removePingReceiver(PacketByteBuf buf) {
        try {
            var p = PingPoint.fromPacketByteBuf(buf);
            removePointAtRenderer(p);
            LOGGER.debug("Received remove ping at " + p.pos.toString());
        } catch (Exception e) {
            LOGGER.error("Fail to deserialize the remove ping packet received", e);
        }
    }

    // Static reference to config screen for reuse
    private static ServerConfigScreen configScreen = null;
    
    /**
     * Handle server config sync packet
     */
    private static void syncConfigReceiver(PacketByteBuf buf) {
        try {
            // Read config values
            int bookId = buf.readInt();
            int highlightColor = buf.readInt();
            boolean quizEnabled = buf.readBoolean();
            int quizTimeout = buf.readInt();
            
            // Read available books
            int bookCount = buf.readInt();
            Map<Integer, String> availableBooks = new HashMap<>();
            for (int i = 0; i < bookCount; i++) {
                int id = buf.readInt();
                String name = buf.readString();
                availableBooks.put(id, name);
            }
            
            // Update config screen if it exists
            if (configScreen != null) {
                configScreen.updateConfig(bookId, highlightColor, quizEnabled, quizTimeout, availableBooks);
            } else {
                // Store for later use when opening config screen
                lastReceivedConfig = new ConfigData(bookId, highlightColor, quizEnabled, quizTimeout, availableBooks);
            }
            
            PingSystem.LOGGER.info("[PingSystem Client] Received server config: bookId={}, highlightColor=0x{}, quizEnabled={}", 
                bookId, Integer.toHexString(highlightColor), quizEnabled);
                
        } catch (Exception e) {
            PingSystem.LOGGER.error("[PingSystem Client] Failed to handle server config sync", e);
        }
    }
    
    // Store last received config for when GUI is opened
    private static ConfigData lastReceivedConfig = null;
    
    private static class ConfigData {
        final int bookId;
        final int highlightColor;
        final boolean quizEnabled;
        final int quizTimeout;
        final Map<Integer, String> availableBooks;
        
        ConfigData(int bookId, int highlightColor, boolean quizEnabled, int quizTimeout, Map<Integer, String> availableBooks) {
            this.bookId = bookId;
            this.highlightColor = highlightColor;
            this.quizEnabled = quizEnabled;
            this.quizTimeout = quizTimeout;
            this.availableBooks = availableBooks;
        }
    }
    
    /**
     * Open server config GUI (OP only)
     */
    public static void openServerConfigGui() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Open the GUI directly
        client.execute(() -> {
            configScreen = new ServerConfigScreen(client.currentScreen);
            if (lastReceivedConfig != null) {
                configScreen.updateConfig(
                    lastReceivedConfig.bookId,
                    lastReceivedConfig.highlightColor,
                    lastReceivedConfig.quizEnabled,
                    lastReceivedConfig.quizTimeout,
                    lastReceivedConfig.availableBooks
                );
            }
            client.setScreen(configScreen);
        });
    }
} 