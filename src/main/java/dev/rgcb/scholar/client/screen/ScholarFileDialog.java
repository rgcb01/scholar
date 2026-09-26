package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.editor.DocumentWorkspace;
import dev.rgcb.scholar.editor.EditorDocumentWorkspace;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.client.ui.ScholarText;
import dev.rgcb.scholar.client.ui.ScholarScreenRendering;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small host-native file/unsaved-work dialog. It never edits the document AST. */
final class ScholarFileDialog extends Screen {
    private enum Mode { OPEN, SAVE, RENAME, UNSAVED, ERROR }
    private final ScholarEditorScreen parent;
    private final EditorDocumentWorkspace workspace;
    private final Mode mode;
    private final Runnable continuation;
    private List<String> names = List.of();
    private String message = "";
    private String draft;
    private int page;
    private EditBox name;

    private ScholarFileDialog(ScholarEditorScreen parent, EditorDocumentWorkspace workspace, Mode mode, Runnable continuation) {
        super(ScholarText.component(switch (mode) {
            case OPEN -> "scholar.dialog.open_document.title";
            case SAVE -> "scholar.dialog.save_document.title";
            case RENAME -> "scholar.dialog.rename_document.title";
            case UNSAVED -> "scholar.dialog.unsaved.title";
            case ERROR -> "scholar.error.document.title";
        }));
        this.parent = parent;
        this.workspace = workspace;
        this.mode = mode;
        this.continuation = continuation;
        draft = workspace.name().orElse("untitled");
    }

    static Screen open(ScholarEditorScreen parent, DocumentWorkspace workspace) {
        return new ScholarFileDialog(parent, workspace, Mode.OPEN, () -> { });
    }
    static Screen save(ScholarEditorScreen parent, DocumentWorkspace workspace, Runnable continuation) {
        return new ScholarFileDialog(parent, workspace, Mode.SAVE, continuation);
    }
    static Screen save(ScholarEditorScreen parent, EditorDocumentWorkspace workspace, Runnable continuation) {
        return new ScholarFileDialog(parent, workspace, Mode.SAVE, continuation);
    }
    static Screen rename(ScholarEditorScreen parent, EditorDocumentWorkspace workspace) {
        return new ScholarFileDialog(parent, workspace, Mode.RENAME, () -> { });
    }
    static Screen unsaved(ScholarEditorScreen parent, EditorDocumentWorkspace workspace, Runnable continuation) {
        return new ScholarFileDialog(parent, workspace, Mode.UNSAVED, continuation);
    }
    static Screen error(ScholarEditorScreen parent, EditorDocumentWorkspace workspace, PersistenceResult<?> result) {
        var screen = new ScholarFileDialog(parent, workspace, Mode.ERROR, () -> { });
        screen.message = result.diagnostics().getFirst().message();
        return screen;
    }

