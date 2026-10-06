package dev.slick.gui;

import dev.slick.client.SlickClient;
import dev.slick.config.SlickConfig;
import dev.slick.core.Colors;
import dev.slick.core.Palette;
import dev.slick.core.Style;

public final class Skin {
    public enum State {
        NORMAL, HOVER, SELECTED, DISABLED
    }

    private static final int ERROR = 0xFFFF5C5C;
    private static final int HIGHLIGHT = 0x40FFFFFF;
    private static final int SHADE = 0x80000000;
    private static final int EDGE = 0xC0000000;

    private final Style style;
    private final Palette palette;
    private final Painter paint;
    private final float radius;
    private final int line;
    private final int pixel;
    private final boolean shadows;

    private Skin(Style style, Palette palette, Painter paint, float radius, boolean shadows) {
        this.style = style;
        this.palette = palette;
        this.paint = paint;
        this.radius = style == Style.BEVEL ? 0.0F : radius;
        this.line = Math.max(1, paint.scale() / 2);
        this.pixel = paint.scale();
        this.shadows = shadows;
    }

    public static Skin of(Canvas canvas) {
        SlickConfig config = SlickClient.config();
        return new Skin(config.style(), config.palette(), new Painter(canvas), config.corners().radius(), config.shadows());
    }

    public Style style() {
        return style;
    }

    public Palette palette() {
        return palette;
    }

    public Painter paint() {
        return paint;
    }

    private float small() {
        return radius * 0.6F;
    }

    private int outline() {
        return Colors.withAlpha(Colors.mix(palette.border(), palette.accent(), 0.55F), 255);
    }

    private int wire() {
        return Colors.fade(palette.text(), 0.28F);
    }

    private int card() {
        return Colors.mix(palette.panel(), Colors.withAlpha(0xFFFFFF, Colors.alpha(palette.panel())), 0.05F);
    }

    public void panel(float x, float y, float w, float h) {
        float r = radius + (radius > 0.0F ? 1.0F : 0.0F);
        switch (style) {
            case CARDS -> paint.rect(x, y, w, h, r, Colors.fade(palette.panel(), 0.12F));
            case MINIMAL -> paint.rect(x, y, w, h, r, Colors.fade(palette.panel(), 0.82F));
            case OUTLINE -> {
                paint.rect(x, y, w, h, r, Colors.fade(palette.panel(), 0.3F));
                paint.ring(x, y, w, h, r, pixel, outline());
            }
            case BEVEL -> {
                shadow(x, y, w, h, 0.0F);
                paint.rect(x, y, w, h, 0.0F, palette.panel());
                paint.ring(x, y, w, h, 0.0F, pixel, EDGE);
                paint.bevel(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, HIGHLIGHT, SHADE, pixel);
            }
            default -> {
                shadow(x, y, w, h, r);
                paint.rect(x, y, w, h, r, palette.panel());
                paint.ring(x, y, w, h, r, 1, palette.border());
            }
        }
    }

    public void card(float x, float y, float w, float h) {
        if (style != Style.CARDS) {
            return;
        }
        float r = radius + (radius > 0.0F ? 1.0F : 0.0F);
        shadow(x, y, w, h, r);
        paint.rect(x, y, w, h, r, card());
        paint.ring(x, y, w, h, r, 1, palette.border());
    }

    public void well(float x, float y, float w, float h) {
        switch (style) {
            case MINIMAL -> {
                if (w <= 27.0F && h <= 27.0F) {
                    paint.rect(x + w / 2.0F - 1.0F, y + h / 2.0F - 1.0F, 2.0F, 2.0F, 1.0F, Colors.fade(palette.muted(), 0.45F));
                } else {
                    paint.rect(x, y, w, h, small(), Colors.fade(palette.slot(), 0.4F));
                }
            }
            case OUTLINE -> paint.ring(x + 0.5F, y + 0.5F, w - 1.0F, h - 1.0F, small(), 1, wire());
            case BEVEL -> {
                paint.rect(x, y, w, h, 0.0F, palette.slot());
                paint.bevel(x, y, w, h, SHADE, HIGHLIGHT, pixel);
            }
            default -> paint.rect(x, y, w, h, small(), palette.slot());
        }
    }

    public void result(float x, float y, float w, float h) {
        switch (style) {
            case MINIMAL -> {
                paint.rect(x + 2.0F, y + 2.0F, w - 4.0F, h - 4.0F, small(), Colors.fade(palette.slot(), 0.4F));
                paint.rect(x + 6.0F, y + h - 2.0F, w - 12.0F, 1.5F, 0.75F, palette.accent());
            }
            case OUTLINE -> paint.ring(x, y, w, h, small(), pixel, palette.accent());
            case BEVEL -> {
                well(x, y, w, h);
                paint.ring(x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, 0.0F, pixel, Colors.fade(palette.accent(), 0.8F));
            }
            default -> {
                well(x, y, w, h);
                paint.ring(x, y, w, h, small(), 1, Colors.fade(palette.accent(), 0.55F));
            }
        }
    }

