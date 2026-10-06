package dev.slick.mixin;

import dev.slick.gui.Canvas;
import dev.slick.gui.MenuRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenMixin {
    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void slick$begin(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        MenuRenderer.beginFrame((Screen) (Object) this);
    }

    @Inject(method = "renderWithTooltip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderDeferredTooltip()V"))
    private void slick$end(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        MenuRenderer.endFrame(new Canvas(graphics));
    }
}
