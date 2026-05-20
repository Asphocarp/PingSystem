package app.jyu.common.integration;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public class VoiceChatWrapper {
	private VoiceChatWrapper() {}

	public static Optional<UUID> getGroupId(Player player) {
		if (!ModContext.HasVoiceChat || VoiceChatIntegration.serverApi == null) return Optional.empty();

		var voiceChatConnection = VoiceChatIntegration.serverApi.getConnectionOf(player.getUUID());
		if (voiceChatConnection == null) return Optional.empty();

		var voiceChatGroup = voiceChatConnection.getGroup();
		if (voiceChatGroup == null) return Optional.empty();

		return Optional.of(voiceChatGroup.getId());
	}

	public static Optional<UUID> getSelfGroupId() {
		if (!ModContext.HasVoiceChat || VoiceChatIntegration.clientApi == null) return Optional.empty();

		var voiceChatGroup = VoiceChatIntegration.clientApi.getGroup();
		if (voiceChatGroup == null) return Optional.empty();

		return Optional.of(voiceChatGroup.getId());
	}
}
