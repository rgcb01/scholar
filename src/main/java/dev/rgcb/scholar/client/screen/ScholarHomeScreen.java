package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.client.ui.ScholarButton;
import dev.rgcb.scholar.integration.ScholarApiRuntime;
import dev.rgcb.scholar.application.ScholarApplication;
import dev.rgcb.scholar.application.DocumentTemplateCatalog;
import dev.rgcb.scholar.application.DocumentTemplateDescriptor;
import dev.rgcb.scholar.application.DocumentTemplateKey;
import dev.rgcb.scholar.application.ScholarDocumentDescriptor;
import dev.rgcb.scholar.application.ScholarDocumentId;
import dev.rgcb.scholar.application.RecoveryCandidate;
import dev.rgcb.scholar.client.ui.ScholarShellRenderer;
import dev.rgcb.scholar.client.ui.ScholarShellStyle;
import dev.rgcb.scholar.client.ui.ScholarControlState;
import dev.rgcb.scholar.client.ui.ScholarScreenRendering;
import dev.rgcb.scholar.client.ui.HomeDocumentCardLayout;
import dev.rgcb.scholar.client.ui.ScholarIcons;
import dev.rgcb.scholar.client.ui.ScholarText;
import dev.rgcb.scholar.client.ui.ShellRect;
import dev.rgcb.scholar.client.ui.ContextMenuLayout;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Production entry point for the Scholar document library. */
public final class ScholarHomeScreen extends Screen {
    private static final List<DocumentTemplateDescriptor> TEMPLATES = DocumentTemplateCatalog.templates();
    private final ScholarApplication application;
    private List<ScholarDocumentDescriptor> documents = List.of();
    private List<RecoveryCandidate> recoveries = List.of();
    private int recoveryIndex;
    private String message = "";
    private int page;
    private boolean choosingTemplate;
    private ScholarDocumentId contextDocument;
    private ShellRect contextBounds;

    private ScholarHomeScreen(ScholarApplication application) {
        super(Component.literal("Scholar"));
        this.application = application;
    }

    public static ScholarHomeScreen create() {
        return new ScholarHomeScreen(ScholarApiRuntime.application());
    }

    static ScholarHomeScreen forApplication(ScholarApplication application) {
        return new ScholarHomeScreen(application);
    }

