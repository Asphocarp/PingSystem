package app.jyu;

import app.jyu.RenderHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;

import static app.jyu.QuizCraft.LOGGER;
import static app.jyu.QuizCraftClient.pingKeyBinding;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.Map;

public class RenderHandler {
    public static final boolean DEBUG = false;
    private static final RenderHandler INSTANCE = new RenderHandler();
    private final MinecraftClient mc;
    // the count for debug
    public int debug_count;
    // safe for multi-thread
    public HashMap<String, CopyOnWriteArrayList<PingPoint>> pings;
    // the ping that is pointed to
    private PingPoint onPing;

    // Store calculated clip-space coordinates (x,y,z,w) for pings from the world render phase
    private final Map<UUID, Vector4f> pingClipCoordinates = new HashMap<>();

    private static final Identifier PING_BASIC = new Identifier(QuizCraft.MOD_ID, "textures/ping/ping_basic.png");

    public RenderHandler() {
        this.mc = MinecraftClient.getInstance();
        this.debug_count = 0;
        this.pings = new HashMap<>();
        this.onPing = null;
    }

    public static RenderHandler getInstance() {
        return INSTANCE;
    }

    public void addPing(PingPoint p) {
        if (pings.get(p.owner) == null) {
            var list = new CopyOnWriteArrayList<PingPoint>();
            list.add(p);
            pings.put(p.owner, list);
        } else {
            var pingList = pings.get(p.owner);
            if (pingList.size() >= ModConfig.pingNumEach) {
                pingList.subList(0, pingList.size() - ModConfig.pingNumEach + 1).clear();
            }
            pingList.add(p);
        }
    }

    public PingPoint getOnPing() {
        return onPing;
    }

    public boolean isOnPing() {
        return onPing != null;
    }

    public void onRenderWorldLast(WorldRenderContext context) {
        if (this.mc.world != null && this.mc.player != null && !this.mc.options.hudHidden) {
            this.renderLocationPings(context);
            this.calculatePingScreenCoordinates(context);
        }
    }

    public static Quaternionf getDegreesQuaternion(Vector3f vector, float degrees) {
        return new Quaternionf().fromAxisAngleDeg(vector, degrees);
    }

    private static Vec3d map(double anglePerPixel, Vec3d cameraDir, Vector3f horizontalRotationAxis,
                             Vector3f verticalRotationAxis, int x, int y, int width, int height) {
        float horizontalRotation = (float) ((x - width / 2f) * anglePerPixel);
        float verticalRotation = (float) ((y - height / 2f) * anglePerPixel);

        final Vector3f temp2 = QuizCraft.Vec3dToV3f(cameraDir);
        Quaternionfc rot1 = getDegreesQuaternion(verticalRotationAxis, verticalRotation);
        Quaternionfc rot2 = getDegreesQuaternion(horizontalRotationAxis, horizontalRotation);
        temp2.rotate(rot1);
        temp2.rotate(rot2);
        return new Vec3d(temp2.x(), temp2.y(), temp2.z());
    }

