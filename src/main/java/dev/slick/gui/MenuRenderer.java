package dev.slick.gui;

import dev.slick.client.SlickClient;
import dev.slick.config.SlickConfig;
import dev.slick.core.Colors;
import dev.slick.core.Motion;
import dev.slick.core.Palette;
import dev.slick.core.Speed;
import dev.slick.core.Style;
import dev.slick.mixin.ContainerScreenAccessor;
import dev.slick.mixin.CreativeScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.CreativeModeTab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MenuRenderer {
    private static final Set<Class<?>> STYLED = styledScreens();
    private static final String SPRITES = "container/";
    private static final int LABEL = 0xFF404040;
    private static final int LABEL_LIGHT = 0xFFE0E0E0;
    private static final int RUNES = 0xFF685E4A;
    private static final int RUNES_OFF = 0xFF342F25;
    private static final int COST_OFF = 0xFF407F10;
    private static final int ERROR = 0xFFFF5C5C;
    private static final int FLAME = 0xFFFF9A3C;
    private static final int FUEL = 0xFFE8B84A;
    private static final int XP = 0xFF7BD35A;

    private static final Motion CLOCK = new Motion();
    private static final Map<Slot, Float> GLOW = new IdentityHashMap<>();

    private static Screen current;
    private static Screen framed;
    private static long openedAt;
    private static boolean pushed;
    private static boolean blurred;
    private static long panels;
    private static long skipped;
    private static long replaced;
    private static long glows;
    private static long labels;

    private MenuRenderer() {
    }

    private static Set<Class<?>> styledScreens() {
        Set<Class<?>> screens = new HashSet<>(List.of(
                InventoryScreen.class, CraftingScreen.class, ContainerScreen.class, ShulkerBoxScreen.class,
                HopperScreen.class, DispenserScreen.class, FurnaceScreen.class, SmokerScreen.class,
                BlastFurnaceScreen.class, BrewingStandScreen.class, CreativeModeInventoryScreen.class,
                AnvilScreen.class, SmithingScreen.class, GrindstoneScreen.class, StonecutterScreen.class,
                CartographyTableScreen.class, EnchantmentScreen.class, LoomScreen.class, MerchantScreen.class,
                BeaconScreen.class));
        screens.addAll(Mounts.SCREENS);
        screens.addAll(Crafters.SCREENS);
        return Set.copyOf(screens);
    }

    public static boolean styled(Object screen) {
        return screen != null && SlickClient.config().menus() && STYLED.contains(screen.getClass());
    }

    public static long panels() {
        return panels;
    }

    public static long skipped() {
        return skipped;
    }

    public static long replaced() {
        return replaced;
    }

    public static long glows() {
        return glows;
    }

    public static long labels() {
        return labels;
    }

    public static boolean framing() {
        return framed != null;
    }

    public static void beginFrame(Screen screen) {
        framed = styled(screen) ? screen : null;
    }

    public static void background(Screen screen, Canvas canvas) {
        if (screen == null || screen != framed) {
            return;
        }
        SlickConfig config = SlickClient.config();
        if (screen != current) {
            current = screen;
            openedAt = System.nanoTime();
            GLOW.clear();
        }
        if (config.blur() && !blurred) {
            blurred = true;
            canvas.blur();
        }
        if (!pushed) {
            pushed = true;
            canvas.push();
            opening(canvas, config.speed());
        }
        canvas.batch(() -> panel((AbstractContainerScreen<?>) screen, Skin.of(canvas)));
        panels++;
    }

    public static void endFrame(Canvas canvas) {
        if (pushed) {
            pushed = false;
            canvas.pop();
        }
        framed = null;
        blurred = false;
    }

    public static boolean skipTexture(String namespace, String path, int x, int y, int w, int h, Canvas canvas) {
        if (framed == null || !"minecraft".equals(namespace)) {
            return false;
        }
        if (path.equals("textures/gui/recipe_book.png")) {
            canvas.batch(() -> Skin.of(canvas).panel(x, y, w, h));
            skipped++;
            return true;
        }
        if (!path.startsWith("textures/gui/container/")) {
            return false;
        }
        skipped++;
        return true;
    }

    public static boolean sprite(String namespace, String path, int x, int y, int w, int h, Canvas canvas) {
        if (framed == null || !"minecraft".equals(namespace)) {
            return false;
        }
        boolean[] drawn = new boolean[1];
        canvas.batch(() -> drawn[0] = replace(path, x, y, w, h, canvas));
        return drawn[0];
    }

    private static boolean replace(String path, int x, int y, int w, int h, Canvas canvas) {
        if (path.startsWith("widget/button")) {
            Skin.of(canvas).button(x, y, w, h, state(path, "widget/button"));
            replaced++;
            return true;
        }
        if (path.startsWith("widget/text_field")) {
            Skin skin = Skin.of(canvas);
            skin.well(x, y, w, h);
            if (path.endsWith("highlighted")) {
                skin.paint().ring(x, y, w, h, 1.0F, 1, skin.palette().accent());
            }
            replaced++;
            return true;
        }
        if (path.startsWith("recipe_book/")) {
            if (!book(path.substring("recipe_book/".length()), x, y, w, h, Skin.of(canvas))) {
                return false;
            }
            replaced++;
            return true;
        }
        if (!path.startsWith(SPRITES) || path.startsWith("container/slot/")) {
            return false;
        }
        String name = path.substring(SPRITES.length());
        int cut = name.indexOf('/');
        String family = cut < 0 ? name : name.substring(0, cut);
        String part = cut < 0 ? "" : name.substring(cut + 1);
        if (!draw(family, part, x, y, w, h, canvas)) {
            return false;
        }
        replaced++;
        return true;
    }

    private static boolean draw(String family, String part, float x, float y, float w, float h, Canvas canvas) {
        if (family.equals("slot") && part.isEmpty()) {
            return true;
        }
        Skin skin = Skin.of(canvas);
        if (part.equals("error") || part.equals("out_of_stock")) {
            skin.error(x, y, w, h, !arrowed(framed));
            return true;
        }
        if (part.equals("scroller") || part.equals("scroller_disabled")) {
            float width = Math.min(w, skin.style() == Style.MINIMAL ? 4.0F : 8.0F);
            skin.thumb(x + (w - width) / 2.0F, y, width, h, part.equals("scroller"));
            return true;
        }
        switch (family) {
            case "creative_inventory" -> {
                if (part.startsWith("tab_")) {
                    skin.tab(x + 1.0F, y + 5.0F, w - 2.0F, 22.0F, part.contains("_selected"));
                    return true;
                }
            }
            case "stonecutter", "loom" -> {
                if (part.startsWith("recipe") || part.startsWith("pattern")) {
                    String base = part.startsWith("recipe") ? "recipe" : "pattern";
                    skin.button(x + 0.5F, y + 0.5F, w - 1.0F, h - 1.0F, state(part, base));
                    return true;
                }
            }
            case "enchanting_table" -> {
                if (part.startsWith("enchantment_slot")) {
                    skin.button(x, y + 0.5F, w, h - 1.0F, state(part, "enchantment_slot"));
                    return true;
                }
            }
            case "beacon" -> {
                if (part.startsWith("button")) {
                    skin.button(x, y, w, h, state(part, "button"));
                    return true;
                }
            }
            case "anvil" -> {
                if (part.startsWith("text_field")) {
                    if (part.endsWith("disabled")) {
                        skin.button(x, y, w, h, Skin.State.DISABLED);
                    } else {
                        skin.well(x, y, w, h);
                    }
                    return true;
                }
            }
            case "villager" -> {
                switch (part) {
                    case "experience_bar_background" -> {
                        skin.track(x, y + 1.0F, w, h - 2.0F);
                        return true;
                    }
                    case "experience_bar_current" -> {
                        skin.fill(x, y + 1.0F, w, h - 2.0F, XP);
                        return true;
                    }
                    case "experience_bar_result" -> {
                        skin.fill(x, y + 1.0F, w, h - 2.0F, Colors.fade(XP, 0.5F));
                        return true;
                    }
                    case "trade_arrow", "trade_arrow_out_of_stock" -> {
                        skin.arrow(x, x + w, y + h / 2.0F, part.equals("trade_arrow") ? skin.palette().muted() : ERROR);
                        return true;
                    }
                    default -> {
                    }
                }
            }
            case "cartography_table" -> {
                if (part.endsWith("map")) {
                    skin.well(x, y, w, h);
                    return true;
                }
            }
            case "crafter" -> {
                if (part.equals("disabled_slot")) {
                    skin.button(x + 0.5F, y + 0.5F, w - 1.0F, h - 1.0F, Skin.State.DISABLED);
                    cross(skin.paint(), x + 5.0F, y + 5.0F, Colors.fade(skin.palette().muted(), 0.6F));
                    return true;
                }
            }
            case "horse", "nautilus", "furnace", "smoker", "blast_furnace", "brewing_stand" -> {
                return !part.isEmpty();
            }
            default -> {
            }
        }
        return false;
    }

    private static boolean book(String part, float x, float y, float w, float h, Skin skin) {
        Painter paint = skin.paint();
        Palette palette = skin.palette();
        if (part.startsWith("slot_")) {
            boolean craftable = !part.contains("uncraftable");
            skin.button(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, craftable ? Skin.State.NORMAL : Skin.State.DISABLED);
            if (!craftable) {
                paint.ring(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, 2.0F, 1, Colors.fade(ERROR, 0.6F));
            }
            if (part.contains("many")) {
                paint.rect(x + w - 6.0F, y + 3.0F, 2.0F, 2.0F, 1.0F, palette.accent());
            }
            return true;
        }
        if (part.equals("tab") || part.equals("tab_selected")) {
            skin.tab(x + 2.0F, y + 1.0F, w - 3.0F, h - 2.0F, part.equals("tab_selected"));
            return true;
        }
        if (part.equals("overlay_recipe")) {
            skin.panel(x, y, w, h);
            return true;
        }
        if (part.startsWith("crafting_overlay") || part.startsWith("furnace_overlay")) {
            Skin.State state = part.contains("disabled") ? Skin.State.DISABLED
                    : part.endsWith("highlighted") ? Skin.State.HOVER : Skin.State.NORMAL;
            skin.button(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, state);
            return true;
        }
        if (part.contains("filter_")) {
            boolean on = part.contains("enabled");
            Skin.State state = on ? Skin.State.SELECTED : part.endsWith("highlighted") ? Skin.State.HOVER : Skin.State.NORMAL;
            skin.button(x, y, w, h, state);
            int color = on ? palette.accent() : palette.muted();
            float cx = x + w / 2.0F;
            float top = y + h / 2.0F - 4.0F;
            for (int k = 0; k < 4; k++) {
                float half = 5.0F - k * 1.25F;
                paint.rect(cx - half, top + k * 2.0F, half * 2.0F, 1.0F, 0.0F, color);
            }
            paint.rect(cx - 0.5F, top + 7.0F, 1.0F, 2.0F, 0.0F, color);
            return true;
        }
        if (part.equals("button") || part.equals("button_highlighted")) {
            skin.button(x, y, w, h, part.endsWith("highlighted") ? Skin.State.HOVER : Skin.State.NORMAL);
            float left = x + w / 2.0F - 5.0F;
            float top = y + h / 2.0F - 4.0F;
            paint.rect(left, top, 10.0F, 8.0F, 1.0F, Colors.fade(palette.accent(), 0.85F));
            paint.rect(left + 4.5F, top, 1.0F, 8.0F, 0.0F, palette.panel());
            for (int k = 0; k < 3; k++) {
                paint.rect(left + 1.5F, top + 2.0F + k * 2.0F, 2.0F, 0.5F, 0.0F, palette.text());
                paint.rect(left + 6.5F, top + 2.0F + k * 2.0F, 2.0F, 0.5F, 0.0F, palette.text());
            }
            return true;
        }
        return false;
    }

    private static void cross(Painter paint, float x, float y, int color) {
        for (int k = 0; k < 8; k++) {
            paint.rect(x + k, y + k, 1.0F, 1.0F, 0.0F, color);
            paint.rect(x + 7 - k, y + k, 1.0F, 1.0F, 0.0F, color);
        }
    }

    private static Skin.State state(String part, String base) {
        String suffix = part.length() > base.length() ? part.substring(base.length()) : "";
        return switch (suffix) {
            case "_highlighted" -> Skin.State.HOVER;
            case "_selected" -> Skin.State.SELECTED;
            case "_disabled" -> Skin.State.DISABLED;
            default -> Skin.State.NORMAL;
        };
    }

    public static int textColor(int color) {
        if (framed == null) {
            return color;
        }
        Palette palette = SlickClient.config().palette();
        int opaque = (color >>> 24) == 0 ? color | 0xFF000000 : color;
        int result = switch (opaque) {
            case LABEL, LABEL_LIGHT -> palette.text();
            case RUNES -> palette.muted();
            case RUNES_OFF -> Colors.fade(palette.muted(), 0.45F);
            case COST_OFF -> 0x9080FF20;
            default -> color;
        };
        if (result != color) {
            labels++;
        }
        return result;
    }

    public static boolean glow(Object screen, Slot hovered, Canvas canvas) {
        if (!styled(screen)) {
            return false;
        }
        SlickConfig config = SlickClient.config();
        Skin skin = Skin.of(canvas);
        float dt = CLOCK.tick(System.nanoTime());
        AbstractContainerMenu menu = ((AbstractContainerScreen<?>) screen).getMenu();
        canvas.batch(() -> glowSlots(screen, menu, hovered, skin, config.speed().rate() * 1.4F, dt));
        return true;
    }

    private static void glowSlots(Object screen, AbstractContainerMenu menu, Slot hovered, Skin skin, float rate, float dt) {
        for (Slot slot : menu.slots) {
            float target = slot == hovered && slot.isActive() && slot.isHighlightable() ? 1.0F : 0.0F;
            // Keyed by identity: creative's inventory tab adds SlotWrappers without addSlot, so every slot.index is 0.
            float value = Motion.approach(GLOW.getOrDefault(slot, 0.0F), target, rate, dt);
            if (value <= 0.0F) {
                GLOW.remove(slot);
                continue;
            }
            GLOW.put(slot, value);
            if (!slot.isActive()) {
                continue;
            }
            float size = result(screen, menu, slot) ? 26.0F : 17.0F;
            float pad = (size - 16.0F) / 2.0F;
            skin.glow(slot.x - pad, slot.y - pad, size, size, value);
            glows++;
        }
    }

    private static void opening(Canvas canvas, Speed speed) {
        if (!speed.animated()) {
            return;
        }
        float t = (System.nanoTime() - openedAt) / (speed.openMillis() * 1.0E6F);
        if (t >= 1.0F) {
            return;
        }
        float eased = Motion.easeOut(t);
        float scale = 0.94F + 0.06F * eased;
        float cx = canvas.width() / 2.0F;
        float cy = canvas.height() / 2.0F;
        canvas.translate(cx, cy + (1.0F - eased) * 8.0F);
        canvas.scale(scale);
        canvas.translate(-cx, -cy);
    }

    private static void panel(AbstractContainerScreen<?> screen, Skin skin) {
        ContainerScreenAccessor frame = (ContainerScreenAccessor) screen;
        Palette palette = skin.palette();
        int x = frame.slick$leftPos();
        int y = frame.slick$topPos();
        int w = frame.slick$imageWidth();
        int h = frame.slick$imageHeight();
        AbstractContainerMenu menu = screen.getMenu();

        skin.panel(x, y, w, h);
        if (skin.style() == Style.CARDS) {
            for (float[] card : cards(screen, menu)) {
                skin.card(x + card[0], y + card[1], card[2], card[3]);
            }
        }
        extras(screen, skin, x, y);

        Inventory inventory = Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory();
        int selected = inventory == null ? -1 : HeldSlot.get(inventory);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            if (result(screen, menu, slot)) {
                skin.result(x + slot.x - 5, y + slot.y - 5, 26, 26);
            } else {
                skin.well(x + slot.x - 0.5F, y + slot.y - 0.5F, 17, 17);
            }
            if (slot.container == inventory && slot.getContainerSlot() == selected) {
                skin.paint().rect(x + slot.x + 4, y + slot.y + 14, 8, 1, 0.5F, palette.accent());
            }
        }

        if (arrowed(screen)) {
            arrow(skin, x, y, screen, menu);
        }
        if (screen instanceof CreativeModeInventoryScreen
                && CreativeScreenAccessor.slick$selectedTab().getType() == CreativeModeTab.Type.INVENTORY) {
            cross(skin.paint(), x + 177, y + 116, Colors.fade(ERROR, 0.8F));
        }
        if (menu instanceof AbstractFurnaceMenu furnace) {
            furnace(skin, x, y, furnace);
        } else if (menu instanceof BrewingStandMenu brewing) {
            brewing(skin, x, y, brewing);
        }
    }

    private static void extras(AbstractContainerScreen<?> screen, Skin skin, int x, int y) {
        if (screen instanceof InventoryScreen) {
            skin.well(x + 26, y + 8, 49, 70);
        } else if (screen instanceof CreativeModeInventoryScreen) {
            CreativeModeTab tab = CreativeScreenAccessor.slick$selectedTab();
            if (tab.getType() == CreativeModeTab.Type.INVENTORY) {
                skin.well(x + 73, y + 6, 32, 43);
            } else {
                if (tab.getType() == CreativeModeTab.Type.SEARCH) {
                    skin.well(x + 80, y + 4, 90, 12);
                }
                if (tab.canScroll()) {
                    scrollTrack(skin, x + 175, y + 18, 112);
                }
            }
        } else if (screen instanceof EnchantmentScreen) {
            skin.well(x + 13, y + 13, 40, 33);
        } else if (screen instanceof SmithingScreen) {
            skin.well(x + 121, y + 20, 40, 60);
        } else if (Mounts.SCREENS.contains(screen.getClass())) {
            skin.well(x + 26, y + 18, 52, 52);
        } else if (screen instanceof StonecutterScreen) {
            skin.well(x + 51, y + 14, 66, 57);
            scrollTrack(skin, x + 119, y + 15, 54);
        } else if (screen instanceof LoomScreen) {
            skin.well(x + 59, y + 12, 58, 58);
            skin.well(x + 140, y + 7, 22, 42);
            scrollTrack(skin, x + 119, y + 13, 56);
        } else if (screen instanceof MerchantScreen) {
            float width = skin.style() == Style.MINIMAL ? 3.0F : 6.0F;
            skin.track(x + 94 + (6 - width) / 2.0F, y + 18, width, 139);
        }
    }

    private static void scrollTrack(Skin skin, float x, float y, float height) {
        float width = skin.style() == Style.MINIMAL ? 4.0F : 8.0F;
        skin.track(x + (12 - width) / 2.0F, y, width, height);
    }

    private static List<float[]> cards(AbstractContainerScreen<?> screen, AbstractContainerMenu menu) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                slots.add(slot);
            }
        }
        int n = slots.size();
        int[] group = new int[n];
        for (int i = 0; i < n; i++) {
            group[i] = i;
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Slot a = slots.get(i);
                Slot b = slots.get(j);
                if (a.container == b.container && Math.abs(a.x - b.x) <= 18 && Math.abs(a.y - b.y) <= 18
                        && !result(screen, menu, a) && !result(screen, menu, b)) {
                    group[find(group, i)] = find(group, j);
                }
            }
        }
        Map<Integer, float[]> boxes = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Slot slot = slots.get(i);
            float pad = result(screen, menu, slot) ? 5.0F : 1.0F;
            float[] box = {slot.x - pad, slot.y - pad, slot.x + 16 + pad, slot.y + 16 + pad};
            boxes.merge(find(group, i), box, (o, v) -> new float[]{
                    Math.min(o[0], v[0]), Math.min(o[1], v[1]), Math.max(o[2], v[2]), Math.max(o[3], v[3])});
        }
        List<float[]> cards = new ArrayList<>();
        for (float[] box : boxes.values()) {
            cards.add(new float[]{box[0] - 3.0F, box[1] - 3.0F, box[2] - box[0] + 6.0F, box[3] - box[1] + 6.0F});
        }
        List<float[]> grown = new ArrayList<>();
        for (float[] card : cards) {
            boolean room = card[1] > 14.0F;
            for (float[] other : cards) {
                if (other != card && other[1] + other[3] > card[1] - 14.0F && other[1] < card[1]
                        && other[0] < card[0] + card[2] && other[0] + other[2] > card[0]) {
                    room = false;
                }
            }
            grown.add(room ? new float[]{card[0], card[1] - 11.0F, card[2], card[3] + 11.0F} : card);
        }
        return grown;
    }

    private static int find(int[] group, int i) {
        while (group[i] != i) {
            group[i] = group[group[i]];
            i = group[i];
        }
        return i;
    }

    private static boolean result(Object screen, AbstractContainerMenu menu, Slot slot) {
        if (screen instanceof InventoryScreen) {
            return false;
        }
        if (slot instanceof ResultSlot || slot instanceof FurnaceResultSlot || slot instanceof MerchantResultSlot
                || Crafters.result(slot)) {
            return true;
        }
        if (menu instanceof ItemCombinerMenu combiner) {
            return slot.index == combiner.getResultSlot();
        }
        if (menu instanceof StonecutterMenu) {
            return slot.index == 1;
        }
        if (menu instanceof LoomMenu loom) {
            return slot == loom.getResultSlot();
        }
        if (menu instanceof CartographyTableMenu || menu instanceof GrindstoneMenu) {
            return slot.index == 2;
        }
        return false;
    }

    private static boolean arrowed(Object screen) {
        return screen instanceof CraftingScreen || screen instanceof InventoryScreen || screen instanceof AnvilScreen
                || screen instanceof SmithingScreen || screen instanceof GrindstoneScreen || screen instanceof MerchantScreen;
    }

    private static void arrow(Skin skin, int x, int y, Object screen, AbstractContainerMenu menu) {
        Slot output = null;
        for (Slot slot : menu.slots) {
            if (result(screen, menu, slot) || (screen instanceof InventoryScreen && slot instanceof ResultSlot)) {
                output = slot;
            }
        }
        if (output == null) {
            return;
        }
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        int right = Integer.MIN_VALUE;
        for (Slot slot : menu.slots) {
            if (slot != output && slot.container != inventory && slot.isActive() && slot.x < output.x
                    && Math.abs(slot.y - output.y) <= 20) {
                right = Math.max(right, slot.x + 17);
            }
        }
        if (right == Integer.MIN_VALUE) {
            return;
        }
        int left = output.x - (result(screen, menu, output) ? 5 : 1);
        skin.arrow(x + right + 6, x + left - 5, y + output.y + 8, skin.palette().muted());
    }

    private static void furnace(Skin skin, int x, int y, AbstractFurnaceMenu menu) {
        skin.track(x + 61, y + 37, 4, 12);
        if (menu.isLit()) {
            float lit = 12.0F * menu.getLitProgress();
            skin.fill(x + 61, y + 49 - lit, 4, lit, FLAME);
        }
        float burn = menu.getBurnProgress();
        skin.track(x + 80, y + 41, 17, 2);
        skin.fill(x + 80, y + 41, 17 * burn, 2, skin.palette().accent());
        int head = burn >= 1.0F ? skin.palette().accent() : skin.palette().muted();
        for (int k = 0; k < 4; k++) {
            skin.paint().rect(x + 98 + k, y + 38 + k, 1, 8 - 2 * k, 0.0F, head);
        }
    }

    private static void brewing(Skin skin, int x, int y, BrewingStandMenu menu) {
        skin.track(x + 60, y + 45, 18, 2);
        skin.fill(x + 60, y + 45, 18 * Brewing.fuel(menu), 2, FUEL);
        skin.track(x + 100, y + 17, 3, 26);
        skin.fill(x + 100, y + 17, 3, 26.0F * Brewing.progress(menu), skin.palette().accent());
    }
}
