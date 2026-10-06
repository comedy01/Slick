package dev.slick.selftest;

import dev.slick.client.SlickClient;
import dev.slick.client.gui.SlickSettingsScreen;
import dev.slick.config.SlickConfig;
import dev.slick.core.Speed;
import dev.slick.core.Theme;
import dev.slick.core.Style;
import dev.slick.mixin.CreativeScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import dev.slick.gui.HeldSlot;
import dev.slick.gui.HotbarRenderer;
import dev.slick.gui.MenuRenderer;
import dev.slick.mixin.ContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public final class SlickSelfTest {
    private static final String WORLD = "slick-selftest";
    private static final int TIMEOUT = 2400;

    private static volatile int frames;

    private final List<Step> steps = new ArrayList<>();
    private final List<String> shots = new ArrayList<>();
    private final Set<String> existingShots = new HashSet<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int delay;
    private int waited;
    private int seenFrames;
    private int patience;

    private long mark;
    private long panelMark;
    private long labelMark;
    private float midway;

    private record Step(int delay, BooleanSupplier ready, Runnable action) {
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 40) {
                started = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
                rememberShots(mc);
                plan(mc);
                delay = steps.get(0).delay();
                log("creating world");
                deleteWorld(mc, WORLD);
                Worlds.create(mc, WORLD, true, 0L);
            }
            return;
        }
        int drawn = frames;
        if (drawn == seenFrames) {
            return;
        }
        seenFrames = drawn;
        if (delay > 0) {
            delay--;
            return;
        }
        Step step = steps.get(index);
        if (!step.ready().getAsBoolean()) {
            if (++waited > TIMEOUT) {
                finish(mc, new AssertionError("timed out at step " + index));
            }
            return;
        }
        waited = 0;
        index++;
        try {
            step.action().run();
        } catch (Throwable e) {
            finish(mc, e);
            return;
        }
        if (index >= steps.size()) {
            finish(mc, null);
        } else {
            delay = steps.get(index).delay();
        }
    }

    public static void frame() {
        frames++;
    }

    private void then(int ticks, Runnable action) {
        steps.add(new Step(ticks, () -> true, action));
    }

    private void when(BooleanSupplier ready, Runnable action) {
        steps.add(new Step(0, ready, action));
    }

    private void within(int ticks, BooleanSupplier ready, Runnable action) {
        then(1, () -> patience = ticks);
        when(() -> ready.getAsBoolean() || --patience < 0, action);
    }

    private void finish(Minecraft mc, Throwable failure) {
        finished = true;
        nameShots(mc);
        if (failure == null) {
            log("ALL CHECKS PASSED");
        } else {
            log("FAILED: " + failure);
            failure.printStackTrace();
        }
        SlickClient.config().resetToDefaults();
        SlickClient.saveConfig();
        mc.stop();
    }

    private void plan(Minecraft mc) {
        SlickConfig config = SlickClient.config();

        when(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null, () -> log("world loaded"));
        then(40, () -> {
            config.resetToDefaults();
            run(mc, "difficulty peaceful");
            run(mc, "gamemode survival @p");
            run(mc, "time set 6000");
            run(mc, "gamerule advance_time false");
            run(mc, "weather clear");
            run(mc, "tp @p 0.5 -60 0.5 0 20");
            run(mc, "item replace entity @p hotbar.0 with minecraft:diamond_sword");
            run(mc, "item replace entity @p hotbar.1 with minecraft:bow");
            run(mc, "item replace entity @p hotbar.2 with minecraft:cobblestone 64");
            run(mc, "item replace entity @p hotbar.3 with minecraft:torch 32");
            run(mc, "item replace entity @p hotbar.4 with minecraft:bread 12");
            run(mc, "item replace entity @p hotbar.5 with minecraft:iron_pickaxe");
            run(mc, "item replace entity @p hotbar.7 with minecraft:water_bucket");
            run(mc, "item replace entity @p weapon.offhand with minecraft:shield");
            run(mc, "item replace entity @p inventory.0 with minecraft:oak_log 32");
            run(mc, "item replace entity @p inventory.4 with minecraft:redstone 48");
            run(mc, "item replace entity @p inventory.10 with minecraft:golden_apple 3");
            run(mc, "item replace entity @p armor.head with minecraft:iron_helmet");
            run(mc, "item replace entity @p armor.chest with minecraft:iron_chestplate");
        });
        within(60, () -> !mc.player.getOffhandItem().isEmpty() && !mc.player.getAbilities().instabuild, () -> {
            check(!mc.player.getAbilities().instabuild, "the player is still in creative");
            HeldSlot.set(mc.player.getInventory(), 0);
            mark = HotbarRenderer.frames();
        });
        then(10, () -> {
            check(HotbarRenderer.frames() > mark, "the modern hotbar was not drawn");
            check(HotbarRenderer.glidePosition() == 0.0F, "the selection is not on slot 1: " + HotbarRenderer.glidePosition());
            screenshot(mc, "hotbar");
            HeldSlot.set(mc.player.getInventory(), 4);
        });
        then(2, () -> {
            midway = HotbarRenderer.glidePosition();
            check(midway > 0.0F && midway < 4.0F, "the selection did not glide, it is at " + midway);
            screenshot(mc, "glide");
        });
        within(60, () -> HotbarRenderer.glidePosition() == 4.0F,
                () -> check(HotbarRenderer.glidePosition() == 4.0F, "the selection never reached slot 5: " + HotbarRenderer.glidePosition()));
        then(2, () -> HeldSlot.set(mc.player.getInventory(), 8));
        within(60, () -> HotbarRenderer.glidePosition() == 8.0F, () -> HeldSlot.set(mc.player.getInventory(), 0));
        then(2, () -> {
            float at = HotbarRenderer.glidePosition();
            check(at > 8.0F || at < 0.5F, "going from slot 9 to slot 1 did not wrap around the edge: " + at);
            screenshot(mc, "wrap");
        });
        within(60, () -> HotbarRenderer.glidePosition() == 0.0F,
                () -> check(HotbarRenderer.glidePosition() == 0.0F, "the wrap did not land on slot 1"));
        then(2, () -> {
            config.setSpeed(Speed.OFF);
            HeldSlot.set(mc.player.getInventory(), 6);
        });
        then(2, () -> {
            check(HotbarRenderer.glidePosition() == 6.0F, "with animations off the selection should snap");
            config.setSpeed(Speed.NORMAL);
            HeldSlot.set(mc.player.getInventory(), 0);
            mark = MenuRenderer.skipped();
            panelMark = MenuRenderer.panels();
            labelMark = MenuRenderer.labels();
            Screens.open(mc, new InventoryScreen(mc.player));
        });
        then(20, () -> {
            check(Screens.current(mc) instanceof InventoryScreen, "the inventory did not open");
            check(MenuRenderer.skipped() > mark, "the vanilla inventory texture was still drawn");
            check(MenuRenderer.panels() > panelMark, "the inventory panel was not drawn");
            check(MenuRenderer.labels() > labelMark, "the inventory labels kept their vanilla color");
            screenshot(mc, "inventory");
            hoverSlot(mc, 36);
            mark = MenuRenderer.glows();
        });
        then(15, () -> {
            check(MenuRenderer.glows() > mark, "hovering a slot did not light it up");
            screenshot(mc, "inventory-hover");
            run(mc, "recipe give @p *");
            run(mc, "item replace entity @p weapon.offhand with shield");
            Ui.hover(mc, 0, 0);
        });
        then(10, () -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) Screens.current(mc);
            Ui.click(screen, ((ContainerScreenAccessor) screen).slick$leftPos() + 114, screen.height / 2 - 13);
        });
        then(20, () -> {
            screenshot(mc, "inventory-recipe-book");
            mc.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, false);
            Inventory inventory = mc.player.getInventory();
            CraftingMenu menu = new CraftingMenu(1, inventory);
            menu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS));
            menu.getSlot(2).set(new ItemStack(Items.OAK_PLANKS));
            menu.getSlot(4).set(new ItemStack(Items.STICK));
            menu.getSlot(0).set(new ItemStack(Items.WOODEN_PICKAXE));
            Screens.open(mc, new CraftingScreen(menu, inventory, Component.translatable("container.crafting")));
        });
        then(20, () -> {
            screenshot(mc, "crafting");
            Screens.open(mc, chest(mc, 6));
        });
        then(20, () -> screenshot(mc, "chest-midnight"));
        for (Theme theme : new Theme[]{Theme.GRAPHITE, Theme.FROST, Theme.DAYLIGHT, Theme.ONYX}) {
            then(2, () -> config.setTheme(theme));
            then(8, () -> screenshot(mc, "chest-" + theme.id()));
        }
        then(2, () -> {
            config.setTheme(Theme.MIDNIGHT);
            Inventory inventory = mc.player.getInventory();
            SimpleContainer hopper = new SimpleContainer(5);
            hopper.setItem(1, new ItemStack(Items.IRON_INGOT, 9));
            hopper.setItem(3, new ItemStack(Items.GOLD_NUGGET, 20));
            Screens.open(mc, new HopperScreen(new HopperMenu(1, inventory, hopper), inventory,
                    Component.translatable("container.hopper")));
        });
        then(20, () -> {
            screenshot(mc, "hopper");
            Inventory inventory = mc.player.getInventory();
            FurnaceMenu menu = new FurnaceMenu(1, inventory);
            menu.getSlot(0).set(new ItemStack(Items.RAW_IRON, 8));
            menu.getSlot(1).set(new ItemStack(Items.COAL, 5));
            Screens.open(mc, new FurnaceScreen(menu, inventory, Component.translatable("container.furnace")));
        });
        then(20, () -> {
            screenshot(mc, "furnace");
            Inventory inventory = mc.player.getInventory();
            Screens.open(mc, new BrewingStandScreen(new BrewingStandMenu(1, inventory), inventory,
                    Component.translatable("container.brewing")));
        });
        then(20, () -> {
            screenshot(mc, "brewing");
            Inventory inventory = mc.player.getInventory();
            AnvilMenu menu = new AnvilMenu(1, inventory);
            menu.getSlot(0).set(new ItemStack(Items.IRON_PICKAXE));
            menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT, 2));
            open(mc, new AnvilScreen(menu, inventory, Component.translatable("container.repair")));
        });
        then(20, () -> {
            check(MenuRenderer.replaced() > mark, "the anvil's text field was not restyled");
            screenshot(mc, "anvil");
            Inventory inventory = mc.player.getInventory();
            EnchantmentMenu menu = new EnchantmentMenu(1, inventory);
            menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
            menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 2));
            menu.costs[0] = 3;
            menu.costs[1] = 12;
            menu.costs[2] = 30;
            open(mc, new EnchantmentScreen(menu, inventory, Component.translatable("container.enchant")));
        });
        then(20, () -> {
            screenshot(mc, "enchanting");
            Inventory inventory = mc.player.getInventory();
            MerchantMenu menu = new MerchantMenu(1, inventory);
            menu.setOffers(Trades.offers());
            open(mc, new MerchantScreen(menu, inventory, Component.literal("Farmer")));
        });
        then(20, () -> {
            screenshot(mc, "villager");
            Inventory inventory = mc.player.getInventory();
            StonecutterMenu menu = new StonecutterMenu(1, inventory);
            menu.getSlot(0).set(new ItemStack(Items.STONE, 16));
            open(mc, new StonecutterScreen(menu, inventory, Component.translatable("container.stonecutter")));
        });
        then(20, () -> {
            screenshot(mc, "stonecutter");
            Inventory inventory = mc.player.getInventory();
            LoomMenu menu = new LoomMenu(1, inventory);
            menu.getSlot(0).set(new ItemStack(Stacks.whiteBanner()));
            menu.getSlot(1).set(new ItemStack(Stacks.redDye(), 4));
            open(mc, new LoomScreen(menu, inventory, Component.translatable("container.loom")));
        });
        then(20, () -> {
            screenshot(mc, "loom");
            Inventory inventory = mc.player.getInventory();
            SmithingMenu menu = new SmithingMenu(1, inventory);
            menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            menu.getSlot(1).set(new ItemStack(Items.DIAMOND_CHESTPLATE));
            open(mc, new SmithingScreen(menu, inventory, Component.translatable("container.upgrade")));
        });
        then(20, () -> {
            screenshot(mc, "smithing");
            Inventory inventory = mc.player.getInventory();
            GrindstoneMenu menu = new GrindstoneMenu(1, inventory);
            menu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
            open(mc, new GrindstoneScreen(menu, inventory, Component.translatable("container.grindstone_title")));
        });
        then(20, () -> {
            screenshot(mc, "grindstone");
            Inventory inventory = mc.player.getInventory();
            open(mc, new CartographyTableScreen(new CartographyTableMenu(1, inventory), inventory,
                    Component.translatable("container.cartography_table")));
        });
        then(20, () -> {
            screenshot(mc, "cartography");
            Inventory inventory = mc.player.getInventory();
            open(mc, new BeaconScreen(new BeaconMenu(1, inventory), inventory, Component.translatable("container.beacon")));
        });
        then(20, () -> {
            screenshot(mc, "beacon");
            open(mc, CrafterScenes.screen(mc.player.getInventory()));
        });
        then(20, () -> {
            screenshot(mc, "crafter");
            Screens.open(mc, null);
            run(mc, "summon minecraft:horse 3 -60 3 {Tame:1b,NoAI:1b}");
        });
        within(60, () -> find(mc, "minecraft:horse") != null, () -> {
            Entity horse = find(mc, "minecraft:horse");
            check(horse != null, "the horse did not show up");
            open(mc, Horses.screen(mc.player.getInventory(), horse));
        });
        then(20, () -> {
            screenshot(mc, "horse");
            mc.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, true);
            open(mc, new CraftingScreen(new CraftingMenu(1, mc.player.getInventory()), mc.player.getInventory(),
                    Component.translatable("container.crafting")));
        });
        then(20, () -> {
            screenshot(mc, "crafting-recipe-book");
            mc.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, false);
            Screens.open(mc, null);
            run(mc, "gamemode creative @p");
        });
        within(60, () -> mc.player.getAbilities().instabuild, () -> Screens.open(mc,
                new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true)));
        then(20, () -> {
            check(Screens.current(mc) instanceof CreativeModeInventoryScreen, "the creative inventory did not open");
            check(MenuRenderer.panels() > 0, "the creative inventory was not restyled");
            screenshot(mc, "creative");
            selectTab(mc, CreativeModeTab.Type.SEARCH);
        });
        then(15, () -> {
            screenshot(mc, "creative-search");
            selectTab(mc, CreativeModeTab.Type.INVENTORY);
        });
        then(15, () -> {
            screenshot(mc, "creative-inventory");
            Screens.open(mc, null);
            run(mc, "gamemode survival @p");
        });
        within(60, () -> !mc.player.getAbilities().instabuild, () -> {
        });
        for (Style style : Style.values()) {
            then(2, () -> {
                config.setStyle(style);
                Screens.open(mc, chest(mc, 3));
            });
            then(20, () -> {
                hoverSlot(mc, 3);
                screenshot(mc, "style-" + style.id() + "-chest");
            });
            then(8, () -> {
                Inventory inventory = mc.player.getInventory();
                FurnaceMenu menu = new FurnaceMenu(1, inventory);
                menu.getSlot(0).set(new ItemStack(Items.RAW_GOLD, 5));
                open(mc, new FurnaceScreen(menu, inventory, Component.translatable("container.furnace")));
            });
            then(15, () -> {
                screenshot(mc, "style-" + style.id() + "-furnace");
                Screens.open(mc, null);
                HeldSlot.set(mc.player.getInventory(), 2);
            });
            then(40, () -> screenshot(mc, "style-" + style.id() + "-hotbar"));
        }
        then(2, () -> {
            config.setStyle(Style.MODERN);
            HeldSlot.set(mc.player.getInventory(), 0);
            config.setMenus(false);
            mark = MenuRenderer.skipped();
            Screens.open(mc, chest(mc, 3));
        });
        then(20, () -> {
            check(MenuRenderer.skipped() == mark, "with Modern Menus off the vanilla chest was still replaced");
            screenshot(mc, "vanilla-chest");
            config.setMenus(true);
            Screens.open(mc, null);
            config.setHotbar(false);
        });
        then(5, () -> mark = HotbarRenderer.frames());
        then(10, () -> {
            check(HotbarRenderer.frames() == mark, "with Modern Hotbar off the modern hotbar was still drawn");
            screenshot(mc, "vanilla-hotbar");
            config.setHotbar(true);
            Screens.open(mc, new SlickSettingsScreen(null, mc.options));
        });
        then(10, () -> {
            Screen screen = Screens.current(mc);
            check(screen instanceof SlickSettingsScreen, "the settings screen did not open");
            screenshot(mc, "settings");
            click(mc, find(screen, "slick.options.theme"));
            check(config.theme() == Theme.GRAPHITE, "the Theme button did not cycle: " + config.theme());
        });
        then(5, () -> Screens.open(mc, null));
        then(10, () -> {
            Path file = SlickClient.configPath();
            check(Files.exists(file), "the config file was not written");
            check(read(file).contains("\"theme\": \"graphite\""), "the new theme was not saved: " + read(file));
        });
    }

    private static AbstractContainerScreen<?> chest(Minecraft mc, int rows) {
        Inventory inventory = mc.player.getInventory();
        SimpleContainer container = new SimpleContainer(rows * 9);
        ItemStack[] loot = {
                new ItemStack(Items.DIAMOND, 7), new ItemStack(Items.EMERALD, 23), new ItemStack(Items.IRON_INGOT, 64),
                new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.GOLDEN_CARROT, 40), new ItemStack(Items.BOOK),
                new ItemStack(Items.REDSTONE, 51), new ItemStack(Items.NAME_TAG), new ItemStack(Items.SADDLE)};
        for (int i = 0; i < loot.length; i++) {
            container.setItem((i * 5) % container.getContainerSize(), loot[i]);
        }
        ChestMenu menu = rows == 6 ? ChestMenu.sixRows(1, inventory, container) : ChestMenu.threeRows(1, inventory, container);
        return new ContainerScreen(menu, inventory, Component.translatable(rows == 6 ? "container.chestDouble" : "container.chest"));
    }

    private void open(Minecraft mc, Screen screen) {
        mark = MenuRenderer.replaced();
        Screens.open(mc, screen);
    }

    private static void selectTab(Minecraft mc, CreativeModeTab.Type type) {
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() == type) {
                ((CreativeScreenAccessor) Screens.current(mc)).slick$selectTab(tab);
                return;
            }
        }
        throw new AssertionError("no creative tab of type " + type);
    }

    private static Entity find(Minecraft mc, String type) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(type)) {
                return entity;
            }
        }
        return null;
    }

    private static void hoverSlot(Minecraft mc, int index) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) Screens.current(mc);
        ContainerScreenAccessor frame = (ContainerScreenAccessor) screen;
        Slot slot = screen.getMenu().getSlot(index);
        Ui.hover(mc, frame.slick$leftPos() + slot.x + 8, frame.slick$topPos() + slot.y + 8);
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private void screenshot(Minecraft mc, String name) {
        log("screenshot: " + name);
        shots.add(name);
        Screenshot.grab(mc.gameDirectory, Screens.renderTarget(mc), message -> {
        });
    }

    private void rememberShots(Minecraft mc) {
        File[] files = new File(mc.gameDirectory, "screenshots").listFiles();
        if (files != null) {
            for (File file : files) {
                existingShots.add(file.getName());
            }
        }
    }

    private void nameShots(Minecraft mc) {
        File dir = new File(mc.gameDirectory, "screenshots");
        File[] files = dir.listFiles((parent, name) -> name.endsWith(".png") && !existingShots.contains(name));
        if (files == null) {
            return;
        }
        List<File> fresh = new ArrayList<>(List.of(files));
        fresh.sort(Comparator.comparingLong(File::lastModified).thenComparing(File::getName));
        for (int i = 0; i < fresh.size() && i < shots.size(); i++) {
            File target = new File(dir, String.format(Locale.ROOT, "sk-%02d-%s.png", i, shots.get(i)));
            if ((!target.exists() || target.delete()) && fresh.get(i).renameTo(target)) {
                log("saved " + target.getName());
            }
        }
    }

    private static void deleteWorld(Minecraft mc, String name) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(name);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void click(Minecraft mc, AbstractWidget widget) {
        Ui.click(Screens.current(mc), widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0);
    }

    private static AbstractWidget find(GuiEventListener node, String key) {
        if (node instanceof AbstractWidget widget && labelled(widget, key)) {
            return widget;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractWidget found = find(child, key);
                if (found != null) {
                    return found;
                }
            }
        }
        if (node instanceof Screen) {
            throw new AssertionError("no widget " + key + " on " + node);
        }
        return null;
    }

    private static boolean labelled(AbstractWidget widget, String key) {
        if (!(widget.getMessage().getContents() instanceof TranslatableContents contents)) {
            return false;
        }
        if (contents.getKey().equals(key)) {
            return true;
        }
        Object[] args = contents.getArgs();
        return args.length > 0
                && args[0] instanceof Component name
                && name.getContents() instanceof TranslatableContents inner
                && inner.getKey().equals(key);
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void log(String message) {
        System.out.println("[slick-selftest] " + message);
    }
}
