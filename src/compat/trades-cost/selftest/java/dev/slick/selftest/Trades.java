package dev.slick.selftest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Optional;

final class Trades {
    private Trades() {
    }

    static MerchantOffers offers() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD, 6), 12, 2, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.WHEAT, 20), new ItemStack(Items.EMERALD), 16, 2, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 4), Optional.of(new ItemCost(Items.BOOK)),
                new ItemStack(Items.ENCHANTED_BOOK), 12, 5, 0.2F));
        offers.add(new MerchantOffer(new ItemCost(Items.PUMPKIN, 6), new ItemStack(Items.EMERALD), 12, 10, 0.05F));
        return offers;
    }
}
