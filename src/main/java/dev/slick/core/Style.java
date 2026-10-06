package dev.slick.core;

import com.google.gson.annotations.SerializedName;

public enum Style implements Choice {
    @SerializedName("modern")
    MODERN("modern"),
    @SerializedName("cards")
    CARDS("cards"),
    @SerializedName("minimal")
    MINIMAL("minimal"),
    @SerializedName("outline")
    OUTLINE("outline"),
    @SerializedName("bevel")
    BEVEL("bevel");

    private final String id;

    Style(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
