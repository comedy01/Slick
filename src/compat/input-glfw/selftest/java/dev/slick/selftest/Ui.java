package dev.slick.selftest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.lang.reflect.Method;

final class Ui {
    private Ui() {
    }

    static void click(Screen screen, double x, double y) {
        MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    static void hover(Minecraft mc, double x, double y) {
        Window window = mc.getWindow();
        double sx = (double) window.getScreenWidth() / window.getGuiScaledWidth();
        double sy = (double) window.getScreenHeight() / window.getGuiScaledHeight();
        try {
            Method move = MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
            move.setAccessible(true);
            move.invoke(mc.mouseHandler, window.handle(), x * sx, y * sy);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
