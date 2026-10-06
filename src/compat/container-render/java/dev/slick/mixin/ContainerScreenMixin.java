package dev.slick.mixin;

import dev.slick.gui.Canvas;
import dev.slick.gui.MenuRenderer;
import net.minecraft.client.gui.GuiGraphics;
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

    @Inject(method = "renderSlotHighlightBack", at = @At("HEAD"), cancellable = true)
    private void slick$glow(GuiGraphics graphics, CallbackInfo ci) {
        if (MenuRenderer.glow(this, hoveredSlot, new Canvas(graphics))) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void slick$front(GuiGraphics graphics, CallbackInfo ci) {
        if (MenuRenderer.styled(this)) {
            ci.cancel();
        }
    }
}
