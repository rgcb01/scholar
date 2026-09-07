package dev.rgcb.scholar.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.editor.TextBoundary;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TableLayoutEngineTest {
    private final TableLayoutEngine engine = new TableLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void laysOutEqualWidthColumnsAndDistributesRemainderToEarlierColumns() {
        var layout = engine.layout(new TableBlock(List.of(row(cell("a"), cell("b"), cell("c"))), 0), 4, 0, 0, 101, textMeasurer);

        var cells = layout.rows().get(0).cells();
        assertEquals(34, cells.get(0).width());
        assertEquals(34, cells.get(1).width());
        assertEquals(33, cells.get(2).width());
        assertEquals(0, cells.get(0).x());
        assertEquals(34, cells.get(1).x());
        assertEquals(68, cells.get(2).x());
        assertEquals(101, layout.width());
    }

    @Test
    void rowHeightUsesTallestWrappedCellAndEmptyCellMinimumLineHeight() {
        var table = new TableBlock(List.of(row(cell("longword"), TableCell.empty())), 0);
        var layout = engine.layout(table, 2, 0, 0, 56, textMeasurer);
        var row = layout.rows().get(0);

        assertTrue(row.cells().get(0).lines().size() > 1);
        assertEquals(row.cells().get(0).height(), row.cells().get(1).height());
        assertTrue(row.height() > textMeasurer.lineHeight(TextStyle.paragraph()) + TableLayoutEngine.CELL_PADDING_Y * 2);
        assertEquals(1, row.cells().get(1).lines().size());
        assertTrue(row.cells().get(1).lines().get(0).textRuns().isEmpty());
    }

    @Test
    void exposesCellBoundsContentBoundsAndSourceMetadata() {
        var table = new TableBlock(List.of(row(cell("alpha beta"), cell("x"))), 0);
        var layout = engine.layout(table, 7, 3, 5, 80, textMeasurer);
        var first = layout.rows().get(0).cells().get(0);
        var run = first.lines().get(0).textRuns().get(0);

        assertEquals(0, first.rowIndex());
        assertEquals(0, first.columnIndex());
        assertEquals(3, first.x());
        assertEquals(5, first.y());
        assertEquals(first.x() + TableLayoutEngine.CELL_PADDING_X, first.contentX());
        assertEquals(first.y() + TableLayoutEngine.CELL_PADDING_Y, first.contentY());
        assertEquals(7, run.sourceBlockIndex());
        assertEquals(0, run.sourceStart());
        assertTrue(run.sourceEnd() > run.sourceStart());
    }

    @Test
    void headerRowsAreMeasuredWithDerivedBoldStyle() {
        var table = new TableBlock(List.of(row(cell("head")), row(cell("body"))), 1);
        var layout = engine.layout(table, 0, 0, 0, 100, textMeasurer);

        var headerRun = layout.rows().get(0).cells().get(0).lines().get(0).textRuns().get(0);
        var bodyRun = layout.rows().get(1).cells().get(0).lines().get(0).textRuns().get(0);

        assertTrue(headerRun.style().marks().contains(TextMark.BOLD));
        assertTrue(bodyRun.style().marks().isEmpty());
    }

    @Test
    void wrapsCellTextInsideColumnContentWidth() {
        var table = new TableBlock(List.of(row(cell("alpha beta gamma"), cell("delta"))), 0);
        var layout = engine.layout(table, 0, 0, 0, 70, textMeasurer);

        assertTrue(layout.rows().get(0).cells().get(0).lines().size() >= 2);
        assertEquals(70, layout.width());
        assertEquals(layout.rows().get(0).height(), layout.height());
    }

    private static TableRow row(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    private static TableCell cell(String text, TextMark... marks) {
        return new TableCell(new TableCellContent(new InlineContent(List.of((InlineNode) new Text(text, Set.of(marks))))));
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            var width = TextBoundary.characterCount(text) * 5;
            return style.marks().contains(TextMark.BOLD) ? width + TextBoundary.characterCount(text) : width;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
