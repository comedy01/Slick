package dev.slick.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.slick.client.gui.SlickSettingsScreen;
import net.minecraft.client.Minecraft;

public final class SlickModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new SlickSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
