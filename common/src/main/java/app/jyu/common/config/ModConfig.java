package app.jyu.common.config;

import app.jyu.common.Constants;
import app.jyu.common.platform.IPlatformContextService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ModConfig {
    public static final int DEFAULT_PING_NUM_EACH = 1;
    public static final boolean DEFAULT_INCLUDE_FLUIDS = false;
    public static final float DEFAULT_ICON_SIZE = 1f;
    public static final int DEFAULT_INFO_COLOR = 0xFFEB9D39;
    public static final long DEFAULT_SECONDS_TO_VANISH = 0;
    public static final int DEFAULT_HIGHLIGHT_COLOR = 0xFFEB9D39;
    public static final byte DEFAULT_SOUND_INDEX = 0;

    public static int pingNumEach = DEFAULT_PING_NUM_EACH;
    public static boolean includeFluids = DEFAULT_INCLUDE_FLUIDS;
    public static float iconSize = DEFAULT_ICON_SIZE;
    public static int infoColor = DEFAULT_INFO_COLOR;
    public static long secondsToVanish = DEFAULT_SECONDS_TO_VANISH;
    public static int highlightColor = DEFAULT_HIGHLIGHT_COLOR;
    public static byte soundIndex = DEFAULT_SOUND_INDEX;

    private ModConfig() {
    }

    public static Path configPath() {
        return IPlatformContextService.INSTANCE.resolveConfigDir(Constants.MOD_ID + ".properties");
    }

    public static void load() {
        load(configPath());
    }

    public static void load(Path file) {
        try {
            if (Files.notExists(file)) {
                save(file);
            }

            Properties cfg = new Properties();
            try (InputStream stream = Files.newInputStream(file)) {
                cfg.load(stream);
            }

            pingNumEach = parseInt(cfg, "pingNumEach", DEFAULT_PING_NUM_EACH);
            includeFluids = Boolean.parseBoolean(cfg.getProperty("includeFluids", String.valueOf(DEFAULT_INCLUDE_FLUIDS)));
            iconSize = parseFloat(cfg, "iconSize", DEFAULT_ICON_SIZE);
            infoColor = parseInt(cfg, "infoColor", DEFAULT_INFO_COLOR);
            secondsToVanish = parseLong(cfg, "secondsToVanish", DEFAULT_SECONDS_TO_VANISH);
            highlightColor = parseInt(cfg, "highlightColor", DEFAULT_HIGHLIGHT_COLOR);
            soundIndex = (byte) parseInt(cfg, "soundIndex", DEFAULT_SOUND_INDEX);
            save(file);
        } catch (IOException e) {
            Constants.LOGGER.error("Failed to load config", e);
        }
    }

    public static void save() {
        save(configPath());
    }

    public static void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Properties cfg = new Properties();
            cfg.setProperty("pingNumEach", String.valueOf(pingNumEach));
            cfg.setProperty("includeFluids", String.valueOf(includeFluids));
            cfg.setProperty("iconSize", String.valueOf(iconSize));
            cfg.setProperty("infoColor", String.valueOf(infoColor));
            cfg.setProperty("secondsToVanish", String.valueOf(secondsToVanish));
            cfg.setProperty("highlightColor", String.valueOf(highlightColor));
            cfg.setProperty("soundIndex", String.valueOf(soundIndex));
            try (OutputStream stream = Files.newOutputStream(file)) {
                cfg.store(stream, "Sophisticated Ping config");
            }
        } catch (IOException e) {
            Constants.LOGGER.error("Failed to save config", e);
        }
    }

    public static void setSoundIndex(int input, int soundCount) {
        soundIndex = (byte) (input < 0 || input >= soundCount ? 0 : input);
    }

    private static int parseInt(Properties cfg, String key, int fallback) {
        try {
            return Integer.parseInt(cfg.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long parseLong(Properties cfg, String key, long fallback) {
        try {
            return Long.parseLong(cfg.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float parseFloat(Properties cfg, String key, float fallback) {
        try {
            return Float.parseFloat(cfg.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
