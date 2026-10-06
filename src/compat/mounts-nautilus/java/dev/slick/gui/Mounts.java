package dev.slick.gui;

import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.NautilusInventoryScreen;

import java.util.Set;

final class Mounts {
    static final Set<Class<?>> SCREENS = Set.of(HorseInventoryScreen.class, NautilusInventoryScreen.class);

    private Mounts() {
    }
}
