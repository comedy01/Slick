package dev.slick.mixin;

import dev.slick.gui.Canvas;
import dev.slick.gui.MenuRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(GuiGraphics.class)
abstract class GraphicsMixin {
    @Inject(method = "blit(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIFFIIIIIII)V",
            at = @At("HEAD"), cancellable = true)
    private void slick$blit(Function<ResourceLocation, RenderType> type, ResourceLocation texture, int x, int y,
                            float u, float v, int width, int height, int srcWidth, int srcHeight, int textureWidth,
                            int textureHeight, int color, CallbackInfo ci) {
        if (MenuRenderer.skipTexture(texture.getNamespace(), texture.getPath(), x, y, width, height,
                new Canvas((GuiGraphics) (Object) this))) {
            ci.cancel();
        }
    }

    @Inject(method = "blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIII)V",
            at = @At("HEAD"), cancellable = true)
    private void slick$sprite(Function<ResourceLocation, RenderType> type, ResourceLocation sprite, int x, int y,
                              int width, int height, int color, CallbackInfo ci) {
        if (MenuRenderer.sprite(sprite.getNamespace(), sprite.getPath(), x, y, width, height,
                new Canvas((GuiGraphics) (Object) this))) {
            ci.cancel();
        }
    }

    @Inject(method = "blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V",
            at = @At("HEAD"), cancellable = true)
    private void slick$partSprite(Function<ResourceLocation, RenderType> type, ResourceLocation sprite,
                                  int spriteWidth, int spriteHeight, int textureX, int textureY, int x, int y,
                                  int width, int height, CallbackInfo ci) {
        if (MenuRenderer.sprite(sprite.getNamespace(), sprite.getPath(), x, y, width, height,
                new Canvas((GuiGraphics) (Object) this))) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int slick$text(int color) {
        return MenuRenderer.textColor(color);
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int slick$string(int color) {
        return MenuRenderer.textColor(color);
    }
}
