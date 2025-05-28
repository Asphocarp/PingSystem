package app.jyu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
import static app.jyu.NetworkingConstants.ANSWER_PACKET;
import static app.jyu.NetworkingConstants.REQUEST_CONFIG_PACKET;
import static app.jyu.NetworkingConstants.UPDATE_CONFIG_PACKET;
import static app.jyu.NetworkingConstants.OPEN_CONFIG_GUI_PACKET;
import static app.jyu.NetworkingConstants.SYNC_CONFIG_PACKET;

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

public class QuizCraft implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final String MOD_ID = "quiz_craft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ArrayList<QuizCraftTeam> teams = new ArrayList<>();
    public static boolean ENABLE_TEAMS = false;
    public static final long GLOW_DURATION_MS = 5000; // 5 seconds in milliseconds
    // Map to store UUIDs of glowing entities and their glow end time (System.currentTimeMillis())
    private static final Map<UUID, Long> glowingEntities = new ConcurrentHashMap<>();
    
    // Constants for block break pings
    public static final java.awt.Color BLOCK_BREAK_PING_COLOR = new java.awt.Color(0xEB9D39);
    public static final byte BLOCK_BREAK_PING_SOUND_INDEX = 0;
    // Constants for entity damage pings
    public static final java.awt.Color ENTITY_DAMAGE_PING_COLOR = new java.awt.Color(0xEB9D39);
    public static final byte ENTITY_DAMAGE_PING_SOUND_INDEX = 2;
    
    // ThreadLocal flag to prevent re-entrancy in damage logic
    public static final ThreadLocal<Boolean> IS_APPLYING_BLOCKED_DAMAGE = ThreadLocal.withInitial(() -> false);
    
    // ThreadLocal to store damage amount for redirect method access
    public static final ThreadLocal<Float> CURRENT_DAMAGE_AMOUNT = ThreadLocal.withInitial(() -> 0.0f);
    
    // Storage for blocked events waiting for ping cancellation
    // Entity ID -> Ping ID // TODO: to optimize (one or many ping per entity)
    public static final Map<UUID, UUID> blockingEntityToPingId = new ConcurrentHashMap<>();
    // Ping ID -> BlockedEntityAttackEvent
    private static final Map<UUID, BlockedEntityAttackEvent> blockedEntityAttacks = new ConcurrentHashMap<>();
    // Block Pos -> Ping ID
    public static final Map<BlockPos, UUID> blockingBlockPosToPingId = new ConcurrentHashMap<>();
    // Ping ID -> BlockedBlockBreakEvent
    private static final Map<UUID, BlockedBlockBreakEvent> blockedBlockBreaks = new ConcurrentHashMap<>();
    // Active pings for answer processing
    private static final Map<UUID, PingPoint> activePings = new ConcurrentHashMap<>();

    public static String[] newSounds = {
            "quiz_craft:ping_location",
            "quiz_craft:ping_item",
            "quiz_craft:ping_enemy",
            "quiz_craft:mozambique_lifeline",
    };
    // currently include newSounds and SoundEvents.BLOCK_ANVIL_BREAK
    public static ArrayList<SoundEvent> soundEventsForPing;

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Make MC Apex Again!");

        // load all quizzes
        Quiz.loadQuizMap();
        Book.loadBooks();

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
        ServerPlayNetworking.registerGlobalReceiver(PING_PACKET, QuizCraft::onReceivingPingPacket);
        ServerPlayNetworking.registerGlobalReceiver(REMOVE_PING_PACKET, QuizCraft::onReceivingRemovePingPacket);
        ServerPlayNetworking.registerGlobalReceiver(ANSWER_PACKET, QuizCraft::onReceivingAnswerPacket);
        
        // register server config event handlers
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_CONFIG_PACKET, QuizCraft::onReceivingRequestConfigPacket);
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_CONFIG_PACKET, QuizCraft::onReceivingUpdateConfigPacket);
        ServerPlayNetworking.registerGlobalReceiver(OPEN_CONFIG_GUI_PACKET, QuizCraft::onReceivingOpenConfigGuiPacket);
        
        PlayerBlockBreakEvents.BEFORE.register(QuizCraft::onBlockBreak);
        ServerTickEvents.END_SERVER_TICK.register(QuizCraft::onEndServerTick);
        
        // Initialize server config when server starts
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Loading QuizCraft server configuration...");
            ServerConfig.loadConfig(server);
        });
        
        // Register commands
        PingConfigCommand.register();
    }

    public static void onReceivingRemovePingPacket(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender){
        // for debug, player here force remove the ping and give up (as wrong answer)

        // get pingToRemove
        PacketByteBuf bufCopy = new PacketByteBuf(buf.copy());
        PingPoint pingToRemove = null;
        try {
            pingToRemove = PingPoint.fromPacketByteBuf(bufCopy);
        } catch (Exception e) {
            LOGGER.error("[QuizCraft Server] Failed to deserialize PingPoint on REMOVE_PING_PACKET receive for event execution.", e);
            return;
        }
        bufCopy.release();
        if (pingToRemove == null) {
            LOGGER.warn("[QuizCraft Server] Could not find ping for remove ping packet from player {}", player.getEntityName());
            return;
        }

        executeBlockedEventsForPing(pingToRemove, false);
        removePingAndMulticast(player, pingToRemove);
    }

    public static void onReceivingAnswerPacket(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        try {
            AnswerPacket answerPacket = AnswerPacket.fromPacketByteBuf(buf);
            LOGGER.info("[QuizCraft Server] Received answer from player {}: ping UUID {}, answer index {}", 
                answerPacket.playerName, answerPacket.pingUUID, answerPacket.answerIndex);
            
            // get the ping
            PingPoint ping = activePings.get(answerPacket.pingUUID);
            if (ping == null) {
                LOGGER.warn("[QuizCraft Server] Could not find ping for answer packet from player {}", answerPacket.playerName);
                return;
            }
            if (ping.quiz == null) {
                LOGGER.warn("[QuizCraft Server] Could not find quiz for ping UUID {}", answerPacket.pingUUID);
                return;
            }
            
            // Check if the answer is correct
            boolean isCorrect = ping.quiz.isCorrectAnswer(answerPacket.answerIndex);
            executeBlockedEventsForPing(ping, isCorrect);
            removePingAndMulticast(player, ping);
        } catch (Exception e) {
            LOGGER.error("[QuizCraft Server] Failed to process answer packet from player {}", player.getEntityName(), e);
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
            LOGGER.error("[QuizCraft Server] Failed to deserialize PingPoint on PING_PACKET receive for glow check.", e);
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
                    LOGGER.info("[QuizCraft Server] PING_PACKET: Received highlight request for {}. Setting glowing until {}.", entity.getName().getString(), glowEndTime);
                    entity.setGlowing(true);
                    glowingEntities.put(entityUUID, glowEndTime); 
                } else {
                    LOGGER.warn("[QuizCraft Server] PING_PACKET: Received highlight request for UUID {}, but entity not found.", entityUUID);
                }
            });
        }
        // --- End: Handle Entity Glow Trigger --- 

        // Proceed to multicast the original ping data to other clients
        multicastPingExcludeSelf(player, PING_PACKET, buf); 
    }

    public static void multicastPingIncludeSelf(ServerPlayerEntity sender, Identifier channelName, PacketByteBuf buf) {
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

    public static void multicastPingExcludeSelf(ServerPlayerEntity sender, Identifier channelName, PacketByteBuf buf) {
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

    private static SoundEvent soundIdxToEvent(byte soundIdx) {
        // if out of range, just return the first one
        try {
            return soundEventsForPing.get(soundIdx);
        } catch (IndexOutOfBoundsException e) {
            return soundEventsForPing.get(0);
        }
    }

    public static void removePingAndMulticast(ServerPlayerEntity sender, PingPoint ping) {
        // TODO: support ping channel (for teams)
        // TODO: 2 maybe only allow owner / same team to remove ping
        // remove the ping
        activePings.remove(ping.id);
        // multicast remove ping
        var senderName = sender.getEntityName();
        for (ServerPlayerEntity teammate : PlayerLookup.world((ServerWorld) sender.getWorld())) {
            var teammateName = teammate.getEntityName();
            try {
                var buf = ping.toPacketByteBuf();
                ServerPlayNetworking.send(teammate, REMOVE_PING_PACKET, buf);
                LOGGER.info("%s send remove ping to %s".formatted(senderName, teammateName));
            } catch (Exception e) {
                LOGGER.error("server fail to serialize the ping packet for %s send remove ping to %s".formatted(senderName, teammateName), e);
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
                            LOGGER.info("[QuizCraft Server] Turning off glow for expired entity: {}", entity.getName().getString());
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
        if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }
        Block block = state.getBlock();
        // Only trigger for ore blocks
        if (!isOreBlock(block)) {
            return true; // Allow break to proceed for non-ore blocks
        }
        // detect if there is already a ping for this block, do not break/ping it again
        if (blockingBlockPosToPingId.containsKey(pos)) {
            return false;
        }
        
        // Create ping, and multicast it
        Vec3d pingPos = Vec3d.ofCenter(pos);
        PingPoint pingToSend = new PingPoint(pingPos, serverPlayer.getEntityName(), BLOCK_BREAK_PING_COLOR, BLOCK_BREAK_PING_SOUND_INDEX);
        try {
            PacketByteBuf buf = pingToSend.toPacketByteBuf();
            multicastPingIncludeSelf(serverPlayer, PING_PACKET, buf);
            LOGGER.info("Created auto-ping for ore break: " + state.getBlock().getName().getString() + " (blocked until ping removed)");
        } catch (Exception e) {
            LOGGER.error("Failed to create ping for block break", e);
            return true; // Allow break to proceed
        }
        // Store stuff
        BlockedBlockBreakEvent blockedEvent = new BlockedBlockBreakEvent(world, player, pos, state, blockEntity);
        blockedBlockBreaks.put(pingToSend.id, blockedEvent);
        activePings.put(pingToSend.id, pingToSend);
        blockingBlockPosToPingId.put(pos, pingToSend.id);
        return false; // Block the break (false = cancel)
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
                activePings.remove(entry.getKey());
            }
            return expired;
        });

        // Clean up expired block breaks
        blockedBlockBreaks.entrySet().removeIf(entry -> {
            BlockedBlockBreakEvent event = entry.getValue();
            boolean expired = currentTime - event.timestamp > BLOCKED_EVENT_TIMEOUT_MS;
            if (expired) {
                LOGGER.warn("Cleaned up expired blocked block break for player: " + event.player.getEntityName() + " (ping ID: " + entry.getKey() + ")");
                blockingBlockPosToPingId.remove(event.pos);
                activePings.remove(entry.getKey());
            }
            return expired;
        });
    }
    
    // Execute blocked events associated with a specific ping ID
    private static void executeBlockedEventsForPing(PingPoint ping, boolean isCorrect) {
        String correctAnswer = "";
        String correctPair = "";
        if (ping != null && ping.quiz != null) {
            correctAnswer = ping.quiz.options[ping.quiz.answer];
            correctPair = String.format("%s: %s", correctAnswer, ping.quiz.question);
        }
        
        // Execute blocked entity damage if it matches this ping ID
        BlockedEntityAttackEvent aEvent = blockedEntityAttacks.remove(ping.id);
        if (aEvent != null) {
            if (isCorrect) {
                // Reward: Resume damage and give player Strength I for 6 seconds
                if (aEvent.entity.isAlive() && aEvent.entity instanceof LivingEntity livingEntity) {
                    // Apply the original damage
                    DamageSource newDamageSource;
                    if (aEvent.player != null && aEvent.player.isAlive()) {
                        newDamageSource = livingEntity.getDamageSources().playerAttack(aEvent.player);
                    } else {
                        newDamageSource = livingEntity.getDamageSources().generic();
                        LOGGER.warn("Original attacker for blocked damage (ping ID: {}) is no longer valid. Using generic damage.", ping.id);
                    }

                    // avoid inf loop
                    IS_APPLYING_BLOCKED_DAMAGE.set(true);
                    try {
                        livingEntity.damage(newDamageSource, aEvent.damageAmount);
                    } finally {
                        IS_APPLYING_BLOCKED_DAMAGE.set(false);
                    }
                    // TODO: add shield broken sound here
                    
                    // Give player Strength I for 6 seconds (120 ticks)
                    if (aEvent.player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                            net.minecraft.entity.effect.StatusEffects.STRENGTH, 120, 0));
                        serverPlayer.sendMessage(net.minecraft.text.Text.literal("§a✓ Correct! You gained Strength for 6 seconds!"), false);
                        serverPlayer.sendMessage(net.minecraft.text.Text.literal("§a   Your answer was: §e" + correctPair), false);
                    }
                }
                LOGGER.info("Executed blocked entity damage with reward for ping ID: " + ping.id + " (player: " + aEvent.player.getEntityName() + ", damage: " + aEvent.damageAmount + ")");
            } else {
                // Penalty: Cancel damage and deal 5 true damage to player
                if (aEvent.player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.damage(serverPlayer.getDamageSources().generic(), 5.0f);
                    serverPlayer.sendMessage(net.minecraft.text.Text.literal("§c✗ Wrong answer! You took 5 damage."), false);
                    serverPlayer.sendMessage(net.minecraft.text.Text.literal("§c   Correct answer was: §e" + correctPair), false);
                }
                LOGGER.info("Applied entity damage penalty for ping ID: " + ping.id + " (player: " + aEvent.player.getEntityName() + ")");
            }
            blockingEntityToPingId.remove(aEvent.entity.getUuid());
        }
        
        // Execute blocked block break if it matches this ping ID
        BlockedBlockBreakEvent bEvent = blockedBlockBreaks.remove(ping.id);
        if (bEvent != null) {
            if (isCorrect) {
                // Reward: Resume break and 25% chance for double drop
                if (bEvent.world.getBlockState(bEvent.pos).equals(bEvent.state)) {
                    bEvent.world.breakBlock(bEvent.pos, true, bEvent.player);
                    
                    // 25% chance for extra break (double drop)
                    if (Math.random() < 0.25) {
                        // Simulate another break by dropping items again
                        var drops = net.minecraft.block.Block.getDroppedStacks(bEvent.state, (net.minecraft.server.world.ServerWorld) bEvent.world, bEvent.pos, bEvent.blockEntity, bEvent.player, net.minecraft.item.ItemStack.EMPTY);
                        for (net.minecraft.item.ItemStack stack : drops) {
                            net.minecraft.block.Block.dropStack(bEvent.world, bEvent.pos, stack);
                        }
                        if (bEvent.player instanceof ServerPlayerEntity serverPlayer) {
                            serverPlayer.sendMessage(net.minecraft.text.Text.literal("§a✓ Correct! Lucky! You got double drops!"), false);
                            serverPlayer.sendMessage(net.minecraft.text.Text.literal("§a   Your answer was: §e" + correctPair), false);
                        }
                    } else {
                        if (bEvent.player instanceof ServerPlayerEntity serverPlayer) {
                            serverPlayer.sendMessage(net.minecraft.text.Text.literal("§a✓ Correct! Block broken successfully!"), false);
                            serverPlayer.sendMessage(net.minecraft.text.Text.literal("§a   Your answer was: §e" + correctPair), false);
                        }
                    }
                }
                LOGGER.info("Executed blocked block break with reward for ping ID: " + ping.id + " (player: " + bEvent.player.getEntityName() + ")");
            } else {
                // Penalty: No drops and destroy iron pickaxe or lower
                if (bEvent.world.getBlockState(bEvent.pos).equals(bEvent.state)) {
                    // Break block without drops
                    bEvent.world.breakBlock(bEvent.pos, false, bEvent.player);
                    
                    // Destroy iron pickaxe or lower
                    if (bEvent.player instanceof ServerPlayerEntity serverPlayer) {
                        net.minecraft.item.ItemStack mainHand = serverPlayer.getMainHandStack();
                        if (isPickaxeIronOrLower(mainHand)) {
                            mainHand.setCount(0); // Destroy the tool
                            serverPlayer.sendMessage(net.minecraft.text.Text.literal("§c✗ Wrong answer! Your pickaxe broke and no drops!"), false);
                            serverPlayer.sendMessage(net.minecraft.text.Text.literal("§c   Correct answer was: §e" + correctPair), false);
                        } else {
                            serverPlayer.sendMessage(net.minecraft.text.Text.literal("§c✗ Wrong answer! No drops!"), false);
                            serverPlayer.sendMessage(net.minecraft.text.Text.literal("§c   Correct answer was: §e" + correctPair), false);
                        }
                    }
                }
                LOGGER.info("Applied block break penalty for ping ID: " + ping.id + " (player: " + bEvent.player.getEntityName() + ")");
            }
            blockingBlockPosToPingId.remove(bEvent.pos);
        }

        // Clean up active ping
        activePings.remove(ping.id);
    }
    
    // Helper method to check if pickaxe is iron or lower tier
    private static boolean isPickaxeIronOrLower(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        net.minecraft.item.Item item = stack.getItem();
        return item == net.minecraft.item.Items.WOODEN_PICKAXE ||
               item == net.minecraft.item.Items.STONE_PICKAXE ||
               item == net.minecraft.item.Items.IRON_PICKAXE ||
               item == net.minecraft.item.Items.GOLDEN_PICKAXE;
    }

    // Called at the start of damage method to store the amount (for redirectBlockedByShield)
    public static void onDamageStart(LivingEntity self, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        CURRENT_DAMAGE_AMOUNT.set(amount);
    }

    public static boolean redirectBlockedByShield(LivingEntity self, DamageSource source) {
        float amount = CURRENT_DAMAGE_AMOUNT.get();
        LOGGER.info(">> redirectBlockedByShield with amount: {}", amount);
        var CURRENT_STRENGTH = 0.2; // TODO: add config for this
        if (self.isDead() || self.getWorld().isClient() || !(source.getSource() instanceof ServerPlayerEntity serverPlayer)) { 
            return self.blockedByShield(source); // Call original method
        }
        if (IS_APPLYING_BLOCKED_DAMAGE.get()) {
            LOGGER.info("<< redirectBlockedByShield: Skipping mixin logic for blocked damage because it's from executeBlockedEventsForPing");
            return false; // Don't block by shield when applying blocked damage
        }

        // Check if should block by shield normally first
        boolean originallyBlocked = self.blockedByShield(source);
        if (originallyBlocked) {
            // if actually blocked by shield, do not add ping
            LOGGER.info("<< redirectBlockedByShield: Skipping mixin logic for blocked damage because the entity is blocking via shield");
            return true; // Return the original blocked result
        }

        // only one ping each entity
        if (blockingEntityToPingId.get(self.getUuid()) != null) {
            LOGGER.info("<< redirectBlockedByShield: Blocking damage and only one ping each entity is allowed");
            self.takeKnockback(CURRENT_STRENGTH, serverPlayer.getX() - self.getX(), serverPlayer.getZ() - self.getZ());
            return true; // Pretend it was blocked
        }
        
        // gen ping
        Vec3d pingPos = self.getBoundingBox().getCenter();
        PingPoint pingToSend = new PingPoint(pingPos, serverPlayer.getEntityName(), ENTITY_DAMAGE_PING_COLOR, ENTITY_DAMAGE_PING_SOUND_INDEX, PingPoint.PingType.ENTITY, self.getUuid());
        
        try {
            PacketByteBuf buf = pingToSend.toPacketByteBuf();
            multicastPingIncludeSelf(serverPlayer, PING_PACKET, buf);
            activePings.put(pingToSend.id, pingToSend);
            LOGGER.info("Created auto-ping for attacked entity: " + self.getName().getString() + " (damage blocked until ping removed)");
        } catch (Exception e) {
            LOGGER.error("<< redirectBlockedByShield: Failed to create ping for attacked entity", e);
            return originallyBlocked;
        }
        
        // Store the mapping between the blocking self and the ping ID
        blockingEntityToPingId.put(self.getUuid(), pingToSend.id);
        // Store the blocked event with actual damage amount from ThreadLocal
        BlockedEntityAttackEvent blockedEvent = new BlockedEntityAttackEvent(
            serverPlayer, self.getWorld(), Hand.MAIN_HAND, self, null, amount, source);
        blockedEntityAttacks.put(pingToSend.id, blockedEvent);
        LOGGER.info("<< redirectBlockedByShield: Blocked damage and created ping for entity: " + self.getName().getString() + " (damage blocked until ping removed)");
        // TODO: fix: not even knockback due to MC-267775
        self.takeKnockback(CURRENT_STRENGTH, serverPlayer.getX() - self.getX(), serverPlayer.getZ() - self.getZ());
        return true; // Pretend it was blocked by shield
    }

    /**
     * Handle request for server configuration from client
     */
    public static void onReceivingRequestConfigPacket(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        LOGGER.info("[QuizCraft Server] Player {} requested server config", player.getEntityName());
        
        // Create and send config sync packet
        try {
            PacketByteBuf configBuf = ServerConfig.createConfigPacket();
            
            // Add available books info
            Map<Integer, String> books = ServerConfig.getAvailableBooks();
            configBuf.writeInt(books.size());
            for (Map.Entry<Integer, String> entry : books.entrySet()) {
                configBuf.writeInt(entry.getKey());
                configBuf.writeString(entry.getValue());
            }
            
            ServerPlayNetworking.send(player, SYNC_CONFIG_PACKET, configBuf);
            LOGGER.info("[QuizCraft Server] Sent config to player {}", player.getEntityName());
            
        } catch (Exception e) {
            LOGGER.error("[QuizCraft Server] Failed to send config to player {}", player.getEntityName(), e);
        }
    }
    
    /**
     * Handle config update from client (OP only)
     */
    public static void onReceivingUpdateConfigPacket(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        LOGGER.info("[QuizCraft Server] Player {} attempting to update server config", player.getEntityName());
        
        // Check permissions
        if (!ServerConfig.hasConfigPermission(player)) {
            LOGGER.warn("[QuizCraft Server] Player {} does not have permission to update server config", player.getEntityName());
            return;
        }
        
        try {
            // Update config from packet
            ServerConfig.updateFromPacket(buf, server);
            LOGGER.info("[QuizCraft Server] Server config updated by player {}", player.getEntityName());
            
            // Broadcast config update to all players
            PacketByteBuf broadcastBuf = ServerConfig.createConfigPacket();
            Map<Integer, String> books = ServerConfig.getAvailableBooks();
            broadcastBuf.writeInt(books.size());
            for (Map.Entry<Integer, String> entry : books.entrySet()) {
                broadcastBuf.writeInt(entry.getKey());
                broadcastBuf.writeString(entry.getValue());
            }
            
            for (ServerPlayerEntity allPlayer : PlayerLookup.all(server)) {
                try {
                    PacketByteBuf playerBuf = PacketByteBufs.create();
                    playerBuf.writeBytes(broadcastBuf.copy());
                    ServerPlayNetworking.send(allPlayer, SYNC_CONFIG_PACKET, playerBuf);
                } catch (Exception e) {
                    LOGGER.error("[QuizCraft Server] Failed to broadcast config update to player {}", allPlayer.getEntityName(), e);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("[QuizCraft Server] Failed to update server config from player {}", player.getEntityName(), e);
        }
    }
    
    /**
     * Handle request to open config GUI (OP only)
     */
    public static void onReceivingOpenConfigGuiPacket(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        LOGGER.info("[QuizCraft Server] Player {} requesting to open config GUI", player.getEntityName());
        
        // Check permissions
        if (!ServerConfig.hasConfigPermission(player)) {
            LOGGER.warn("[QuizCraft Server] Player {} does not have permission to open config GUI", player.getEntityName());
            return;
        }
        
        // Simply send current config - the client will handle opening the GUI
        onReceivingRequestConfigPacket(server, player, handler, PacketByteBufs.create(), responseSender);
    }
}