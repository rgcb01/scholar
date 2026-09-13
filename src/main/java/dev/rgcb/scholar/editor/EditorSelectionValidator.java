package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.data.DatasetTableResolver;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.math.editor.MathExpressionEditor;
import java.util.Objects;
import java.util.Optional;

public final class EditorSelectionValidator {
    private final MathExpressionEditor mathEditor = new MathExpressionEditor();
    private final TableEditor tableEditor = new TableEditor();
    private final PlotEditor plotEditor = new PlotEditor();
    private final DiagramEditor diagramEditor = new DiagramEditor();
    private final DatasetTableResolver datasetTableResolver = new DatasetTableResolver();

    public boolean isValid(EditorState state) {
        Objects.requireNonNull(state, "state");
        return isValid(state.document(), state.selection());
    }

    public boolean isValid(Document document, EditorSelection selection) {
        try {
            validate(document, selection);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public void validate(Document document, EditorSelection selection) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(selection, "selection");
        if (selection instanceof TextSelection textSelection) {
            validateTextSelection(document, textSelection);
        } else if (selection instanceof BlockSelection blockSelection) {
            validateBlockSelection(document, blockSelection);
        } else if (selection instanceof EquationEditingSelection equationEditingSelection) {
            validateEquationSelection(document, equationEditingSelection);
        } else if (selection instanceof TableEditingSelection tableEditingSelection) {
            validateTableSelection(document, tableEditingSelection);
        } else if (selection instanceof PlotEditingSelection plotEditingSelection) {
            validatePlotSelection(document, plotEditingSelection);
        } else if (selection instanceof DiagramEditingSelection diagramEditingSelection) {
            validateDiagramSelection(document, diagramEditingSelection);
        } else if (selection instanceof FigureCaptionSelection figureCaptionSelection) {
            validateFigureCaptionSelection(document, figureCaptionSelection);
        } else {
            throw new IllegalArgumentException("Unsupported editor selection: " + selection.getClass().getName());
        }
    }

    public Optional<EditorSelection> normalizedFallback(Document document, EditorSelection selection) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(selection, "selection");
        if (isValid(document, selection)) {
            return Optional.of(selection);
        }
        return firstValidSelection(document);
    }

    public Optional<EditorSelection> firstValidSelection(Document document) {
        Objects.requireNonNull(document, "document");
        for (var blockIndex = 0; blockIndex < document.blocks().size(); blockIndex++) {
            var block = document.blocks().get(blockIndex);
            if (EditableInlineBlock.supports(block)) {
                return Optional.of(new TextSelection(new DocumentPosition(blockIndex, 0), new DocumentPosition(blockIndex, 0)));
            }
            return Optional.of(new BlockSelection(blockIndex));
        }
        return Optional.empty();
    }

    private void validateTextSelection(Document document, TextSelection selection) {
        validateDocumentPosition(document, selection.anchor());
        validateDocumentPosition(document, selection.active());
        var range = selection.range();
        for (var blockIndex = range.start().blockIndex(); blockIndex <= range.end().blockIndex(); blockIndex++) {
            if (!EditableInlineBlock.supports(document.blocks().get(blockIndex))) {
                throw new IllegalArgumentException("Text selection cannot cross non-editable block " + blockIndex + ".");
            }
        }
    }

    private void validateDocumentPosition(Document document, DocumentPosition position) {
        if (position.blockIndex() >= document.blocks().size()) {
            throw new IllegalArgumentException("text selection block index is outside the document.");
        }
        var block = document.blocks().get(position.blockIndex());
        if (!EditableInlineBlock.supports(block)) {
            throw new IllegalArgumentException("Text selection requires an editable inline block.");
        }
        TextBoundary.validateOffset(InlineContentEditor.logicalText(EditableInlineBlock.contentOf(block)), position.characterOffset());
    }

    private void validateBlockSelection(Document document, BlockSelection selection) {
        var blockIndex = selection.blockIndex();
        if (blockIndex >= document.blocks().size()) {
            throw new IllegalArgumentException("block selection index is outside the document.");
        }
        var block = document.blocks().get(blockIndex);
        if (EditableInlineBlock.supports(block) && !(block instanceof dev.rgcb.scholar.document.Heading)) {
            throw new IllegalArgumentException("Paragraph blocks use text selection.");
        }
    }