    public void glow(float x, float y, float w, float h, float value) {
        int accent = palette.accent();
        switch (style) {
            case MINIMAL -> {
                paint.rect(x, y, w, h, small() + 1.0F, Colors.fade(accent, 0.16F * value));
                float bar = (w - 6.0F) * value;
                paint.rect(x + (w - bar) / 2.0F, y + h - 1.5F, bar, 1.5F, 0.75F, accent);
            }
            case OUTLINE -> {
                paint.rect(x, y, w, h, small(), Colors.fade(accent, 0.1F * value));
                paint.ring(x, y, w, h, small(), pixel, Colors.fade(accent, value));
            }
            case BEVEL -> {
                paint.rect(x, y, w, h, 0.0F, Colors.fade(accent, 0.4F * value));
                paint.bevel(x, y, w, h, Colors.fade(HIGHLIGHT, value * 1.5F), Colors.fade(SHADE, value), pixel);
            }
            default -> {
                paint.rect(x, y, w, h, small(), Colors.fade(accent, 0.28F * value));
                paint.ring(x, y, w, h, small(), line, Colors.fade(accent, value));
            }
        }
    }

    public void button(float x, float y, float w, float h, State state) {
        int accent = palette.accent();
        int base = state == State.DISABLED ? Colors.fade(palette.slot(), 0.5F) : palette.slot();
        if (state == State.HOVER) {
            base = Colors.mix(base, Colors.withAlpha(accent, Colors.alpha(base)), 0.22F);
        }
        switch (style) {
            case MINIMAL -> {
                int fill = state == State.SELECTED ? Colors.fade(accent, 0.18F) : Colors.fade(base, 0.45F);
                paint.rect(x, y, w, h, small() + 1.0F, fill);
                if (state == State.HOVER || state == State.SELECTED) {
                    paint.rect(x + 3.0F, y + h - 1.5F, w - 6.0F, 1.5F, 0.75F,
                            state == State.SELECTED ? accent : Colors.fade(accent, 0.6F));
                }
            }
            case OUTLINE -> {
                if (state == State.SELECTED || state == State.HOVER) {
                    paint.rect(x, y, w, h, small(), Colors.fade(accent, state == State.SELECTED ? 0.22F : 0.1F));
                }
                int ring = switch (state) {
                    case HOVER, SELECTED -> accent;
                    case DISABLED -> Colors.fade(wire(), 0.5F);
                    default -> wire();
                };
                paint.ring(x, y, w, h, small(), state == State.SELECTED ? pixel : 1, ring);
            }
            case BEVEL -> {
                if (state == State.SELECTED) {
                    paint.rect(x, y, w, h, 0.0F, Colors.mix(palette.slot(), Colors.withAlpha(accent, 255), 0.4F));
                    paint.bevel(x, y, w, h, SHADE, HIGHLIGHT, pixel);
                } else {
                    int face = Colors.mix(Colors.withAlpha(palette.panel(), 255), state == State.HOVER
                            ? Colors.withAlpha(accent, 255) : 0xFFFFFFFF, state == State.DISABLED ? 0.0F : 0.12F);
                    paint.rect(x, y, w, h, 0.0F, state == State.DISABLED ? Colors.fade(face, 0.6F) : face);
                    paint.ring(x, y, w, h, 0.0F, 1, EDGE);
                    paint.bevel(x, y, w, h, HIGHLIGHT, SHADE, pixel);
                }
            }
            default -> {
                if (state == State.SELECTED) {
                    paint.rect(x, y, w, h, small(), Colors.mix(palette.slot(), Colors.withAlpha(accent, 255), 0.3F));
                    paint.ring(x, y, w, h, small(), line, accent);
                } else {
                    paint.rect(x, y, w, h, small(), base);
                    paint.ring(x, y, w, h, small(), 1, state == State.HOVER ? accent : palette.border());
                }
            }
        }
    }

    public void tab(float x, float y, float w, float h, boolean selected) {
        if (style == Style.CARDS) {
            shadow(x, y, w, h, radius);
            paint.rect(x, y, w, h, small(), selected ? Colors.mix(card(), Colors.withAlpha(palette.accent(), 255), 0.25F) : card());
            paint.ring(x, y, w, h, small(), selected ? line : 1, selected ? palette.accent() : palette.border());
            return;
        }
        button(x, y, w, h, selected ? State.SELECTED : State.NORMAL);
    }

