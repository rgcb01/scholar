package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.analysis.AnalysisFormatter;
import dev.rgcb.scholar.analysis.AnalysisKind;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.editor.ComputationDialogKind;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitParser;
import dev.rgcb.scholar.client.ui.ScholarScreenRendering;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Scoped authoring form for dataset analysis and derived plot overlays. */
final class ScholarAnalysisDialog extends Screen {
    private final ScholarEditorScreen parent;
    private final EditorSession session;
    private final ComputationDialogKind mode;
    private final AnalysisFormatter formatter = new AnalysisFormatter();
    private int datasetIndex;
    private int xIndex;
    private int yIndex;
    private int kindIndex;
    private int fitIndex;
    private int notationIndex;
    private EditBox displayUnit;
    private final List<Button> selectors = new ArrayList<>();
    private int openSelector = -1;
    private int scroll;
    private String error = "";

    ScholarAnalysisDialog(ScholarEditorScreen parent, EditorSession session, ComputationDialogKind mode) {
        super(Component.literal(switch (mode) {
            case INSERT_ANALYSIS -> "Insert Analysis";
            case EDIT_ANALYSIS -> "Edit Analysis";
            case ADD_FIT_OVERLAY -> "Add Fit to Plot";
            default -> throw new IllegalArgumentException("Not an analysis dialog mode.");
        }));
        this.parent = parent;
        this.session = session;
        this.mode = mode;
    }

