package dev.slick.gui;

import net.minecraft.world.inventory.BrewingStandMenu;

final class Brewing {
    private static final int FUEL = 20;
    private static final float TICKS = 400.0F;

    private Brewing() {
    }

    static float fuel(BrewingStandMenu menu) {
        return Math.max(0.0F, Math.min(1.0F, (float) menu.getFuel() / FUEL));
    }

    static float progress(BrewingStandMenu menu) {
        int ticks = menu.getBrewingTicks();
        return ticks > 0 ? Math.max(0.0F, 1.0F - ticks / TICKS) : 0.0F;
    }
}
