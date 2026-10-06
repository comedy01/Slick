package dev.slick.mixin;

import dev.slick.gui.Canvas;
import dev.slick.gui.Dimmer;
import dev.slick.gui.MenuRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
abstract class ContainerScreenMixin {
    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
    private void slick$panel(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (MenuRenderer.framing() && Dimmer.pending()) {
            screen.renderBackground(graphics);
        }
        MenuRenderer.background(screen, new Canvas(graphics));
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0, shift = At.Shift.AFTER))
    private void slick$glow(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        MenuRenderer.glow(this, hoveredSlot, new Canvas(graphics));
    }

    @Inject(method = "renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V", at = @At("HEAD"), cancellable = true)
    private static void slick$highlight(GuiGraphics graphics, int x, int y, int offset, CallbackInfo ci) {
        if (MenuRenderer.framing()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void slick$coloredHighlight(GuiGraphics graphics, int x, int y, int offset, int color, CallbackInfo ci) {
        if (MenuRenderer.framing()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void slick$tooltip(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        MenuRenderer.endFrame(new Canvas(graphics));
    }
}
