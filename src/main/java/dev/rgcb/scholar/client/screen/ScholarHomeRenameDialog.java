package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.application.ScholarApplication;
import dev.rgcb.scholar.application.ScholarDocumentDescriptor;
import dev.rgcb.scholar.client.ui.ScholarScreenRendering;
import dev.rgcb.scholar.persistence.PersistenceResult;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Home rename changes document metadata without opening an editor session. */
final class ScholarHomeRenameDialog extends Screen {
    private final ScholarHomeScreen parent;
    private final ScholarApplication application;
    private final ScholarDocumentDescriptor document;
    private EditBox name;
    private String message = "";

    ScholarHomeRenameDialog(ScholarHomeScreen parent, ScholarApplication application,
                            ScholarDocumentDescriptor document) {
        super(Component.literal("Rename Scholar Document"));
        this.parent = parent;
        this.application = application;
        this.document = document;
    }

    @Override protected void init() {
        var width = Math.min(300, this.width - 20);
        var x = (this.width - width) / 2;
        name = new EditBox(font, x, 52, width, 20, Component.literal("Document name"));
        name.setMaxLength(64);
        name.setValue(document.displayName());
        addRenderableWidget(name);
        setInitialFocus(name);
        addRenderableWidget(Button.builder(Component.literal("Rename"), button -> rename())
                .bounds(x, 88, width / 2 - 3, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(x + width / 2 + 3, 88, width / 2 - 3, 20).build());
    }

    private void rename() {
        var result = application.renameDocument(document.id(), name.getValue());
        if (result instanceof PersistenceResult.Success<?>) minecraft.setScreen(parent);
        else message = result.diagnostics().getFirst().message();
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && getFocused() == name) {
            rename();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF202020);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        ScholarScreenRendering.renderWidgets(renderables, graphics, mouseX, mouseY, partialTick);
        if (!message.isEmpty()) graphics.drawCenteredString(font, message, width / 2, height - 28, 0xFFFFDD88);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