    public void onRenderGameOverlayPost(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double threshold = Math.min(width, height) / 5.0; // used to be 25, TODO: add config for this
        double thresholdSquared = threshold * threshold;
        
        // Track the nearest ping to screen center
        PingPoint nearestPing = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (var entry : this.pings.entrySet()) {
            for (var ping : entry.getValue()) {
                Vector4f clipPos = pingClipCoordinates.get(ping.id);

                if (clipPos == null) {
                    continue; // Skip if coords weren't calculated (e.g., ping added mid-frame)
                }

                // Check if behind camera (W <= 0)
                if (clipPos.w <= 0) {
                    // TODO: Handle points behind camera (e.g., clamp to edge, show arrow)
                    // For now, just skip rendering the icon/info
                    continue;
                }

                // Convert clip space to NDC (-1 to 1)
                float ndcX = clipPos.x / clipPos.w;
                float ndcY = clipPos.y / clipPos.w;
                // float ndcZ = clipPos.z / clipPos.w; // Depth value if needed

                // Convert NDC to Screen Coordinates (0 to width/height)
                // Y needs to be flipped: (1 - ndcY) because screen Y origin is top-left
                double screenX = halfWidth + (ndcX * halfWidth);
                double screenY = halfHeight - (ndcY * halfHeight); // Flipped Y

                // --- Clamping Logic (similar to old getIconCenter) ---
                double margin = ModConfig.iconSize / 2.0; // Example margin
                double clampedX = MathHelper.clamp(screenX, margin, width - margin);
                double clampedY = MathHelper.clamp(screenY, margin, height - margin);
                boolean isClamped = clampedX != screenX || clampedY != screenY;
                
                // If clamped, adjust the other coordinate proportionally (like before)
                // This part might need refinement depending on desired border behavior
                if (isClamped) {
                   if (Math.abs(screenX - halfWidth) > Math.abs(screenY - halfHeight)) { // Clamped horizontally first
                       screenY = halfHeight + (screenY - halfHeight) * (clampedX - halfWidth) / (screenX - halfWidth);
                       screenX = clampedX;
                   } else { // Clamped vertically first
                       screenX = halfWidth + (screenX - halfWidth) * (clampedY - halfHeight) / (screenY - halfHeight);
                       screenY = clampedY;
                   }
                   // Re-clamp after proportional adjustment if it went out of bounds again (edge cases)
                   screenX = MathHelper.clamp(screenX, margin, width - margin);
                   screenY = MathHelper.clamp(screenY, margin, height - margin);
                }
                // TODO: Render an arrow indicator if isClamped?
                // ------------------------------------------------------------

                // Render the icon at the calculated screen position
                renderIconHUD(drawContext, screenX, screenY, ping);

                // Check if this ping is the nearest to screen center
                double deltaX = screenX - halfWidth, deltaY = screenY - halfHeight;
                double distanceSquared = deltaX * deltaX + deltaY * deltaY;
                if (distanceSquared <= thresholdSquared && distanceSquared < nearestDistanceSquared) {
                    nearestPing = ping;
                    nearestDistanceSquared = distanceSquared;
                }
            }
        }

        // Set onPing to the nearest ping and render its info
        if (nearestPing != null) {
            renderInfoHUD(drawContext, (int) halfWidth, (int) halfHeight, nearestPing);
            onPing = nearestPing;
        } else {
            onPing = null;
        }
    }

    public static Vec3d XY2Vec3d(Vector2i xy) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        assert client.cameraEntity != null;
        Vec3d cameraDirection = client.cameraEntity.getRotationVec(1.0f);
        double fov = client.options.getFov().getValue();
        double angleSize = fov / height;

        Vector3f verticalRotationAxis = QuizCraft.Vec3dToV3f(cameraDirection);
        verticalRotationAxis.cross(new Vector3f(0, 1, 0));
        verticalRotationAxis.normalize();
        Vector3f horizontalRotationAxis = QuizCraft.Vec3dToV3f(cameraDirection);
        horizontalRotationAxis.cross(verticalRotationAxis);
        horizontalRotationAxis.normalize();
        verticalRotationAxis = QuizCraft.Vec3dToV3f(cameraDirection);
        verticalRotationAxis.cross(horizontalRotationAxis);
        cameraDirection.normalize();

