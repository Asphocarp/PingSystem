package app.jyu.common.screen;

import app.jyu.common.compat.Component;
import app.jyu.common.config.ClientConfig;
import app.jyu.common.config.PlayerInfoMode;
import app.jyu.common.config.TeamColorMode;
import app.jyu.common.integration.TeamContext;
import app.jyu.common.integration.TeamContextHandler;
import app.jyu.common.resource.LanguageUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.Collections;
import java.util.List;

import static app.jyu.common.CommonClient.Game;
import static app.jyu.common.config.ClientConfig.MAX_CHANNEL_LENGTH;
import static app.jyu.common.config.ClientConfig.MAX_CORRECTION_PERIOD;
import static app.jyu.common.config.ClientConfig.MAX_PING_DISTANCE;
import static app.jyu.common.config.ClientConfig.MAX_PING_DURATION;

public class SettingsScreen extends Screen {
	private static final int WHITE = 0xFFFFFF;
	private static final int GRAY = 0xA0A0A0;
	private static final int LINE_LENGTH = 170;

	private final ClientConfig config;

	private Screen parent;
	private OptionsList list;
	private EditBox channelTextField;

	public SettingsScreen() {
		super(LanguageUtils.settings("title").get());
		this.config = ClientConfig.HANDLER.getConfig();
	}

	public SettingsScreen(Screen parent) {
		this();
		this.parent = parent;
	}

	@Override
	public void tick() {
		this.channelTextField.tick();
	}

	@Override
	protected void init() {
		this.list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);

		this.list.addSmall(getPingVolumeOption(), getPingDurationOption());
		this.list.addSmall(getPingDistanceOption(), getCorrectionPeriodOption());
		this.list.addSmall(getItemIconsVisibleOption(), getDirectionIndicatorVisibleOption());
		this.list.addSmall(getPlayerInfoModeOption(), getTeamColorModeOption());
		this.list.addSmall(getPingSizeOption(), null);

		final var yOffset = 50 + 25 * this.list.children().size();
		this.channelTextField = new EditBox(this.font, this.width / 2 - 100, yOffset, 200, 20, Component.empty());
		this.channelTextField.setMaxLength(MAX_CHANNEL_LENGTH);
		this.channelTextField.setValue(config.getChannel());
		this.channelTextField.setResponder(config::setChannel);
		this.addWidget(this.channelTextField);

