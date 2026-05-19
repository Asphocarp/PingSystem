package app.jyu.common.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import app.jyu.common.compat.Component;
import app.jyu.common.config.ClientConfig;
import app.jyu.common.resource.LanguageUtils;
import app.jyu.common.screen.SettingsScreen;
import org.apache.logging.log4j.util.TriConsumer;

import static app.jyu.common.CommonClient.Game;
import static app.jyu.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

public class ClientCommandBuilder {
	private ClientCommandBuilder() {}

	public static <S> LiteralArgumentBuilder<S> build(TriConsumer<CommandContext<S>, Boolean, MutableComponent> responseHandler) {
		var langConfig = LanguageUtils.command("config");
		var langChannel = LanguageUtils.command("channel");

		var cmdChannel = LiteralArgumentBuilder.<S>literal("channel")
			.executes((context) -> {
				var currentChannel = ClientConfig.HANDLER.getConfig().getChannel();

				if (currentChannel.isEmpty()) {
					responseHandler.accept(context, true, langChannel.path("get", "response", "default").get());
				} else {
					responseHandler.accept(context, true, langChannel.path("get", "response")
						.get(Component.literal(currentChannel).withStyle(ChatFormatting.YELLOW))
					);
				}

				return 1;
			})
			.then(RequiredArgumentBuilder.<S, String>argument("channel_name", StringArgumentType.string()).executes((context) -> {
				var newChannel = context.getArgument("channel_name", String.class);

				if (newChannel.length() > MAX_CHANNEL_LENGTH) {
					responseHandler.accept(context, false, langChannel.path("set", "reject").get(MAX_CHANNEL_LENGTH));
					return 0;
				}

				ClientConfig.HANDLER.getConfig().setChannel(newChannel);
				ClientConfig.HANDLER.save();

				if (newChannel.isEmpty()) {
					responseHandler.accept(context, true, langChannel.path("set", "response", "default").get());
				} else {
					responseHandler.accept(context, true, langChannel.path("set", "response")
						.get(Component.literal(newChannel).withStyle(ChatFormatting.YELLOW))
					);
				}

				return 1;
			}));

		var cmdConfig = LiteralArgumentBuilder.<S>literal("config")
			.executes((context) -> {
				Game.tell(() -> Game.setScreen(new SettingsScreen()));
				return 1;
			});

		Command<S> helpCallback = (context) -> {
			responseHandler.accept(context, true, LanguageUtils.join(
				Component.empty(),
				Component.literal("/sophisticated_ping config"),
				langConfig.path("description").wrapped().withStyle(ChatFormatting.GRAY),
				Component.literal("/sophisticated_ping channel"),
				langChannel.path("get", "description").wrapped().withStyle(ChatFormatting.GRAY),
				Component.literal("/sophisticated_ping channel <channel_name>"),
				langChannel.path("set", "description").wrapped().withStyle(ChatFormatting.GRAY)
			));
			return 1;
		};

		var cmdHelp = LiteralArgumentBuilder.<S>literal("help")
			.executes(helpCallback);

		return LiteralArgumentBuilder.<S>literal("sophisticated_ping")
			.executes(helpCallback)
			.then(cmdHelp)
			.then(cmdConfig)
			.then(cmdChannel);
	}
}
