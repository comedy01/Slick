package dev.slick.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface HudInvoker {
    @Invoker("getCameraPlayer")
    Player slick$cameraPlayer();

    @Invoker("extractSlot")
    void slick$slot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker delta, Player player, ItemStack stack, int seed);
}
