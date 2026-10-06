package dev.slick.showcase;

import net.fabricmc.api.ClientModInitializer;

public final class SlickShowcaseFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Showcase.start();
    }
}
