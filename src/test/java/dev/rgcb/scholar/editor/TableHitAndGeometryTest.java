package dev.rgcb.scholar.editor;

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
import dev.rgcb.scholar.layout.TableLayoutEngine;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TableHitAndGeometryTest {
    private final TableLayoutEngine layoutEngine = new TableLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();
    private final TableHitTester hitTester = new TableHitTester();
    private final TableCaretGeometryResolver caretResolver = new TableCaretGeometryResolver();
    private final TableSelectionGeometryResolver selectionResolver = new TableSelectionGeometryResolver();
    private final TableCellTextNavigator navigator = new TableCellTextNavigator();

    @Test
    void hitTestingFindsCellsPaddingEmptyCellsAndWrappedTextOffsets() {
        var layout = layoutEngine.layout(new TableBlock(List.of(
                row(cell("alpha beta"), TableCell.empty()),
                row(cell("gamma"), cell("delta"))), 0), 0, 0, 0, 80, textMeasurer);

        assertEquals(new TableCellHit(new TableCellCoordinate(0, 0), 0), hitTester.hit(layout, 1, 1, textMeasurer).orElseThrow());
        assertEquals(new TableCellHit(new TableCellCoordinate(0, 1), 0), hitTester.hit(layout, 60, 5, textMeasurer).orElseThrow());
        assertEquals(new TableCellCoordinate(1, 0), hitTester.hit(layout, 1, layout.rows().get(1).y() + 2, textMeasurer).orElseThrow().cell());
        assertTrue(layout.rows().get(0).cells().get(0).lines().size() > 1);
        assertTrue(hitTester.hit(layout, 38, 6, textMeasurer).orElseThrow().characterOffset() > 0);
    }

    @Test
    void internalBordersResolveToLeftAndUpperCells() {
        var layout = layoutEngine.layout(new TableBlock(List.of(
                row(cell("a"), cell("b")),
                row(cell("c"), cell("d"))), 0), 0, 0, 0, 80, textMeasurer);
        var verticalBorderX = layout.rows().get(0).cells().get(0).x() + layout.rows().get(0).cells().get(0).width();
        var horizontalBorderY = layout.rows().get(0).y() + layout.rows().get(0).height();

        assertEquals(new TableCellCoordinate(0, 0), hitTester.hit(layout, verticalBorderX, 5, textMeasurer).orElseThrow().cell());
        assertEquals(new TableCellCoordinate(0, 0), hitTester.hit(layout, 5, horizontalBorderY, textMeasurer).orElseThrow().cell());
    }

    @Test
    void directCellHitKeepsOffsetsLocalToThatCellAtSharedBorder() {
        var layout = layoutEngine.layout(new TableBlock(List.of(row(cell("a"), cell("longer"))), 0), 0, 0, 0, 100, textMeasurer);
        var leftCell = layout.rows().get(0).cells().get(0);
        var sharedBorderX = leftCell.x() + leftCell.width();

        var hit = hitTester.hit(leftCell, sharedBorderX, 5, textMeasurer).orElseThrow();

        assertEquals(new TableCellCoordinate(0, 0), hit.cell());
        assertEquals(1, hit.characterOffset());
    }

    @Test
    void caretGeometryResolvesStartMiddleEndAndEmptyCell() {
        var layout = layoutEngine.layout(new TableBlock(List.of(row(cell("abcd"), TableCell.empty())), 0), 0, 0, 0, 120, textMeasurer);
        var textCell = layout.rows().get(0).cells().get(0);
        var emptyCell = layout.rows().get(0).cells().get(1);

        assertEquals(textCell.contentX(), caretResolver.resolve(textCell, 0, textMeasurer).x());
        assertEquals(textCell.contentX() + 20, caretResolver.resolve(textCell, 2, textMeasurer).x());
        assertEquals(textCell.contentX() + 40, caretResolver.resolve(textCell, 4, textMeasurer).x());
        assertEquals(emptyCell.contentX(), caretResolver.resolve(emptyCell, 0, textMeasurer).x());
    }

    @Test
    void selectionGeometrySupportsStyledAndReversedSelectionsAcrossWrappedLines() {
        var layout = layoutEngine.layout(new TableBlock(List.of(row(cell(
                new Text("abc ", Set.of(TextMark.BOLD)),
                new Text("def ghi", Set.of())))), 0), 0, 0, 0, 52, textMeasurer);
        var cell = layout.rows().get(0).cells().get(0);
        var rects = selectionResolver.resolve(cell, new TableCellTextSelection(new TableCellCoordinate(0, 0), 8, 1), textMeasurer);

        assertTrue(rects.size() >= 2);
        assertTrue(rects.stream().allMatch(rect -> rect.width() > 0 && rect.height() > 0));
    }

    @Test
    void visualNavigationClampsWithinWrappedCellAndUsesPreferredX() {
        var layout = layoutEngine.layout(new TableBlock(List.of(row(cell("alpha beta gamma"))), 0), 0, 0, 0, 50, textMeasurer);
        var cell = layout.rows().get(0).cells().get(0);

        var down = navigator.moveDown(cell, 2, textMeasurer, cell.contentX() + 20).orElseThrow();
        var up = navigator.moveUp(cell, down, textMeasurer, cell.contentX() + 20).orElseThrow();
        var bottomClamp = navigator.moveDown(cell, 16, textMeasurer, cell.contentX() + 20).orElseThrow();

        assertTrue(down > 2);
        assertEquals(2, up);
        assertEquals(16, bottomClamp);
        assertEquals(0, navigator.lineStart(cell, 2).orElseThrow());
        assertTrue(navigator.lineEnd(cell, 2).orElseThrow() > 0);
    }

    private static TableRow row(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    private static TableCell cell(String text) {
        return cell(new Text(text, Set.of()));
    }

    private static TableCell cell(Text... text) {
        return new TableCell(new TableCellContent(new InlineContent(List.of(text).stream().map(InlineNode.class::cast).toList())));
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            var width = TextBoundary.characterCount(text) * 10;
            return style.marks().contains(TextMark.BOLD) ? width + TextBoundary.characterCount(text) * 2 : width;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
