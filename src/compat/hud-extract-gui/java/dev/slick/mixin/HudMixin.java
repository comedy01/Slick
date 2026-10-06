package dev.slick.mixin;

import dev.slick.gui.Canvas;
import dev.slick.gui.HotbarRenderer;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class HudMixin {
    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void slick$hotbar(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (!HotbarRenderer.active()) {
            return;
        }
        ci.cancel();
        HudInvoker hud = (HudInvoker) this;
        Player player = hud.slick$cameraPlayer();
        if (player == null) {
            return;
        }
        boolean indicator = Minecraft.getInstance().options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR;
        HotbarRenderer.draw(new Canvas(graphics), player,
                (x, y, stack, seed) -> hud.slick$slot(graphics, x, y, delta, player, stack, seed), indicator);
    }
}