		this.addWidget(this.list);
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
			.bounds(this.width / 2 - 100, this.height - 27, 200, 20)
			.build());
	}

	@Override
	public void onClose() {
		ClientConfig.HANDLER.save();

		if (parent != null && this.minecraft != null) {
			this.minecraft.setScreen(parent);
			return;
		}

		super.onClose();
	}

	@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		this.list.render(matrices, mouseX, mouseY, delta);
		drawCenteredString(matrices, this.font, this.title, this.width / 2, 20, WHITE);

		drawString(matrices, this.font, LanguageUtils.settings("channel").get(), this.width / 2 - 100, this.channelTextField.getY() - 12, GRAY);
		this.channelTextField.render(matrices, mouseX, mouseY, delta);

		if (this.channelTextField.getValue().isEmpty()) {
			drawString(matrices, this.font, getChannelPlaceholder(), this.width / 2 - 100 + 4, this.channelTextField.getY() + 6, WHITE);
		}

		super.render(matrices, mouseX, mouseY, delta);

		var tooltipLines = getHoveredButtonTooltip(this.list, mouseX, mouseY);

		if (tooltipLines.isEmpty() && this.channelTextField.isHoveredOrFocused() && !this.channelTextField.isFocused()) {
			tooltipLines = this.font.split(LanguageUtils.settings("channel.tooltip").get(), LINE_LENGTH);
		}

		this.renderTooltip(matrices, tooltipLines, mouseX, mouseY);
	}

	private static List<FormattedCharSequence> getHoveredButtonTooltip(OptionsList buttonList, int mouseX, int mouseY) {
		return Collections.emptyList();
	}

	private MutableComponent getChannelPlaceholder() {
		if (Game.player == null) {
			return Component.empty();
		}

		final var teamContext = TeamContextHandler.getSelfContext();
		final MutableComponent placeholder;

		if (teamContext == TeamContext.NONE) {
			placeholder = LanguageUtils.of("value", "global").get();
		} else {
			placeholder = LanguageUtils.settings("channel").path("placeholder")
				.get(LanguageUtils.of("value", teamContext.toString()).get());
		}

		return placeholder
			.withStyle(ChatFormatting.ITALIC)
			.withStyle(ChatFormatting.DARK_GRAY);
	}

	private OptionInstance<Integer> getPingVolumeOption() {
		final var text = LanguageUtils.settings("ping_volume");

		return OptionUtils.ofInt(
			text.getKey(),
			0, 100, 1,
			value -> {
				if (value == 0) {
					return text.get(CommonComponents.OPTION_OFF);
				}

				return text.get(LanguageUtils.UNIT_PERCENT.get(value));
			},
			config::getPingVolume,
			config::setPingVolume
		);
	}

	private OptionInstance<Integer> getPingDurationOption() {
		final var text = LanguageUtils.settings("ping_duration");

		return OptionUtils.ofInt(
			text.getKey(),
			1, MAX_PING_DURATION, 1,
			value -> {
				if (value >= MAX_PING_DURATION) {
					return text.get(LanguageUtils.VALUE_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_SECONDS.get(value));
			},
			config::getPingDuration,
			config::setPingDuration
		);
	}

	private OptionInstance<Integer> getPingDistanceOption() {
		final var text = LanguageUtils.settings("ping_distance");

		return OptionUtils.ofInt(
			text.getKey(),
			0, MAX_PING_DISTANCE, 16,
			value -> {
				if (value == 0) {
					return text.get(LanguageUtils.VALUE_HIDDEN);
				}

				if (value >= MAX_PING_DISTANCE) {
					return text.get(LanguageUtils.VALUE_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_METERS.get(value));
			},
			config::getPingDistance,
			config::setPingDistance
		);
	}

	private OptionInstance<Float> getCorrectionPeriodOption() {
		final var text = LanguageUtils.settings("correction_period");

		return OptionUtils.ofFloat(
			text.getKey(),
			0.1f, MAX_CORRECTION_PERIOD, 0.1f,
			value -> {
				if (value >= MAX_CORRECTION_PERIOD) {
					return text.get(LanguageUtils.VALUE_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_SECONDS.get("%.1f".formatted(value)));
			},
			config::getCorrectionPeriod,
			config::setCorrectionPeriod
		);
	}

	private OptionInstance<Boolean> getItemIconsVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("item_icon_visible").getKey(),
			config::isItemIconVisible,
			config::setItemIconVisible
		);
	}

	private OptionInstance<Boolean> getDirectionIndicatorVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("direction_indicator_visible").getKey(),
			config::isDirectionIndicatorVisible,
			config::setDirectionIndicatorVisible
		);
	}

	private OptionInstance<PlayerInfoMode> getPlayerInfoModeOption() {
		return OptionUtils.ofEnum(
			LanguageUtils.settings("player_info_mode").getKey(),
			PlayerInfoMode.class,
			mode -> LanguageUtils.of("value", mode.toString()).get(),
			mode -> {
				if (mode != PlayerInfoMode.HOLD) return ImmutableList.of();

				final var keyPlayerListTitle = Component.translatable(Game.options.keyPlayerList.getName());
				final var keyPlayerListName = Game.options.keyPlayerList.getTranslatedKeyMessage();

				return this.font.split(
					LanguageUtils.settings("player_info_mode")
						.path("hold", "tooltip")
						.get(keyPlayerListTitle, keyPlayerListName),
					LINE_LENGTH
				);
			},
			config::getPlayerInfoMode,
			config::setPlayerInfoMode
		);
	}

	private OptionInstance<TeamColorMode> getTeamColorModeOption() {
		return OptionUtils.ofEnum(
			LanguageUtils.settings("team_color_mode").getKey(),
			TeamColorMode.class,
			mode -> LanguageUtils.of("value", mode.toString()).get(),
			mode -> ImmutableList.of(),
			config::getTeamColorMode,
			config::setTeamColorMode
		);
	}

	private OptionInstance<Integer> getPingSizeOption() {
		final var text = LanguageUtils.settings("ping_size");

		return OptionUtils.ofInt(
			text.getKey(),
			40, 300, 10,
			value -> text.get(LanguageUtils.UNIT_PERCENT.get(value)),
			config::getPingSize,
			config::setPingSize
		);
	}
}
