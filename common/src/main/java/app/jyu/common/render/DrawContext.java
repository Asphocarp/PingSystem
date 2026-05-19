package app.jyu.common.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import app.jyu.common.math.MathUtils;

import static app.jyu.common.CommonClient.Game;
import static app.jyu.common.resource.ResourceConstants.ARROW_TEXTURE_ID;
import static app.jyu.common.resource.ResourceConstants.PING_TEXTURE_ID;
import static app.jyu.common.resource.ResourceReloadListener.hasCustomTexture;

public class DrawContext {

	private static final int SHADOW_BLACK = FastColor.ARGB32.color(64, 0, 0, 0);
	private static final int LIGHT_VALUE_MAX = 0xF000F0;

	@Getter
	private PoseStack matrices;

	public DrawContext(PoseStack matrices) {
		this.matrices = matrices;
	}

	public void renderLabel(Component text, float yOffset, PlayerInfo player, int color) {
		var extraWidth = (player != null) ? 10f : 0f;
		var textMetrics = new Vec2(
			Game.font.width(text) + extraWidth,
			Game.font.lineHeight
		);
		var textOffset = textMetrics.scale(-0.5f).add(new Vec2(0f, textMetrics.y * yOffset));

		matrices.pushPose();
		matrices.translate(textOffset.x, textOffset.y, 0);
		GuiComponent.fill(matrices, -2, -2, (int)textMetrics.x + 1, (int)textMetrics.y, SHADOW_BLACK);
		Game.font.draw(matrices, text, extraWidth, 0f, color);

		if (player != null) {
			matrices.translate(-0.5, -0.5, 0);
			renderPlayerHead(player);
		}

		matrices.popPose();
	}

	public void renderPlayerHead(PlayerInfo player) {
		RenderSystem.setShaderTexture(0, player.getSkinLocation());
		RenderSystem.enableBlend();
		GuiComponent.blit(matrices, 0, 0, 0, 8, 8, 8, 8, 64, 64);
		GuiComponent.blit(matrices, 0, 0, 0, 40, 8, 8, 8, 64, 64); // Overlay (hat)
		RenderSystem.disableBlend();
	}

	public void renderPing(ItemStack itemStack, boolean drawItemIcon, int color) {
		if (itemStack != null && drawItemIcon) {
			renderGuiItemModel(itemStack);
		} else if (hasCustomTexture()) {
			renderTexture(PING_TEXTURE_ID, 12, color);
		} else {
			renderDefaultPingIcon(color);
		}
	}

	public void renderGuiItemModel(ItemStack itemStack) {
		var model = Game.getItemRenderer().getModel(itemStack, null, null, 0);

		Game.getTextureManager()
			.getTexture(TextureAtlas.LOCATION_BLOCKS)
			.setFilter(false, false);

		RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		var matrixStack = RenderSystem.getModelViewStack();
		matrixStack.pushPose();
		matrixStack.mulPoseMatrix(matrices.last().pose());
		matrixStack.translate(0f, 0f, -0.5f);
		matrixStack.scale(16f, -16f, 16f);
		RenderSystem.applyModelViewMatrix();

		var immediate = Game.renderBuffers().bufferSource();
		var bl = !model.usesBlockLight();
		if (bl) {
			Lighting.setupForFlatItems();
		}

		var matrixStackDummy = new PoseStack();
		Game.getItemRenderer().render(
			itemStack,
			ItemTransforms.TransformType.GUI,
			false,
			matrixStackDummy,
			immediate,
			LIGHT_VALUE_MAX,
			OverlayTexture.NO_OVERLAY,
			model
		);
		immediate.endBatch();
		RenderSystem.enableDepthTest();

		if (bl) {
			Lighting.setupFor3DItems();
		}

		matrixStack.popPose();
		RenderSystem.applyModelViewMatrix();
	}

	public void renderDefaultPingIcon(int color) {
		matrices.pushPose();
		MathUtils.rotateZ(matrices, (float)(Math.PI / 4f));
		matrices.translate(-2.5, -2.5, 0);
		GuiComponent.fill(matrices, 0, 0, 5, 5, color);
		matrices.popPose();
	}

	public void renderTexture(ResourceLocation texture, int size, int color) {
		final var offset = size / -2;
		final float a = FastColor.ARGB32.alpha(color) / 255f;
		final float r = FastColor.ARGB32.red(color) / 255f;
		final float g = FastColor.ARGB32.green(color) / 255f;
		final float b = FastColor.ARGB32.blue(color) / 255f;

		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShaderColor(r, g, b, a);
		RenderSystem.enableBlend();
		GuiComponent.blit(
			matrices,
			offset,
			offset,
			0,
			0,
			0,
			size,
			size,
			size,
			size
		);
		RenderSystem.disableBlend();
	}

	public void renderArrowIcon(int color) {
		renderTexture(ARROW_TEXTURE_ID, 10, color);
	}
}
