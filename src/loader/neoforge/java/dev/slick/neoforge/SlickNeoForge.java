package dev.slick.neoforge;

import dev.slick.client.SlickClient;
import dev.slick.client.gui.SlickSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = SlickClient.MOD_ID, dist = Dist.CLIENT)
public final class SlickNeoForge {
    public SlickNeoForge(IEventBus modBus, ModContainer container) {
        SlickClient.init(FMLPaths.CONFIGDIR.get());

        KeyMapping settings = SlickClient.createSettingsKey();
        SlickClient.setKeys(settings);
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(settings));

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new SlickSettingsScreen(parent, Minecraft.getInstance().options));
    }
}
