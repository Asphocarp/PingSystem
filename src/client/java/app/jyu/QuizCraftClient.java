package app.jyu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import app.jyu.QuizCraftClient;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;
import static app.jyu.QuizCraft.LOGGER;
import static app.jyu.NetworkingConstants.PING_PACKET;
import static app.jyu.NetworkingConstants.REMOVE_PING_PACKET;
import static app.jyu.NetworkingConstants.ANSWER_PACKET;
import static app.jyu.NetworkingConstants.SYNC_CONFIG_PACKET;
import static app.jyu.NetworkingConstants.OPEN_CONFIG_GUI_PACKET;
import static app.jyu.NetworkingConstants.REQUEST_CONFIG_PACKET;

import net.minecraft.entity.projectile.ProjectileUtil;

import java.util.HashMap;
import java.util.Map;

public class QuizCraftClient implements ClientModInitializer {
    public static final double MAX_REACH = 512.0D;
    public static KeyBinding pingKeyBinding;
    public static KeyBinding answerKey1;
    public static KeyBinding answerKey2;
    public static KeyBinding answerKey3;
    public static KeyBinding answerKey4;

    private static ServerConfigScreen configScreen = null;
    private static ConfigData lastReceivedConfig = null;
    private static boolean pendingOpenConfigGui = false;

    @Override
    public void onInitializeClient() {
        pingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quiz_craft.ping",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.quiz_craft.quiz_craft"
        ));

        answerKey1 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quiz_craft.answer1",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_1,
                "category.quiz_craft.quiz_craft"
        ));

        answerKey2 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quiz_craft.answer2",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_2,
                "category.quiz_craft.quiz_craft"
        ));

        answerKey3 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quiz_craft.answer3",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_3,
                "category.quiz_craft.quiz_craft"
        ));

        answerKey4 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quiz_craft.answer4",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_4,
                "category.quiz_craft.quiz_craft"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(QuizCraftClient::checkKeyPress);
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickHandler.getInstance()::onClientTick);

        WorldRenderEvents.LAST.register(RenderHandler.getInstance()::onRenderWorldLast);
        HudRenderCallback.EVENT.register(RenderHandler.getInstance()::onRenderGameOverlayPost);

        ClientPlayNetworking.registerGlobalReceiver(PING_PACKET, (client, handler, buf, responseSender) -> {
            pingReceiver(buf);
        });
        ClientPlayNetworking.registerGlobalReceiver(REMOVE_PING_PACKET, (client, handler, buf, responseSender) -> {
            removePingReceiver(buf);
        });

        ClientPlayNetworking.registerGlobalReceiver(SYNC_CONFIG_PACKET, (client, handler, buf, responseSender) -> {
            syncConfigReceiver(buf);
        });

        ClientPlayNetworking.registerGlobalReceiver(OPEN_CONFIG_GUI_PACKET, (client, handler, buf, responseSender) -> {
            if (client.player != null) {
                LOGGER.info("[QuizCraft Client] Received OPEN_CONFIG_GUI_PACKET. Requesting fresh config from server.");
                pendingOpenConfigGui = true;
                ClientPlayNetworking.send(REQUEST_CONFIG_PACKET, PacketByteBufs.create());
            }
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

        RenderHandler renderer = RenderHandler.getInstance();
        if (renderer.isOnPing()) {
            assert client.player != null;
            var player = client.player;
            PingPoint currentPing = renderer.getOnPing();

            if (answerKey1.wasPressed()) handleAnswerKey(currentPing, 0, player.getGameProfile().getName());
            if (answerKey2.wasPressed()) handleAnswerKey(currentPing, 1, player.getGameProfile().getName());
            if (answerKey3.wasPressed()) handleAnswerKey(currentPing, 2, player.getGameProfile().getName());
            if (answerKey4.wasPressed()) handleAnswerKey(currentPing, 3, player.getGameProfile().getName());
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

    private static void handlePingAction(MinecraftClient client, ClientPlayerEntity player, boolean includeFluids) {
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
        BlockHitResult blockHitResult = cameraEntity.getWorld().raycast(new RaycastContext(
                cameraPos, endVec, RaycastContext.ShapeType.OUTLINE,
                includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                cameraEntity));
        double currentMaxDistSq = endVec.squaredDistanceTo(cameraPos);
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            currentMaxDistSq = blockHitResult.getPos().squaredDistanceTo(cameraPos);
        }
        Predicate<Entity> entityPredicate = entity -> !entity.isSpectator() && entity.canHit();
        EntityHitResult entityHitResult = ProjectileUtil.raycast(
                cameraEntity, cameraPos, endVec, searchBox, entityPredicate, currentMaxDistSq);
        if (entityHitResult != null) {
            double entityDistSq = entityHitResult.getPos().squaredDistanceTo(cameraPos);
            if (entityDistSq < currentMaxDistSq || blockHitResult.getType() == HitResult.Type.MISS) {
                LOGGER.debug("Raycast hit entity: " + entityHitResult.getEntity().getName().getString());
                return entityHitResult;
            }
        }
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
    
    private static void syncConfigReceiver(PacketByteBuf buf) {
        try {
            int bookId = buf.readInt();
            int highlightColor = buf.readInt();
            boolean quizEnabled = buf.readBoolean();
            int quizTimeout = buf.readInt();
            
            int bookCount = buf.readInt();
            Map<Integer, String> availableBooks = new HashMap<>();
            for (int i = 0; i < bookCount; i++) {
                int id = buf.readInt();
                String name = buf.readString();
                availableBooks.put(id, name);
            }
            
            lastReceivedConfig = new ConfigData(bookId, highlightColor, quizEnabled, quizTimeout, availableBooks);
            QuizCraft.LOGGER.info("[QuizCraft Client] Received and stored server config: bookId={}, highlightColor=0x{}, quizEnabled={}", 
                bookId, Integer.toHexString(highlightColor), quizEnabled);

            if (pendingOpenConfigGui) {
                pendingOpenConfigGui = false;
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player == null) return;
                    
                    configScreen = new ServerConfigScreen(MinecraftClient.getInstance().currentScreen);
                    configScreen.updateConfig(
                        lastReceivedConfig.bookId,
                        lastReceivedConfig.highlightColor,
                        lastReceivedConfig.quizEnabled,
                        lastReceivedConfig.quizTimeout,
                        lastReceivedConfig.availableBooks
                    );
                    MinecraftClient.getInstance().setScreen(configScreen);
                    LOGGER.info("[QuizCraft Client] ServerConfigScreen opened after pending config sync.");
                });
            } else if (configScreen != null && configScreen == MinecraftClient.getInstance().currentScreen) {
                MinecraftClient.getInstance().execute(() -> {
                    configScreen.updateConfig(
                        lastReceivedConfig.bookId,
                        lastReceivedConfig.highlightColor,
                        lastReceivedConfig.quizEnabled,
                        lastReceivedConfig.quizTimeout,
                        lastReceivedConfig.availableBooks
                    );
                    LOGGER.info("[QuizCraft Client] Updated open ServerConfigScreen with fresh config.");
                 });
            }
                
        } catch (Exception e) {
            QuizCraft.LOGGER.error("[QuizCraft Client] Failed to handle server config sync", e);
            if (pendingOpenConfigGui) {
                pendingOpenConfigGui = false;
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(Text.literal("Failed to retrieve server config for GUI. Please try again.").formatted(Formatting.RED), false);
                    }
                });
            }
        }
    }
    
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
    
    public static void openServerConfigGui_Internal() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
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