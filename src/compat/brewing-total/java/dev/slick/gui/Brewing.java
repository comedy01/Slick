package dev.slick.gui;

import net.minecraft.world.inventory.BrewingStandMenu;

final class Brewing {
    private Brewing() {
    }

    static float fuel(BrewingStandMenu menu) {
        int total = menu.getTotalFuel();
        return total > 0 ? Math.min(1.0F, (float) menu.getFuel() / total) : 0.0F;
    }

    static float progress(BrewingStandMenu menu) {
        int ticks = menu.getBrewingTicks();
        int total = menu.getTotalBrewingTicks();
        return ticks > 0 && total > 0 ? 1.0F - (float) ticks / total : 0.0F;
    }
}
