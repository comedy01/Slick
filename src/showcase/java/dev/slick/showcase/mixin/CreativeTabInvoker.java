package dev.slick.showcase.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeTabInvoker {
    @Invoker("getTabX")
    int slickShowcase$tabX(CreativeModeTab tab);

    @Invoker("getTabY")
    int slickShowcase$tabY(CreativeModeTab tab);
}
