package dev.slick.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

final class FillBatch extends Quads implements GuiElementRenderState {
    FillBatch(ScreenRectangle scissor) {
        super(scissor);
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }
}