    public void track(float x, float y, float w, float h) {
        float r = Math.min(w, h) / 2.0F;
        switch (style) {
            case OUTLINE -> paint.ring(x, y, w, h, r, 1, wire());
            case BEVEL -> {
                paint.rect(x, y, w, h, 0.0F, palette.slot());
                paint.bevel(x, y, w, h, SHADE, HIGHLIGHT, Math.max(1, pixel / 2));
            }
            case MINIMAL -> paint.rect(x, y, w, h, r, Colors.fade(palette.slot(), 0.4F));
            default -> paint.rect(x, y, w, h, r, palette.slot());
        }
    }

    public void thumb(float x, float y, float w, float h, boolean enabled) {
        int color = enabled ? (style == Style.MINIMAL || style == Style.OUTLINE ? palette.accent() : palette.muted())
                : Colors.fade(palette.muted(), 0.3F);
        if (style == Style.BEVEL) {
            paint.rect(x, y, w, h, 0.0F, color);
            paint.bevel(x, y, w, h, HIGHLIGHT, SHADE, Math.max(1, pixel / 2));
            return;
        }
        paint.rect(x, y, w, h, Math.min(w, h) / 2.0F, color);
    }

    public void fill(float x, float y, float w, float h, int color) {
        if (w <= 0.0F || h <= 0.0F) {
            return;
        }
        paint.rect(x, y, w, h, style == Style.BEVEL ? 0.0F : Math.min(w, h) / 2.0F, color);
    }

    public void arrow(float from, float to, float mid, int color) {
        float length = to - from;
        if (length < 6.0F) {
            return;
        }
        paint.rect(from, mid - 1.0F, length - 3.0F, 2.0F, style == Style.BEVEL ? 0.0F : 1.0F, color);
        for (int k = 0; k < 4; k++) {
            paint.rect(to - 5.0F + k, mid - 4.0F + k, 1.0F, 8.0F - 2.0F * k, 0.0F, color);
        }
    }

    public void error(float x, float y, float w, float h, boolean withArrow) {
        if (Math.abs(w - h) < 1.0F) {
            paint.rect(x, y, w, h, small(), Colors.fade(ERROR, 0.2F));
            paint.ring(x, y, w, h, small(), line, ERROR);
            return;
        }
        float mid = y + h / 2.0F;
        if (withArrow) {
            arrow(x + 3.0F, x + w - 3.0F, mid, Colors.fade(ERROR, 0.8F));
        }
        for (int k = -4; k <= 4; k++) {
            paint.rect(x + w / 2.0F + k - 0.5F, mid - k * 1.2F - 0.75F, 1.5F, 1.5F, 0.0F, ERROR);
        }
    }

    public void hotbarBase(float x, float y, float w, float h) {
        switch (style) {
            case CARDS -> {
            }
            case MINIMAL -> paint.rect(x, y, w, h, radius + 1.0F, Colors.fade(palette.panel(), 0.3F));
            case OUTLINE -> {
                paint.rect(x, y, w, h, radius, Colors.fade(palette.panel(), 0.3F));
                paint.ring(x, y, w, h, radius, pixel, outline());
            }
            default -> panel(x, y, w, h);
        }
    }

    public void hotbarSlot(float x, float y, float w, float h, boolean boxes) {
        switch (style) {
            case CARDS -> {
                shadow(x, y, w, h, small());
                paint.rect(x, y, w, h, small(), card());
                paint.ring(x, y, w, h, small(), 1, palette.border());
            }
            case MINIMAL -> {
                if (boxes) {
                    well(x, y, w, h);
                }
            }
            default -> {
                if (boxes) {
                    well(x, y, w, h);
                }
            }
        }
    }

    public void selection(float x, float y, float w, float h) {
        int accent = palette.accent();
        switch (style) {
            case MINIMAL -> {
                paint.rect(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, radius * 0.8F, Colors.fade(accent, 0.14F));
                paint.rect(x + 4.0F, y + h - 3.0F, w - 8.0F, 2.0F, 1.0F, accent);
            }
            case OUTLINE -> paint.ring(x, y, w, h, radius * 0.8F, pixel, accent);
            case BEVEL -> {
                paint.rect(x, y, w, h, 0.0F, Colors.fade(accent, 0.3F));
                paint.ring(x, y, w, h, 0.0F, pixel, accent);
                paint.bevel(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, HIGHLIGHT, SHADE, pixel);
            }
            case CARDS -> {
                paint.rect(x - 0.5F, y - 0.5F, w + 1.0F, h + 1.0F, radius * 0.8F, Colors.fade(accent, 0.25F));
                paint.ring(x - 0.5F, y - 0.5F, w + 1.0F, h + 1.0F, radius * 0.8F, line, accent);
            }
            default -> {
                paint.rect(x, y, w, h, radius * 0.8F, Colors.fade(accent, 0.22F));
                paint.ring(x, y, w, h, radius * 0.8F, line, accent);
            }
        }
    }

    private void shadow(float x, float y, float w, float h, float r) {
        if (shadows) {
            paint.shadow(x, y, w, h, r, palette.shadow());
        }
    }
}
