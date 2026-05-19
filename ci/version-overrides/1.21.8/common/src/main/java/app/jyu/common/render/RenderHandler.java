package app.jyu.common.render;

import app.jyu.common.Constants;
import app.jyu.common.PingPoint;
import app.jyu.common.SophisticatedPingClientCommon;
import app.jyu.common.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RenderHandler {
    private static final RenderHandler INSTANCE = new RenderHandler();
    private static final ResourceLocation PING_BASIC = Constants.id("textures/ping/ping_basic.png");

    private final Map<String, CopyOnWriteArrayList<PingPoint>> pings = new HashMap<>();
    private final Map<UUID, Vector4f> pingClipCoordinates = new HashMap<>();
    private PingPoint onPing;

    private RenderHandler() {
    }

    public static RenderHandler getInstance() {
        return INSTANCE;
    }

    public void addPing(PingPoint point) {
        CopyOnWriteArrayList<PingPoint> pingList = pings.computeIfAbsent(point.owner(), ignored -> new CopyOnWriteArrayList<>());
        if (pingList.size() >= ModConfig.pingNumEach) {
            pingList.subList(0, pingList.size() - ModConfig.pingNumEach + 1).clear();
        }
        pingList.add(point);
    }

    public PingPoint getOnPing() {
        return onPing;
    }

    public boolean isOnPing() {
        return onPing != null;
    }

    public void removeOnPing() {
        if (onPing == null) {
            return;
        }
        removePing(onPing);
    }

    public void removePing(PingPoint point) {
        CopyOnWriteArrayList<PingPoint> pingList = pings.get(point.owner());
        if (pingList == null) {
            return;
        }
        pingList.removeIf(candidate -> candidate.id().equals(point.id()));
        pingClipCoordinates.remove(point.id());
    }

    public void resetOnPing() {
        onPing = null;
    }

    public void onRenderWorld(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.options.hideGui) {
            return;
        }
        removeExpiredPings();
        calculatePingScreenCoordinates(context);
    }

    public void onRenderGui(GuiGraphics guiGraphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }

        boolean foundOnPing = false;
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;

        for (CopyOnWriteArrayList<PingPoint> pingList : pings.values()) {
            for (PingPoint ping : pingList) {
                Vector4f clipPos = pingClipCoordinates.get(ping.id());
                if (clipPos == null || clipPos.w <= 0) {
                    continue;
                }

                float ndcX = clipPos.x / clipPos.w;
                float ndcY = clipPos.y / clipPos.w;
                double screenX = halfWidth + ndcX * halfWidth;
                double screenY = halfHeight - ndcY * halfHeight;
                double margin = Math.max(8.0, ModConfig.iconSize * 4.0);
                screenX = Mth.clamp(screenX, margin, width - margin);
                screenY = Mth.clamp(screenY, margin, height - margin);

                renderIcon(guiGraphics, screenX, screenY);

                if (!foundOnPing) {
                    double dx = screenX - halfWidth;
                    double dy = screenY - halfHeight;
                    double threshold = Math.min(width, height) / 25.0;
                    if (dx * dx + dy * dy <= threshold * threshold) {
                        renderInfo(guiGraphics, (int) halfWidth + 5, (int) halfHeight + 5, ping);
                        onPing = ping;
                        foundOnPing = true;
                    }
                }
            }
        }

        if (!foundOnPing) {
            onPing = null;
        }
    }

    private void removeExpiredPings() {
        for (CopyOnWriteArrayList<PingPoint> pingList : pings.values()) {
            pingList.removeIf(ping -> {
                boolean remove = ping.shouldVanish(ModConfig.secondsToVanish);
                if (remove) {
                    pingClipCoordinates.remove(ping.id());
                }
                return remove;
            });
        }
    }

    private void calculatePingScreenCoordinates(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        pingClipCoordinates.clear();
        Vec3 cameraPos = context.camera.getPosition();
        Matrix4f projectionMatrix = new Matrix4f(context.projectionMatrix);

        for (CopyOnWriteArrayList<PingPoint> pingList : pings.values()) {
            for (PingPoint ping : pingList) {
                Vec3 targetPos = effectiveTargetPos(client, ping, context.tickDelta);
                context.poseStack.pushPose();
                context.poseStack.translate(targetPos.x - cameraPos.x, targetPos.y - cameraPos.y, targetPos.z - cameraPos.z);
                Matrix4f modelView = context.poseStack.last().pose();
                Vector4f clipPos = new Matrix4f(projectionMatrix).mul(modelView).transform(new Vector4f(0, 0, 0, 1));
                pingClipCoordinates.put(ping.id(), clipPos);
                context.poseStack.popPose();
            }
        }
    }

    private Vec3 effectiveTargetPos(Minecraft client, PingPoint ping, float tickDelta) {
        if (ping.type() != PingPoint.PingType.ENTITY || ping.entityUuid() == null || client.level == null) {
            return ping.pos();
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(ping.entityUuid())) {
                Vec3 feet = entity.getPosition(tickDelta);
                return feet.add(0, entity.getBbHeight() / 2.0, 0);
            }
        }
        return ping.pos();
    }

    private void renderIcon(GuiGraphics guiGraphics, double centerX, double centerY) {
        int size = Math.max(8, Math.round(8 * ModConfig.iconSize));
        int x = (int) Math.round(centerX - size / 2.0);
        int y = (int) Math.round(centerY - size / 2.0);
        guiGraphics.blit(RenderType::guiTextured, PING_BASIC, x, y, 0, 0, size, size, size, size);
    }

    private void renderInfo(GuiGraphics guiGraphics, int x, int y, PingPoint ping) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        double distance = client.player.position().distanceTo(ping.pos());
        guiGraphics.drawString(client.font, "%.0f m".formatted(distance), x, y, ModConfig.infoColor, true);
        y += client.font.lineHeight + 2;

        if (!ping.owner().equals(client.player.getGameProfile().getName())) {
            guiGraphics.drawString(client.font, ping.owner(), x, y, ModConfig.infoColor, true);
            y += client.font.lineHeight + 2;
        }

        guiGraphics.drawString(client.font, "Cancel (" + humanReadableHotkey() + ")", x, y, 0xFFFFFFFF, true);
    }

    private static String humanReadableHotkey() {
        String key = SophisticatedPingClientCommon.PING_KEY.getTranslatedKeyMessage().getString();
        if (key == null || key.isBlank()) {
            return "C";
        }
        return key.toUpperCase();
    }
}
