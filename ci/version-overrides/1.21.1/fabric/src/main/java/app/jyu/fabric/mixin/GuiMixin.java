package app.jyu.fabric.mixin;

import app.jyu.fabric.event.GuiRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class)
public abstract class GuiMixin {
	@Inject(method = "render", at = @At(value = "HEAD"))
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0, 0, -90);
		GuiRenderCallback.START.invoker().onRenderGui(guiGraphics, deltaTracker.getGameTimeDeltaTicks());
		guiGraphics.pose().popPose();
	}
}
