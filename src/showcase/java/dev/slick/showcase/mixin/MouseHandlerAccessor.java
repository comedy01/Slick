package dev.slick.showcase.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void slickShowcase$setX(double x);

    @Accessor("ypos")
    void slickShowcase$setY(double y);
}
