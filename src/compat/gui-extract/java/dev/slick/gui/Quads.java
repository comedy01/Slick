package dev.slick.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2fc;

import java.util.Arrays;

// Many solid fills as one GUI element. Every fill submitted on its own makes GuiRenderState scan the elements already
// placed for overlaps, and open a new layer when a fill sits inside the previous one, which is close to quadratic for
// the thousands of pixel rows Painter draws. One element is placed once and keeps its fills in order.
abstract class Quads {
    private final ScreenRectangle scissor;
    private float[] corners = new float[8 * 64];
    private int[] colors = new int[64];
    private int count;
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;
    private ScreenRectangle bounds;

    Quads(ScreenRectangle scissor) {
        this.scissor = scissor;
    }

    void add(Matrix3x2fc pose, int x0, int y0, int x1, int y1, int color) {
        if (count == colors.length) {
            corners = Arrays.copyOf(corners, corners.length * 2);
            colors = Arrays.copyOf(colors, colors.length * 2);
        }
        int at = count * 8;
        corner(pose, x0, y0, at);
        corner(pose, x0, y1, at + 2);
        corner(pose, x1, y1, at + 4);
        corner(pose, x1, y0, at + 6);
        colors[count++] = color;
    }

    private void corner(Matrix3x2fc pose, float x, float y, int at) {
        float px = pose.m00() * x + pose.m10() * y + pose.m20();
        float py = pose.m01() * x + pose.m11() * y + pose.m21();
        corners[at] = px;
        corners[at + 1] = py;
        minX = Math.min(minX, px);
        minY = Math.min(minY, py);
        maxX = Math.max(maxX, px);
        maxY = Math.max(maxY, py);
    }

    boolean isEmpty() {
        return count == 0;
    }

    void seal() {
        int x = (int) Math.floor(minX);
        int y = (int) Math.floor(minY);
        ScreenRectangle area = new ScreenRectangle(x, y, (int) Math.ceil(maxX) - x, (int) Math.ceil(maxY) - y);
        bounds = scissor == null ? area : scissor.intersection(area);
    }

    public void buildVertices(VertexConsumer consumer) {
        for (int i = 0; i < count; i++) {
            int at = i * 8;
            int color = colors[i];
            for (int k = 0; k < 8; k += 2) {
                consumer.addVertex(corners[at + k], corners[at + k + 1], 0.0F).setColor(color);
            }
        }
    }

    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    public ScreenRectangle scissorArea() {
        return scissor;
    }

    public ScreenRectangle bounds() {
        return bounds;
    }
}
