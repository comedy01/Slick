package dev.slick.selftest;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

final class Stacks {
    private Stacks() {
    }

    static Item whiteBanner() {
        return Items.BANNER.white();
    }

    static Item redDye() {
        return Items.DYE.red();
    }
}
