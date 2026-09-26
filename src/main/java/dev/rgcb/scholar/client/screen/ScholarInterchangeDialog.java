package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.client.ui.ScholarScreenRendering;
import dev.rgcb.scholar.client.ui.ScholarText;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.interchange.CsvDatasetInterchange;
import dev.rgcb.scholar.interchange.InterchangeFiles;
import dev.rgcb.scholar.interchange.ScholarInterchangeService;
import dev.rgcb.scholar.layout.LaidOutDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Minecraft-native file boundary; format logic stays in the interchange layer. */
final class ScholarInterchangeDialog extends Screen {
    private enum Mode { IMPORT_CSV, EXPORT_CSV, EXPORT_MARKDOWN, EXPORT_PDF }
    private final ScholarEditorScreen parent;
    private final EditorSession session;
    private final Mode mode;
    private final Supplier<LaidOutDocument> layout;
    private final ScholarInterchangeService interchange = new ScholarInterchangeService();
    private Path directory;
    private List<Path> csvFiles = List.of();
    private int fileIndex = -1;
    private int datasetIndex;
    private int listPage;
    private int columnPage;
    private CsvDatasetInterchange.Preview preview;
    private EditBox name;
    private String nameDraft;
    private String message = "";
    private boolean busy;
    private long previewRequest;
    private int inputY;
    private int previewTop;

    private int visibleRows() { return Math.max(1, Math.min(6, (height - 290) / 23)); }
    private int visibleColumns() { return height < 300 ? 2 : 4; }

    private ScholarInterchangeDialog(ScholarEditorScreen parent, EditorSession session, Mode mode,
                                     Supplier<LaidOutDocument> layout) {
        super(ScholarText.component(switch (mode) {
            case IMPORT_CSV -> "scholar.import.csv.title";
            case EXPORT_CSV -> "scholar.export.csv.title";
            case EXPORT_MARKDOWN -> "scholar.export.markdown.title";
            case EXPORT_PDF -> "scholar.export.pdf.title";
        }));
        this.parent = parent;
        this.session = session;
        this.mode = mode;
        this.layout = layout;
    }

    static Screen importCsv(ScholarEditorScreen parent, EditorSession session) {
        return new ScholarInterchangeDialog(parent, session, Mode.IMPORT_CSV, () -> null);
    }
    static Screen exportCsv(ScholarEditorScreen parent, EditorSession session) {
        return new ScholarInterchangeDialog(parent, session, Mode.EXPORT_CSV, () -> null);
    }
    static Screen exportMarkdown(ScholarEditorScreen parent, EditorSession session) {
        return new ScholarInterchangeDialog(parent, session, Mode.EXPORT_MARKDOWN, () -> null);
    }
    static Screen exportPdf(ScholarEditorScreen parent, EditorSession session, Supplier<LaidOutDocument> layout) {
        return new ScholarInterchangeDialog(parent, session, Mode.EXPORT_PDF, layout);
    }

