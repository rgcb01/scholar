package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.editor.MathCaretSelection;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import dev.rgcb.scholar.mechanical.MechanicalPrimitive;
import dev.rgcb.scholar.mechanical.MechanicalPrimitiveKind;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class EditorContextActionResolverTest {
    private final EditorContextActionResolver resolver = new EditorContextActionResolver();

    @Test
    void textCaretMenuUsesExistingActionsAndHidesCutCopy() {
        var actions = allActions();
        var state = new EditorState(document(paragraph("abc")), new DocumentPosition(0, 1));

        var entries = resolver.resolve(state, actions);

        assertIds(entries, EditorActionId.PASTE, EditorActionId.BOLD, EditorActionId.ITALIC, EditorActionId.INSERT_CROSS_REFERENCE);
        assertFalse(ids(entries).contains(EditorActionId.CUT));
        assertFalse(ids(entries).contains(EditorActionId.COPY));
        assertSame(actions.get(EditorActionId.BOLD), action(entries, EditorActionId.BOLD));
        entries.stream().filter(entry -> entry.kind() == ContextMenuEntryKind.ACTION)
                .map(entry -> entry.action().orElseThrow())
                .forEach(action -> assertSame(BuiltInEditorActionCatalog.require(action.id()), action.descriptor()));
    }

    @Test
    void textRangeMenuIncludesClipboardAndInlineFormatting() {
        var state = new EditorState(document(paragraph("abcdef")), new DocumentPosition(0, 1), new DocumentPosition(0, 4));

        assertIds(resolver.resolve(state, allActions()),
                EditorActionId.CUT,
                EditorActionId.COPY,
                EditorActionId.PASTE,
                EditorActionId.BOLD,
                EditorActionId.ITALIC,
                EditorActionId.INSERT_CROSS_REFERENCE);
    }

    @Test
    void headingTextMenuAddsBlockStyleActions() {
        var state = new EditorState(document(heading(2, "Title")), new DocumentPosition(0, 1));

        var ids = ids(resolver.resolve(state, allActions()));

        assertTrue(ids.contains(EditorActionId.PARAGRAPH));
        assertTrue(ids.contains(EditorActionId.HEADING_1));
        assertTrue(ids.contains(EditorActionId.HEADING_6));
    }

    @Test
    void equationEditingMenuUsesMathActionsWithoutDuplicatingExecution() {
        var actions = allActions();
        var state = new EditorState(
                document(new EquationBlock(new MathSequence(List.of(new MathIdentifier("x"))))),
                new EquationEditingSelection(0, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))),
                Optional.empty());

        var entries = resolver.resolve(state, actions);

        assertTrue(ids(entries).contains(EditorActionId.MATH_INSERT_FRACTION));
        assertTrue(ids(entries).contains(EditorActionId.MATH_INSERT_ROOT));
        assertSame(actions.get(EditorActionId.MATH_INSERT_FRACTION), action(entries, EditorActionId.MATH_INSERT_FRACTION));
    }

    @Test
    void blockSelectionMenusAreSpecificToSelectedBlockType() {
        var document = document(
                heading(1, "H"),
                new EquationBlock(new MathSequence(List.of())),
                TableBlock.empty(2, 2),
                plotBlock(),
                diagramBlock(),
                figureBlock(),
                new TableOfContentsBlock());

        assertTrue(ids(resolver.resolve(blockState(document, 1), allActions())).contains(EditorActionId.MATH_INSERT_ROOT));
        assertTrue(ids(resolver.resolve(blockState(document, 2), allActions())).contains(EditorActionId.TABLE_INSERT_ROW_ABOVE));
        assertTrue(ids(resolver.resolve(blockState(document, 3), allActions())).contains(EditorActionId.FIGURE_WRAP_PLOT));
        assertTrue(ids(resolver.resolve(blockState(document, 4), allActions())).contains(EditorActionId.FIGURE_WRAP_DIAGRAM));
        assertTrue(ids(resolver.resolve(blockState(document, 5), allActions())).contains(EditorActionId.FIGURE_EDIT_CAPTION));
        assertTrue(ids(resolver.resolve(blockState(document, 6), allActions())).contains(EditorActionId.TOGGLE_OUTLINE));
    }

    @Test
    void tableCellMenuIncludesCellActionsAndFormatting() {
        var state = new EditorState(
                document(TableBlock.empty(2, 2)),
                new TableEditingSelection(0, TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)),
                Optional.empty());

        var ids = ids(resolver.resolve(state, allActions()));

        assertTrue(ids.contains(EditorActionId.PASTE));
        assertTrue(ids.contains(EditorActionId.BOLD));
        assertTrue(ids.contains(EditorActionId.TABLE_INSERT_COLUMN_RIGHT));
    }

    @Test
    void plotEditingMenuIncludesPlotActionsOnly() {
        var state = new EditorState(
                document(plotBlock()),
                new PlotEditingSelection(0, new PlotPropertyTarget(PlotProperty.TITLE)),
                Optional.empty());

        var ids = ids(resolver.resolve(state, allActions()));

        assertTrue(ids.contains(EditorActionId.PLOT_TOGGLE_GRID));
        assertTrue(ids.contains(EditorActionId.PLOT_ADD_POINT));
        assertFalse(ids.contains(EditorActionId.DIAGRAM_ADD_NODE));
    }

    @Test
    void diagramCanvasMenuIncludesAuthoringAndWorkspaceActions() {
        var state = new EditorState(
                document(diagramBlock()),
                new DiagramEditingSelection(0, new DiagramPropertyTarget(DiagramProperty.CANVAS)),
                Optional.empty());

        var ids = ids(resolver.resolve(state, allActions()));

        assertTrue(ids.contains(EditorActionId.DIAGRAM_ADD_NODE));
        assertFalse(ids.contains(EditorActionId.DIAGRAM_ADD_RESISTOR));
        assertFalse(ids.contains(EditorActionId.DIAGRAM_ADD_MECHANICAL_LINE));
        assertTrue(ids.contains(EditorActionId.DIAGRAM_WORKSPACE_TALLER));
    }

    @Test
    void diagramElementMenuIncludesElementOperationsForElectricalAndMechanicalTargets() {
        var electricalId = new DiagramElementId("r1");
        var mechanicalId = new DiagramElementId("m1");
        var document = document(diagramBlock(
                new ElectricalComponent(electricalId, new DiagramBounds(10, 10, 40, 20),
                        ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "1k"),
                new MechanicalPrimitive(mechanicalId, new DiagramBounds(80, 10, 40, 20), MechanicalPrimitiveKind.LINE)));

        var electricalIds = ids(resolver.resolve(new EditorState(
                document,
                new DiagramEditingSelection(0, new DiagramElementTarget(0, electricalId)),
                Optional.empty()), allActions()));
        var mechanicalIds = ids(resolver.resolve(new EditorState(
                document,
                new DiagramEditingSelection(0, new DiagramElementTarget(1, mechanicalId)),
                Optional.empty()), allActions()));

        assertTrue(electricalIds.contains(EditorActionId.DIAGRAM_ROTATE_CLOCKWISE));
        assertTrue(electricalIds.contains(EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT));
        assertFalse(electricalIds.contains(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE));
        assertTrue(mechanicalIds.contains(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE));
        assertFalse(mechanicalIds.contains(EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT));
    }

    @Test
    void diagramMenuResolvesDiagramContainedByFigure() {
        var componentId = new DiagramElementId("r1");
        var diagram = diagramBlock(new ElectricalComponent(
                componentId,
                new DiagramBounds(10, 10, 40, 20),
                ElectricalComponentKind.RESISTOR,
                ElectricalOrientation.DEG_0,
                "R1",
                "1k"));
        var state = new EditorState(
                document(new FigureBlock("fig-circuit", diagram, inline("Circuit"))),
                new DiagramEditingSelection(0, new DiagramElementTarget(0, componentId)),
                Optional.empty());

        var ids = assertDoesNotThrow(() -> ids(resolver.resolve(state, allActions())));

        assertTrue(ids.contains(EditorActionId.DIAGRAM_ROTATE_CLOCKWISE));
        assertTrue(ids.contains(EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT));
        assertFalse(ids.contains(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE));
    }

    @Test
    void figureCaptionMenuUsesTextClipboardAndFormatting() {
        var state = new EditorState(
                document(new FigureBlock("fig-1", plotBlock(), inline("caption"))),
                new FigureCaptionSelection(0, 0, 3),
                Optional.empty());

        assertIds(resolver.resolve(state, allActions()),
                EditorActionId.CUT,
                EditorActionId.COPY,
                EditorActionId.PASTE,
                EditorActionId.BOLD,
                EditorActionId.ITALIC);
    }

    @Test
    void emptyAreaMenuOnlyOffersPasteWhenAvailable() {
        assertIds(resolver.resolveEmptyArea(new EditorState(document(paragraph("abc")), new DocumentPosition(0, 0)), allActions()),
                EditorActionId.PASTE);
    }

    @Test
    void missingRegisteredActionsAreSkippedWithoutCrashing() {
        var actions = allActions();
        actions.remove(EditorActionId.BOLD);
        var state = new EditorState(document(paragraph("abc")), new DocumentPosition(0, 1), new DocumentPosition(0, 2));

        var ids = assertDoesNotThrow(() -> ids(resolver.resolve(state, actions)));

        assertFalse(ids.contains(EditorActionId.BOLD));
        assertTrue(ids.contains(EditorActionId.ITALIC));
    }

    @Test
    void separatorsAreNotExposedAtEdgesOrRepeatedAfterMissingActions() {
        var actions = new HashMap<EditorActionId, EditorAction>();
        actions.put(EditorActionId.PASTE, BuiltInEditorActions.paste());
        var state = new EditorState(document(paragraph("abc")), new DocumentPosition(0, 1), new DocumentPosition(0, 2));

        var entries = resolver.resolve(state, actions);

        assertIds(entries, EditorActionId.PASTE);
        assertTrue(entries.stream().noneMatch(entry -> entry.kind() == ContextMenuEntryKind.SEPARATOR));
    }

    private static EditorState blockState(Document document, int blockIndex) {
        return new EditorState(document, new BlockSelection(blockIndex), Optional.empty());
    }

    private static void assertIds(List<ContextMenuEntry> entries, EditorActionId... expected) {
        assertEquals(List.of(expected), ids(entries));
    }

    private static List<EditorActionId> ids(List<ContextMenuEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.kind() == ContextMenuEntryKind.ACTION)
                .map(entry -> entry.action().orElseThrow().id())
                .toList();
    }

    private static EditorAction action(List<ContextMenuEntry> entries, EditorActionId id) {
        return entries.stream()
                .filter(entry -> entry.kind() == ContextMenuEntryKind.ACTION)
                .map(entry -> entry.action().orElseThrow())
                .filter(action -> action.id() == id)
                .findFirst()
                .orElseThrow();
    }

    private static Map<EditorActionId, EditorAction> allActions() {
        var map = new HashMap<EditorActionId, EditorAction>();
        Stream.of(
                        BuiltInEditorActions.editMenuActions(),
                        BuiltInEditorActions.insertMenuActions(),
                        BuiltInEditorActions.formatMenuActions(),
                        BuiltInEditorActions.tableMenuActions(),
                        BuiltInEditorActions.plotMenuActions(),
                        BuiltInEditorActions.diagramMenuActions(),
                        BuiltInEditorActions.figureMenuActions(),
                        BuiltInEditorActions.viewMenuActions(),
                        BuiltInEditorActions.dataMenuActions())
                .flatMap(List::stream)
                .forEach(action -> map.putIfAbsent(action.id(), action));
        return map;
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String value) {
        return new Paragraph(inline(value));
    }

    private static Heading heading(int level, String value) {
        return new Heading(level, inline(value));
    }

    private static InlineContent inline(String value) {
        return new InlineContent(List.of(new Text(value, Set.of())).stream().map(InlineNode.class::cast).toList());
    }

    private static PlotBlock plotBlock() {
        return new PlotBlock(PlotDefinition.of(
                "Motion",
                AxisDefinition.linear("t"),
                AxisDefinition.linear("x"),
                List.of(new PlotSeries("v", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))))));
    }

    private static FigureBlock figureBlock() {
        return FigureBlock.emptyCaption("fig-plot", plotBlock());
    }

    private static DiagramBlock diagramBlock(DiagramElement... elements) {
        return new DiagramBlock(new DiagramDefinition(
                "Diagram",
                new DiagramCanvas(200, 120),
                List.of(elements),
                List.of()));
    }
}
