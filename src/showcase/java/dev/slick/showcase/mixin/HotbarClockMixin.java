package dev.slick.showcase.mixin;

import dev.slick.gui.HotbarRenderer;
import dev.slick.showcase.Clock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = HotbarRenderer.class, remap = false)
abstract class HotbarClockMixin {
    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Ljava/lang/System;nanoTime()J"))
    private static long slickShowcase$nanos() {
        return Clock.nanos();
    }
}
