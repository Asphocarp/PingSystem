package app.jyu.common.resource;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import app.jyu.common.Global;
import app.jyu.common.compat.LegacyMigrationHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static app.jyu.common.resource.ResourceConstants.PING_TEXTURE_ID;

public class ResourceReloadListener implements PreparableReloadListener {

	@Override
	public CompletableFuture<Void> reload(PreparationBarrier helper, ResourceManager resourceManager, Executor loadExecutor, Executor applyExecutor) {
		return reloadTextures(helper, resourceManager, loadExecutor, applyExecutor);
	}

	private static int numCustomTextures;

	public static boolean hasCustomTexture() {
		return numCustomTextures > 1;
	}

	public static CompletableFuture<Void> reloadTextures(PreparationBarrier helper, ResourceManager resourceManager, Executor loadExecutor, Executor applyExecutor) {
		return CompletableFuture
			.supplyAsync(() -> {
				LegacyMigrationHandler.checkResources(resourceManager);

				numCustomTextures = resourceManager.getResource(PING_TEXTURE_ID).isPresent() ? 1 : 0;

				return true;
			}, loadExecutor)
			.thenCompose(helper::wait)
			.thenAcceptAsync((ignored) -> {}, applyExecutor);
	}
}
