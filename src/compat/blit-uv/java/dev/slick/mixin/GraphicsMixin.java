package dev.slick.mixin;

import dev.slick.gui.Canvas;
import dev.slick.gui.LegacySprites;
import dev.slick.gui.MenuRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
abstract class GraphicsMixin {
    @Inject(method = "blit(Lnet/minecraft/resources/ResourceLocation;IIIIIIIFFII)V", at = @At("HEAD"), cancellable = true)
    private void slick$blit(ResourceLocation texture, int x0, int x1, int y0, int y1, int offset, int width, int height,
                            float u, float v, int textureWidth, int textureHeight, CallbackInfo ci) {
        if (slick$replace(texture, (int) u, (int) v, x0, y0, x1 - x0, y1 - y0)) {
            ci.cancel();
        }
    }

    @Inject(method = "blitNineSliced(Lnet/minecraft/resources/ResourceLocation;IIIIIIIIIIII)V", at = @At("HEAD"),
            cancellable = true)
    private void slick$nineSliced(ResourceLocation texture, int x, int y, int width, int height, int left, int top,
                                  int right, int bottom, int sourceWidth, int sourceHeight, int u, int v,
                                  CallbackInfo ci) {
        if (MenuRenderer.framing() && slick$sprite(texture,
                LegacySprites.name(texture.getPath(), u, v, sourceWidth, sourceHeight), x, y, width, height)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean slick$replace(ResourceLocation texture, int u, int v, int x, int y, int width, int height) {
        if (!MenuRenderer.framing() || !"minecraft".equals(texture.getNamespace())) {
            return false;
        }
        String sprite = LegacySprites.name(texture.getPath(), u, v, width, height);
        if (sprite == null) {
            return MenuRenderer.skipTexture(texture.getNamespace(), texture.getPath(), x, y, width, height,
                    new Canvas((GuiGraphics) (Object) this));
        }
        return slick$sprite(texture, sprite, x, y, width, height);
    }

    @Unique
    private boolean slick$sprite(ResourceLocation texture, String sprite, int x, int y, int width, int height) {
        if (sprite == null || sprite.isEmpty()) {
            return false;
        }
        return MenuRenderer.sprite(texture.getNamespace(), sprite, x, y, width, height,
                new Canvas((GuiGraphics) (Object) this));
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
