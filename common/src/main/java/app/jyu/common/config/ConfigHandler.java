package app.jyu.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.SneakyThrows;
import app.jyu.common.compat.LegacyMigrationHandler;
import app.jyu.common.platform.IPlatformContextService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static app.jyu.common.Global.LOGGER;
import static app.jyu.common.Global.MOD_ID;

public class ConfigHandler <T extends IConfig> {

	private final Gson gson;
	private final Class<T> configType;
	private final Path configPath;

	@Getter
	private T config;
	private int configHash;

	public static <T extends IConfig> ConfigHandler<T> of(Class<T> configType, String configExtension) {
		LegacyMigrationHandler.migrateConfig(configExtension);

		return new ConfigHandler<>(configType, IPlatformContextService.INSTANCE.resolveConfigDir(MOD_ID + configExtension));
	}

	@SneakyThrows
	public ConfigHandler(Class<T> configType, Path configPath) {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.configType = configType;
		this.configPath = configPath;

		this.configHash = 0;
		this.config = configType.getDeclaredConstructor().newInstance();
	}

	public void save() {
		if (configHash == config.hashCode()) {
			return;
		}

		config.onUpdate();

		if (!Files.exists(configPath)) {
			try {
				Files.createDirectories(configPath.getParent());
				Files.createFile(configPath);
			} catch (IOException e) {
				LOGGER.error("Creating Config failed: " + e);
				return;
			}
		}

		try {
			var writer = Files.newBufferedWriter(configPath);
			gson.toJson(config, writer);
			writer.close();
		} catch (Exception e) {
			LOGGER.error("Saving Config failed: " + e);
			return;
		}

		configHash = config.hashCode();
		LOGGER.info("Saved " + config);
	}

	@SneakyThrows
	public void load() {
		if (!Files.exists(configPath)) {
			save();
			return;
		}

		try {
			var reader = Files.newBufferedReader(configPath);
			config = gson.fromJson(reader, configType);
			reader.close();
		} catch (Exception e) {
			config = null;
		}

		if (config == null) {
			config = configType.getDeclaredConstructor().newInstance();
			LOGGER.error("Config is broken -> reset to defaults");

			save();
			return;
		}

		config.validate();
		configHash = config.hashCode();
		LOGGER.info("Loaded " + config);
	}
}