    @Override protected void init() {
        var listed = application.documents();
        if (listed instanceof PersistenceResult.Success<List<ScholarDocumentDescriptor>> success) documents = success.value();
        else message = listed.diagnostics().getFirst().message();
        var discovered = application.recoveryCandidates();
        if (discovered instanceof PersistenceResult.Success<List<RecoveryCandidate>> success) {
            recoveries = success.value();
            recoveryIndex = Math.min(recoveryIndex, Math.max(0, recoveries.size() - 1));
            if (!success.diagnostics().isEmpty()) message = ScholarText.get("scholar.recovery.warning");
        } else message = ScholarText.get("scholar.recovery.error");
        if (!application.recoveryDiagnostics().isEmpty()) message = ScholarText.get("scholar.recovery.error");

        if (!recoveries.isEmpty()) {
            var actionY = 54;
            addRenderableWidget(ScholarButton.create(ScholarText.component("scholar.recovery.recover"), b -> recoverSelected())
                    .bounds(width - 154, actionY, 68, 20).build());
            addRenderableWidget(ScholarButton.create(ScholarText.component("scholar.recovery.discard"), b -> discardSelected())
                    .bounds(width - 80, actionY, 68, 20).build());
            if (recoveries.size() > 1) {
                addRenderableWidget(ScholarButton.create(Component.literal("<"), b -> {
                    recoveryIndex = Math.floorMod(recoveryIndex - 1, recoveries.size());
                }).bounds(width - 202, actionY, 20, 20).build());
                addRenderableWidget(ScholarButton.create(Component.literal(">"), b -> {
                    recoveryIndex = (recoveryIndex + 1) % recoveries.size();
                }).bounds(width - 178, actionY, 20, 20).build());
            }
        }

        var layout = cardLayout();
        page = layout.page();
        if (layout.maxPage() > 0) {
            var y = height - 28;
            var previous = addRenderableWidget(ScholarButton.create(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                    .bounds(width / 2 - 32, y, 28, 20).build());
            previous.active = page > 0;
            var next = addRenderableWidget(ScholarButton.create(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                    .bounds(width / 2 + 4, y, 28, 20).build());
            next.active = page < layout.maxPage();
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScholarShellRenderer.drawScreenBackground(graphics, width, height);
        ScholarShellRenderer.drawRaisedPanel(graphics, 0, 0, width, 32, ScholarShellStyle.HEADER_BACKGROUND);
        ScholarIcons.SCHOLAR.render(graphics, 15, 11, 1, ScholarShellStyle.SCIENTIFIC_ACCENT);
        graphics.drawString(font, "Scholar", 28, 11, ScholarShellStyle.TEXT_PRIMARY, false);
        graphics.drawCenteredString(font, ScholarText.get("scholar.home.library"), width / 2, 11, ScholarShellStyle.TEXT_SECONDARY);
        if (choosingTemplate) {
            renderTemplateChooser(graphics, mouseX, mouseY);
            ScholarScreenRendering.renderWidgets(renderables, graphics, mouseX, mouseY, partialTick);
            return;
        }
        if (!recoveries.isEmpty()) renderRecovery(graphics);
        var layout = cardLayout();
        renderNewCard(graphics, layout.newDocumentCard(), mouseX, mouseY);
        var first = layout.page() * layout.documentsPerPage();
        for (var visible = 0; visible < layout.documentCards().size(); visible++) {
            renderDocumentCard(graphics, layout.documentCards().get(visible), documents.get(first + visible), mouseX, mouseY);
        }
        ScholarScreenRendering.renderWidgets(renderables, graphics, mouseX, mouseY, partialTick);
        if (contextBounds != null) {
            ScholarShellRenderer.drawPopupPanel(graphics, contextBounds);
            var actions = new String[] {"scholar.action.file_open", "scholar.action.file_rename", "scholar.action.delete"};
            for (var row = 0; row < actions.length; row++) {
                var rowY = contextBounds.y() + row * ContextMenuLayout.ROW_HEIGHT;
                if (contextBounds.contains(mouseX, mouseY)
                        && mouseY >= rowY && mouseY < rowY + ContextMenuLayout.ROW_HEIGHT) {
                    ScholarShellRenderer.drawMenuRow(graphics, new ShellRect(contextBounds.x() + 4, rowY + 1,
                            contextBounds.width() - 8, ContextMenuLayout.ROW_HEIGHT - 2), true);
                }
                graphics.drawString(font, ScholarText.get(actions[row]), contextBounds.x() + 12, rowY + 5,
                        row == 2 ? ScholarShellStyle.TEXT_DANGER : ScholarShellStyle.TEXT_PRIMARY, false);
            }
        }
        if (!message.isEmpty()) graphics.drawCenteredString(font, message, width / 2, height - 14, 0xFFFFB86B);
    }

    @Override public boolean isPauseScreen() { return false; }

    private String clipped(String value, int width) { return font.plainSubstrByWidth(value, Math.max(30, width)); }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!choosingTemplate && button == 1) {
            var layout = cardLayout();
            var index = layout.documentIndexAt(mouseX, mouseY);
            if (index >= 0) {
                contextDocument = documents.get(index).id();
                contextBounds = ContextMenuLayout.compute((int) mouseX, (int) mouseY, width, height, 3);
                return true;
            }
            contextDocument = null;
            contextBounds = null;
            return true;
        }
        if (button == 0) {
            if (contextBounds != null) {
                var selected = contextDocument;
                var inside = contextBounds.contains(mouseX, mouseY);
                var row = ((int) mouseY - contextBounds.y()) / ContextMenuLayout.ROW_HEIGHT;
                contextDocument = null;
                contextBounds = null;
                if (inside) {
                    var descriptor = documents.stream().filter(item -> item.id().equals(selected)).findFirst();
                    descriptor.ifPresent(item -> {
                        if (row == 0) open(item);
                        else if (row == 1) minecraft.setScreen(new ScholarHomeRenameDialog(this, application, item));
                        else if (row == 2) minecraft.setScreen(new ScholarDeleteDialog(this, item));
                    });
                }
                return true;
            }
            if (choosingTemplate) {
                for (var index = 0; index < TEMPLATES.size(); index++) {
                    if (templateCard(index).contains(mouseX, mouseY)) {
                        createDocument(TEMPLATES.get(index).id());
                        return true;
                    }
                }
                choosingTemplate = false;
                return true;
            }
            var layout = cardLayout();
            if (layout.newDocumentCard().contains(mouseX, mouseY)) {
                choosingTemplate = true;
                return true;
            }
            var first = layout.page() * layout.documentsPerPage();
            for (var index = 0; index < layout.documentCards().size(); index++) {
                if (layout.documentCards().get(index).contains(mouseX, mouseY)) {
                    open(documents.get(first + index));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (contextBounds != null) {
                contextBounds = null;
                contextDocument = null;
                return true;
            }
            if (choosingTemplate) {
                choosingTemplate = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private HomeDocumentCardLayout.Result cardLayout() {
        return HomeDocumentCardLayout.compute(width, height, documents.size(), page, recoveries.isEmpty() ? 48 : 98);
    }

    private void renderRecovery(GuiGraphics graphics) {
        var candidate = recoveries.get(recoveryIndex);
        var bounds = new ShellRect(8, 38, width - 16, 48);
        ScholarShellRenderer.drawPanel(graphics, bounds, ScholarShellStyle.PANEL_ELEVATED_BACKGROUND, true);
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + 2, bounds.bottom(), ScholarShellStyle.SCIENTIFIC_ACCENT);
        var count = recoveries.size() > 1 ? " " + (recoveryIndex + 1) + "/" + recoveries.size() : "";
        graphics.drawString(font, clipped(ScholarText.get("scholar.recovery.title") + count, Math.max(40, width - 230)),
                18, 47, ScholarShellStyle.TEXT_PRIMARY, false);
        var sourceKey = candidate.sourceState() == RecoveryCandidate.SourceState.UNTITLED
                ? "scholar.recovery.source_unsaved" : candidate.sourceState() == RecoveryCandidate.SourceState.EXISTING_DOCUMENT
                ? "scholar.recovery.source_saved" : "scholar.recovery.source_missing";
        var captured = DateTimeFormatter.ofPattern(ScholarText.get("scholar.home.modified_pattern")).format(
                Instant.ofEpochMilli(candidate.capturedAtEpochMillis()).atZone(ZoneId.systemDefault()));
        var detail = candidate.displayName() + " - " + ScholarText.get(sourceKey) + " - " + captured;
        graphics.drawString(font, clipped(detail, Math.max(40, width - 230)), 18, 66,
                ScholarShellStyle.TEXT_SECONDARY, false);
    }

    private void recoverSelected() {
        var result = application.recover(recoveries.get(recoveryIndex).recoveryId());
        if (result instanceof PersistenceResult.Success<dev.rgcb.scholar.application.ApplicationDocumentWorkspace> success) {
            minecraft.setScreen(ScholarEditorScreen.forApplication(application, success.value()));
        } else message = result.diagnostics().getFirst().message();
    }

    private void discardSelected() {
        var result = application.discardRecovery(recoveries.get(recoveryIndex).recoveryId());
        if (result instanceof PersistenceResult.Failure<Boolean> failure) {
            message = failure.diagnostics().getFirst().message();
        } else {
            recoveries = recoveries.stream().filter(candidate -> !candidate.recoveryId().equals(
                    recoveries.get(recoveryIndex).recoveryId())).toList();
            recoveryIndex = Math.min(recoveryIndex, Math.max(0, recoveries.size() - 1));
            rebuildWidgets();
        }
    }

    private void renderNewCard(GuiGraphics graphics, ShellRect card, int mouseX, int mouseY) {
        renderCardSurface(graphics, card, card.contains(mouseX, mouseY));
        var pageX = card.x() + (card.width() - 42) / 2;
        var pageY = card.y() + 16;
        ScholarShellRenderer.drawRaisedPanel(graphics, pageX, pageY, 42, 55, 0xFFE8E5DA);
        ScholarIcons.NEW_DOCUMENT.render(graphics, pageX + 13, pageY + 19, 2, 0xFF24313A);
        graphics.drawCenteredString(font, ScholarText.get("scholar.home.new_document"), card.x() + card.width() / 2, card.bottom() - 34, 0xFFFFFFFF);
        graphics.drawCenteredString(font, ScholarText.get("scholar.home.new_document.description"), card.x() + card.width() / 2,
                card.bottom() - 20, 0xFFABB4C0);
    }

    private void renderDocumentCard(GuiGraphics graphics, ShellRect card, ScholarDocumentDescriptor descriptor,
                                    int mouseX, int mouseY) {
        renderCardSurface(graphics, card, card.contains(mouseX, mouseY));
        var page = new ShellRect(card.x() + 15, card.y() + 9, card.width() - 30, 72);
        graphics.fill(page.x(), page.y(), page.right(), page.bottom(), 0xFFEEECE3);
        graphics.renderOutline(page.x(), page.y(), page.width(), page.height(), 0xFF77766F);
        var preview = descriptor.preview();
        var titleWidth = Math.max(22, Math.min(page.width() - 18, 28 + preview.title().length() * 2));
        graphics.fill(page.x() + 9, page.y() + 9, page.x() + 9 + titleWidth, page.y() + 12, 0xFF394650);
        var lineCount = Math.max(2, Math.min(5, preview.blockCount() + 2));
        for (var line = 0; line < lineCount; line++) {
            var lineWidth = page.width() - 22 - (line % 3) * 10;
            graphics.fill(page.x() + 9, page.y() + 19 + line * 8, page.x() + 9 + lineWidth,
                    page.y() + 21 + line * 8, 0xFF889097);
        }
        if (preview.blockCount() > 2) {
            graphics.renderOutline(page.right() - 28, page.bottom() - 20, 18, 11, 0xFF56646E);
        }
        graphics.drawString(font, clipped(descriptor.displayName(), card.width() - 16), card.x() + 8,
                card.bottom() - 35, 0xFFFFFFFF, false);
        var modified = descriptor.modifiedAtEpochMillis() == 0 ? ScholarText.get("scholar.home.recovered_document")
                : DateTimeFormatter.ofPattern(ScholarText.get("scholar.home.modified_pattern")).format(
                Instant.ofEpochMilli(descriptor.modifiedAtEpochMillis()).atZone(ZoneId.systemDefault()));
        graphics.drawString(font, clipped(modified, card.width() - 16), card.x() + 8,
                card.bottom() - 20, 0xFFABB4C0, false);
    }

    private static void renderCardSurface(GuiGraphics graphics, ShellRect card, boolean hovered) {
        ScholarShellRenderer.drawPanel(graphics, card, ScholarShellStyle.PANEL_ELEVATED_BACKGROUND, true);
        if (hovered) {
            ScholarShellRenderer.drawControl(graphics, card, ScholarControlState.HOVERED);
            graphics.fill(card.x(), card.y() + 3, card.x() + 2, card.bottom() - 3,
                    ScholarShellStyle.ACTIVE_INDICATOR);
        }
    }

    private void createDocument(DocumentTemplateKey template) {
        var result = application.createDocument(template);
        if (result instanceof PersistenceResult.Success<dev.rgcb.scholar.application.ApplicationDocumentWorkspace> success) {
            minecraft.setScreen(ScholarEditorScreen.forApplication(application, success.value()));
        } else message = result.diagnostics().getFirst().message();
    }

    void confirmDelete(ScholarDocumentId id) {
        var result = application.deleteDocument(id);
        if (result instanceof PersistenceResult.Failure<Boolean> failure) {
            message = failure.diagnostics().getFirst().message();
        } else {
            message = result.diagnostics().isEmpty() ? "" : result.diagnostics().getFirst().message();
        }
        var listed = application.documents();
        if (listed instanceof PersistenceResult.Success<List<ScholarDocumentDescriptor>> success) {
            documents = success.value();
            page = cardLayout().page();
        } else message = listed.diagnostics().getFirst().message();
    }

    private ShellRect templateCard(int index) {
        var cardWidth = Math.min(300, width - 24);
        var wideCardWidth = 184;
        var gap = 12;
        var totalWidth = TEMPLATES.size() * wideCardWidth + Math.max(0, TEMPLATES.size() - 1) * gap;
        if (width < 610 || totalWidth > width - 24) {
            return new ShellRect((width - cardWidth) / 2, Math.max(55, height / 2 - 42) + index * 34,
                    cardWidth, 28);
        }
        return new ShellRect((width - totalWidth) / 2 + index * (wideCardWidth + gap),
                Math.max(52, height / 2 - 72), wideCardWidth, 144);
    }

    private void renderTemplateChooser(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(0, 32, width, height, ScholarShellStyle.MODAL_OVERLAY);
        graphics.drawCenteredString(font, ScholarText.get("scholar.template.choose"), width / 2, Math.max(38, height / 2 - 98), 0xFFFFFFFF);
        for (var index = 0; index < TEMPLATES.size(); index++) {
            var template = TEMPLATES.get(index);
            renderTemplateCard(graphics, templateCard(index), ScholarText.get(template.titleKey()),
                    ScholarText.get(template.descriptionKey()), template.preview().twoColumns(), mouseX, mouseY);
        }
    }

    private void renderTemplateCard(GuiGraphics graphics, ShellRect card, String title, String subtitle,
                                    boolean twoColumns, int mouseX, int mouseY) {
        renderCardSurface(graphics, card, card.contains(mouseX, mouseY));
        if (width < 610) {
            graphics.drawCenteredString(font, clipped(title, card.width() - 12),
                    card.x() + card.width() / 2, card.y() + 10, 0xFFFFFFFF);
            return;
        }
        var preview = new ShellRect(card.x() + 48, card.y() + 12, 88, 82);
        graphics.fill(preview.x(), preview.y(), preview.right(), preview.bottom(), 0xFFF6F2E8);
        graphics.renderOutline(preview.x(), preview.y(), preview.width(), preview.height(), 0xFF8A806F);
        var center = preview.x() + preview.width() / 2;
        if (twoColumns) graphics.fill(center, preview.y() + 20, center + 1, preview.bottom() - 8, 0xFFCBC4B5);
        for (var row = 0; row < 5; row++) {
            if (twoColumns) {
                graphics.fill(preview.x() + 7, preview.y() + 24 + row * 9, center - 5, preview.y() + 26 + row * 9, 0xFF73706A);
                graphics.fill(center + 5, preview.y() + 24 + row * 9, preview.right() - 7, preview.y() + 26 + row * 9, 0xFF73706A);
            } else {
                graphics.fill(preview.x() + 9, preview.y() + 19 + row * 10, preview.right() - 9, preview.y() + 21 + row * 10, 0xFF73706A);
            }
        }
        graphics.drawCenteredString(font, clipped(title, card.width() - 12), card.x() + card.width() / 2,
                card.bottom() - 37, 0xFFFFFFFF);
        graphics.drawCenteredString(font, clipped(subtitle, card.width() - 12), card.x() + card.width() / 2, card.bottom() - 21, 0xFFABB4C0);
    }

    private void open(ScholarDocumentDescriptor descriptor) {
        var result = application.openActiveDocument(descriptor.id());
        if (result instanceof PersistenceResult.Success<dev.rgcb.scholar.application.ApplicationDocumentWorkspace> success) {
            minecraft.setScreen(ScholarEditorScreen.forApplication(application, success.value()));
        } else message = result.diagnostics().getFirst().message();
    }
}
