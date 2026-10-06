package dev.slick.core;

public record Palette(int panel, int slot, int border, int text, int muted, int accent, int shadow) {
    public static Palette of(Theme theme, Accent accent, int opacityPercent) {
        float opacity = Math.max(0, Math.min(100, opacityPercent)) / 100.0F;
        int accentColor = accent == Accent.THEME ? theme.accent().color() : accent.color();
        return new Palette(
                Colors.fade(theme.panel(), opacity),
                Colors.fade(theme.slot(), 0.35F + 0.65F * opacity),
                theme.border(),
                theme.text(),
                theme.muted(),
                accentColor,
                theme == Theme.DAYLIGHT ? 0x30000000 : 0x60000000);
    }
}
