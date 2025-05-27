package app.jyu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static app.jyu.NetworkingConstants.PING_PACKET;
import static app.jyu.NetworkingConstants.REMOVE_PING_PACKET;

// Data classes for storing blocked events
class BlockedEntityAttackEvent {
    public final net.minecraft.entity.player.PlayerEntity player;
    public final net.minecraft.world.World world;
    public final Hand hand;
    public final Entity entity;
    public final net.minecraft.util.hit.EntityHitResult hitResult;
    public final float damageAmount;
    public final DamageSource damageSource;
    public final long timestamp;
    
    public BlockedEntityAttackEvent(net.minecraft.entity.player.PlayerEntity player, net.minecraft.world.World world, Hand hand, Entity entity, net.minecraft.util.hit.EntityHitResult hitResult, float damageAmount, DamageSource damageSource) {
        this.player = player;
        this.world = world;
        this.hand = hand;
        this.entity = entity;
        this.hitResult = hitResult;
        this.damageAmount = damageAmount;
        this.damageSource = damageSource;
        this.timestamp = System.currentTimeMillis();
    }
}

class BlockedBlockBreakEvent {
    public final net.minecraft.world.World world;
    public final net.minecraft.entity.player.PlayerEntity player;
    public final BlockPos pos;
    public final BlockState state;
    public final net.minecraft.block.entity.BlockEntity blockEntity;
    public final long timestamp;
    
