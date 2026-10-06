package dev.slick.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.slick.core.Accent;
import dev.slick.core.Corners;
import dev.slick.core.Palette;
import dev.slick.core.Speed;
import dev.slick.core.Style;
import dev.slick.core.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SlickConfig {
    public static final String FILE_NAME = "slick.json";

    public static final int MIN_OPACITY = 30;
    public static final int MAX_OPACITY = 100;

    private static final Logger LOGGER = LoggerFactory.getLogger("slick");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean hotbar = true;
    private boolean hotbarSlots = true;
    private boolean wrap = true;
    private boolean lift = true;
    private boolean menus = true;
    private boolean blur = true;
    private Style style = Style.MODERN;
    private Theme theme = Theme.MIDNIGHT;
    private Accent accent = Accent.THEME;
    private int opacity = 90;
    private Corners corners = Corners.ROUND;
    private boolean shadows = true;
    private Speed speed = Speed.NORMAL;

    private transient Palette palette;

    public boolean hotbar() {
        return hotbar;
    }

    public void setHotbar(boolean value) {
        hotbar = value;
    }

    public boolean hotbarSlots() {
        return hotbarSlots;
    }

    public void setHotbarSlots(boolean value) {
        hotbarSlots = value;
    }

    public boolean wrap() {
        return wrap;
    }

    public void setWrap(boolean value) {
        wrap = value;
    }

    public boolean lift() {
        return lift;
    }

    public void setLift(boolean value) {
        lift = value;
    }

    public boolean menus() {
        return menus;
    }

    public void setMenus(boolean value) {
        menus = value;
    }

    public boolean blur() {
        return blur;
    }

    public void setBlur(boolean value) {
        blur = value;
    }

    public Style style() {
        return style;
    }

    public void setStyle(Style value) {
        style = value == null ? Style.MODERN : value;
    }

    public Theme theme() {
        return theme;
    }

    public void setTheme(Theme value) {
        theme = value == null ? Theme.MIDNIGHT : value;
        palette = null;
    }

    public Accent accent() {
        return accent;
    }

    public void setAccent(Accent value) {
        accent = value == null ? Accent.THEME : value;
        palette = null;
    }

    public int opacity() {
        return opacity;
    }

    public void setOpacity(int value) {
        opacity = Math.max(MIN_OPACITY, Math.min(MAX_OPACITY, value));
        palette = null;
    }

    public Corners corners() {
        return corners;
    }

    public void setCorners(Corners value) {
        corners = value == null ? Corners.ROUND : value;
    }

    public boolean shadows() {
        return shadows;
    }

    public void setShadows(boolean value) {
        shadows = value;
    }

    public Speed speed() {
        return speed;
    }

    public void setSpeed(Speed value) {
        speed = value == null ? Speed.NORMAL : value;
    }

    public Palette palette() {
        if (palette == null) {
            palette = Palette.of(theme, accent, opacity);
        }
        return palette;
    }

    public void resetToDefaults() {
        SlickConfig defaults = new SlickConfig();
        hotbar = defaults.hotbar;
        hotbarSlots = defaults.hotbarSlots;
        wrap = defaults.wrap;
        lift = defaults.lift;
        menus = defaults.menus;
        blur = defaults.blur;
        style = defaults.style;
        theme = defaults.theme;
        accent = defaults.accent;
        opacity = defaults.opacity;
        corners = defaults.corners;
        shadows = defaults.shadows;
        speed = defaults.speed;
        palette = null;
    }

    private void sanitize() {
        setStyle(style);
        setTheme(theme);
        setAccent(accent);
        setOpacity(opacity);
        setCorners(corners);
        setSpeed(speed);
    }

    public static SlickConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            SlickConfig fresh = new SlickConfig();
            fresh.saveQuietly(file);
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            SlickConfig loaded = GSON.fromJson(reader, SlickConfig.class);
            if (loaded == null) {
                throw new JsonParseException("config file is empty");
            }
            loaded.sanitize();
            return loaded;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}; using defaults. {}", file, e.toString());
            moveAside(file);
            SlickConfig fresh = new SlickConfig();
            fresh.saveQuietly(file);
            return fresh;
        }
    }

    public void save(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.write(temp, (GSON.toJson(this) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void saveQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            save(file);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up unreadable config {}: {}", file, e.toString());
        }
    }
}
