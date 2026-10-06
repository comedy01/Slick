package dev.slick.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Canvas {
    private static final Field RENDER_STATE;
    private static final Field SCISSOR_STACK;
    private static final Method PEEK;
    private static FillBatch batch;

    static {
        Field state = null;
        Field stack = null;
        Method peek = null;
        try {
            state = GuiGraphicsExtractor.class.getDeclaredField("guiRenderState");
            state.setAccessible(true);
            stack = GuiGraphicsExtractor.class.getDeclaredField("scissorStack");
            stack.setAccessible(true);
            peek = stack.getType().getDeclaredMethod("peek");
            peek.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            state = null;
        }
        RENDER_STATE = state;
        SCISSOR_STACK = stack;
        PEEK = peek;
    }

    private final GuiGraphicsExtractor graphics;

    public Canvas(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    public int width() {
        return graphics.guiWidth();
    }

    public int height() {
        return graphics.guiHeight();
    }

    public int guiScale() {
        return Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
    }

    public void push() {
        graphics.pose().pushMatrix();
    }

    public void pop() {
        graphics.pose().popMatrix();
    }

    public void translate(float x, float y) {
        graphics.pose().translate(x, y);
    }

    public void scale(float scale) {
        graphics.pose().scale(scale, scale);
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        if ((color >>> 24) == 0 || x1 <= x0 || y1 <= y0) {
            return;
        }
        if (batch != null) {
            batch.add(graphics.pose(), x0, y0, x1, y1, color);
        } else {
            graphics.fill(x0, y0, x1, y1, color);
        }
    }

    public void batch(Runnable draw) {
        if (batch != null || RENDER_STATE == null) {
            draw.run();
            return;
        }
        batch = new FillBatch(currentScissor());
        try {
            draw.run();
        } finally {
            submit();
        }
    }

    private void submit() {
        FillBatch done = batch;
        batch = null;
        if (done == null || done.isEmpty()) {
            return;
        }
        done.seal();
        try {
            ((GuiRenderState) RENDER_STATE.get(graphics)).addGuiElement((GuiElementRenderState) done);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private ScreenRectangle currentScissor() {
        try {
            return (ScreenRectangle) PEEK.invoke(SCISSOR_STACK.get(graphics));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public void scissor(int x0, int y0, int x1, int y1) {
        boolean batching = batch != null;
        submit();
        graphics.enableScissor(x0, y0, x1, y1);
        if (batching) {
            batch = new FillBatch(currentScissor());
        }
    }

    public void endScissor() {
        boolean batching = batch != null;
        submit();
        graphics.disableScissor();
        if (batching) {
            batch = new FillBatch(currentScissor());
        }
    }

    public void blur() {
        graphics.blurBeforeThisStratum();
    }
}
