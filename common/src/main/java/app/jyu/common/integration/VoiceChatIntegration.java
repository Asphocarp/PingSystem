package app.jyu.common.integration;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

import static app.jyu.common.Global.MOD_ID;

@ForgeVoicechatPlugin
public class VoiceChatIntegration implements VoicechatPlugin {

	public static VoicechatServerApi serverApi = null;
	public static VoicechatClientApi clientApi = null;

	@Override
	public String getPluginId() {
		return MOD_ID;
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
		registration.registerEvent(ClientVoicechatInitializationEvent.class, this::onClientInitialized);
	}

	private void onServerStarted(VoicechatServerStartedEvent event) {
		serverApi = event.getVoicechat();
	}

	private void onClientInitialized(ClientVoicechatInitializationEvent event) {
		clientApi = event.getVoicechat();
	}
}
