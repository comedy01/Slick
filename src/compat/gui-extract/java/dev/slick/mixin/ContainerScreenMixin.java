package dev.slick.mixin;

import dev.slick.gui.Canvas;
import dev.slick.gui.MenuRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    @Inject(method = "extractSlotHighlightBack", at = @At("HEAD"), cancellable = true)
    private void slick$glow(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (MenuRenderer.glow(this, hoveredSlot, new Canvas(graphics))) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void slick$front(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (MenuRenderer.styled(this)) {
            ci.cancel();
        }
    }
}
