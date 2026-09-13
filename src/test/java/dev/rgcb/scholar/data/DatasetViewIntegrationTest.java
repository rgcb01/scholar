package dev.rgcb.scholar.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.editor.BlockSelection;
import dev.rgcb.scholar.editor.ClipboardAdapter;
import dev.rgcb.scholar.editor.EditorActionContext;
import dev.rgcb.scholar.editor.EditorActionId;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.editor.BuiltInEditorActions;
import dev.rgcb.scholar.editor.DocumentEditor;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.TableCellCoordinate;
import dev.rgcb.scholar.editor.TableCellTextSelection;
import dev.rgcb.scholar.editor.TableEditingSelection;
import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.data.clipboard.DatasetClipboardPayload;
import dev.rgcb.scholar.markdown.MarkdownSerializer;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DatasetViewIntegrationTest {
    private final DatasetTableResolver tableResolver = new DatasetTableResolver();
    private final DatasetPlotResolver plotResolver = new DatasetPlotResolver();

    @Test
    void datasetBackedTableResolvesCurrentValuesAndColumnNames() {
        var document = document(List.of(tableView("projectile-test")), projectile());

        var table = tableResolver.resolve(document, (TableBlock) document.blocks().get(0));

        assertEquals("Time", cellText(table, 0, 0));
        assertEquals("Height", cellText(table, 0, 1));
        assertEquals("5", cellText(table, 2, 1));

        var renamed = document(List.of(tableView("projectile-test")), projectile().withColumnDisplayName("height", "Vertical Position"));
        assertEquals("Vertical Position", cellText(tableResolver.resolve(renamed, (TableBlock) renamed.blocks().get(0)), 0, 1));
    }

    @Test
    void datasetBackedTableCanUseStableColumnSubset() {
        var document = document(List.of(new TableBlock(new DatasetTableBinding("projectile-test", List.of("height")))), projectile());

        var table = tableResolver.resolve(document, (TableBlock) document.blocks().get(0));

        assertEquals(1, table.columnCount());
        assertEquals("Height", cellText(table, 0, 0));
        assertEquals("5", cellText(table, 2, 0));
    }

    @Test
    void datasetBackedPlotResolvesNumericColumnsAndSkipsBadRows() {
        var dataset = new ScientificDataset(
                "mixed",
                "Mixed",
                List.of(new DatasetColumn("time", "Time", DatasetColumnType.NUMBER), new DatasetColumn("height", "Height", DatasetColumnType.TEXT)),
                List.of(
                        new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("0"))),
                        new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.text("bad"))),
                        new DatasetRow(List.of(DatasetValue.missing(), DatasetValue.number("2")))));
        var document = document(List.of(plotView("mixed")), dataset);

        var plot = plotResolver.resolve(document, (PlotBlock) document.blocks().get(0));

        assertEquals(List.of(new DataPoint(0, 0)), plot.definition().series().get(0).points());
    }

    @Test
    void criticalPropagationTableAndPlotUpdateAfterDatasetCellEditAndUndo() {
        var session = new EditorSession(document(List.of(paragraph("dataset"), tableView("projectile-test"), plotView("projectile-test")), projectile()), 0);

        assertTrue(session.editDatasetCell("projectile-test", 1, "height", DatasetValue.number("8")));

        var updated = session.current().document();
        assertEquals("8", updated.datasets().get(0).rows().get(1).values().get(1).displayText());
        assertEquals("8", cellText(tableResolver.resolve(updated, (TableBlock) updated.blocks().get(1)), 2, 1));
        assertEquals(new DataPoint(1, 8), plotResolver.resolve(updated, (PlotBlock) updated.blocks().get(2)).definition().series().get(0).points().get(1));

        session.undo();

        var restored = session.current().document();
        assertEquals("5", restored.datasets().get(0).rows().get(1).values().get(1).displayText());
        assertEquals("5", cellText(tableResolver.resolve(restored, (TableBlock) restored.blocks().get(1)), 2, 1));
        assertEquals(new DataPoint(1, 5), plotResolver.resolve(restored, (PlotBlock) restored.blocks().get(2)).definition().series().get(0).points().get(1));
    }

    @Test
    void datasetDeletionCreatesBrokenViewsAndUndoRestoresThem() {
        var session = new EditorSession(document(List.of(paragraph("dataset"), tableView("projectile-test"), plotView("projectile-test")), projectile()), 0);

        assertTrue(session.deleteDataset("projectile-test"));

        var broken = session.current().document();
        assertEquals("[Missing dataset: projectile-test]", cellText(tableResolver.resolve(broken, (TableBlock) broken.blocks().get(1)), 0, 0));
        assertTrue(plotResolver.resolve(broken, (PlotBlock) broken.blocks().get(2)).definition().series().get(0).points().isEmpty());

        session.undo();
        assertEquals("5", cellText(tableResolver.resolve(session.current().document(), (TableBlock) session.current().document().blocks().get(1)), 2, 1));
    }

    @Test
    void ordinaryTableAndPlotRemainUnchanged() {
        var table = new TableBlock(List.of(row("A"), row("B")), 1);
        var plot = new PlotBlock(PlotDefinition.of("Manual", AxisDefinition.linear("x"), AxisDefinition.linear("y"),
                List.of(new PlotSeries("Series", PlotSeriesKind.LINE, List.of(new DataPoint(1, 2))))));
        var document = document(List.of(table, plot), projectile());

        assertEquals(table, tableResolver.resolve(document, table));
        assertEquals(List.of(new DataPoint(1, 2)), plotResolver.resolve(document, plot).definition().series().get(0).points());
    }

    @Test
    void datasetCopyPasteRemapsDuplicateDatasetId() {
        var session = new EditorSession(document(List.of(paragraph("x")), projectile()), 0);
        var copy = session.copyDatasetForClipboard("projectile-test").orElseThrow();
        var clipboard = new FakeClipboard();
        clipboard.text = copy.plainText();
        var sidecar = new ScholarClipboardService();
        sidecar.install(copy.plainText(), copy.payload().orElseThrow());

        assertInstanceOf(DatasetClipboardPayload.class, copy.payload().orElseThrow());
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar)).documentChanged());
        assertEquals(List.of("projectile-test", "projectile-test-2"), session.current().document().datasets().stream().map(ScientificDataset::id).toList());
    }

    @Test
    void datasetBackedTableClipboardPreservesSemanticReference() {
        var session = new EditorSession(document(List.of(paragraph("dataset"), tableView("projectile-test")), projectile()), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(1), Optional.empty()));
        var copy = session.copyForClipboard().orElseThrow();

        var payload = assertInstanceOf(dev.rgcb.scholar.table.clipboard.TableClipboardPayload.class, copy.payload().orElseThrow());
        assertTrue(payload.table().datasetBinding().isPresent());
    }

    @Test
    void markdownExportsDatasetBackedTableAsResolvedSnapshot() {
        var document = document(List.of(new TableBlock(new DatasetTableBinding("projectile-test"))), projectile());

        assertEquals("""
                | Time | Height |
                | --- | --- |
                | 0 | 0 |
                | 1 | 5 |
                | 2 | 0 |
                """, new MarkdownSerializer().serialize(document));
    }

    @Test
    void dataActionsAreRegisteredAndGuarded() {
        var actions = BuiltInEditorActions.dataMenuActions();
        var session = new EditorSession(document(List.of(paragraph("x")), projectile()), 0);

        assertEquals(List.of(EditorActionId.DATA_NEW_DATASET, EditorActionId.DATA_INSERT_DATASET_TABLE, EditorActionId.DATA_BIND_PLOT_TO_DATASET),
                actions.stream().map(action -> action.id()).toList());
        assertTrue(actions.get(0).isEnabled(new EditorActionContext(session, new FakeClipboard())));
        assertTrue(actions.get(1).isEnabled(new EditorActionContext(session, new FakeClipboard())));
    }

    @Test
    void textInsertionPreservesDatasetsAndBoundViews() {
        var document = document(List.of(paragraph("x"), tableView("projectile-test")), projectile());
        var editor = new DocumentEditor();
        var state = new EditorState(document, new DocumentPosition(0, 1));

        var result = editor.insertText(state, "!");

        assertEquals(List.of("projectile-test"), result.document().datasets().stream().map(ScientificDataset::id).toList());
        assertEquals("5", cellText(tableResolver.resolve(result.document(), (TableBlock) result.document().blocks().get(1)), 2, 1));
    }

    @Test
    void paragraphBreakPreservesDatasetsAndBoundViews() {
        var document = document(List.of(paragraph("x"), tableView("projectile-test")), projectile());
        var editor = new DocumentEditor();
        var state = new EditorState(document, new DocumentPosition(0, 1));

        var result = editor.insertParagraphBreak(state, Set.of());

        assertEquals(List.of("projectile-test"), result.document().datasets().stream().map(ScientificDataset::id).toList());
        assertEquals("5", cellText(tableResolver.resolve(result.document(), (TableBlock) result.document().blocks().get(2)), 2, 1));
    }

    @Test
    void datasetBackedTableCellsCanBeSelectedAgainstResolvedView() {
        var document = document(List.of(paragraph("x"), tableView("projectile-test")), projectile());

        var state = new EditorState(
                document,
                new TableEditingSelection(1, TableCellTextSelection.caret(new TableCellCoordinate(2, 1), 1)),
                Optional.empty());

        assertTrue(state.isTableEditingSelection());
    }

    @Test
    void datasetBackedTableCellNavigationUsesResolvedViewAndTypingEditsDataset() {
        var session = new EditorSession(document(List.of(paragraph("x"), tableView("projectile-test")), projectile()), 0);
        session.setCurrent(new EditorState(
                session.current().document(),
                new TableEditingSelection(1, TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)),
                Optional.empty()));

        session.moveNextTableCell();
        assertEquals(new TableCellCoordinate(0, 1), session.current().tableEditingSelection().selection().cell());

        session.setCurrent(new EditorState(
                session.current().document(),
                new TableEditingSelection(1, new TableCellTextSelection(new TableCellCoordinate(2, 1), 0, 1)),
                Optional.empty()));
        assertTrue(session.typeText("8"));
        assertEquals(List.of("projectile-test"), session.current().document().datasets().stream().map(ScientificDataset::id).toList());
        assertEquals("8", session.current().document().datasets().get(0).rows().get(1).values().get(1).displayText());
        assertEquals("8", cellText(tableResolver.resolve(session.current().document(), (TableBlock) session.current().document().blocks().get(1)), 2, 1));
    }

    @Test
    void datasetBackedTableHeaderTypingRenamesColumn() {
        var session = new EditorSession(document(List.of(paragraph("x"), tableView("projectile-test")), projectile()), 0);
        session.setCurrent(new EditorState(
                session.current().document(),
                new TableEditingSelection(1, new TableCellTextSelection(new TableCellCoordinate(0, 1), 0, 6)),
                Optional.empty()));

        assertTrue(session.typeText("Altitude"));

        assertEquals("Altitude", session.current().document().datasets().get(0).columns().get(1).displayName());
        assertEquals("Altitude", cellText(tableResolver.resolve(session.current().document(), (TableBlock) session.current().document().blocks().get(1)), 0, 1));
    }

    private static Document document(List<BlockNode> blocks, ScientificDataset dataset) {
        return new Document(blocks, List.of(dataset));
    }

    private static ScientificDataset projectile() {
        return new ScientificDataset(
                "projectile-test",
                "Projectile Test",
                List.of(
                        new DatasetColumn("time", "Time", DatasetColumnType.NUMBER),
                        new DatasetColumn("height", "Height", DatasetColumnType.NUMBER)),
                List.of(
                        new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("0"))),
                        new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("5"))),
                        new DatasetRow(List.of(DatasetValue.number("2"), DatasetValue.number("0")))));
    }

    private static TableBlock tableView(String datasetId) {
        return new TableBlock(new DatasetTableBinding(datasetId));
    }

    private static PlotBlock plotView(String datasetId) {
        return new PlotBlock(PlotDefinition.of("Projectile", AxisDefinition.linear("Time"), AxisDefinition.linear("Height"),
                List.of(new PlotSeries("Height", PlotSeriesKind.LINE, new DatasetPlotBinding(datasetId, "time", "height")))));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static TableRow row(String value) {
        return new TableRow(List.of(new dev.rgcb.scholar.document.TableCell(new dev.rgcb.scholar.document.TableCellContent(new InlineContent(List.of((InlineNode) new Text(value, Set.of())))))));
    }

    private static String cellText(TableBlock table, int row, int column) {
        return ((Text) table.rows().get(row).cells().get(column).content().content().nodes().get(0)).content();
    }

    private static final class FakeClipboard implements ClipboardAdapter {
        private String text = "";

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean setText(String text) {
            this.text = text;
            return true;
        }
    }
}
