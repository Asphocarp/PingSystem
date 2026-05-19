package app.jyu.common;

import app.jyu.common.platform.IPlatformNetworkService;
import app.jyu.common.platform.IPlatformServerEventService;
import app.jyu.common.platform.IPlatformSoundService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SophisticatedPingCommon {
    public static final long GLOW_DURATION_MS = 5000;
    private static final Map<UUID, Long> GLOWING_ENTITIES = new ConcurrentHashMap<>();

    private static final List<String> CUSTOM_SOUNDS = List.of(
            "ping_location",
            "ping_item",
            "ping_enemy",
            "mozambique_lifeline"
    );


    private SophisticatedPingCommon() {
    }

    public static void init() {
        Constants.LOGGER.info("Make MC Apex Again!");
        IPlatformSoundService.INSTANCE.registerSounds(CUSTOM_SOUNDS);
        IPlatformServerEventService.INSTANCE.registerEndServerTick(SophisticatedPingCommon::onEndServerTick);
    }

    public static int soundCount() {
        return CUSTOM_SOUNDS.size() + 1;
    }

    public static void onPingPacket(MinecraftServer server, ServerPlayer sender, PingPoint point) {
        if (point == null || point.isCorrupt()) {
            Constants.LOGGER.warn("Ignoring corrupt ping packet from {}", sender.getGameProfile().getName());
            return;
        }

        handleGlow(server, sender, point);
        multicastPing(sender, point);
    }

    public static void onRemovePingPacket(ServerPlayer sender, PingPoint point) {
        if (point == null || point.isCorrupt()) {
            Constants.LOGGER.warn("Ignoring corrupt remove ping packet from {}", sender.getGameProfile().getName());
            return;
        }

        for (ServerPlayer teammate : sender.serverLevel().players()) {
            if (teammate.getUUID().equals(sender.getUUID())) {
                continue;
            }
            IPlatformNetworkService.INSTANCE.sendRemovePingToClient(teammate, point);
        }
    }

    private static void multicastPing(ServerPlayer sender, PingPoint point) {
        SoundEvent sound = soundIdxToEvent(point.sound());
        for (ServerPlayer teammate : sender.serverLevel().players()) {
            teammate.level().playSound(
                    null,
                    teammate.blockPosition(),
                    sound,
                    SoundSource.BLOCKS,
                    1f,
                    1f
            );

            if (teammate.getUUID().equals(sender.getUUID())) {
                continue;
            }

            IPlatformNetworkService.INSTANCE.sendPingToClient(teammate, point);
            Constants.LOGGER.debug("{} sent ping to {}", sender.getGameProfile().getName(), teammate.getGameProfile().getName());
        }
    }

    private static void handleGlow(MinecraftServer server, ServerPlayer sender, PingPoint point) {
        if (point.type() != PingPoint.PingType.ENTITY || point.entityUuid() == null) {
            return;
        }

        UUID entityUuid = point.entityUuid();
        long glowEndTime = System.currentTimeMillis() + GLOW_DURATION_MS;
        server.execute(() -> {
            Entity entity = findEntity(server, sender.serverLevel(), entityUuid);
            if (entity == null) {
                Constants.LOGGER.warn("Received highlight request for {}, but entity was not found", entityUuid);
                return;
            }

            entity.setGlowingTag(true);
            GLOWING_ENTITIES.put(entityUuid, glowEndTime);
        });
    }

    private static Entity findEntity(MinecraftServer server, ServerLevel preferredLevel, UUID entityUuid) {
        Entity entity = preferredLevel.getEntity(entityUuid);
        if (entity != null) {
            return entity;
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (level == preferredLevel) {
                continue;
            }
            entity = level.getEntity(entityUuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static void onEndServerTick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        GLOWING_ENTITIES.entrySet().removeIf(entry -> {
            if (now < entry.getValue()) {
                return false;
            }

            server.execute(() -> {
                for (ServerLevel level : server.getAllLevels()) {
                    Entity entity = level.getEntity(entry.getKey());
                    if (entity != null && entity.isCurrentlyGlowing()) {
                        entity.setGlowingTag(false);
                    }
                }
            });
            return true;
        });
    }

    private static SoundEvent soundIdxToEvent(byte soundIdx) {
        if (soundIdx >= 0 && soundIdx < CUSTOM_SOUNDS.size()) {
            return IPlatformSoundService.INSTANCE.sound(CUSTOM_SOUNDS.get(soundIdx));
        }
        return SoundEvents.ANVIL_BREAK;
    }
}
