package dev.slick.gui;

import dev.slick.core.Colors;

public final class Painter {
    private static long fills;

    private final Canvas canvas;
    private final int scale;

    public Painter(Canvas canvas) {
        this.canvas = canvas;
        this.scale = canvas.guiScale();
    }

    public static long fills() {
        return fills;
    }

    public int scale() {
        return scale;
    }

    public void rect(float x, float y, float w, float h, float radius, int color) {
        if (Colors.alpha(color) == 0) {
            return;
        }
        int x0 = px(x);
        int y0 = px(y);
        int x1 = px(x + w);
        int y1 = px(y + h);
        canvas.push();
        canvas.scale(1.0F / scale);
        fillRounded(x0, y0, x1, y1, limit(radius, x0, y0, x1, y1), color);
        canvas.pop();
    }

    public void ring(float x, float y, float w, float h, float radius, int thickness, int color) {
        if (Colors.alpha(color) == 0 || thickness <= 0) {
            return;
        }
        int x0 = px(x);
        int y0 = px(y);
        int x1 = px(x + w);
        int y1 = px(y + h);
        canvas.push();
        canvas.scale(1.0F / scale);
        ringRounded(x0, y0, x1, y1, limit(radius, x0, y0, x1, y1), thickness, color);
        canvas.pop();
    }

    public void bevel(float x, float y, float w, float h, int topLeft, int bottomRight, int thickness) {
        int x0 = px(x);
        int y0 = px(y);
        int x1 = px(x + w);
        int y1 = px(y + h);
        if (x1 - x0 <= 2 * thickness || y1 - y0 <= 2 * thickness) {
            return;
        }
        canvas.push();
        canvas.scale(1.0F / scale);
        fill(x0, y0, x1 - thickness, y0 + thickness, topLeft);
        fill(x0, y0 + thickness, x0 + thickness, y1 - thickness, topLeft);
        fill(x0 + thickness, y1 - thickness, x1, y1, bottomRight);
        fill(x1 - thickness, y0, x1, y1 - thickness, bottomRight);
        canvas.pop();
    }

    public void shadow(float x, float y, float w, float h, float radius, int color) {
        if (Colors.alpha(color) == 0) {
            return;
        }
        int x0 = px(x);
        int y0 = px(y + 1.0F);
        int x1 = px(x + w);
        int y1 = px(y + h + 1.0F);
        int spread = Math.max(3, scale * 3);
        int r = limit(radius, x0, y0, x1, y1);
        canvas.push();
        canvas.scale(1.0F / scale);
        for (int i = 1; i <= spread; i++) {
            float fall = 1.0F - (float) i / (spread + 1);
            int layer = Colors.fade(color, fall * fall / 2.0F);
            ringRounded(x0 - i, y0 - i, x1 + i, y1 + i, r + i, 1, layer);
        }
        canvas.pop();
    }

    private int px(float value) {
        return Math.round(value * scale);
    }

    private int limit(float radius, int x0, int y0, int x1, int y1) {
        int r = Math.round(radius * scale);
        return Math.max(0, Math.min(r, Math.min(x1 - x0, y1 - y0) / 2));
    }

    private void fillRounded(int x0, int y0, int x1, int y1, int r, int color) {
        if (r <= 0) {
            fill(x0, y0, x1, y1, color);
            return;
        }
        fill(x0, y0 + r, x1, y1 - r, color);
        for (int row = 0; row < r; row++) {
            float in = inset(r, row);
            span(x0, x1, y0 + row, in, color);
            span(x0, x1, y1 - 1 - row, in, color);
        }
    }

    private void span(int x0, int x1, int y, float in, int color) {
        int full = (int) Math.ceil(in);
        fill(x0 + full, y, x1 - full, y + 1, color);
        float cover = full - in;
        if (full > 0 && cover > 0.03F) {
            int edge = Colors.fade(color, cover);
            fill(x0 + full - 1, y, x0 + full, y + 1, edge);
            fill(x1 - full, y, x1 - full + 1, y + 1, edge);
        }
    }

    private void ringRounded(int x0, int y0, int x1, int y1, int r, int t, int color) {
        int h = y1 - y0;
        int w = x1 - x0;
        if (w <= 2 * t || h <= 2 * t) {
            fillRounded(x0, y0, x1, y1, r, color);
            return;
        }
        int inner = Math.max(0, r - t);
        int edge = Math.max(r, t);
        for (int row = 0; row < edge && row < h / 2; row++) {
            ringRow(x0, x1, y0 + row, row, r, t, inner, color);
            ringRow(x0, x1, y1 - 1 - row, row, r, t, inner, color);
        }
        fill(x0, y0 + edge, x0 + t, y1 - edge, color);
        fill(x1 - t, y0 + edge, x1, y1 - edge, color);
    }

    private void ringRow(int x0, int x1, int y, int row, int r, int t, int inner, int color) {
        float out = row < r ? inset(r, row) : 0.0F;
        if (row < t) {
            span(x0, x1, y, out, color);
            return;
        }
        int innerRow = row - t;
        int in = t + (innerRow < inner ? Math.round(inset(inner, innerRow)) : 0);
        int full = (int) Math.ceil(out);
        fill(x0 + full, y, x0 + in, y + 1, color);
        fill(x1 - in, y, x1 - full, y + 1, color);
        float cover = full - out;
        if (full > 0 && cover > 0.03F) {
            int soft = Colors.fade(color, cover);
            fill(x0 + full - 1, y, x0 + full, y + 1, soft);
            fill(x1 - full, y, x1 - full + 1, y + 1, soft);
        }
    }

    static float inset(int r, int row) {
        float dy = r - row - 0.5F;
        return r - (float) Math.sqrt(Math.max(0.0F, r * r - dy * dy));
    }

    private void fill(int x0, int y0, int x1, int y1, int color) {
        fills++;
        canvas.fill(x0, y0, x1, y1, color);
    }
}
