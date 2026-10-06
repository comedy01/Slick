package dev.slick.selftest.neoforge;

import dev.slick.selftest.SlickSelfTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "slick_selftest", dist = Dist.CLIENT)
public final class SlickSelfTestNeoForge {
    public SlickSelfTestNeoForge() {
        SlickSelfTest test = new SlickSelfTest();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> test.tick());
    }
}
