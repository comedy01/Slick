package dev.slick.mixin;

import dev.slick.gui.Canvas;
import dev.slick.gui.MenuRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
abstract class GraphicsMixin {
    @Inject(method = "blit(Lnet/minecraft/resources/ResourceLocation;IIIIIIIFFII)V", at = @At("HEAD"), cancellable = true)
    private void slick$blit(ResourceLocation texture, int x0, int x1, int y0, int y1, int offset, int width, int height,
                            float u, float v, int textureWidth, int textureHeight, CallbackInfo ci) {
        if (MenuRenderer.skipTexture(texture.getNamespace(), texture.getPath(), x0, y0, x1 - x0, y1 - y0,
                new Canvas((GuiGraphics) (Object) this))) {
            ci.cancel();
        }
    }

    @Inject(method = "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIII)V", at = @At("HEAD"), cancellable = true)
    private void slick$sprite(ResourceLocation sprite, int x, int y, int offset, int width, int height, CallbackInfo ci) {
        if (MenuRenderer.sprite(sprite.getNamespace(), sprite.getPath(), x, y, width, height,
                new Canvas((GuiGraphics) (Object) this))) {
            ci.cancel();
        }
    }

    @Inject(method = "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIIIIIII)V", at = @At("HEAD"), cancellable = true)
    private void slick$partSprite(ResourceLocation sprite, int spriteWidth, int spriteHeight, int textureX, int textureY,
                                  int x, int y, int offset, int width, int height, CallbackInfo ci) {
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
