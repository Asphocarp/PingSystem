package app.jyu.fabric;

import app.jyu.common.CommonClient;
import app.jyu.common.command.ClientCommandBuilder;
import app.jyu.common.network.PingLocationS2CPacket;
import app.jyu.common.resource.LanguageUtils;
import app.jyu.common.resource.ResourceReloadListener;
import app.jyu.fabric.payload.FabricPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static app.jyu.common.Global.MOD_ID;

@Environment(EnvType.CLIENT)
public class FabricClient implements ClientModInitializer {
	public static final ResourceLocation RELOAD_LISTENER_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "reload-listener");

	@Override
	public void onInitializeClient() {
		CommonClient.INSTANCE.onInit();

		ClientPlayNetworking.registerGlobalReceiver(
			FabricPayloads.PingLocationS2C.TYPE,
			(payload, context) -> CommonClient.INSTANCE.onPingLocationPacket(payload.packet())
		);

		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
			.registerReloadListener(new IdentifiableResourceReloadListener() {
				@Override
				public ResourceLocation getFabricId() {
					return RELOAD_LISTENER_ID;
				}

				@Override
				public CompletableFuture<Void> reload(PreparationBarrier helper, ResourceManager resourceManager, Executor loadExecutor, Executor applyExecutor) {
					return ResourceReloadListener.reloadTextures(helper, resourceManager, loadExecutor, applyExecutor);
				}
			});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandBuilder.build((context, success, response) -> {
			if (success) {
				context.getSource().sendFeedback(LanguageUtils.withModPrefix(response));
			} else {
				context.getSource().sendError(LanguageUtils.withModPrefix(response));
			}
		})));
	}
}
