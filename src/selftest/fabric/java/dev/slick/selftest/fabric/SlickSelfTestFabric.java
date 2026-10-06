package dev.slick.selftest.fabric;

import dev.slick.selftest.SlickSelfTest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class SlickSelfTestFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SlickSelfTest test = new SlickSelfTest();
        ClientTickEvents.END_CLIENT_TICK.register(client -> test.tick());
    }
}
