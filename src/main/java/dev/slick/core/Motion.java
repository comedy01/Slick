package dev.slick.core;

public final class Motion {
    private long last;

    public float tick(long nanos) {
        if (last == 0L) {
            last = nanos;
            return 0.0F;
        }
        float dt = (nanos - last) / 1.0E9F;
        last = nanos;
        return Math.max(0.0F, Math.min(0.1F, dt));
    }

    public static float approach(float current, float target, float rate, float dt) {
        if (rate <= 0.0F) {
            return target;
        }
        float next = current + (target - current) * (1.0F - (float) Math.exp(-rate * dt));
        return Math.abs(target - next) < 0.001F ? target : next;
    }

    public static float easeOut(float t) {
        float k = 1.0F - Math.max(0.0F, Math.min(1.0F, t));
        return 1.0F - k * k * k;
    }
}
