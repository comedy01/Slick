package dev.slick.core;

public final class Colors {
    private Colors() {
    }

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    public static int fade(int color, float factor) {
        return withAlpha(color, Math.round(alpha(color) * Math.max(0.0F, Math.min(1.0F, factor))));
    }

    public static int mix(int from, int to, float t) {
        float k = Math.max(0.0F, Math.min(1.0F, t));
        int a = Math.round(channel(from, 24) + (channel(to, 24) - channel(from, 24)) * k);
        int r = Math.round(channel(from, 16) + (channel(to, 16) - channel(from, 16)) * k);
        int g = Math.round(channel(from, 8) + (channel(to, 8) - channel(from, 8)) * k);
        int b = Math.round(channel(from, 0) + (channel(to, 0) - channel(from, 0)) * k);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int color, int shift) {
        return (color >>> shift) & 0xFF;
    }
}
