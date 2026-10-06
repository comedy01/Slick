package dev.slick.core;

import com.google.gson.annotations.SerializedName;

public enum Speed implements Choice {
    @SerializedName("off")
    OFF("off", 0.0F, 0),
    @SerializedName("fast")
    FAST("fast", 28.0F, 90),
    @SerializedName("normal")
    NORMAL("normal", 17.0F, 150),
    @SerializedName("relaxed")
    RELAXED("relaxed", 10.0F, 240);

    private final String id;
    private final float rate;
    private final int openMillis;

    Speed(String id, float rate, int openMillis) {
        this.id = id;
        this.rate = rate;
        this.openMillis = openMillis;
    }

    @Override
    public String id() {
        return id;
    }

    public float rate() {
        return rate;
    }

    public int openMillis() {
        return openMillis;
    }

    public boolean animated() {
        return this != OFF;
    }
}