    @Override protected void init() {
        var w = Math.min(300, width - 20);
        var x = (width - w) / 2;
        if (mode == Mode.OPEN) {
            var legacy = (DocumentWorkspace) workspace;
            var result = legacy.list();
            if (result instanceof PersistenceResult.Success<List<String>> success) {
                names = success.value();
                if (names.isEmpty()) message = ScholarText.get("scholar.document.none_saved");
            } else message = result.diagnostics().getFirst().message();
            var rows = Math.max(1, Math.min(8, (height - 112) / 24));
            page = Math.max(0, Math.min(page, Math.max(0, (names.size() - 1) / rows)));
            for (var i = page * rows; i < Math.min(names.size(), (page + 1) * rows); i++) {
                var selected = names.get(i);
                addRenderableWidget(Button.builder(Component.literal(selected), b -> {
                    var loaded = legacy.open(selected);
                    if (loaded instanceof PersistenceResult.Success<?>) {
                        minecraft.setScreen(ScholarEditorScreen.forWorkspace(legacy));
                    } else message = loaded.diagnostics().getFirst().message();
                }).bounds(x, 38 + (i - page * rows) * 24, w, 20).build());
            }
            var y = height - 56;
            var previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); }).bounds(x, y, 30, 20).build());
            previous.active = page > 0;
            var next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); }).bounds(x + 36, y, 30, 20).build());
            next.active = (page + 1) * rows < names.size();
            addRenderableWidget(Button.builder(ScholarText.component("scholar.dialog.cancel"), b -> onClose()).bounds(x + w - 80, y, 80, 20).build());
        } else if (mode == Mode.SAVE || mode == Mode.RENAME) {
            name = new EditBox(font, x, 52, w, 20, ScholarText.component("scholar.document.name"));
            name.setMaxLength(64);
            name.setValue(draft);
            name.setResponder(value -> draft = value);
            addRenderableWidget(name);
            setInitialFocus(name);
            addRenderableWidget(Button.builder(ScholarText.component(mode == Mode.RENAME
                    ? "scholar.action.file_rename" : "scholar.action.file_save"), b -> saveNamedDocument()).bounds(x, 88, w / 2 - 3, 20).build());
            addRenderableWidget(Button.builder(ScholarText.component("scholar.dialog.cancel"), b -> onClose()).bounds(x + w / 2 + 3, 88, w / 2 - 3, 20).build());
        } else if (mode == Mode.UNSAVED) {
            message = ScholarText.get("scholar.confirm.save_changes");
            addRenderableWidget(Button.builder(ScholarText.component("scholar.action.file_save"), b -> {
                if (workspace.name().isEmpty()) minecraft.setScreen(save(parent, workspace, continuation));
                else {
                    var result = workspace.save();
                    if (result instanceof PersistenceResult.Success<?>) continuation.run();
                    else message = result.diagnostics().getFirst().message();
                }
            }).bounds(x, 78, w / 3 - 4, 20).build());
            addRenderableWidget(Button.builder(ScholarText.component("scholar.dialog.discard"), b -> continuation.run()).bounds(x + w / 3, 78, w / 3 - 4, 20).build());
            addRenderableWidget(Button.builder(ScholarText.component("scholar.dialog.cancel"), b -> onClose()).bounds(x + w * 2 / 3, 78, w / 3, 20).build());
        } else {
            addRenderableWidget(Button.builder(ScholarText.component("scholar.dialog.back"), b -> onClose()).bounds(x, 88, w, 20).build());
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF202020);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        ScholarScreenRendering.renderWidgets(renderables, graphics, mouseX, mouseY, partialTick);
        if (!message.isEmpty()) {
            var display = message.length() > 256 ? message.substring(0, 253) + "..." : message;
            var lines = font.split(Component.literal(display), Math.max(40, width - 24));
            var y = mode == Mode.UNSAVED ? 42 : mode == Mode.ERROR ? 40 : height - 28;
            for (var line : lines.stream().limit(2).toList()) {
                graphics.drawString(font, line, 12, y, 0xFFFFDD88); y += 10;
            }
        }
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private void saveNamedDocument() {
        var selected = name.getValue();
        if (mode == Mode.RENAME) {
            var result = workspace.rename(selected);
            if (result instanceof PersistenceResult.Success<?>) minecraft.setScreen(parent);
            else message = result.diagnostics().getFirst().message();
            return;
        }
        if (workspace instanceof DocumentWorkspace legacy) {
            var listed = legacy.list();
            if (listed instanceof PersistenceResult.Failure<?>) {
                message = listed.diagnostics().getFirst().message();
                return;
            }
            var existing = ((PersistenceResult.Success<List<String>>) listed).value();
            java.util.function.Predicate<String> sameFileName = n -> n.equals(selected)
                    || java.io.File.separatorChar == '\\' && n.equalsIgnoreCase(selected);
            if (existing.stream().anyMatch(sameFileName) && workspace.name().filter(sameFileName).isEmpty()) {
                minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(confirmed -> {
                    minecraft.setScreen(this);
                    if (confirmed) commitSave(selected);
                }, ScholarText.component("scholar.confirm.replace_document"), Component.literal(selected)));
                return;
            }
        }
        commitSave(selected);
    }
    private void commitSave(String selected) {
        var result = workspace.saveAs(selected);
        if (result instanceof PersistenceResult.Success<?>) { minecraft.setScreen(parent); continuation.run(); }
        else message = result.diagnostics().getFirst().message();
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((mode == Mode.SAVE || mode == Mode.RENAME) && getFocused() == name && (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)) {
            saveNamedDocument();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
