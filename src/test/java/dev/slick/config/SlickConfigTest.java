package dev.slick.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.slick.core.Accent;
import dev.slick.core.Corners;
import dev.slick.core.Speed;
import dev.slick.core.Style;
import dev.slick.core.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class SlickConfigTest {
    @TempDir
    Path dir;

    @Test
    void savesAndLoadsEverySetting() throws IOException {
        Path file = dir.resolve(SlickConfig.FILE_NAME);
        SlickConfig config = new SlickConfig();
        config.setStyle(Style.BEVEL);
        config.setTheme(Theme.FROST);
        config.setAccent(Accent.ORANGE);
        config.setOpacity(55);
        config.setCorners(Corners.SQUARE);
        config.setSpeed(Speed.RELAXED);
        config.setWrap(false);
        config.setBlur(false);
        config.save(file);

        SlickConfig loaded = SlickConfig.load(file);
        assertEquals(Style.BEVEL, loaded.style());
        assertEquals(Theme.FROST, loaded.theme());
        assertEquals(Accent.ORANGE, loaded.accent());
        assertEquals(55, loaded.opacity());
        assertEquals(Corners.SQUARE, loaded.corners());
        assertEquals(Speed.RELAXED, loaded.speed());
        assertFalse(loaded.wrap());
        assertFalse(loaded.blur());
        assertTrue(Files.readString(file).contains("\"theme\": \"frost\""));
    }

    @Test
    void brokenFileFallsBackToDefaultsAndIsKept() throws IOException {
        Path file = dir.resolve(SlickConfig.FILE_NAME);
        Files.writeString(file, "{ not json");
        SlickConfig loaded = SlickConfig.load(file);
        assertEquals(Theme.MIDNIGHT, loaded.theme());
        assertTrue(Files.exists(dir.resolve(SlickConfig.FILE_NAME + ".broken")));
    }

    @Test
    void outOfRangeValuesAreClamped() throws IOException {
        Path file = dir.resolve(SlickConfig.FILE_NAME);
        Files.writeString(file, "{\"opacity\": 5, \"theme\": \"nope\"}");
        SlickConfig loaded = SlickConfig.load(file);
        assertEquals(SlickConfig.MIN_OPACITY, loaded.opacity());
        assertEquals(Theme.MIDNIGHT, loaded.theme());
    }

    @Test
    void resetRestoresDefaults() {
        SlickConfig config = new SlickConfig();
        config.setTheme(Theme.ONYX);
        config.setHotbar(false);
        config.resetToDefaults();
        assertEquals(Theme.MIDNIGHT, config.theme());
        assertTrue(config.hotbar());
        assertEquals(Accent.BLUE.color(), config.palette().accent());
    }
}
