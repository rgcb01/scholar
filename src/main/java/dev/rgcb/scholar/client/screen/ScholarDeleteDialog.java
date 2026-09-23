package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.application.ScholarDocumentDescriptor;
import dev.rgcb.scholar.client.ui.ScholarScreenRendering;
import dev.rgcb.scholar.client.ui.ScholarShellRenderer;
import dev.rgcb.scholar.client.ui.ScholarShellStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/** Confirmation only; deletion is performed through ScholarApplication. */
final class ScholarDeleteDialog extends Screen {
    private final ScholarHomeScreen home;
    private final ScholarDocumentDescriptor document;

    ScholarDeleteDialog(ScholarHomeScreen home, ScholarDocumentDescriptor document) {
        super(Component.literal("Delete document?"));
        this.home = home;
        this.document = document;
    }

    @Override protected void init() {
        var x = width / 2 - 102;
        var y = height / 2 + 22;
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(x, y, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Delete").withStyle(ChatFormatting.RED), b -> {
            home.confirmDelete(document.id());
            minecraft.setScreen(home);
        }).bounds(x + 106, y, 98, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF20242A);
        var x = width / 2 - 126;
        var y = height / 2 - 54;
        ScholarShellRenderer.drawRaisedPanel(graphics, x, y, 252, 122, ScholarShellStyle.PANEL);
        graphics.drawCenteredString(font, "Delete document?", width / 2, y + 15, 0xFFFFC0B7);
        graphics.drawCenteredString(font, font.plainSubstrByWidth("\"" + document.displayName() + "\"", 232),
                width / 2, y + 38, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "will be permanently deleted.", width / 2, y + 53, 0xFFFFFFFF);
        ScholarScreenRendering.renderWidgets(renderables, graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { minecraft.setScreen(home); }
    @Override public boolean isPauseScreen() { return false; }
}
