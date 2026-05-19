package app.jyu.common.render;

import app.jyu.common.math.MathUtils;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix3x2fStack;

import static app.jyu.common.CommonClient.Game;
import static app.jyu.common.resource.ResourceConstants.ARROW_TEXTURE_ID;
import static app.jyu.common.resource.ResourceConstants.PING_TEXTURE_ID;
import static app.jyu.common.resource.ResourceReloadListener.hasCustomTexture;

public class DrawContext {
	private static final int SHADOW_BLACK = 0x40000000;

	@Getter
	private final GuiGraphics guiGraphics;

	@Getter
	private final Matrix3x2fStack matrices;

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

		matrices.pushMatrix();
		matrices.translate(textOffset.x, textOffset.y);
		guiGraphics.fill(-2, -2, (int)textMetrics.x + 1, (int)textMetrics.y, SHADOW_BLACK);
		guiGraphics.drawString(Game.font, text, (int)extraWidth, 0, color, false);

		if (player != null) {
			matrices.translate(-0.5f, -0.5f);
			renderPlayerHead(player);
		}

		matrices.popMatrix();
	}

	public void renderPlayerHead(PlayerInfo player) {
		var texture = player.getSkin().texture();
		guiGraphics.blit(texture, 0, 0, 8, 8, 0f, 0.125f, 0.125f, 0.25f);
		guiGraphics.blit(texture, 0, 0, 8, 8, 0f, 0.625f, 0.125f, 0.75f);
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
		matrices.pushMatrix();
		matrices.translate(-8f, -8f);
		guiGraphics.renderFakeItem(itemStack, 0, 0);
		matrices.popMatrix();
	}

	public void renderDefaultPingIcon(int color) {
		matrices.pushMatrix();
		MathUtils.rotateZ(matrices, (float)(Math.PI / 4f));
		matrices.translate(-2.5f, -2.5f);
		guiGraphics.fill(0, 0, 5, 5, color);
		matrices.popMatrix();
	}

	public void renderTexture(ResourceLocation texture, int size, int color) {
		final var offset = size / -2;
		guiGraphics.blit(texture, offset, offset, size, size, 0f, 0f, 1f, 1f);
	}

	public void renderArrowIcon(int color) {
		renderTexture(ARROW_TEXTURE_ID, 10, color);
	}
}