    public BlockedBlockBreakEvent(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity) {
        this.world = world;
        this.player = player;
        this.pos = pos;
        this.state = state;
        this.blockEntity = blockEntity;
        this.timestamp = System.currentTimeMillis();
    }
}

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
    
    // ThreadLocal flag to prevent re-entrancy in damage logic
    public static final ThreadLocal<Boolean> IS_APPLYING_BLOCKED_DAMAGE = ThreadLocal.withInitial(() -> false);
    
    // Storage for blocked events waiting for ping cancellation
    // Ping ID -> ArrayList of BlockedEntityAttackEvent
    private static final Map<UUID, BlockedEntityAttackEvent> blockedEntityAttacks = new ConcurrentHashMap<>();
    // Entity ID -> Ping ID // TODO: to optimize (one or many ping per entity)
    public static final Map<UUID, UUID> blockingEntityToPingId = new ConcurrentHashMap<>();
    // Ping ID -> BlockedBlockBreakEvent
    private static final Map<UUID, BlockedBlockBreakEvent> blockedBlockBreaks = new ConcurrentHashMap<>();

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

        // register all new sound events
        soundEventsForPing = new ArrayList<>();
        Arrays.stream(newSounds).forEach((soundStr) -> {
            Identifier soundId = new Identifier(soundStr);
            SoundEvent soundEvent = SoundEvent.of(soundId);
            Registry.register(Registries.SOUND_EVENT, soundId, soundEvent);
            soundEventsForPing.add(soundEvent);
        });
        soundEventsForPing.add(SoundEvents.BLOCK_ANVIL_BREAK);

        // register all event handlers
        ServerPlayNetworking.registerGlobalReceiver(PING_PACKET, PingSystem::onReceivingPingPacket);
        ServerPlayNetworking.registerGlobalReceiver(REMOVE_PING_PACKET, PingSystem::onReceivingRemovePingPacket);
        PlayerBlockBreakEvents.BEFORE.register(PingSystem::onBlockBreak);
        ServerTickEvents.END_SERVER_TICK.register(PingSystem::onEndServerTick);

        // load all quizzes
        Quiz.getQuizMap();
    }

    public static void onReceivingRemovePingPacket(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender){
        // Extract ping ID before multicasting to determine which events to execute
        PacketByteBuf bufCopy = new PacketByteBuf(buf.copy());
        UUID removedPingId = null;
        try {
            PingPoint removedPing = PingPoint.fromPacketByteBuf(bufCopy);
            removedPingId = removedPing.id;
        } catch (Exception e) {
            LOGGER.error("[PingSystem Server] Failed to deserialize PingPoint on REMOVE_PING_PACKET receive for event execution.", e);
        }
        bufCopy.release();
        // Handle ping removal
        multicastRemovePingIncludeSelf(player, REMOVE_PING_PACKET, buf);
        // Execute only the blocked events associated with this specific ping
        if (removedPingId != null) {
            executeBlockedEventsForPing(removedPingId);
        }
    }

    public static void onReceivingPingPacket(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender){
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
        multicastPingExcludeSelf(player, PING_PACKET, buf); 
    }

    public static void multicastPingIncludeSelf(ServerPlayerEntity sender, Identifier channelName, PacketByteBuf buf) {
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
                // play sound for all // TODO: how to play for only a few people?
                teammate.getWorld().playSound(
                        null, // Player - if non-null, will play sound for every nearby player *except* the specified player
                        teammate.getBlockPos(), // The position of where the sound will come from
                        soundEvent,
                        SoundCategory.BLOCKS, // This determines which of the volume sliders affect this sound
                        1f, // Volume multiplier, 1 is normal, 0.5 is half volume, etc
                        1f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
                );
                var bufNew = PacketByteBufs.copy(buf.asByteBuf());
                ServerPlayNetworking.send(teammate, channelName, bufNew);
                LOGGER.info("%s send ping to %s".formatted(senderName, teammateName));
            }
        }
    }

    public static void multicastPingExcludeSelf(ServerPlayerEntity sender, Identifier channelName, PacketByteBuf buf) {
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

    public static void multicastRemovePingIncludeSelf(ServerPlayerEntity sender, Identifier channelName, PacketByteBuf buf) {
        if (ENABLE_TEAMS) {
            // TODO implement teams
        } else {
            for (ServerPlayerEntity teammate : PlayerLookup.world((ServerWorld) sender.getWorld())) {
                var senderName = sender.getEntityName();
                var teammateName = teammate.getEntityName();
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

    // Server tick handler to turn off glowing and clean up expired blocked events
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
        
        // Clean up expired blocked events (timeout after 30 seconds)
        cleanupExpiredBlockedEvents(currentTime);
    }
    
    // Check if a block is an ore/mineral block
    private static boolean isOreBlock(Block block) {
        Identifier blockId = Registries.BLOCK.getId(block);
        String blockName = blockId.getPath();
        
        // Check for all types of ores (including deepslate variants)
        return blockName.contains("_ore") || 
               blockName.equals("coal_ore") ||
               blockName.equals("iron_ore") ||
               blockName.equals("gold_ore") ||
               blockName.equals("diamond_ore") ||
               blockName.equals("emerald_ore") ||
               blockName.equals("lapis_ore") ||
               blockName.equals("redstone_ore") ||
               blockName.equals("copper_ore") ||
               blockName.equals("nether_gold_ore") ||
               blockName.equals("nether_quartz_ore") ||
               blockName.equals("ancient_debris") ||
               // Deepslate variants
               blockName.equals("deepslate_coal_ore") ||
               blockName.equals("deepslate_iron_ore") ||
               blockName.equals("deepslate_gold_ore") ||
               blockName.equals("deepslate_diamond_ore") ||
               blockName.equals("deepslate_emerald_ore") ||
               blockName.equals("deepslate_lapis_ore") ||
               blockName.equals("deepslate_redstone_ore") ||
               blockName.equals("deepslate_copper_ore");
    }
    
    // Server-side event handler for block breaking - blocks the break and creates a ping (ONLY FOR ORES)
    private static boolean onBlockBreak(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity) {
        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            Block block = state.getBlock();
            
            // Only trigger for ore blocks
            if (!isOreBlock(block)) {
                return true; // Allow break to proceed for non-ore blocks
            }
            // Store the blocked break event
            UUID pingId = UUID.randomUUID();
            BlockedBlockBreakEvent blockedEvent = new BlockedBlockBreakEvent(world, player, pos, state, blockEntity);
            blockedBlockBreaks.put(pingId, blockedEvent);
            
            // Create ping for the ore block being broken
            Vec3d pingPos = Vec3d.ofCenter(pos);
            PingPoint pingToSend = new PingPoint(pingPos, serverPlayer.getEntityName(), new java.awt.Color(0x00FF00), (byte)0, PingPoint.PingType.LOCATION, null);
            pingToSend.id = pingId; // Associate ping with blocked event
            
            // Create and send ping packet to all players
            try {
                PacketByteBuf buf = pingToSend.toPacketByteBuf();
                multicastPingIncludeSelf(serverPlayer, PING_PACKET, buf);
                LOGGER.info("Created auto-ping for ore break: " + state.getBlock().getName().getString() + " (blocked until ping removed)");
            } catch (Exception e) {
                LOGGER.error("Failed to create ping for block break", e);
                // If ping creation fails, allow the break to proceed
                blockedBlockBreaks.remove(pingId);
                return true; // Allow break to proceed
            }
            
            return false; // Block the break (false = cancel)
        }
        return true; // Allow break to proceed
    }
    
    // Clean up blocked events that have expired (timeout after 30 seconds)
    private static void cleanupExpiredBlockedEvents(long currentTime) {
        final long BLOCKED_EVENT_TIMEOUT_MS = 30000; // 30 seconds
        
        // Clean up expired entity damage blocks
        blockedEntityAttacks.entrySet().removeIf(entry -> {
            BlockedEntityAttackEvent event = entry.getValue();
            boolean expired = currentTime - event.timestamp > BLOCKED_EVENT_TIMEOUT_MS;
            if (expired) {
                LOGGER.warn("Cleaned up expired blocked entity damage for player: " + event.player.getEntityName() + " (ping ID: " + entry.getKey() + ")");
                blockingEntityToPingId.remove(event.entity.getUuid());
            }
            return expired;
        });

        // Clean up expired block breaks
        blockedBlockBreaks.entrySet().removeIf(entry -> {
            BlockedBlockBreakEvent event = entry.getValue();
            boolean expired = currentTime - event.timestamp > BLOCKED_EVENT_TIMEOUT_MS;
            if (expired) {
                LOGGER.warn("Cleaned up expired blocked block break for player: " + event.player.getEntityName() + " (ping ID: " + entry.getKey() + ")");
            }
            return expired;
        });
    }
    
    // Execute blocked events associated with a specific ping ID
    private static void executeBlockedEventsForPing(UUID pingId) {
        // Execute blocked entity damage if it matches this ping ID
        BlockedEntityAttackEvent aEvent = blockedEntityAttacks.remove(pingId);
        if (aEvent != null) {
            if (aEvent.entity.isAlive() && aEvent.entity instanceof LivingEntity livingEntity) {
                // It's safer to create a new DamageSource using the attacker (aEvent.player)
                // at the time of dealing damage, to avoid issues with stale DamageSource objects.
                // aEvent.player is the player whose attack was originally blocked.
                DamageSource newDamageSource;
                if (aEvent.player != null && aEvent.player.isAlive()) {
                    newDamageSource = livingEntity.getDamageSources().playerAttack(aEvent.player);
                } else {
                    // Fallback if the original attacker is no longer valid
                    newDamageSource = livingEntity.getDamageSources().generic();
                    LOGGER.warn("Original attacker for blocked damage (ping ID: {}) is no longer valid. Using generic damage.", pingId);
                }

                IS_APPLYING_BLOCKED_DAMAGE.set(true);
                try {
                    livingEntity.damage(newDamageSource, aEvent.damageAmount);
                } finally {
                    IS_APPLYING_BLOCKED_DAMAGE.set(false); // Or .set(false) if you prefer explicit false over initial value
                }
            }
            blockingEntityToPingId.remove(aEvent.entity.getUuid());
            LOGGER.info("Executed blocked entity damage for ping ID: " + pingId + " (player: " + aEvent.player.getEntityName() + ", damage: " + aEvent.damageAmount + ")");
        }
        // Execute blocked block break if it matches this ping ID
        BlockedBlockBreakEvent bEvent = blockedBlockBreaks.remove(pingId);
        if (bEvent != null) {
            if (bEvent.world.getBlockState(bEvent.pos).equals(bEvent.state)) {
                bEvent.world.breakBlock(bEvent.pos, true, bEvent.player);
            }
            LOGGER.info("Executed blocked block break for ping ID: " + pingId + " (player: " + bEvent.player.getEntityName() + ")");
        }
    }

    public static void beforeInvokingBlockedByShieldInDamage(LivingEntity self, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LOGGER.info(">> beforeInvokingBlockedByShieldInDamage");
        if (IS_APPLYING_BLOCKED_DAMAGE.get()) {
            LOGGER.info("<< beforeInvokingBlockedByShieldInDamage: Skipping mixin logic for blocked damage because it's from executeBlockedEventsForPing");
            return; // Skip mixin logic if this damage application is from executeBlockedEventsForPing
        }
        if (self.isDead() || self.getWorld().isClient() || !(source.getSource() instanceof ServerPlayerEntity serverPlayer)) { 
            return; 
        }

        // only one ping each entity
        if (blockingEntityToPingId.get(self.getUuid()) != null) {
            LOGGER.info("<< beforeInvokingBlockedByShieldInDamage: Blocking damage and only one ping each entity is allowed");
            // TODO: why setReturnValue to true but still not excuting takeShieldHit;
            self.takeKnockback(0.5, serverPlayer.getX() - self.getX(), serverPlayer.getZ() - self.getZ());
            cir.setReturnValue(true);
            return;
        }
        // gen ping
        Vec3d pingPos = self.getBoundingBox().getCenter();
        PingPoint pingToSend = new PingPoint(pingPos, serverPlayer.getEntityName(), new java.awt.Color(0xFF0000), (byte)2, PingPoint.PingType.ENTITY, self.getUuid());
        try {
            PacketByteBuf buf = pingToSend.toPacketByteBuf();
            multicastPingIncludeSelf(serverPlayer, PING_PACKET, buf);
            LOGGER.info("Created auto-ping for attacked entity: " + self.getName().getString() + " (damage blocked until ping removed)");
        } catch (Exception e) {
            LOGGER.error("<< beforeInvokingBlockedByShieldInDamage: Failed to create ping for attacked entity", e);
            return;
        }
        // Store the mapping between the blocking self and the ping ID
        blockingEntityToPingId.put(self.getUuid(), pingToSend.id);
        // Store the blocked event
        BlockedEntityAttackEvent blockedEvent = new BlockedEntityAttackEvent(
            serverPlayer, self.getWorld(), Hand.MAIN_HAND, self, null, amount, source);
        blockedEntityAttacks.put(pingToSend.id, blockedEvent);
        LOGGER.info("<< beforeInvokingBlockedByShieldInDamage: Blocked damage and created ping for entity: " + self.getName().getString() + " (damage blocked until ping removed)");
        // TODO: why setReturnValue to true but still not excuting takeShieldHit; and why the dir is reversed
        self.takeKnockback(0.5, serverPlayer.getX() - self.getX(), serverPlayer.getZ() - self.getZ());
        cir.setReturnValue(true);
    }
}