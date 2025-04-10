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
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
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
import app.jyu.ClientTickHandler;
import app.jyu.ModConfig;
import app.jyu.PingPoint;
import app.jyu.RenderHandler;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.UUID;

import static app.jyu.PingSystem.LOGGER;
import static app.jyu.NetworkingConstants.PING_PACKET;
import static app.jyu.NetworkingConstants.REMOVE_PING_PACKET;

import net.minecraft.entity.projectile.ProjectileUtil;

public class PingSystemClient implements ClientModInitializer {
    public static final double MAX_REACH = 512.0D;
    public static KeyBinding pingKeyBinding;

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        pingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ping_system.ping", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_C, // The keycode of the key
                "category.ping_system.ping_system" // The translation key of the keybinding's category.
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

        ModConfig.loadConfig(ModConfig.CFG_FILE);
    }

    private static void checkKeyPress(MinecraftClient client) {
        while (pingKeyBinding.wasPressed()) {
            assert client.player != null;
            var player = client.player;
            assert client.cameraEntity != null;
            handlePingAction(client, player, ModConfig.includeFluids);
        }
    }

    // Renamed from pingDirection to handlePingAction
    private static void handlePingAction(MinecraftClient client, ClientPlayerEntity player,
                                         boolean includeFluids) {
        assert client.world != null;
        assert client.cameraEntity != null;
        float tickDelta = client.getTickDelta(); 
        HitResult hit = raycast(client.cameraEntity, MAX_REACH, tickDelta, includeFluids);

        PingPoint pingToSend = null;
        Vec3d pingPos = null;

        switch (Objects.requireNonNull(hit).getType()) {
            case MISS -> player.sendMessage(Text.literal("Too far"), true);
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState blockState = client.world.getBlockState(blockPos);
                Block block = blockState.getBlock();
                final Text blockMes = block.getName();
                player.sendMessage(blockMes, true);
                pingPos = hit.getPos();
                pingToSend = new PingPoint(pingPos, player.getEntityName(), new Color(ModConfig.highlightColor), ModConfig.soundIndex, PingPoint.PingType.LOCATION, null);
            }
            case ENTITY -> {
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                final Text entityMes = entity.getName();
                player.sendMessage(entityMes, true);
                // Use the center of the entity's bounding box for pingPos
                pingPos = entity.getBoundingBox().getCenter(); 
                pingToSend = new PingPoint(pingPos, player.getEntityName(), new Color(ModConfig.highlightColor), ModConfig.soundIndex, PingPoint.PingType.ENTITY, entity.getUuid());
            }
        }

        if (pingToSend != null) {
            processPing(pingToSend);
        }
    }

    // Renamed from pingPosition to processPing and accepts PingPoint
    private static void processPing(PingPoint p) {
        LOGGER.debug("Processing Ping at " + p.pos + " Type: " + p.type + (p.entityUUID != null ? " Entity: " + p.entityUUID : ""));
        RenderHandler renderer = RenderHandler.getInstance();
        if (renderer.isOnPing()) {
            renderer.removeOnPing();
            sendRemovePingToServer(renderer.getOnPing());
            renderer.resetOnPing(); 
        } else {
            addPointToRenderer(p);
            sendPingToServer(p); // Only send the main ping packet
        }
    }

    private static void addPointToRenderer(PingPoint p) {
        RenderHandler.getInstance().addPing(p);
    }

    private void removePointAtRenderer(PingPoint p) {
        RenderHandler.getInstance().removePing(p);
    }

    private static HitResult raycast(
            Entity cameraEntity,
            double maxDistance,
            float tickDelta,
            boolean includeFluids
    ) {
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
}