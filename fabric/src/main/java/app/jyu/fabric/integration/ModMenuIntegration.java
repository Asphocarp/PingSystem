package app.jyu.fabric.integration;

import app.jyu.common.Constants;
import app.jyu.common.SophisticatedPingCommon;
import app.jyu.common.config.ModConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::create;
    }

    private Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title." + Constants.MOD_ID + ".config"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config." + Constants.MOD_ID + ".general"));

        general.addEntry(entryBuilder.startIntField(Component.translatable("config." + Constants.MOD_ID + ".pingNumEach"), ModConfig.pingNumEach)
                .setDefaultValue(ModConfig.DEFAULT_PING_NUM_EACH)
                .setTooltip(Component.translatable("config." + Constants.MOD_ID + ".pingNumEach.description"))
                .setSaveConsumer(value -> ModConfig.pingNumEach = value)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config." + Constants.MOD_ID + ".includeFluids"), ModConfig.includeFluids)
                .setDefaultValue(ModConfig.DEFAULT_INCLUDE_FLUIDS)
                .setTooltip(Component.translatable("config." + Constants.MOD_ID + ".includeFluids.description"))
                .setSaveConsumer(value -> ModConfig.includeFluids = value)
                .build());
        general.addEntry(entryBuilder.startFloatField(Component.translatable("config." + Constants.MOD_ID + ".iconSize"), ModConfig.iconSize)
                .setDefaultValue(ModConfig.DEFAULT_ICON_SIZE)
                .setTooltip(Component.translatable("config." + Constants.MOD_ID + ".iconSize.description"))
                .setSaveConsumer(value -> ModConfig.iconSize = value)
                .build());
        general.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config." + Constants.MOD_ID + ".infoColor"), ModConfig.infoColor)
                .setDefaultValue(ModConfig.DEFAULT_INFO_COLOR)
                .setTooltip(Component.translatable("config." + Constants.MOD_ID + ".infoColor.description"))
                .setSaveConsumer(value -> ModConfig.infoColor = value)
                .build());
        general.addEntry(entryBuilder.startLongField(Component.translatable("config." + Constants.MOD_ID + ".secondsToVanish"), ModConfig.secondsToVanish)
                .setDefaultValue(ModConfig.DEFAULT_SECONDS_TO_VANISH)
                .setTooltip(Component.translatable("config." + Constants.MOD_ID + ".secondsToVanish.description"))
                .setSaveConsumer(value -> ModConfig.secondsToVanish = value)
                .build());
        general.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config." + Constants.MOD_ID + ".highlightColor"), ModConfig.highlightColor)
                .setDefaultValue(ModConfig.DEFAULT_HIGHLIGHT_COLOR)
                .setTooltip(Component.translatable("config." + Constants.MOD_ID + ".highlightColor.description"))
                .setSaveConsumer(value -> ModConfig.highlightColor = value)
                .build());
        general.addEntry(entryBuilder.startIntField(Component.translatable("config." + Constants.MOD_ID + ".soundIndex"), (int) ModConfig.soundIndex)
                .setDefaultValue((int) ModConfig.DEFAULT_SOUND_INDEX)
                .setTooltip(Component.translatable("config." + Constants.MOD_ID + ".soundIndex.description"))
                .setSaveConsumer(value -> ModConfig.setSoundIndex(value, SophisticatedPingCommon.soundCount()))
                .build());

        return builder.setSavingRunnable(ModConfig::save).build();
    }
}
