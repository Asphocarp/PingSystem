package app.jyu;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Server-side global configuration for QuizCraft
 * This config is shared across all players and can only be modified by OPs
 */
public class ServerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ServerConfig");
    private static final String CONFIG_FILENAME = "quiz_craft.server.properties";
    
    // Default values
    public static final int DEFAULT_CURRENT_BOOK_ID = 1;
    public static final int DEFAULT_HIGHLIGHT_COLOR = 0xFFEB9D39;
    public static final boolean DEFAULT_QUIZ_ENABLED = true;
    public static final int DEFAULT_QUIZ_TIMEOUT_SECONDS = 30;
    
    // Current configuration values
    private static volatile int currentBookId = DEFAULT_CURRENT_BOOK_ID;
    private static volatile int highlightColor = DEFAULT_HIGHLIGHT_COLOR;
    private static volatile boolean quizEnabled = DEFAULT_QUIZ_ENABLED;
    private static volatile int quizTimeoutSeconds = DEFAULT_QUIZ_TIMEOUT_SECONDS;
    
    // Cache for book info
    private static final Map<Integer, String> bookInfoCache = new ConcurrentHashMap<>();
    
    static {
        initializeBookInfoCache();
    }
    
    private static void initializeBookInfoCache() {
        // Initialize with known book IDs and their descriptions
        bookInfoCache.put(0, "CET-4 (College English Test Band 4)");
        bookInfoCache.put(1, "CET-6 (College English Test Band 6)"); 
        bookInfoCache.put(2, "CET-6 High Frequency");
        bookInfoCache.put(3, "CET-4 Must 2000");
        bookInfoCache.put(4, "CET-4 Core");
        bookInfoCache.put(5, "CET-6 Core");
        bookInfoCache.put(6, "CET Phrase Core");
        bookInfoCache.put(7, "TEM-4 (Test for English Majors-4)");
        bookInfoCache.put(8, "TEM-4 Core Light");
        bookInfoCache.put(9, "TEM-8 (Test for English Majors-8)");
        bookInfoCache.put(10, "NEEP (National Entrance Exam for Postgraduates)");
        bookInfoCache.put(11, "NEEP2");
        bookInfoCache.put(12, "NEEP2 Core");
        bookInfoCache.put(13, "IELTS (International English Language Testing System)");
        bookInfoCache.put(14, "IELTS Core");
        bookInfoCache.put(15, "TOEFL (Test of English as a Foreign Language)");
        bookInfoCache.put(16, "TOEFL Core");
        bookInfoCache.put(17, "TOEFL Core Light");
    }
    
    /**
     * Load configuration from file
     */
    public static void loadConfig(MinecraftServer server) {
        Path configPath = getConfigPath(server);
        
        if (!Files.exists(configPath)) {
            LOGGER.info("Server config file not found, creating with defaults: {}", configPath);
            saveConfig(server);
            return;
        }
        
        try {
            Properties props = new Properties();
            props.load(Files.newInputStream(configPath));
            
            currentBookId = Integer.parseInt(props.getProperty("currentBookId", String.valueOf(DEFAULT_CURRENT_BOOK_ID)));
            highlightColor = Integer.parseInt(props.getProperty("highlightColor", String.valueOf(DEFAULT_HIGHLIGHT_COLOR)));
            quizEnabled = Boolean.parseBoolean(props.getProperty("quizEnabled", String.valueOf(DEFAULT_QUIZ_ENABLED)));
            quizTimeoutSeconds = Integer.parseInt(props.getProperty("quizTimeoutSeconds", String.valueOf(DEFAULT_QUIZ_TIMEOUT_SECONDS)));
            
            LOGGER.info("Loaded server config: bookId={}, highlightColor=0x{}, quizEnabled={}, quizTimeout={}s", 
                currentBookId, Integer.toHexString(highlightColor), quizEnabled, quizTimeoutSeconds);
                
        } catch (Exception e) {
            LOGGER.error("Failed to load server config, using defaults", e);
            resetToDefaults();
        }
    }
    
    /**
     * Save configuration to file
     */
    public static void saveConfig(MinecraftServer server) {
        Path configPath = getConfigPath(server);
        
        try {
            // Ensure parent directory exists
            Files.createDirectories(configPath.getParent());
            
            Properties props = new Properties();
            props.setProperty("currentBookId", String.valueOf(currentBookId));
            props.setProperty("highlightColor", String.valueOf(highlightColor));
            props.setProperty("quizEnabled", String.valueOf(quizEnabled));
            props.setProperty("quizTimeoutSeconds", String.valueOf(quizTimeoutSeconds));
            
            props.store(Files.newOutputStream(configPath), "QuizCraft Server Configuration");
            LOGGER.info("Saved server config to: {}", configPath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to save server config", e);
        }
    }
    
    private static Path getConfigPath(MinecraftServer server) {
        return Paths.get(server.getRunDirectory().getAbsolutePath(), "config", CONFIG_FILENAME);
    }
    
    private static void resetToDefaults() {
        currentBookId = DEFAULT_CURRENT_BOOK_ID;
        highlightColor = DEFAULT_HIGHLIGHT_COLOR;
        quizEnabled = DEFAULT_QUIZ_ENABLED;
        quizTimeoutSeconds = DEFAULT_QUIZ_TIMEOUT_SECONDS;
    }
    
    // Getters
    public static int getCurrentBookId() { return currentBookId; }
    public static int getHighlightColor() { return highlightColor; }
    public static boolean isQuizEnabled() { return quizEnabled; }
    public static int getQuizTimeoutSeconds() { return quizTimeoutSeconds; }
    
    // Setters (with validation)
    public static void setCurrentBookId(int bookId) {
        if (bookId >= 0 && bookId < 18) { // Valid book ID range
            currentBookId = bookId;
            LOGGER.info("Server config: Set current book ID to {} ({})", bookId, getBookName(bookId));
        } else {
            LOGGER.warn("Invalid book ID: {}, keeping current value: {}", bookId, currentBookId);
        }
    }
    
    public static void setHighlightColor(int color) {
        highlightColor = color;
        LOGGER.info("Server config: Set highlight color to 0x{}", Integer.toHexString(color));
    }
    
    public static void setQuizEnabled(boolean enabled) {
        quizEnabled = enabled;
        LOGGER.info("Server config: Set quiz enabled to {}", enabled);
    }
    
    public static void setQuizTimeoutSeconds(int timeout) {
        if (timeout > 0 && timeout <= 300) { // 1 second to 5 minutes
            quizTimeoutSeconds = timeout;
            LOGGER.info("Server config: Set quiz timeout to {} seconds", timeout);
        } else {
            LOGGER.warn("Invalid quiz timeout: {}, keeping current value: {}", timeout, quizTimeoutSeconds);
        }
    }
    
    // Utility methods
    public static String getBookName(int bookId) {
        return bookInfoCache.getOrDefault(bookId, "Unknown Book " + bookId);
    }
    
    public static Map<Integer, String> getAvailableBooks() {
        return new ConcurrentHashMap<>(bookInfoCache);
    }
    
    /**
     * Create a packet with current config for client synchronization
     */
    public static PacketByteBuf createConfigPacket() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(currentBookId);
        buf.writeInt(highlightColor);
        buf.writeBoolean(quizEnabled);
        buf.writeInt(quizTimeoutSeconds);
        return buf;
    }
    
    /**
     * Update config from client packet (for GUI changes)
     */
    public static void updateFromPacket(PacketByteBuf buf, MinecraftServer server) {
        try {
            setCurrentBookId(buf.readInt());
            setHighlightColor(buf.readInt());
            setQuizEnabled(buf.readBoolean());
            setQuizTimeoutSeconds(buf.readInt());
            
            // Save immediately after update
            saveConfig(server);
            
        } catch (Exception e) {
            LOGGER.error("Failed to update server config from packet", e);
        }
    }
    
    /**
     * Check if a player has permission to modify server config
     */
    public static boolean hasConfigPermission(ServerPlayerEntity player) {
        return player.hasPermissionLevel(2); // OP level 2 or higher
    }
} 