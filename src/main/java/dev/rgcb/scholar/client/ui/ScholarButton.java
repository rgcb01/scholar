package dev.rgcb.scholar.client.ui;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Standard Scholar action button for dialogs and launch surfaces. */
public final class ScholarButton extends Button {
    private long pressedUntil;

    private ScholarButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public static Builder create(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) pressedUntil = System.currentTimeMillis() + 120L;
        return handled;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var state = !active ? ScholarControlState.DISABLED
                : System.currentTimeMillis() < pressedUntil ? ScholarControlState.PRESSED
                : isFocused() ? ScholarControlState.FOCUSED
                : isHoveredOrFocused() ? ScholarControlState.HOVERED
                : ScholarControlState.NORMAL;
        var bounds = new ShellRect(getX(), getY(), getWidth(), getHeight());
        if (state == ScholarControlState.NORMAL) {
            graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(),
                    ScholarShellStyle.PANEL_ELEVATED_BACKGROUND);
            graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    ScholarShellStyle.SUBTLE_SEPARATOR);
        } else {
            ScholarShellRenderer.drawControl(graphics, bounds, state);
        }
        var color = active ? ScholarShellStyle.TEXT_PRIMARY : ScholarShellStyle.TEXT_DISABLED;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
    }

    public static final class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = ScholarShellStyle.CONTROL_HEIGHT;

        private Builder(Component message, OnPress onPress) {
            this.message = Objects.requireNonNull(message, "message");
            this.onPress = Objects.requireNonNull(onPress, "onPress");
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public ScholarButton build() {
            return new ScholarButton(x, y, width, height, message, onPress);
        }
    }
}
