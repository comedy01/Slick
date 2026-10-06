package dev.slick.mixin;

import dev.slick.gui.Canvas;
import dev.slick.gui.MenuRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenMixin {
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void slick$panel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        MenuRenderer.background((Screen) (Object) this, new Canvas(graphics));
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void slick$begin(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        MenuRenderer.beginFrame((Screen) (Object) this);
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V"))
    private void slick$end(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        MenuRenderer.endFrame(new Canvas(graphics));
    }
}
