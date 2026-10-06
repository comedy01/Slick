package dev.slick.selftest;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class CrafterScenes {
    private CrafterScenes() {
    }

    static Screen screen(Inventory inventory) {
        CrafterMenu menu = new CrafterMenu(1, inventory);
        menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
        menu.getSlot(4).set(new ItemStack(Items.IRON_INGOT));
        return new CrafterScreen(menu, inventory, Component.translatable("container.crafter"));
    }
}
