package dev.slick.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos")
    int slick$leftPos();

    @Accessor("topPos")
    int slick$topPos();

    @Accessor("imageWidth")
    int slick$imageWidth();

    @Accessor("imageHeight")
    int slick$imageHeight();
}
