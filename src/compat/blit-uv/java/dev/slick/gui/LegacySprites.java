package dev.slick.gui;

public final class LegacySprites {
    public static final String KEEP = "";

    private static final String GUI = "textures/gui/";
    private static final String CONTAINER = GUI + "container/";

    private LegacySprites() {
    }

    public static String name(String path, int u, int v, int w, int h) {
        return switch (path) {
            case CONTAINER + "creative_inventory/tabs.png" -> creative(u, v, w, h);
            case CONTAINER + "stonecutter.png" -> stonecutter(u, v, w, h);
            case CONTAINER + "loom.png" -> loom(u, v, w, h);
            case CONTAINER + "enchanting_table.png" -> enchanting(v, w, h);
            case CONTAINER + "beacon.png" -> beacon(u, v, w, h);
            case CONTAINER + "villager2.png" -> villager(u, v, w, h);
            case CONTAINER + "anvil.png" -> anvil(u, v, w, h);
            case CONTAINER + "smithing.png" -> error(u, v, w, h, "smithing");
            case CONTAINER + "grindstone.png" -> error(u, v, w, h, "grindstone");
            case CONTAINER + "cartography_table.png" -> cartography(u, v, w, h);
            case CONTAINER + "inventory.png" -> u == 0 && (v == 166 || v == 198) ? KEEP : null;
            case GUI + "recipe_book.png" -> book(u, v, w, h);
            case GUI + "recipe_button.png" -> "recipe_book/button" + (v >= 19 ? "_highlighted" : "");
            case GUI + "widgets.png" -> u == 0 && w == 200 && h == 20 ? button(v) : null;
            default -> null;
        };
    }

    private static String creative(int u, int v, int w, int h) {
        if (w == 12 && h == 15) {
            return scroller("creative_inventory", u >= 244);
        }
        if (w == 26 && h == 32) {
            String side = v >= 64 ? "bottom" : "top";
            String state = v == 32 || v == 96 ? "selected" : "unselected";
            return "container/creative_inventory/tab_" + side + "_" + state + "_" + (u / 26 + 1);
        }
        return null;
    }

    private static String stonecutter(int u, int v, int w, int h) {
        if (w == 12 && h == 15) {
            return scroller("stonecutter", u >= 188);
        }
        if (w == 16 && h == 18) {
            return "container/stonecutter/recipe" + (v == 184 ? "_selected" : v == 202 ? "_highlighted" : "");
        }
        return null;
    }

    private static String loom(int u, int v, int w, int h) {
        if (w == 12 && h == 15) {
            return scroller("loom", u >= 244);
        }
        if (w == 14 && h == 14) {
            return "container/loom/pattern" + (v == 180 ? "_selected" : v == 194 ? "_highlighted" : "");
        }
        return null;
    }

    private static String enchanting(int v, int w, int h) {
        if (w == 108 && h == 19) {
            return "container/enchanting_table/enchantment_slot"
                    + (v == 185 ? "_disabled" : v == 204 ? "_highlighted" : "");
        }
        return w == 16 && h == 16 ? KEEP : null;
    }

    private static String beacon(int u, int v, int w, int h) {
        if (w == 22 && h == 22 && v == 219) {
            return "container/beacon/button" + switch (u) {
                case 22 -> "_selected";
                case 44 -> "_disabled";
                case 66 -> "_highlighted";
                default -> "";
            };
        }
        return w == 18 && h == 18 ? KEEP : null;
    }

    private static String villager(int u, int v, int w, int h) {
        if (u == 311 && v == 0) {
            return "container/villager/out_of_stock";
        }
        if (h == 5 && v == 186) {
            return "container/villager/experience_bar_background";
        }
        if (h == 5 && v == 191) {
            return "container/villager/experience_bar_current";
        }
        if (h == 3 && v == 182) {
            return "container/villager/experience_bar_result";
        }
        if (w == 6 && h == 27 && v == 199) {
            return scroller("villager", u >= 6);
        }
        if (w == 10 && h == 9 && v == 171) {
            return "container/villager/trade_arrow" + (u >= 25 ? "_out_of_stock" : "");
        }
        return u == 0 && v == 176 ? KEEP : null;
    }

    private static String anvil(int u, int v, int w, int h) {
        if (u == 0 && w == 110 && h == 16) {
            return "container/anvil/text_field" + (v >= 182 ? "_disabled" : "");
        }
        return error(u, v, w, h, "anvil");
    }

    private static String error(int u, int v, int w, int h, String family) {
        return w == 28 && h == 21 && v == 0 && u >= 176 ? "container/" + family + "/error" : null;
    }

    private static String cartography(int u, int v, int w, int h) {
        if (w == 28 && h == 21) {
            return "container/cartography_table/error";
        }
        if (u == 176 && v == 66) {
            return "container/cartography_table/duplicated_map";
        }
        if (u == 176 && v == 132) {
            return "container/cartography_table/scaled_map";
        }
        if (u == 176 && v == 0) {
            return "container/cartography_table/map";
        }
        return u == 0 && v == 166 ? KEEP : null;
    }

    private static String book(int u, int v, int w, int h) {
        if (u == 1 && v == 1) {
            return null;
        }
        if (w == 35 && h == 27) {
            return "recipe_book/tab" + (u >= 188 ? "_selected" : "");
        }
        if (w == 25 && h == 25) {
            return "recipe_book/slot_" + (v >= 231 ? "many_" : "") + (u >= 54 ? "uncraftable" : "craftable");
        }
        if (w == 26 && h == 16) {
            boolean furnace = v >= 182;
            boolean hover = furnace ? v >= 200 : v >= 59;
            return "recipe_book/" + (furnace ? "furnace_" : "") + "filter_" + (u >= 180 ? "enabled" : "disabled")
                    + (hover ? "_highlighted" : "");
        }
        if ((u == 152 || u == 178) && v >= 78 && v < 182) {
            boolean furnace = v >= 130;
            boolean hover = furnace ? v >= 156 : v >= 104;
            return "recipe_book/" + (furnace ? "furnace" : "crafting") + "_overlay" + (u == 178 ? "_disabled" : "")
                    + (hover ? "_highlighted" : "");
        }
        if (u == 82 && v == 208) {
            return "recipe_book/overlay_recipe";
        }
        return KEEP;
    }

    private static String button(int v) {
        return v == 46 ? "widget/button_disabled" : v == 86 ? "widget/button_highlighted" : "widget/button";
    }

    private static String scroller(String family, boolean disabled) {
        return "container/" + family + "/scroller" + (disabled ? "_disabled" : "");
    }
}
