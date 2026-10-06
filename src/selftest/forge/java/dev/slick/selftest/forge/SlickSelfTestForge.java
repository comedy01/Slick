package dev.slick.selftest.forge;

import dev.slick.selftest.SlickSelfTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("slick_selftest")
public final class SlickSelfTestForge {
    public SlickSelfTestForge() {
        SlickSelfTest test = new SlickSelfTest();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                test.tick();
            }
        });
    }
}
