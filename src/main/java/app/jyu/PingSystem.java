package app.jyu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static app.jyu.NetworkingConstants.PING_PACKET;
import static app.jyu.NetworkingConstants.REMOVE_PING_PACKET;

public class PingSystem implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final String MOD_ID = "ping_system";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ArrayList<PingSystemTeam> teams = new ArrayList<>();
    public static boolean ENABLE_TEAMS = false;
    public static final long GLOW_DURATION_MS = 5000; // 5 seconds in milliseconds
    // Map to store UUIDs of glowing entities and their glow end time (System.currentTimeMillis())
    private static final Map<UUID, Long> glowingEntities = new ConcurrentHashMap<>();

    public static String[] newSounds = {
            "ping_system:ping_location",
            "ping_system:ping_item",
            "ping_system:ping_enemy",
            "ping_system:mozambique_lifeline",
    };
    // currently include newSounds and SoundEvents.BLOCK_ANVIL_BREAK
    public static ArrayList<SoundEvent> soundEventsForPing;

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Make MC Apex Again!");

        // TODO (later) add team command, save state to file

        // register receiver
        ServerPlayNetworking.registerGlobalReceiver(PING_PACKET, ((server, player, handler, buf, responseSender) -> {
            // --- Start: Handle Entity Glow Trigger --- 
            // Need to deserialize PingPoint here to check its type
            // Clone the buffer because deserialization might consume it, and multicast needs the original
            PacketByteBuf bufCopy = new PacketByteBuf(buf.copy()); // Use constructor for deep copy
            PingPoint pingPoint = null;
            try {
                pingPoint = PingPoint.fromPacketByteBuf(bufCopy);
            } catch (Exception e) {
                LOGGER.error("[PingSystem Server] Failed to deserialize PingPoint on PING_PACKET receive for glow check.", e);
            }
            bufCopy.release(); // Release the copied buffer

            if (pingPoint != null && pingPoint.type == PingPoint.PingType.ENTITY && pingPoint.entityUUID != null) {
                UUID entityUUID = pingPoint.entityUUID;
                long glowEndTime = System.currentTimeMillis() + GLOW_DURATION_MS;

                server.execute(() -> { // Ensure execution on the main server thread
                    Entity entity = player.getServerWorld().getEntity(entityUUID);
                    if (entity == null) {
                        for (ServerWorld world : server.getWorlds()) {
                            if (world == player.getServerWorld()) continue;
                            entity = world.getEntity(entityUUID);
                            if (entity != null) break;
                        }
                    }

                    if (entity != null) {
                        LOGGER.info("[PingSystem Server] PING_PACKET: Received highlight request for {}. Setting glowing until {}.", entity.getName().getString(), glowEndTime);
                        entity.setGlowing(true);
                        glowingEntities.put(entityUUID, glowEndTime); 
                    } else {
                        LOGGER.warn("[PingSystem Server] PING_PACKET: Received highlight request for UUID {}, but entity not found.", entityUUID);
                    }
                });
            }
            // --- End: Handle Entity Glow Trigger --- 

            // Proceed to multicast the original ping data to other clients
            multicastPing(player, PING_PACKET, buf); 
        }));
        
        ServerPlayNetworking.registerGlobalReceiver(REMOVE_PING_PACKET, ((server, player, handler, buf, responseSender) -> {
            multicastRemovePing(player, REMOVE_PING_PACKET, buf);
        }));

        // register all new sound events
        soundEventsForPing = new ArrayList<>();
        Arrays.stream(newSounds).forEach((soundStr) -> {
            Identifier soundId = new Identifier(soundStr);
            SoundEvent soundEvent = SoundEvent.of(soundId);
            Registry.register(Registries.SOUND_EVENT, soundId, soundEvent);
            soundEventsForPing.add(soundEvent);
        });
        soundEventsForPing.add(SoundEvents.BLOCK_ANVIL_BREAK);

        // Register server tick event to handle glow duration
        ServerTickEvents.END_SERVER_TICK.register(PingSystem::onEndServerTick);
    }

    public static void multicastPing(ServerPlayerEntity sender, Identifier channelName, PacketByteBuf buf) {
        if (ENABLE_TEAMS) {
            // TODO implement teams
        } else {
            SoundEvent soundEvent;
            try {
                var p = PingPoint.fromPacketByteBuf(buf);
                soundEvent = soundIdxToEvent(p.sound);
            } catch (Exception e) {
                LOGGER.error("server fail to deserialize the ping packet", e);
                return;
            }
            for (ServerPlayerEntity teammate : PlayerLookup.world((ServerWorld) sender.getWorld())) {
                var senderName = sender.getEntityName();
                var teammateName = teammate.getEntityName();
                // play sound for all // TODO how to play for only one
                teammate.getWorld().playSound(
                        null, // Player - if non-null, will play sound for every nearby player *except* the specified player
                        teammate.getBlockPos(), // The position of where the sound will come from
                        soundEvent,
                        SoundCategory.BLOCKS, // This determines which of the volume sliders affect this sound
                        1f, // Volume multiplier, 1 is normal, 0.5 is half volume, etc
                        1f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
                );
                // packet skip oneself
                if (Objects.equals(teammateName, senderName)) {
                    continue;
                }
                var bufNew = PacketByteBufs.copy(buf.asByteBuf());
                ServerPlayNetworking.send(teammate, channelName, bufNew);
                LOGGER.info("%s send ping to %s".formatted(senderName, teammateName));
            }
        }
    }

    private static SoundEvent soundIdxToEvent(byte soundIdx) {
        // if out of range, just return the first one
        try {
            return soundEventsForPing.get(soundIdx);
        } catch (IndexOutOfBoundsException e) {
            return soundEventsForPing.get(0);
        }
    }

    public static void multicastRemovePing(ServerPlayerEntity sender, Identifier channelName, PacketByteBuf buf) {
        if (ENABLE_TEAMS) {
            // TODO implement teams
        } else {
            for (ServerPlayerEntity teammate : PlayerLookup.world((ServerWorld) sender.getWorld())) {
                var senderName = sender.getEntityName();
                var teammateName = teammate.getEntityName();
                // packet skip oneself
                if (Objects.equals(teammateName, senderName)) {
                    continue;
                }
                var bufNew = PacketByteBufs.copy(buf.asByteBuf());
                ServerPlayNetworking.send(teammate, channelName, bufNew);
                LOGGER.info("%s send remove ping to %s".formatted(senderName, teammateName));
            }
        }
    }

    @NotNull
    static Vector3d Vec3dToVector3d(Vec3d cameraDir) {
        Vector3d ret = new Vector3d();
        ret.x = cameraDir.x;
        ret.y = cameraDir.y;
        ret.z = cameraDir.z;
        return ret;
    }

    public static Vector3f Vec3dToV3f(Vec3d v) {
        var ret = new Vector3f();
        ret.y = (float) v.y;
        ret.z = (float) v.z;
        ret.x = (float) v.x;
        return ret;
    }

    // Server tick handler to turn off glowing
    private static void onEndServerTick(MinecraftServer server) {
        long currentTime = System.currentTimeMillis();
        
        // Use iterator to safely remove entries while iterating
        glowingEntities.entrySet().removeIf(entry -> {
            UUID entityUUID = entry.getKey();
            long endTime = entry.getValue();

            if (currentTime >= endTime) {
                server.execute(() -> { // Ensure execution on the main server thread
                    for (ServerWorld world : server.getWorlds()) {
                        Entity entity = world.getEntity(entityUUID);
                        if (entity != null && entity.isGlowing()) { // Check if it's still glowing (might have been turned off otherwise)
                            LOGGER.info("[PingSystem Server] Turning off glow for expired entity: {}", entity.getName().getString());
                            entity.setGlowing(false);
                        }
                    }
                });
                return true; // Remove from map
            }
            return false; // Keep in map
        });
    }
}