package dev.slick.gui;

import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.world.inventory.NonInteractiveResultSlot;
import net.minecraft.world.inventory.Slot;

import java.util.Set;

final class Crafters {
    static final Set<Class<?>> SCREENS = Set.of(CrafterScreen.class);

    private Crafters() {
    }

    static boolean result(Slot slot) {
        return slot instanceof NonInteractiveResultSlot;
    }
}