    private void validateEquationSelection(Document document, EquationEditingSelection selection) {
        var blockIndex = selection.blockIndex();
        if (blockIndex >= document.blocks().size()) {
            throw new IllegalArgumentException("equation editing block index is outside the document.");
        }
        if (!(document.blocks().get(blockIndex) instanceof EquationBlock equationBlock)) {
            throw new IllegalArgumentException("Equation editing selection requires an EquationBlock.");
        }
        mathEditor.validateSelection(equationBlock.expression(), selection.selection());
    }

    private void validateTableSelection(Document document, TableEditingSelection selection) {
        var blockIndex = selection.blockIndex();
        if (blockIndex >= document.blocks().size()) {
            throw new IllegalArgumentException("table editing block index is outside the document.");
        }
        if (!(document.blocks().get(blockIndex) instanceof TableBlock tableBlock)) {
            throw new IllegalArgumentException("Table editing selection requires a TableBlock.");
        }
        var editableTable = tableBlock.datasetBinding().isPresent()
                ? datasetTableResolver.resolve(document, tableBlock)
                : tableBlock;
        tableEditor.validateSelection(editableTable, selection.selection());
    }

    private void validatePlotSelection(Document document, PlotEditingSelection selection) {
        var blockIndex = selection.blockIndex();
        if (blockIndex >= document.blocks().size()) {
            throw new IllegalArgumentException("plot editing block index is outside the document.");
        }
        var plotBlock = plotBlockAt(document, blockIndex);
        if (plotBlock == null) {
            throw new IllegalArgumentException("Plot editing selection requires a PlotBlock.");
        }
        plotEditor.validateSelection(plotBlock, selection.target());
    }

    private void validateDiagramSelection(Document document, DiagramEditingSelection selection) {
        var blockIndex = selection.blockIndex();
        if (blockIndex >= document.blocks().size()) {
            throw new IllegalArgumentException("diagram editing block index is outside the document.");
        }
        var diagramBlock = diagramBlockAt(document, blockIndex);
        if (diagramBlock == null) {
            throw new IllegalArgumentException("Diagram editing selection requires a DiagramBlock.");
        }
        diagramEditor.validateSelection(diagramBlock, selection.target());
    }

    private void validateFigureCaptionSelection(Document document, FigureCaptionSelection selection) {
        var blockIndex = selection.blockIndex();
        if (blockIndex >= document.blocks().size()) {
            throw new IllegalArgumentException("figure caption block index is outside the document.");
        }
        if (!(document.blocks().get(blockIndex) instanceof FigureBlock figureBlock)) {
            throw new IllegalArgumentException("Figure caption selection requires a FigureBlock.");
        }
        var length = InlineContentEditor.characterCount(figureBlock.caption());
        TextBoundary.validateOffset(InlineContentEditor.logicalText(figureBlock.caption()), selection.anchorOffset());
        TextBoundary.validateOffset(InlineContentEditor.logicalText(figureBlock.caption()), selection.activeOffset());
        if (selection.anchorOffset() > length || selection.activeOffset() > length) {
            throw new IllegalArgumentException("figure caption selection offset is outside the caption.");
        }
    }

    private static PlotBlock plotBlockAt(Document document, int blockIndex) {
        var block = document.blocks().get(blockIndex);
        if (block instanceof PlotBlock plotBlock) {
            return plotBlock;
        }
        if (block instanceof FigureBlock figureBlock && figureBlock.content() instanceof PlotBlock plotBlock) {
            return plotBlock;
        }
        return null;
    }

    private static DiagramBlock diagramBlockAt(Document document, int blockIndex) {
        var block = document.blocks().get(blockIndex);
        if (block instanceof DiagramBlock diagramBlock) {
            return diagramBlock;
        }
        if (block instanceof FigureBlock figureBlock && figureBlock.content() instanceof DiagramBlock diagramBlock) {
            return diagramBlock;
        }
        return null;
    }
}
