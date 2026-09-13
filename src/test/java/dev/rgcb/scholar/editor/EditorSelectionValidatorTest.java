package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.math.MathIdentifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EditorSelectionValidatorTest {
    private final EditorSelectionValidator validator = new EditorSelectionValidator();

    @Test
    void validatesTextSelectionOffsetsAndEditableRange() {
        var document = document(paragraph("abc"), heading("def"));

        assertTrue(validator.isValid(document, new TextSelection(new DocumentPosition(0, 1), new DocumentPosition(1, 2))));
        assertFalse(validator.isValid(document, new TextSelection(new DocumentPosition(0, 4), new DocumentPosition(0, 4))));
        assertFalse(validator.isValid(document, new TextSelection(new DocumentPosition(2, 0), new DocumentPosition(2, 0))));
    }

    @Test
    void textSelectionCannotCrossAtomicBlock() {
        var document = document(paragraph("abc"), new EquationBlock(new MathIdentifier("x")), paragraph("def"));

        assertThrows(IllegalArgumentException.class, () -> new EditorState(document, new DocumentPosition(0, 3), new DocumentPosition(2, 0)));
        assertFalse(validator.isValid(document, new TextSelection(new DocumentPosition(0, 3), new DocumentPosition(2, 0))));
    }

    @Test
    void validatesNestedTableSelectionAgainstResolvedDatasetBackedView() {
        var document = new Document(
                List.of(new TableBlock(new DatasetTableBinding("projectile", List.of("time")))),
                List.of(projectileDataset()));
        var valid = new TableEditingSelection(0, TableCellTextSelection.caret(new TableCellCoordinate(1, 0), 1));
        var staleColumn = new TableEditingSelection(0, TableCellTextSelection.caret(new TableCellCoordinate(1, 1), 0));

        assertTrue(validator.isValid(document, valid));
        assertFalse(validator.isValid(document, staleColumn));
    }

    @Test
    void fallbackKeepsValidSelectionAndChoosesFirstLegalStateForStaleSelection() {
        var document = document(new EquationBlock(new MathIdentifier("x")), paragraph("abc"));
        var valid = new BlockSelection(0);
        var stale = new BlockSelection(9);

        assertEquals(Optional.of(valid), validator.normalizedFallback(document, valid));
        assertEquals(Optional.of(new BlockSelection(0)), validator.normalizedFallback(document, stale));
    }

    @Test
    void editorStateRejectsTextCaretInsideAtomicBlock() {
        var document = document(new EquationBlock(new MathIdentifier("x")));

        assertThrows(IllegalArgumentException.class, () -> new EditorState(document, new DocumentPosition(0, 0)));
    }

    @Test
    void tableCutEditResultPreservesDocumentDatasets() {
        var table = new TableBlock(List.of(row(cell("A"))), 0);
        var dataset = projectileDataset();
        var source = new TableEditor.DocumentSource(new Document(List.of(table), List.of(dataset)), 0, table);
        var cut = new TableEditor().cut(source, new TableCellTextSelection(new TableCellCoordinate(0, 0), 0, 1)).orElseThrow();

        assertEquals(List.of(dataset), cut.editResult().document().datasets());
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(inline(text));
    }

    private static Heading heading(String text) {
        return new Heading(2, inline(text));
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of((InlineNode) new Text(text, Set.of())));
    }

    private static TableRow row(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(inline(text)));
    }

    private static ScientificDataset projectileDataset() {
        return new ScientificDataset(
                "projectile",
                "Projectile",
                List.of(new DatasetColumn("time", "Time", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow(List.of(DatasetValue.number("0")))));
    }
}
