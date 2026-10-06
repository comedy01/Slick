package dev.slick.core;

import com.google.gson.annotations.SerializedName;

public enum Theme implements Choice {
    @SerializedName("midnight")
    MIDNIGHT("midnight", 0xFF1B202B, 0xFF0E1117, 0xFF363D4D, 0xFFE9EDF5, 0xFF8E97A8, Accent.BLUE),
    @SerializedName("graphite")
    GRAPHITE("graphite", 0xFF232324, 0xFF161617, 0xFF3D3D40, 0xFFEDEDED, 0xFF9A9A9E, Accent.TEAL),
    @SerializedName("frost")
    FROST("frost", 0x30FFFFFF, 0x48000000, 0x66FFFFFF, 0xFFFFFFFF, 0xFFD7DDE8, Accent.WHITE),
    @SerializedName("daylight")
    DAYLIGHT("daylight", 0xFFF3F4F7, 0xFFB4BBC8, 0xFFC3C8D2, 0xFF1E222B, 0xFF5F6776, Accent.BLUE),
    @SerializedName("onyx")
    ONYX("onyx", 0xFF050505, 0xFF141414, 0xFF2B2B2B, 0xFFFFFFFF, 0xFF8A8A8A, Accent.VIOLET);

    private final String id;
    private final int panel;
    private final int slot;
    private final int border;
    private final int text;
    private final int muted;
    private final Accent accent;

    Theme(String id, int panel, int slot, int border, int text, int muted, Accent accent) {
        this.id = id;
        this.panel = panel;
        this.slot = slot;
        this.border = border;
        this.text = text;
        this.muted = muted;
        this.accent = accent;
    }

    @Override
    public String id() {
        return id;
    }

    public int panel() {
        return panel;
    }

    public int slot() {
        return slot;
    }

    public int border() {
        return border;
    }

    public int text() {
        return text;
    }

    public int muted() {
        return muted;
    }

    public Accent accent() {
        return accent;
    }
}
