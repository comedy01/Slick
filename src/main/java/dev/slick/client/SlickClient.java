package dev.slick.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.slick.client.gui.SlickSettingsScreen;
import dev.slick.config.SlickConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

public final class SlickClient {
    public static final String MOD_ID = "slick";

    private static SlickConfig config = new SlickConfig();
    private static Path configPath;
    private static KeyMapping settingsKey;

    private SlickClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(SlickConfig.FILE_NAME);
        config = SlickConfig.load(configPath);
    }

    public static KeyMapping createSettingsKey() {
        return KeyFactory.create("key.slick.settings", InputConstants.UNKNOWN.getValue());
    }

    public static void setKeys(KeyMapping settings) {
        settingsKey = settings;
    }

    public static void tick(Minecraft mc) {
        if (settingsKey == null) {
            return;
        }
        boolean open = false;
        while (settingsKey.consumeClick()) {
            open = true;
        }
        if (open && mc.player != null) {
            SlickSettingsScreen.open(mc, null);
        }
    }

    public static SlickConfig config() {
        return config;
    }

    public static Path configPath() {
        return configPath;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
    }
}
