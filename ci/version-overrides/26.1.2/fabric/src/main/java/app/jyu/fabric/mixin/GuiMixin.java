package app.jyu.fabric.mixin;

import app.jyu.fabric.event.GuiRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class)
public abstract class GuiMixin {
	@Inject(method = "extractRenderState", at = @At(value = "TAIL"))
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
		guiGraphics.pose().pushMatrix();
		GuiRenderCallback.START.invoker().onRenderGui(guiGraphics, deltaTracker.getGameTimeDeltaTicks());
		guiGraphics.pose().popMatrix();
	}
}
