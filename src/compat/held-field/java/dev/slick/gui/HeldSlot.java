package dev.slick.gui;

import net.minecraft.world.entity.player.Inventory;

public final class HeldSlot {
    private HeldSlot() {
    }

    public static int get(Inventory inventory) {
        return inventory.selected;
    }

    public static void set(Inventory inventory, int slot) {
        inventory.selected = slot;
    }
}
