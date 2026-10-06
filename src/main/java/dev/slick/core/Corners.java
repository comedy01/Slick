package dev.slick.core;

import com.google.gson.annotations.SerializedName;

public enum Corners implements Choice {
    @SerializedName("square")
    SQUARE("square", 0.0F),
    @SerializedName("soft")
    SOFT("soft", 2.0F),
    @SerializedName("round")
    ROUND("round", 4.0F);

    private final String id;
    private final float radius;

    Corners(String id, float radius) {
        this.id = id;
        this.radius = radius;
    }

    @Override
    public String id() {
        return id;
    }

    public float radius() {
        return radius;
    }
}
