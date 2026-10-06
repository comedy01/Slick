package dev.slick.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public abstract class SlickOptionsScreen extends OptionsSubScreen {
    private int rows;

    protected SlickOptionsScreen(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    protected abstract void addOptions();

    @Override
    protected void init() {
        rows = 0;
        addOptions();
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height - 27, 200, 20)
                .build());
    }

    protected void addHeader(Component text) {
        int y = height / 6 - 12 + rows * 24;
        addRenderableWidget(new Header(text, width / 2 - 155, y));
        rows++;
    }

    protected void addRow(AbstractWidget left, AbstractWidget right) {
        int y = height / 6 - 12 + rows * 24;
        left.setPosition(width / 2 - 155, y);
        addRenderableWidget(left);
        if (right != null) {
            right.setPosition(width / 2 + 5, y);
            addRenderableWidget(right);
        }
        rows++;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
    }

    private static final class Header extends AbstractWidget {
        Header(Component text, int x, int y) {
            super(x, y, 310, 20, text);
            active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, getMessage(), getX() + getWidth() / 2,
                    getY() + (getHeight() - font.lineHeight) / 2 + 1, 0xFFFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }
    }
}
