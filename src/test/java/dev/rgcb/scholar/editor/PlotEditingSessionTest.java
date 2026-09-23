package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlotEditingSessionTest {
    @Test
    void enterAndEscapeTransitionBetweenBlockAndPlotEditingSelection() {
        var document = document();
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));

        assertFalse(session.enter());
        var editing = assertInstanceOf(PlotEditingSelection.class, session.current().selection());
        assertEquals(1, editing.blockIndex());
        assertEquals(new PlotPropertyTarget(PlotProperty.TITLE), editing.target());

        session.exitPlotEditing();
        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void targetNavigationIsSelectionOnlyAndCreatesNoHistory() {
        var session = editingSession();
        assertFalse(session.canUndo());

        session.moveNextPlotTarget();
        assertEquals(new PlotPropertyTarget(PlotProperty.X_AXIS_LABEL), session.current().plotEditingSelection().target());
        session.movePreviousPlotTarget();
        assertEquals(new PlotPropertyTarget(PlotProperty.TITLE), session.current().plotEditingSelection().target());
        assertFalse(session.canUndo());
    }

    @Test
    void titleEditUsesGlobalHistoryAndRestoresPlotEditingSelection() {
        var session = editingSession();
        assertTrue(session.applyPlotText("Edited Plot"));
        assertEquals("Edited Plot", currentPlot(session).definition().title());
        assertTrue(session.undo());
        assertEquals("Position vs Time", currentPlot(session).definition().title());
        assertInstanceOf(PlotEditingSelection.class, session.current().selection());
        assertTrue(session.redo());
        assertEquals("Edited Plot", currentPlot(session).definition().title());
    }

    @Test
    void pointEditingAndStructuralSeriesActionsRemainInPlotMode() {
        var session = editingSession();
        session.setPlotEditingTarget(new PlotSeriesTarget(0));
        assertTrue(session.addPlotPoint());
        var pointTarget = assertInstanceOf(PlotPointTarget.class, session.current().plotEditingSelection().target());
        assertEquals(2, pointTarget.pointIndex());
        assertTrue(session.applyPlotPoint(2.5, 6.25));
        assertEquals(new DataPoint(2.5, 6.25), currentPlot(session).definition().series().get(0).points().get(2));
        assertTrue(session.setPlotSeriesKind(PlotSeriesKind.SCATTER));
        assertEquals(PlotSeriesKind.SCATTER, currentPlot(session).definition().series().get(0).kind());
        assertTrue(session.deletePlotPoint());
        assertInstanceOf(PlotPointTarget.class, session.current().plotEditingSelection().target());
    }

    @Test
    void gridLegendAndSeriesMutationsAreUndoable() {
        var session = editingSession();
        assertTrue(session.togglePlotGrid());
        assertFalse(currentPlot(session).definition().gridVisible());
        assertTrue(session.togglePlotLegend());
        assertFalse(currentPlot(session).definition().legendVisible());
        assertTrue(session.addPlotSeries(PlotSeriesKind.SCATTER));
        assertEquals(3, currentPlot(session).definition().series().size());
        assertTrue(session.undo());
        assertEquals(2, currentPlot(session).definition().series().size());
    }

    @Test
    void normalTypingDeletionFormattingAndClipboardAreDisabledInPlotEditingMode() {
        var session = editingSession();
        var before = session.current().document();

        assertFalse(session.typeText("x"));
        assertFalse(session.deleteBackward());
        assertFalse(session.deleteForward());
        assertFalse(session.supportsInlineFormatting());
        assertFalse(session.supportsBlockStyle());
        assertTrue(session.copyForClipboard().isEmpty());
        assertTrue(session.cutForClipboard().isEmpty());
        assertFalse(session.pasteText("x"));
        assertEquals(before, session.current().document());
    }

    @Test
    void plotMenuActionsExposeSharedStructuralCommandsAndSelectionStates() {
        var session = editingSession();
        var context = new EditorActionContext(session, new FakeClipboard());
        var actions = BuiltInEditorActions.plotMenuActions();

        assertEquals(List.of(
                EditorActionId.PLOT_TOGGLE_GRID,
                EditorActionId.PLOT_TOGGLE_LEGEND,
                EditorActionId.PLOT_ADD_LINE_SERIES,
                EditorActionId.PLOT_ADD_SCATTER_SERIES,
                EditorActionId.PLOT_SET_SERIES_LINE,
                EditorActionId.PLOT_SET_SERIES_SCATTER,
                EditorActionId.PLOT_ADD_POINT,
                EditorActionId.PLOT_DELETE_POINT,
                EditorActionId.PLOT_DELETE_SERIES,
                EditorActionId.PLOT_X_UNIT_AUTO,
                EditorActionId.PLOT_X_UNIT_SECOND,
                EditorActionId.PLOT_X_UNIT_METRE,
                EditorActionId.PLOT_Y_UNIT_AUTO,
                EditorActionId.PLOT_Y_UNIT_CELSIUS,
                EditorActionId.PLOT_Y_UNIT_KELVIN,
                EditorActionId.PLOT_Y_UNIT_CELSIUS_DIFFERENCE,
                EditorActionId.PLOT_Y_UNIT_KELVIN_DIFFERENCE,
                EditorActionId.PLOT_Y_UNIT_VOLT), actions.stream().map(EditorAction::id).toList());
        assertEquals(ActionSelectionState.ON, actions.get(0).selectionState(context));
        assertEquals(ActionSelectionState.ON, actions.get(1).selectionState(context));
        assertTrue(actions.get(2).isEnabled(context));
        assertFalse(actions.get(4).isEnabled(context));

        session.setPlotEditingTarget(new PlotSeriesTarget(0));
        assertTrue(actions.get(4).isEnabled(context));
        assertEquals(ActionSelectionState.ON, actions.get(4).selectionState(context));
        assertEquals(ActionSelectionState.OFF, actions.get(5).selectionState(context));
    }

    private static EditorSession editingSession() {
        var document = document();
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        session.enter();
        return session;
    }

    private static PlotBlock currentPlot(EditorSession session) {
        return (PlotBlock) session.current().document().blocks().get(1);
    }

    private static Document document() {
        return new Document(List.of(
                paragraph("Before"),
                new PlotBlock(new PlotDefinition(
                        "Position vs Time",
                        AxisDefinition.linear("Time (s)"),
                        AxisDefinition.linear("Position (m)"),
                        List.of(
                                new PlotSeries("Motion", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))),
                                new PlotSeries("Samples", PlotSeriesKind.SCATTER, List.of(new DataPoint(0.5, 0.25)))),
                        true,
                        true,
                        PlotDefinition.DEFAULT_HEIGHT)),
                paragraph("After")));
    }

    private static Paragraph paragraph(String value) {
        return new Paragraph(new InlineContent(List.of(new Text(value, Set.of()))));
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
