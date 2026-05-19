package app.jyu.common.screen;

import app.jyu.common.compat.Component;
import app.jyu.common.config.ClientConfig;
import app.jyu.common.integration.TeamContext;
import app.jyu.common.integration.TeamContextHandler;
import app.jyu.common.resource.LanguageUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;

import static app.jyu.common.CommonClient.Game;
import static app.jyu.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

public class SettingsScreen extends Screen {
	private static final int WHITE = 0xFFFFFF;
	private static final int GRAY = 0xA0A0A0;

	private final ClientConfig config;
	private final Screen parent;
	private EditBox channelTextField;

	public SettingsScreen() {
		this(null);
	}

	public SettingsScreen(Screen parent) {
		super(LanguageUtils.settings("title").get());
		this.parent = parent;
		this.config = ClientConfig.HANDLER.getConfig();
	}

	@Override
	public void tick() {
		if (channelTextField != null) {
			channelTextField.tick();
		}
	}

	@Override
	protected void init() {
		this.channelTextField = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 10, 200, 20, Component.empty());
		this.channelTextField.setMaxLength(MAX_CHANNEL_LENGTH);
		this.channelTextField.setValue(config.getChannel());
		this.channelTextField.setResponder(config::setChannel);
		this.addWidget(this.channelTextField);

		this.addRenderableWidget(new Button(this.width / 2 - 100, this.height - 27, 200, 20, CommonComponents.GUI_DONE, button -> onClose()));
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
		drawCenteredString(matrices, this.font, this.title, this.width / 2, 20, WHITE);
		drawString(matrices, this.font, LanguageUtils.settings("channel").get(), this.width / 2 - 100, this.channelTextField.y - 12, GRAY);
		this.channelTextField.render(matrices, mouseX, mouseY, delta);

		if (this.channelTextField.getValue().isEmpty()) {
			drawString(matrices, this.font, getChannelPlaceholder(), this.width / 2 - 100 + 4, this.channelTextField.y + 6, WHITE);
		}

		super.render(matrices, mouseX, mouseY, delta);
	}

	private MutableComponent getChannelPlaceholder() {
		if (Game.player == null) {
			return Component.empty();
		}

		if (TeamContextHandler.getSelfContext() == TeamContext.NONE) {
			return LanguageUtils.of("value", "global").get()
				.withStyle(ChatFormatting.ITALIC)
				.withStyle(ChatFormatting.DARK_GRAY);
		}

		return LanguageUtils.settings("channel").path("placeholder")
			.get(LanguageUtils.of("value", TeamContext.VANILLA_TEAM.toString()).get())
			.withStyle(ChatFormatting.ITALIC)
			.withStyle(ChatFormatting.DARK_GRAY);
	}
}