    @Override protected void init() {
        var w = Math.min(360, width - 24);
        var x = (width - w) / 2;
        var y = Math.max(26, height / 2 - 118);
        if (mode == ComputationDialogKind.EDIT_ANALYSIS) preselect();
        if (mode == ComputationDialogKind.ADD_FIT_OVERLAY) {
            selectors.add(addRenderableWidget(Button.builder(Component.empty(), button -> toggle(0))
                    .bounds(x, y + 28, w, 20).build()));
            addRenderableWidget(Button.builder(Component.literal("Add Fit"), button -> apply())
                    .bounds(x, y + 66, w / 2 - 3, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                    .bounds(x + w / 2 + 3, y + 66, w / 2 - 3, 20).build());
        } else {
            for (var i = 0; i < 4; i++) {
                var field = i;
                selectors.add(addRenderableWidget(Button.builder(Component.empty(), button -> toggle(field))
                        .bounds(x, y + 20 + i * 31, w, 20).build()));
            }
            selectors.add(addRenderableWidget(Button.builder(Component.empty(), button -> toggle(4))
                    .bounds(x, y + 144, w, 20).build()));
            displayUnit = new EditBox(font, x, y + 179, w, 20, Component.literal("Display unit (optional)"));
            displayUnit.setHint(Component.literal("Display unit (optional)"));
            displayUnit.setMaxLength(64);
            addRenderableWidget(displayUnit);
            if (mode == ComputationDialogKind.EDIT_ANALYSIS) {
                var block = selectedAnalysis();
                displayUnit.setValue(block.displayUnit().map(value -> value.displaySymbol(
                        dev.rgcb.scholar.quantity.UnitRegistry.builtIn())).orElse(""));
            }
            addRenderableWidget(Button.builder(Component.literal("Apply"), button -> apply())
                    .bounds(x, y + 207, w / 2 - 3, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                    .bounds(x + w / 2 + 3, y + 207, w / 2 - 3, 20).build());
        }
        refreshLabels();
    }

    private void preselect() {
        var block = selectedAnalysis();
        var datasets = session.current().document().datasets();
        for (var i = 0; i < datasets.size(); i++) if (datasets.get(i).id().equals(block.datasetId())) datasetIndex = i;
        kindIndex = block.kind().ordinal();
        var columns = numericColumns();
        for (var i = 0; i < columns.size(); i++) {
            if (columns.get(i).id().equals(block.yColumnId())) yIndex = i;
            if (block.xColumnId().filter(columns.get(i).id()::equals).isPresent()) xIndex = i;
        }
        notationIndex = block.notation().ordinal();
    }

    private DatasetAnalysisBlock selectedAnalysis() {
        return (DatasetAnalysisBlock) session.current().document().blocks()
                .get(session.current().blockSelection().blockIndex());
    }

    private List<dev.rgcb.scholar.data.DatasetColumn> numericColumns() {
        var datasets = session.current().document().datasets();
        if (datasets.isEmpty()) return List.of();
        return datasets.get(Math.min(datasetIndex, datasets.size() - 1)).columns().stream()
                .filter(column -> column.type() == DatasetColumnType.NUMBER).toList();
    }

    private List<DatasetAnalysisBlock> fits() {
        return session.availableFitAnalyses();
    }

    private List<String> options(int field) {
        if (mode == ComputationDialogKind.ADD_FIT_OVERLAY) return fits().stream()
                .map(value -> value.kind().name().replace('_', ' ') + " - " + value.id()).toList();
        return switch (field) {
            case 0 -> session.current().document().datasets().stream().map(value -> value.displayLabel()).toList();
            case 1 -> List.of("Descriptive Statistics", "Linear Regression", "Quadratic Fit", "Cubic Fit");
            case 2, 3 -> numericColumns().stream().map(value -> value.displayName()
                    + value.unit().map(unit -> " (" + unit.displaySymbol(dev.rgcb.scholar.quantity.UnitRegistry.builtIn()) + ")").orElse("")).toList();
            case 4 -> List.of("Decimal", "Scientific", "Engineering");
            default -> List.of();
        };
    }

    private int selected(int field) {
        if (mode == ComputationDialogKind.ADD_FIT_OVERLAY) return fitIndex;
        return switch (field) {
            case 0 -> datasetIndex;
            case 1 -> kindIndex;
            case 2 -> xIndex;
            case 3 -> yIndex;
            case 4 -> notationIndex;
            default -> 0;
        };
    }

    private void choose(int field, int index) {
        if (mode == ComputationDialogKind.ADD_FIT_OVERLAY) fitIndex = index;
        else switch (field) {
            case 0 -> { datasetIndex = index; xIndex = 0; yIndex = 0; }
            case 1 -> kindIndex = index;
            case 2 -> xIndex = index;
            case 3 -> yIndex = index;
            case 4 -> notationIndex = index;
            default -> { }
        }
        openSelector = -1;
        refreshLabels();
    }

    private void refreshLabels() {
        var names = mode == ComputationDialogKind.ADD_FIT_OVERLAY
                ? List.of("Fit") : List.of("Dataset", "Analysis", "X column", "Y column", "Number format");
        for (var i = 0; i < selectors.size(); i++) {
            var values = options(i);
            var index = selected(i);
            var label = i == 2 && kindIndex == 0 ? "X column: not used" : names.get(i) + ": "
                    + (values.isEmpty() ? "[none]" : values.get(Math.min(index, values.size() - 1)));
            selectors.get(i).setMessage(Component.literal(label + "  v"));
            selectors.get(i).active = !(i == 2 && kindIndex == 0);
        }
    }

    private void toggle(int field) {
        openSelector = openSelector == field ? -1 : field;
        scroll = Math.max(0, selected(field) - 2);
    }

    private void apply() {
        try {
            boolean changed;
            if (mode == ComputationDialogKind.ADD_FIT_OVERLAY) {
                var available = fits();
                if (available.isEmpty()) throw new IllegalArgumentException("Insert a regression analysis first.");
                changed = session.addFitOverlay(available.get(Math.min(fitIndex, available.size() - 1)).id());
            } else {
                var datasets = session.current().document().datasets();
                var columns = numericColumns();
                if (datasets.isEmpty() || columns.isEmpty()) throw new IllegalArgumentException("Select a dataset with numeric columns.");
                var kind = AnalysisKind.values()[kindIndex];
                var x = kind.isFit() ? Optional.of(columns.get(Math.min(xIndex, columns.size() - 1)).id()) : Optional.<String>empty();
                var y = columns.get(Math.min(yIndex, columns.size() - 1)).id();
                var unitText = displayUnit.getValue().trim();
                Optional<UnitExpression> unit = unitText.isEmpty() ? Optional.empty()
                        : Optional.of(new UnitParser().parseRequired(unitText));
                var notation = NumberNotation.values()[notationIndex];
                changed = mode == ComputationDialogKind.INSERT_ANALYSIS
                        ? session.insertAnalysis(datasets.get(datasetIndex).id(), kind, x, y, unit, notation)
                        : session.editAnalysis(datasets.get(datasetIndex).id(), kind, x, y, unit, notation);
            }
            if (!changed) throw new IllegalArgumentException("Analysis is not computable or no document change was made.");
            parent.refreshComputationLayout();
            onClose();
        } catch (IllegalArgumentException exception) { error = exception.getMessage(); }
    }

    private int popupTop() { return Math.max(8, Math.min(height - 130, selectors.get(openSelector).getY() + 22)); }
    private int popupRows() { return Math.min(6, options(openSelector).size()); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF202020);
        graphics.drawCenteredString(font, title, width / 2, Math.max(12, height / 2 - 145), 0xFFFFFFFF);
        ScholarScreenRendering.renderWidgets(renderables, graphics, mouseX, mouseY, partialTick);
        if (mode != ComputationDialogKind.ADD_FIT_OVERLAY && !session.current().document().datasets().isEmpty()
                && !numericColumns().isEmpty()) {
            try {
                var dataset = session.current().document().datasets().get(datasetIndex);
                var kind = AnalysisKind.values()[kindIndex];
                var candidate = new DatasetAnalysisBlock("preview", dataset.id(), kind,
                        kind.isFit() ? Optional.of(numericColumns().get(xIndex).id()) : Optional.empty(),
                        numericColumns().get(yIndex).id(), Optional.empty(), NumberNotation.values()[notationIndex]);
                var lines = formatter.lines(session.current().document(), candidate, false);
                var previewY = Math.min(height - 58, Math.max(26, height / 2 - 118) + 242);
                for (var i = 0; i < Math.min(3, lines.size()); i++) {
                    var preview = lines.get(i);
                    while (font.width(preview) > Math.min(360, width - 24) && preview.length() > 4) {
                        preview = preview.substring(0, preview.length() - (preview.endsWith("...") ? 4 : 1)) + "...";
                    }
                    graphics.drawCenteredString(font, preview, width / 2, previewY + i * 12, 0xFFD1D5DB);
                }
            } catch (RuntimeException ignored) { }
        }
        if (!error.isEmpty()) graphics.drawCenteredString(font, error, width / 2, height - 18, 0xFFFFDD88);
        if (openSelector >= 0) {
            graphics.flush();
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(0, 0, 200);
                var button = selectors.get(openSelector);
                var top = popupTop();
                var rows = popupRows();
                graphics.fill(button.getX() - 2, top - 2, button.getX() + button.getWidth() + 2,
                        top + rows * 20 + 2, 0xFFAAAAAA);
                graphics.fill(button.getX(), top, button.getX() + button.getWidth(), top + rows * 20, 0xFF252B30);
                var values = options(openSelector);
                for (var row = 0; row < rows && scroll + row < values.size(); row++) {
                    if (scroll + row == selected(openSelector)) graphics.fill(button.getX() + 2, top + row * 20,
                            button.getX() + button.getWidth() - 2, top + (row + 1) * 20, 0xFF405A69);
                    graphics.drawString(font, values.get(scroll + row), button.getX() + 7, top + row * 20 + 6, 0xFFFFFFFF);
                }
                graphics.flush();
            } finally { graphics.pose().popPose(); }
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (openSelector < 0) return super.mouseClicked(mouseX, mouseY, button);
        var selector = selectors.get(openSelector);
        var top = popupTop();
        if (mouseX >= selector.getX() && mouseX < selector.getX() + selector.getWidth()
                && mouseY >= top && mouseY < top + popupRows() * 20) {
            var chosen = scroll + ((int) mouseY - top) / 20;
            if (chosen < options(openSelector).size()) choose(openSelector, chosen);
            return true;
        }
        openSelector = -1;
        return true;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (openSelector < 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        scroll = Math.max(0, Math.min(Math.max(0, options(openSelector).size() - popupRows()), scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (openSelector < 0) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { openSelector = -1; return true; }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
            scroll = Math.max(0, Math.min(Math.max(0, options(openSelector).size() - popupRows()),
                    scroll + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1)));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER && !options(openSelector).isEmpty()) { choose(openSelector, scroll); return true; }
        return true;
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
