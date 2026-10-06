package dev.slick.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoreTest {
    @Test
    void glideStartsOnTheSelectedSlot() {
        Glide glide = new Glide(9);
        assertEquals(3.0F, glide.update(3, 17.0F, 0.016F, true));
    }

    @Test
    void glideMovesSmoothlyAndSettles() {
        Glide glide = new Glide(9);
        glide.update(0, 17.0F, 0.016F, true);
        float first = glide.update(4, 17.0F, 0.016F, true);
        assertTrue(first > 0.0F && first < 4.0F, "moved part of the way: " + first);
        float last = first;
        for (int i = 0; i < 200; i++) {
            last = glide.update(4, 17.0F, 0.016F, true);
        }
        assertEquals(4.0F, last);
    }

    @Test
    void glideWrapsAroundTheShortWay() {
        Glide glide = new Glide(9);
        glide.update(8, 17.0F, 0.016F, true);
        float step = glide.update(0, 17.0F, 0.016F, true);
        assertTrue(step > 8.0F, "went past the right edge: " + step);
        float last = step;
        for (int i = 0; i < 200; i++) {
            last = glide.update(0, 17.0F, 0.016F, true);
        }
        assertEquals(0.0F, last);
    }

    @Test
    void glideWithoutWrapSlidesBack() {
        Glide glide = new Glide(9);
        glide.update(8, 17.0F, 0.016F, false);
        float step = glide.update(0, 17.0F, 0.016F, false);
        assertTrue(step < 8.0F && step > 0.0F, "slid left across the bar: " + step);
    }

    @Test
    void glideSnapsWhenAnimationsAreOff() {
        Glide glide = new Glide(9);
        glide.update(2, 17.0F, 0.016F, true);
        assertEquals(7.0F, glide.update(7, Speed.OFF.rate(), 0.016F, true));
    }

    @Test
    void approachReachesItsTarget() {
        float value = 0.0F;
        for (int i = 0; i < 300; i++) {
            value = Motion.approach(value, 1.0F, 17.0F, 0.016F);
        }
        assertEquals(1.0F, value);
        assertEquals(1.0F, Motion.approach(0.0F, 1.0F, 0.0F, 0.016F));
    }

    @Test
    void easeOutIsClampedAndMonotonic() {
        assertEquals(0.0F, Motion.easeOut(-1.0F));
        assertEquals(1.0F, Motion.easeOut(2.0F));
        assertTrue(Motion.easeOut(0.5F) > 0.5F);
    }

    @Test
    void motionClampsLongPauses() {
        Motion motion = new Motion();
        assertEquals(0.0F, motion.tick(1_000_000_000L));
        assertEquals(0.1F, motion.tick(5_000_000_000L));
    }

    @Test
    void paletteUsesTheThemeAccentUnlessOverridden() {
        assertEquals(Accent.BLUE.color(), Palette.of(Theme.MIDNIGHT, Accent.THEME, 90).accent());
        assertEquals(Accent.PINK.color(), Palette.of(Theme.MIDNIGHT, Accent.PINK, 90).accent());
    }

    @Test
    void opacityOnlyFadesThePanel() {
        Palette solid = Palette.of(Theme.GRAPHITE, Accent.THEME, 100);
        Palette faint = Palette.of(Theme.GRAPHITE, Accent.THEME, 40);
        assertEquals(255, Colors.alpha(solid.panel()));
        assertEquals(102, Colors.alpha(faint.panel()));
        assertEquals(solid.text(), faint.text());
    }

    @Test
    void colorsFadeAndMix() {
        assertEquals(0x80FFFFFF, Colors.fade(0xFFFFFFFF, 128 / 255.0F));
        assertEquals(0xFF808080, Colors.mix(0xFF000000, 0xFFFFFFFF, 128 / 255.0F));
    }
}
