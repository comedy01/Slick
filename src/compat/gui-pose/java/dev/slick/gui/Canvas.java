package dev.slick.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class Canvas {
    private final GuiGraphics graphics;

    public Canvas(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public int width() {
        return graphics.guiWidth();
    }

    public int height() {
        return graphics.guiHeight();
    }

    public int guiScale() {
        return Math.max(1, (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale()));
    }

    public void push() {
        graphics.pose().pushPose();
    }

    public void pop() {
        graphics.pose().popPose();
    }

    public void translate(float x, float y) {
        graphics.pose().translate(x, y, 0.0F);
    }

    public void scale(float scale) {
        graphics.pose().scale(scale, scale, 1.0F);
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        if ((color >>> 24) == 0 || x1 <= x0 || y1 <= y0) {
            return;
        }
        graphics.fill(x0, y0, x1, y1, color);
    }

    public void batch(Runnable draw) {
        draw.run();
    }

    public void scissor(int x0, int y0, int x1, int y1) {
        graphics.enableScissor(x0, y0, x1, y1);
    }

    public void endScissor() {
        graphics.disableScissor();
    }

    public void blur() {
        graphics.flush();
        Minecraft.getInstance().gameRenderer.processBlurEffect();
    }
}
