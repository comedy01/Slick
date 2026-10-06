package dev.slick.core;

import com.google.gson.annotations.SerializedName;

public enum Accent implements Choice {
    @SerializedName("theme")
    THEME("theme", 0),
    @SerializedName("blue")
    BLUE("blue", 0xFF5B8CFF),
    @SerializedName("violet")
    VIOLET("violet", 0xFF9B7BFF),
    @SerializedName("pink")
    PINK("pink", 0xFFFF6FAE),
    @SerializedName("red")
    RED("red", 0xFFFF5C5C),
    @SerializedName("orange")
    ORANGE("orange", 0xFFFF9A3C),
    @SerializedName("yellow")
    YELLOW("yellow", 0xFFFFD447),
    @SerializedName("green")
    GREEN("green", 0xFF5AD67D),
    @SerializedName("teal")
    TEAL("teal", 0xFF3CCFC0),
    @SerializedName("white")
    WHITE("white", 0xFFF2F4F8);

    private final String id;
    private final int color;

    Accent(String id, int color) {
        this.id = id;
        this.color = color;
    }

    @Override
    public String id() {
        return id;
    }

    public int color() {
        return color;
    }
}
