package dev.slick.core;

public final class Glide {
    private final int slots;
    private float position = -1.0F;

    public Glide(int slots) {
        this.slots = slots;
    }

    public float position() {
        return position;
    }

    public void reset() {
        position = -1.0F;
    }

    public float update(int target, float rate, float dt, boolean wrap) {
        if (position < 0.0F || rate <= 0.0F) {
            position = target;
            return position;
        }
        float delta = offset(target, wrap);
        position += delta * (1.0F - (float) Math.exp(-rate * dt));
        if (Math.abs(offset(target, wrap)) < 0.002F) {
            position = target;
        }
        if (wrap) {
            position = ((position % slots) + slots) % slots;
        }
        return position;
    }

    private float offset(int target, boolean wrap) {
        float delta = target - position;
        if (wrap) {
            float half = slots / 2.0F;
            if (delta > half) {
                delta -= slots;
            } else if (delta < -half) {
                delta += slots;
            }
        }
        return delta;
    }
}
