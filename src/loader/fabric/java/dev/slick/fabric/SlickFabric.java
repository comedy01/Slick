package dev.slick.fabric;

import dev.slick.client.KeyRegistrar;
import dev.slick.client.SlickClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class SlickFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SlickClient.init(FabricLoader.getInstance().getConfigDir());
        SlickClient.setKeys(KeyRegistrar.register(SlickClient.createSettingsKey()));
    }
}
