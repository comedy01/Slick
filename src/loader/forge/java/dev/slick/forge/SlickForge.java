package dev.slick.forge;

import dev.slick.client.SlickClient;
import dev.slick.client.gui.SlickSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkConstants;

@Mod(SlickClient.MOD_ID)
public final class SlickForge {
    public SlickForge() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        SlickClient.init(FMLPaths.CONFIGDIR.get());

        KeyMapping settings = SlickClient.createSettingsKey();
        SlickClient.setKeys(settings);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(settings));

        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> new SlickSettingsScreen(parent, client.options)));
    }
}