        return map(angleSize, cameraDirection, horizontalRotationAxis, verticalRotationAxis, xy.x, xy.y, width, height);
    }

    /**
     * cx, cy: center of the icon
     * ping: the PingPoint
     */
    private void renderIconHUD(DrawContext drawContext, double cx, double cy, PingPoint ping) {
        double zLevel = 0;
        var realResizer = ModConfig.iconSize / 32;
        float u = 0, v = 0, width = 256 * realResizer, height = 256 * realResizer;
        float pixelWidth = 0.00390625F / realResizer;

        double x = cx - width / 2;
        double y = cy - height / 2;

        // TODO add background
        RenderSystem.setShaderTexture(0, PING_BASIC);

        // MatrixStack matrixStack = drawContext.getMatrices();
        // matrixStack.push();
        
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderSystem.enableBlend();
        // You might need a specific blend func here, e.g., RenderSystem.defaultBlendFunc();
        // Or: RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.defaultBlendFunc();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        buffer.vertex(x, y + height, zLevel).texture(u * pixelWidth, (v + height) * pixelWidth).next();
        buffer.vertex(x + width, y + height, zLevel).texture((u + width) * pixelWidth, (v + height) * pixelWidth).next();
        buffer.vertex(x + width, y, zLevel).texture((u + width) * pixelWidth, v * pixelWidth).next();
        buffer.vertex(x, y, zLevel).texture(u * pixelWidth, v * pixelWidth).next();

        tessellator.draw();
        
        // matrixStack.pop();
    }

    private void renderInfoHUD(DrawContext drawContext, int topLeftX, int topLeftY, PingPoint ping) {
        // the input X, Y is currently just the center of the screen
        topLeftX += 8;

        MinecraftClient client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null) {
            return;
        }

        List<String> textLines = new ArrayList<>();
        var playerPos = player.getPos(); 
        // calc dist
        // TODO: fix: use real dist for entity
        double dist;
        if (ping.type == PingPoint.PingType.ENTITY) {
            // get current entity pos
            Entity entity = findEntityByUUID(ping.entityUUID);
            if (entity != null) {
                dist = playerPos.distanceTo(entity.getPos());
            } else {
                dist = 0;
            }
        } else {
            dist = playerPos.distanceTo(ping.pos);
        }
        // show quiz info
        if (ping.quiz != null) {
            textLines.add(ping.quiz.question);
            for (int i = 0; i < ping.quiz.options.length; i++) {
                textLines.add("%d. %s".formatted(i + 1, ping.quiz.options[i]));
            }
        } else {
            LOGGER.error("PingPoint has no quiz: {}", ping.id);
        }

        // total height
        int totalHeight = textLines.size() * (client.textRenderer.fontHeight + 2) + client.textRenderer.fontHeight;
        topLeftY -= totalHeight*0.5;
        // render text
        for (String line : textLines) {
            drawContext.drawText(client.textRenderer, line, topLeftX, topLeftY, ModConfig.infoColor, true);
            topLeftY += client.textRenderer.fontHeight + 2;
        }

        String hotkey = humanReadableHotkey(pingKeyBinding);
        String keyIndicator = "";
        if (!ping.owner.equals(player.getEntityName())) {
            keyIndicator = "Cancel (%s) | %.1f m by %s".formatted(hotkey, dist, ping.owner);
        } else {
            keyIndicator = "Cancel (%s) | %.1f m".formatted(hotkey, dist);
        }
        drawContext.drawText(client.textRenderer, keyIndicator, topLeftX, topLeftY, 0xFFFFFFFF, true);
    }

    @NotNull
    public static String humanReadableHotkey(KeyBinding keybinding) {
        var hotkeyPath = KeyBindingHelper.getBoundKeyOf(keybinding).toString().split("\\.");
        // enough for keyboard
        var hotkey = hotkeyPath[hotkeyPath.length - 1].toUpperCase();
        // add "m" for mouse
        if (hotkeyPath.length == 3 && hotkeyPath[1].equals("mouse")) {
            hotkey = "M" + hotkey;
        }
        return hotkey;
    }

    public void renderLocationPings(WorldRenderContext wrc) {
        Entity cameraEntity = mc.getCameraEntity();
        if (cameraEntity == null || mc.world == null) {
            return;
        }

        for (var entry : this.pings.entrySet()) {
            var pingList = entry.getValue();
            
            // Only remove expired pings and their screen coords
            pingList.removeIf(ping -> {
                boolean shouldRemove = ping.shouldVanish(ModConfig.secondsToVanish);
                if (shouldRemove) {
                    // No need to handle glowing here anymore
                    pingClipCoordinates.remove(ping.id);
                    return true; // Remove the ping
                }

                // Only render location box if it's a location ping 
                // OR an entity ping whose entity is gone (fallback)
                if (ping.type == PingPoint.PingType.LOCATION || 
                   (ping.type == PingPoint.PingType.ENTITY && (ping.entityUUID == null || findEntityByUUID(ping.entityUUID) == null)))
                {
                    highlightPingLocation(ping, mc, wrc);
                }
                 
                return false; // Keep the ping
            });
        }
    }

    private static void highlightPingLocation(PingPoint ping, MinecraftClient mc, WorldRenderContext wrc) {
        MatrixStack matrices = wrc.matrixStack();
        matrices.push();
        
        // Translate the matrix stack to the ping's position relative to the camera
        Vec3d cameraPos = wrc.camera().getPos();
        // Don't subtract cameraPos here, keep ping.pos in world coords for definition
        // Vec3d relativePingPos = ping.pos.subtract(cameraPos); 
        // Instead, translate relative to the camera *before* drawing
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        // Now translate to the actual ping position in world space
        matrices.translate(ping.pos.x, ping.pos.y, ping.pos.z);

        // Get the final transformation matrix
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Define Box size centered at the translated origin (which is now ping.pos)
        float size = 0.3f;
        if (ping.type == PingPoint.PingType.LOCATION) {
            size = 1.0f;
        }
        float halfSize = size / 2.0f;
        // Define vertices relative to the translated origin (0,0,0)
        float minX = -halfSize;
        float minY = -halfSize; 
        float minZ = -halfSize;
        float maxX = halfSize;
        float maxY = halfSize;
        float maxZ = halfSize;
        
        // Color
        float r = ping.color.getRed() / 255f;
        float g = ping.color.getGreen() / 255f;
        float b = ping.color.getBlue() / 255f;
        float fillAlpha = 0.3f;
        float lineAlpha = 1.0f;
        
        // Setup rendering
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.polygonOffset(-3f, -3f);
        RenderSystem.enablePolygonOffset();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        // Draw filled box, passing the matrix
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        drawFilledBox(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, fillAlpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // Draw outline, passing the matrix
        buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        drawBoxOutline(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, lineAlpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // Cleanup
        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        
        matrices.pop();
    }
    
    private Entity findEntityByUUID(UUID uuid) {
        if (mc.world == null) return null;
        for (Entity entity : mc.world.getEntities()) {
            if (entity.getUuid().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Draws a filled box using the provided BufferBuilder and transformation matrix.
     * Replicates malilib's RenderUtils.drawBoxAllSidesBatchedQuads.
     * Assumes BufferBuilder has been initialized with QUADS draw mode and POSITION_COLOR format.
     */
    private static void drawFilledBox(BufferBuilder buffer, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        // West side (-X)
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).next();

        // East side (+X)
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).next();

        // North side (-Z)
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).next();

        // South side (+Z)
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).next();

        // Top side (+Y)
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).next();

        // Bottom side (-Y)
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).next();
    }

    /**
     * Draws the outline of a box using the provided BufferBuilder and transformation matrix.
     * Replicates malilib's RenderUtils.drawBoxAllEdgesBatchedLines.
     * Assumes BufferBuilder has been initialized with DEBUG_LINES draw mode and POSITION_COLOR format.
     */
    private static void drawBoxOutline(BufferBuilder buffer, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        // West side (-X)
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).next();

        // East side (+X)
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).next();

        // North side (-Z) (connecting lines)
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).next();

        // South side (+Z) (connecting lines)
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).next();
    }

    public void updateData(MinecraftClient mc) {
        // do nothing?
    }

    public void removeOnPing() {
        pings.get(onPing.owner).removeIf((p -> p.id.equals(onPing.id)));
    }

    public void removePing(PingPoint p) {
        pings.get(p.owner).removeIf((lhs -> lhs.id.equals(p.id)));
    }

    public void resetOnPing() {
        this.onPing = null;
    }

    // New method to calculate screen coordinates using render context
    private void calculatePingScreenCoordinates(WorldRenderContext context) {
        MatrixStack matrixStack = context.matrixStack();
        Matrix4f projectionMatrix = context.projectionMatrix();
        float tickDelta = context.tickDelta();
        Vec3d cameraPos = context.camera().getPos(); // Get camera world position

        // Combine projection matrix once
        Matrix4f projMatrixCopy = new Matrix4f(projectionMatrix);

        // Clear previous frame's coords
        pingClipCoordinates.clear(); 

        for (var entry : this.pings.entrySet()) {
            for (var ping : entry.getValue()) {
                // Determine the effective target position with interpolation for entities
                Vec3d effectiveTargetPos;
                if (ping.type == PingPoint.PingType.ENTITY && ping.entityUUID != null) {
                    Entity targetEntity = findEntityByUUID(ping.entityUUID);
                    if (targetEntity != null) {
                        Vec3d interpolatedFeetPos = targetEntity.getLerpedPos(tickDelta);
                        effectiveTargetPos = interpolatedFeetPos.add(0, targetEntity.getHeight() / 2.0, 0);
                    } else {
                        effectiveTargetPos = ping.pos;
                    }
                } else {
                    effectiveTargetPos = ping.pos;
                }

                // Simulate render transform: Translate matrix stack to the object's position relative to camera
                matrixStack.push();
                matrixStack.translate(effectiveTargetPos.x - cameraPos.x, 
                                    effectiveTargetPos.y - cameraPos.y, 
                                    effectiveTargetPos.z - cameraPos.z);

                // Get the specific ModelView matrix for this translated position
                Matrix4f currentModelViewMatrix = matrixStack.peek().getPositionMatrix();
                
                // Calculate MVP for this specific point
                Matrix4f mvpMatrix = new Matrix4f(projMatrixCopy).mul(currentModelViewMatrix);

                // Transform the origin (0,0,0,1) because the translation is now baked into the matrix
                Vector4f clipPos = mvpMatrix.transform(new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
                
                pingClipCoordinates.put(ping.id, clipPos);
                matrixStack.pop(); // Restore matrix stack state
            }
        }
    }
}