    @Override protected void init() {
        try {
            directory = InterchangeFiles.directory(minecraft.gameDirectory.toPath());
            try (var paths = Files.list(directory)) {
                csvFiles = paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".csv"))
                        .sorted().toList();
            }
        } catch (IOException failed) { message = failed.getMessage(); }
        var w = Math.min(430, width - 24);
        var x = (width - w) / 2;
        var rowsPerPage = visibleRows();
        var inventorySize = mode == Mode.IMPORT_CSV ? csvFiles.size()
                : mode == Mode.EXPORT_CSV ? session.current().document().datasets().size() : 0;
        listPage = Math.min(listPage, Math.max(0, (inventorySize - 1) / rowsPerPage));
        if (mode == Mode.IMPORT_CSV) {
            var count = Math.min(rowsPerPage, Math.max(0, csvFiles.size() - listPage * rowsPerPage));
            for (var i = 0; i < count; i++) {
                var index = listPage * rowsPerPage + i;
                var file = csvFiles.get(index);
                addRenderableWidget(Button.builder(Component.literal(file.getFileName().toString()), b -> selectFile(index))
                        .bounds(x, 46 + i * 23, w, 20).build());
            }
            if (csvFiles.isEmpty() && message.isEmpty()) message = ScholarText.get("scholar.import.csv.place_file", directory);
        } else {
            if (mode == Mode.EXPORT_CSV) {
                var datasets = session.current().document().datasets();
                for (var i = 0; i < Math.min(rowsPerPage, Math.max(0, datasets.size() - listPage * rowsPerPage)); i++) {
                    var index = listPage * rowsPerPage + i;
                    var dataset = datasets.get(index);
                    addRenderableWidget(Button.builder(Component.literal(dataset.displayLabel()), b -> selectDataset(index))
                            .bounds(x, 46 + i * 23, w, 20).build());
                }
                if (datasets.isEmpty()) message = ScholarText.get("scholar.export.no_datasets");
            }
        }
        var count = mode == Mode.IMPORT_CSV ? csvFiles.size()
                : mode == Mode.EXPORT_CSV ? session.current().document().datasets().size() : 0;
        var navigationY = 46 + rowsPerPage * 23;
        if (count > rowsPerPage) {
            var previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { listPage--; rebuildWidgets(); })
                    .bounds(x, navigationY, 32, 20).build());
            previous.active = listPage > 0;
            var next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { listPage++; rebuildWidgets(); })
                    .bounds(x + 36, navigationY, 32, 20).build());
            next.active = (listPage + 1) * rowsPerPage < count;
        }
        inputY = mode == Mode.EXPORT_MARKDOWN || mode == Mode.EXPORT_PDF
                ? Math.min(height - 104, 100) : Math.min(height - 104, navigationY + 25);
        previewTop = inputY + 58;
        name = new EditBox(font, x, inputY, w, 20, ScholarText.component(mode == Mode.IMPORT_CSV
                ? "scholar.dataset.name" : "scholar.export.filename"));
        name.setMaxLength(120);
        var defaultName = switch (mode) {
            case IMPORT_CSV -> "Imported dataset";
            case EXPORT_CSV -> "dataset.csv";
            case EXPORT_MARKDOWN -> "document.md";
            case EXPORT_PDF -> "document.pdf";
        };
        name.setValue(nameDraft == null ? defaultName : nameDraft);
        name.setResponder(value -> nameDraft = value);
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(ScholarText.component(mode == Mode.IMPORT_CSV
                ? "scholar.import.action" : "scholar.export.action"), b -> submit())
                .bounds(x, inputY + 28, w / 2 - 3, 20).build());
        addRenderableWidget(Button.builder(ScholarText.component("scholar.dialog.cancel"), b -> onClose())
                .bounds(x + w / 2 + 3, inputY + 28, w / 2 - 3, 20).build());
        if (mode == Mode.IMPORT_CSV && preview != null && preview.columns().size() > visibleColumns()) {
            var previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> changeColumnPage(-1))
                    .bounds(x + w - 72, previewTop - 5, 32, 20).build());
            previous.active = columnPage > 0;
            var next = addRenderableWidget(Button.builder(Component.literal(">"), b -> changeColumnPage(1))
                    .bounds(x + w - 36, previewTop - 5, 32, 20).build());
            next.active = (columnPage + 1) * visibleColumns() < preview.columns().size();
        }
    }

    private void changeColumnPage(int step) {
        columnPage += step;
        rebuildWidgets();
    }

    private void selectFile(int index) {
        var selected = csvFiles.get(index);
        var request = ++previewRequest;
        preview = null;
        fileIndex = -1;
        message = ScholarText.get("scholar.import.reading", selected.getFileName());
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try { return interchange.previewCsv(selected); }
            catch (IOException failed) { throw new java.io.UncheckedIOException(failed); }
        }).whenComplete((result, error) -> minecraft.execute(() -> {
            if (request != previewRequest) return;
            if (error != null) {
                var cause = error.getCause() == null ? error : error.getCause();
                message = ScholarText.get("scholar.error.import_csv", cause.getMessage());
                return;
            }
            preview = result;
            fileIndex = index;
            columnPage = 0;
            rebuildWidgets();
            name.setValue(selected.getFileName().toString().replaceFirst("(?i)\\.csv$", ""));
            message = result.warnings().isEmpty() ? "" : result.warnings().getFirst();
        }));
    }

    private void selectDataset(int index) {
        datasetIndex = index;
        name.setValue(session.current().document().datasets().get(index).displayLabel() + ".csv");
    }

    private void submit() {
        if (busy) return;
        if (directory == null) {
            message = ScholarText.get("scholar.error.interchange_directory");
            return;
        }
        if (mode == Mode.IMPORT_CSV) {
            if (preview == null || fileIndex < 0) { message = ScholarText.get("scholar.error.select_csv"); return; }
            try {
                if (!interchange.importCsv(session, preview, name.getValue())) {
                    message = ScholarText.get("scholar.error.dataset_import_scope");
                    return;
                }
                minecraft.setScreen(parent);
            } catch (IllegalArgumentException failure) { message = failure.getMessage(); }
            return;
        }
        var raw = name.getValue().trim();
        if (raw.isEmpty() || raw.equals(".") || raw.equals("..") || raw.contains("/") || raw.contains("\\") || raw.contains(":")) {
            message = ScholarText.get("scholar.error.filename_path_separators");
            return;
        }
        var extension = mode == Mode.EXPORT_CSV ? ".csv" : mode == Mode.EXPORT_PDF ? ".pdf" : ".md";
        if (!raw.toLowerCase(java.util.Locale.ROOT).endsWith(extension)) raw += extension;
        var target = directory.resolve(raw);
        var confirmedName = raw;
        if (Files.exists(target)) {
            minecraft.setScreen(new ConfirmScreen(confirmed -> {
                minecraft.setScreen(this);
                if (confirmed) export(target, true);
            }, ScholarText.component("scholar.confirm.replace_export"), Component.literal(confirmedName)));
        } else export(target, false);
    }

    private void export(Path target, boolean overwrite) {
        var document = session.current().document();
        var dataset = mode == Mode.EXPORT_CSV && !document.datasets().isEmpty()
                ? document.datasets().get(Math.min(datasetIndex, document.datasets().size() - 1)) : null;
        if (mode == Mode.EXPORT_CSV && dataset == null) { message = ScholarText.get("scholar.error.no_dataset_selected"); return; }
        LaidOutDocument pages;
        try { pages = mode == Mode.EXPORT_PDF ? layout.get() : null; }
        catch (RuntimeException failed) {
            message = ScholarText.get("scholar.error.document_layout", failed.getMessage());
            return;
        }
        if (mode == Mode.EXPORT_PDF && pages == null) { message = ScholarText.get("scholar.error.document_layout_not_ready"); return; }
        busy = true;
        message = ScholarText.get("scholar.export.progress");
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                if (mode == Mode.EXPORT_CSV) interchange.exportCsv(dataset, target, overwrite);
                else if (mode == Mode.EXPORT_MARKDOWN) interchange.exportMarkdown(document, target, overwrite);
                else interchange.exportPdf(document, pages, target, overwrite);
                minecraft.execute(() -> { busy = false; message = ScholarText.get("scholar.export.success", target); });
            } catch (Exception failed) {
                minecraft.execute(() -> { busy = false; message = ScholarText.get("scholar.error.export", failed.getMessage()); });
            }
        });
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF202020);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFFFF);
        ScholarScreenRendering.renderWidgets(renderables, graphics, mouseX, mouseY, partialTick);
        var x = Math.max(12, (width - 430) / 2);
        if (mode == Mode.IMPORT_CSV && preview != null) {
            graphics.drawString(font, ScholarText.get("scholar.dataset.rows", preview.rows().size()), x, previewTop, 0xFFE0E0E0);
            for (var i = columnPage * visibleColumns(); i < Math.min((columnPage + 1) * visibleColumns(), preview.columns().size()); i++) {
                var column = preview.columns().get(i);
                var y = previewTop + 16 + (i - columnPage * visibleColumns()) * 12;
                if (y < height - 48) graphics.drawString(font, column.displayName() + " - "
                                + ScholarText.get("scholar.dataset.column_type." + column.type().name().toLowerCase(java.util.Locale.ROOT))
                                + column.unit().map(unit -> " [" + column.quantitySemantics().valuePrefix(true)
                                        + dev.rgcb.scholar.quantity.UnitRegistry.builtIn().displaySymbol(unit, true) + "]").orElse(""),
                        x, y, 0xFFE0E0E0);
            }
            for (var i = 0; i < Math.min(2, preview.rows().size()); i++) {
                var row = preview.rows().get(i);
                var y = previewTop + 22 + visibleColumns() * 12 + i * 12;
                if (y < height - 48) graphics.drawString(font, row.values().stream().map(v -> v.displayText()).limit(4)
                        .reduce((a, b) -> a + " | " + b).orElse(""), x, y, 0xFFBBBBBB);
            }
        }
        if (!message.isEmpty()) {
            var lines = font.split(Component.literal(message), Math.max(80, width - 24));
            var y = height - 36;
            for (var line : lines.stream().limit(2).toList()) { graphics.drawString(font, line, 12, y, 0xFFFFDD88); y += 10; }
        }
    }

    @Override public void onClose() { if (!busy) minecraft.setScreen(parent); }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && getFocused() == name) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
