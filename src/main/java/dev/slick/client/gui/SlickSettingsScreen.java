package dev.slick.client.gui;

import dev.slick.client.SlickClient;
import dev.slick.config.SlickConfig;
import dev.slick.core.Accent;
import dev.slick.core.Choice;
import dev.slick.core.Corners;
import dev.slick.core.Speed;
import dev.slick.core.Style;
import dev.slick.core.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

public final class SlickSettingsScreen extends SlickOptionsScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public SlickSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("slick.options.title"));
    }

    public static void open(Minecraft mc, Screen parent) {
        ScreenOpener.open(mc, new SlickSettingsScreen(parent, mc.options));
    }

    @Override
    protected void addOptions() {
        SlickConfig config = SlickClient.config();

        addHeader(Component.translatable("slick.options.section.look"));
        addRow(
                cycle("slick.options.style", Style.values(), config::style, config::setStyle),
                cycle("slick.options.theme", Theme.values(), config::theme, config::setTheme));
        addRow(
                cycle("slick.options.accent", Accent.values(), config::accent, config::setAccent),
                new StepSlider("slick.options.opacity", SlickConfig.MIN_OPACITY, SlickConfig.MAX_OPACITY, 5,
                        config.opacity(), value -> (int) value + "%", value -> config.setOpacity((int) value)));
        addRow(
                cycle("slick.options.corners", Corners.values(), config::corners, config::setCorners),
                toggle("slick.options.shadows", config::shadows, config::setShadows));
        addRow(
                cycle("slick.options.speed", Speed.values(), config::speed, config::setSpeed),
                null);

        addHeader(Component.translatable("slick.options.section.hotbar"));
        addRow(
                toggle("slick.options.hotbar", config::hotbar, config::setHotbar),
                toggle("slick.options.hotbar_slots", config::hotbarSlots, config::setHotbarSlots));
        addRow(
                toggle("slick.options.wrap", config::wrap, config::setWrap),
                toggle("slick.options.lift", config::lift, config::setLift));

        addHeader(Component.translatable("slick.options.section.menus"));
        addRow(
                toggle("slick.options.menus", config::menus, config::setMenus),
                toggle("slick.options.blur", config::blur, config::setBlur));

        addRow(keyBindsButton(), resetButton(config));
    }

    @Override
    public void removed() {
        super.removed();
        SlickClient.saveConfig();
    }

    private AbstractWidget keyBindsButton() {
        return Button.builder(Component.translatable("slick.options.keys"),
                        button -> ScreenOpener.open(minecraft, KeyBinds.screen(this, options)))
                .width(WIDTH)
                .tooltip(Tooltip.create(Component.translatable("slick.options.keys.tooltip")))
                .build();
    }

    private AbstractWidget resetButton(SlickConfig config) {
        return Button.builder(Component.translatable("slick.options.reset"), button -> {
                    config.resetToDefaults();
                    ScreenOpener.open(minecraft, new SlickSettingsScreen(lastScreen, options));
                })
                .width(WIDTH)
                .tooltip(Tooltip.create(Component.translatable("slick.options.reset.tooltip")))
                .build();
    }

    private static AbstractWidget toggle(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        return Button.builder(toggleLabel(key, getter.getAsBoolean()), button -> {
                    setter.accept(!getter.getAsBoolean());
                    button.setMessage(toggleLabel(key, getter.getAsBoolean()));
                })
                .width(WIDTH)
                .tooltip(Tooltip.create(Component.translatable(key + ".tooltip")))
                .build();
    }

    private static Component toggleLabel(String key, boolean on) {
        Component state = Component.translatable(on ? "options.on" : "options.off");
        return Component.translatable("options.generic_value", Component.translatable(key), state);
    }

    private static <T extends Enum<T> & Choice> AbstractWidget cycle(
            String key, T[] values, Supplier<T> getter, Consumer<T> setter) {
        return Button.builder(cycleLabel(key, getter.get()), button -> {
                    T next = values[(getter.get().ordinal() + 1) % values.length];
                    setter.accept(next);
                    button.setMessage(cycleLabel(key, getter.get()));
                })
                .width(WIDTH)
                .tooltip(Tooltip.create(Component.translatable(key + ".tooltip")))
                .build();
    }

    private static Component cycleLabel(String key, Choice value) {
        return Component.translatable("options.generic_value", Component.translatable(key),
                Component.translatable(key + "." + value.id()));
    }

    private static final class StepSlider extends AbstractSliderButton {
        private final String captionKey;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<String> format;
        private final DoubleConsumer onChange;

        StepSlider(String captionKey, double min, double max, double step, double initial,
                   DoubleFunction<String> format, DoubleConsumer onChange) {
            super(0, 0, WIDTH, HEIGHT, Component.empty(), 0.0);
            this.captionKey = captionKey;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.onChange = onChange;
            this.value = (snap(initial) - min) / (max - min);
            setTooltip(Tooltip.create(Component.translatable(captionKey + ".tooltip")));
            updateMessage();
        }

        private double snap(double raw) {
            double clamped = Math.max(min, Math.min(max, raw));
            double snapped = min + Math.round((clamped - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        private double current() {
            return snap(min + value * (max - min));
        }

        @Override
        protected void updateMessage() {
            Component shown = Component.literal(format.apply(current()));
            setMessage(Component.translatable("options.generic_value", Component.translatable(captionKey), shown));
        }

        @Override
        protected void applyValue() {
            double snapped = current();
            value = (snapped - min) / (max - min);
            onChange.accept(snapped);
        }
    }
}
