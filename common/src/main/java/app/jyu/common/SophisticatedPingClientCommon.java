package app.jyu.common;

import app.jyu.common.config.ModConfig;
import app.jyu.common.platform.IPlatformClientEventService;
import app.jyu.common.platform.IPlatformContextService;
import app.jyu.common.platform.IPlatformNetworkService;
import app.jyu.common.render.RenderHandler;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.Predicate;

public final class SophisticatedPingClientCommon {
    public static final double MAX_REACH = 512.0D;
    public static final KeyMapping PING_KEY = new KeyMapping(
            "key.sophisticated_ping.ping",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.sophisticated_ping.sophisticated_ping"
    );

    private SophisticatedPingClientCommon() {
    }

    public static void init() {
        ModConfig.load();
        IPlatformContextService.INSTANCE.registerKeyMapping(PING_KEY);
        IPlatformClientEventService.INSTANCE.registerEndClientTick(SophisticatedPingClientCommon::onClientTick);
        IPlatformClientEventService.INSTANCE.registerRenderWorld(RenderHandler.getInstance()::onRenderWorld);
        IPlatformClientEventService.INSTANCE.registerRenderGui(RenderHandler.getInstance()::onRenderGui);
    }

    public static void receivePing(PingPoint point) {
        if (point == null || point.isCorrupt()) {
            Constants.LOGGER.warn("Ignoring corrupt client ping packet");
            return;
        }
        RenderHandler.getInstance().addPing(point);
    }

    public static void receiveRemovePing(PingPoint point) {
        if (point == null || point.isCorrupt()) {
            Constants.LOGGER.warn("Ignoring corrupt client remove ping packet");
            return;
        }
        RenderHandler.getInstance().removePing(point);
    }

    private static void onClientTick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        while (PING_KEY.consumeClick()) {
            handlePingAction(client, client.player, ModConfig.includeFluids);
        }
    }

    private static void handlePingAction(Minecraft client, LocalPlayer player, boolean includeFluids) {
        Entity cameraEntity = client.cameraEntity;
        if (client.level == null || cameraEntity == null) {
            return;
        }

        HitResult hit = raycast(cameraEntity, MAX_REACH, client.getFrameTime(), includeFluids);
        if (hit == null) {
            return;
        }

        PingPoint pingToSend = null;
        switch (hit.getType()) {
            case MISS -> player.displayClientMessage(Component.literal("Too far"), true);
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockState blockState = client.level.getBlockState(blockHit.getBlockPos());
                Block block = blockState.getBlock();
                player.displayClientMessage(block.getName(), true);
                pingToSend = PingPoint.location(hit.getLocation(), player.getGameProfile().getName(), ModConfig.highlightColor, ModConfig.soundIndex);
            }
            case ENTITY -> {
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                player.displayClientMessage(entity.getName(), true);
                pingToSend = PingPoint.entity(
                        entity.getBoundingBox().getCenter(),
                        player.getGameProfile().getName(),
                        ModConfig.highlightColor,
                        ModConfig.soundIndex,
                        entity.getUUID()
                );
            }
        }

        if (pingToSend != null) {
            processPing(pingToSend);
        }
    }

    private static void processPing(PingPoint point) {
        RenderHandler renderer = RenderHandler.getInstance();
        if (renderer.isOnPing()) {
            PingPoint existing = renderer.getOnPing();
            renderer.removeOnPing();
            IPlatformNetworkService.INSTANCE.sendRemovePingToServer(existing);
            renderer.resetOnPing();
            return;
        }

        renderer.addPing(point);
        IPlatformNetworkService.INSTANCE.sendPingToServer(point);
    }

    private static HitResult raycast(Entity cameraEntity, double maxDistance, float tickDelta, boolean includeFluids) {
        Vec3 cameraPos = cameraEntity.getEyePosition(tickDelta);
        Vec3 rotationVec = cameraEntity.getViewVector(tickDelta);
        Vec3 endVec = cameraPos.add(rotationVec.scale(maxDistance));
        AABB searchBox = cameraEntity.getBoundingBox().expandTowards(rotationVec.scale(maxDistance)).inflate(1.0D);

        BlockHitResult blockHitResult = cameraEntity.level().clip(new ClipContext(
                cameraPos,
                endVec,
                ClipContext.Block.OUTLINE,
                includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
                cameraEntity
        ));

        double currentMaxDistSq = endVec.distanceToSqr(cameraPos);
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            currentMaxDistSq = blockHitResult.getLocation().distanceToSqr(cameraPos);
        }

        Predicate<Entity> entityPredicate = entity -> !entity.isSpectator() && entity.isPickable();
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                cameraEntity,
                cameraPos,
                endVec,
                searchBox,
                entityPredicate,
                currentMaxDistSq
        );

        if (entityHitResult == null) {
            return blockHitResult;
        }

        double entityDistSq = entityHitResult.getLocation().distanceToSqr(cameraPos);
        if (entityDistSq < currentMaxDistSq || blockHitResult.getType() == HitResult.Type.MISS) {
            return entityHitResult;
        }
        return Objects.requireNonNull(blockHitResult);
    }
}
