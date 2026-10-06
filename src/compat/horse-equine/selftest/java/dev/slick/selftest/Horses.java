package dev.slick.selftest;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;

final class Horses {
    private Horses() {
    }

    static Screen screen(Inventory inventory, Entity entity) {
        AbstractHorse horse = (AbstractHorse) entity;
        HorseInventoryMenu menu = new HorseInventoryMenu(1, inventory, new SimpleContainer(1), horse, 0);
        return new HorseInventoryScreen(menu, inventory, horse, 0);
    }
}
