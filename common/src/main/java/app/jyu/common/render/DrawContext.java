package app.jyu.common.render;

import app.jyu.common.math.MathUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

import static app.jyu.common.CommonClient.Game;
import static app.jyu.common.resource.ResourceConstants.ARROW_TEXTURE_ID;
import static app.jyu.common.resource.ResourceConstants.PING_TEXTURE_ID;
import static app.jyu.common.resource.ResourceReloadListener.hasCustomTexture;

public class DrawContext {
	private static final int SHADOW_BLACK = 0x40000000;

	@Getter
	private final GuiGraphics guiGraphics;

	@Getter
	private final PoseStack matrices;

	public DrawContext(GuiGraphics guiGraphics) {
		this.guiGraphics = guiGraphics;
		this.matrices = guiGraphics.pose();
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
		guiGraphics.fill(-2, -2, (int)textMetrics.x + 1, (int)textMetrics.y, SHADOW_BLACK);
		guiGraphics.drawString(Game.font, text, (int)extraWidth, 0, color, false);

		if (player != null) {
			matrices.translate(-0.5, -0.5, 0);
			renderPlayerHead(player);
		}

		matrices.popPose();
	}

	public void renderPlayerHead(PlayerInfo player) {
		var texture = player.getSkinLocation();
		guiGraphics.blit(texture, 0, 0, 0, 8, 8, 8, 8, 64, 64);
		guiGraphics.blit(texture, 0, 0, 0, 40, 8, 8, 8, 64, 64);
	}

	public void renderPing(ItemStack itemStack, boolean drawItemIcon, int color) {
		if (itemStack != null && drawItemIcon) {
			renderGuiItemModel(itemStack);
			return;
		}

		if (hasCustomTexture()) {
			renderTexture(PING_TEXTURE_ID, 12, color);
			return;
		}

		renderDefaultPingIcon(color);
	}

	public void renderGuiItemModel(ItemStack itemStack) {
		matrices.pushPose();
		matrices.translate(-8f, -8f, 0f);
		guiGraphics.renderFakeItem(itemStack, 0, 0);
		matrices.popPose();
	}

	public void renderDefaultPingIcon(int color) {
		matrices.pushPose();
		MathUtils.rotateZ(matrices, (float)(Math.PI / 4f));
		matrices.translate(-2.5, -2.5, 0);
		guiGraphics.fill(0, 0, 5, 5, color);
		matrices.popPose();
	}

	public void renderTexture(ResourceLocation texture, int size, int color) {
		final var offset = size / -2;
		final float a = ((color >>> 24) & 0xFF) / 255f;
		final float r = ((color >>> 16) & 0xFF) / 255f;
		final float g = ((color >>> 8) & 0xFF) / 255f;
		final float b = (color & 0xFF) / 255f;

		guiGraphics.setColor(r, g, b, a);
		guiGraphics.blit(texture, offset, offset, 0, 0, size, size, size, size);
		guiGraphics.setColor(1f, 1f, 1f, 1f);
	}

	public void renderArrowIcon(int color) {
		renderTexture(ARROW_TEXTURE_ID, 10, color);
	}
}
