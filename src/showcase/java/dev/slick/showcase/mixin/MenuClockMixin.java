package dev.slick.showcase.mixin;

import dev.slick.gui.MenuRenderer;
import dev.slick.showcase.Clock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MenuRenderer.class, remap = false)
abstract class MenuClockMixin {
    @Redirect(method = {"background", "glow", "opening"}, at = @At(value = "INVOKE", target = "Ljava/lang/System;nanoTime()J"))
    private static long slickShowcase$nanos() {
        return Clock.nanos();
    }
}
