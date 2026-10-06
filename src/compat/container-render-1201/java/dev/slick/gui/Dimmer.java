package dev.slick.gui;

public final class Dimmer {
    private static boolean dimmed;

    private Dimmer() {
    }

    public static void reset() {
        dimmed = false;
    }

    public static boolean pending() {
        return !dimmed;
    }

    public static boolean claim() {
        if (dimmed) {
            return false;
        }
        dimmed = true;
        return true;
    }
}
