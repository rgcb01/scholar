package dev.rgcb.scholar.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.clipboard.MathClipboardPayload;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentFragmentTest {
    @Test
    void paragraphRootRetainsSemanticContent() {
        var root = new Paragraph(inline(new Text("abc", Set.of())));
        assertSame(root, roots(fragment(root)).getFirst());
    }

    @Test
    void headingRootRetainsLevelAndSectionIdentity() {
        var root = new Heading("h1", 3, inline(new Text("Title", Set.of())));
        var fragment = fragment(root);
        assertSame(root, roots(fragment).getFirst());
        assertEquals(Set.of(key(StableIdentityKind.SECTION, "h1")), fragment.identities().provided());
    }

    @Test
    void equationRootRetainsStructuralMathAndEquationIdentity() {
        var expression = new MathFraction(new MathIdentifier("x"), new MathNumber("2"));
        var root = new EquationBlock("eq1", expression);
        var fragment = fragment(root);
        assertSame(expression, ((EquationBlock) roots(fragment).getFirst()).expression());
        assertEquals(Set.of(key(StableIdentityKind.EQUATION, "eq1")), fragment.identities().provided());
    }

    @Test
    void tableRootRetainsStructureAndTableIdentity() {
        var root = TableBlock.empty(2, 3).withId("t1");
        var fragment = fragment(root);
        assertSame(root, roots(fragment).getFirst());
        assertEquals(Set.of(key(StableIdentityKind.TABLE, "t1")), fragment.identities().provided());
    }

    @Test
    void plotRootHasNoInventedGlobalIdentity() {
        var root = plot(List.of());
        var fragment = fragment(root);
        assertSame(root, roots(fragment).getFirst());
        assertTrue(fragment.identities().provided().isEmpty());
    }

    @Test
    void diagramRootHasNoInventedGlobalIdentity() {
        var root = diagram();
        var fragment = fragment(root);
        assertSame(root, roots(fragment).getFirst());
        assertTrue(fragment.identities().provided().isEmpty());
        assertEquals(0.75, ((DiagramBlock) roots(fragment).getFirst()).workspaceAspectRatio());
    }

    @Test
    void figureRemainsOneRootWithCaptionAndPlotOwnedInside() {
        var content = plot(List.of());
        var caption = inline(new CrossReference(CrossReferenceTargetKind.FIGURE, "other"));
        var root = new FigureBlock("f1", content, caption);
        var fragment = fragment(root);
        assertEquals(1, roots(fragment).size());
        assertSame(root, roots(fragment).getFirst());
        assertSame(content, root.content());
        assertSame(caption, root.caption());
        assertEquals(Set.of(key(StableIdentityKind.FIGURE, "f1")), fragment.identities().provided());
        assertEquals(Set.of(key(StableIdentityKind.FIGURE, "other")), fragment.identities().referenced());
    }

    @Test
    void figureCanRetainWholeDiagramWithoutGlobalizingItsElements() {
        var root = FigureBlock.emptyCaption("f1", diagram());
        var fragment = fragment(root);
        assertSame(root.content(), ((FigureBlock) roots(fragment).getFirst()).content());
        assertEquals(Set.of(key(StableIdentityKind.FIGURE, "f1")), fragment.identities().provided());
    }

    @Test
    void tocRootContainsOnlySemanticMarker() {
        var root = new TableOfContentsBlock();
        var fragment = fragment(root);
        assertSame(root, roots(fragment).getFirst());
        assertEquals(0, TableOfContentsBlock.class.getRecordComponents().length);
        assertTrue(fragment.identities().provided().isEmpty());
        assertTrue(fragment.identities().referenced().isEmpty());
        assertTrue(fragment.resources().isEmpty());
    }

    @Test
    void orderedMixedBlocksRetainOrderAndOptionalIdsRemainAbsent() {
        var paragraph = new Paragraph(inline());
        var heading = new Heading(1, inline());
        var equation = new EquationBlock(new MathNumber("1"));
        var table = TableBlock.empty(1, 1);
        var fragment = fragment(paragraph, heading, equation, table, plot(List.of()), diagram());
        assertEquals(List.of(paragraph, heading, equation, table, plot(List.of()), diagram()), roots(fragment));
        assertTrue(fragment.identities().provided().isEmpty());
    }

    @Test
    void plainInlineTextIsPreserved() {
        assertInlineMarks(Set.of());
    }

    @Test
    void boldInlineTextIsPreserved() {
        assertInlineMarks(Set.of(TextMark.BOLD));
    }

    @Test
    void italicInlineTextIsPreserved() {
        assertInlineMarks(Set.of(TextMark.ITALIC));
    }

    @Test
    void combinedInlineMarksArePreserved() {
        assertInlineMarks(Set.of(TextMark.BOLD, TextMark.ITALIC));
    }

    @Test
    void mixedInlineOrderSegmentationAndRawReferenceRemainUnchanged() {
        var reference = new CrossReference(CrossReferenceTargetKind.FIGURE, "fig-a");
        var content = inline(new Text("a", Set.of(TextMark.BOLD)), reference, new Text("b", Set.of(TextMark.ITALIC)));
        var fragment = new DocumentFragment(new FragmentContent.InlineSegments(List.of(content)), List.of());
        assertSame(content, ((FragmentContent.InlineSegments) fragment.content()).segments().getFirst());
        assertSame(reference, content.nodes().get(1));
        assertEquals("fig-a", reference.targetId());
        assertEquals(Set.of(key(StableIdentityKind.FIGURE, "fig-a")), fragment.identities().referenced());
        assertTrue(fragment.identities().provided().isEmpty());
    }

    @Test
    void inlineSegmentsPreserveEmptyBoundaryAndNoHeadingIdentity() {
        var empty = inline();
        var fragment = new DocumentFragment(new FragmentContent.InlineSegments(List.of(empty, empty)), List.of());
        assertEquals(2, ((FragmentContent.InlineSegments) fragment.content()).segments().size());
        assertTrue(fragment.identities().provided().isEmpty());
        var noOp = new DocumentFragment(new FragmentContent.InlineSegments(List.of(empty)), List.of());
        assertEquals(1, ((FragmentContent.InlineSegments) noOp.content()).segments().size());
    }

    @Test
    void mathLocalPayloadRemainsDistinctAndIsNotAnEquationBlockRoot() {
        var sequence = new MathSequence(List.of(new MathFraction(new MathNumber("1"), new MathNumber("2"))));
        var payload = new MathClipboardPayload(sequence);
        var fragment = fragment(new EquationBlock(sequence));
        assertSame(sequence, payload.fragment());
        assertSame(sequence, ((EquationBlock) roots(fragment).getFirst()).expression());
        assertFalse(FragmentContent.class.isAssignableFrom(MathClipboardPayload.class));
    }

    @Test
    void datasetResourcePrimaryUsesOneCompleteDatasetAndPreservesSourceIdentity() {
        var dataset = dataset("d1");
        var selected = key(StableIdentityKind.DATASET, "d1");
        var fragment = new DocumentFragment(new FragmentContent.ResourcePrimary(Set.of(selected)), List.of(dataset));
        assertSame(dataset, fragment.resources().getFirst());
        assertEquals(Set.of(selected), fragment.identities().provided());
        assertEquals("column", dataset.columns().getFirst().id());
        assertEquals("row", dataset.rows().getFirst().id().orElseThrow());
    }

    @Test
    void duplicateDatasetIdentityIsRejectedEvenForDifferentValues() {
        var root = new FragmentContent.Blocks(List.of(TableBlock.empty(1, 1)));
        var dataset = dataset("d1");
        assertThrows(IllegalArgumentException.class, () -> new DocumentFragment(root, List.of(dataset, dataset)));
        assertThrows(IllegalArgumentException.class, () -> new DocumentFragment(root,
                List.of(dataset, dataset.withDisplayName("different"))));
    }

    @Test
    void equalDatasetContentWithDistinctIdsIsNotDeduplicated() {
        var first = dataset("d1");
        var second = first.withId("d2");
        var fragment = new DocumentFragment(new FragmentContent.ResourcePrimary(Set.of(
                key(StableIdentityKind.DATASET, "d1"), key(StableIdentityKind.DATASET, "d2"))), List.of(first, second));
        assertEquals(List.of(first, second), fragment.resources());
        assertEquals(first.columns(), second.columns());
        assertEquals(first.rows(), second.rows());
        assertEquals(2, fragment.identities().provided().size());
    }

    @Test
    void tableAndPlotShareOneDatasetWithoutResolvingDerivedViews() {
        var table = new TableBlock(new DatasetTableBinding("d1", List.of("column")));
        var plot = plot(List.of(new PlotSeries("bound", PlotSeriesKind.LINE, new DatasetPlotBinding("d1", "column", "column"))));
        var dataset = dataset("d1");
        var fragment = new DocumentFragment(new FragmentContent.Blocks(List.of(table, plot)), List.of(dataset));
        assertEquals(List.of(dataset), fragment.resources());
        assertSame(table, roots(fragment).getFirst());
        assertSame(plot, roots(fragment).getLast());
        assertEquals(Set.of(key(StableIdentityKind.DATASET, "d1")), fragment.identities().referenced());
        assertTrue(plot.definition().series().getFirst().points().isEmpty());
        assertEquals(table.rows(), ((TableBlock) roots(fragment).getFirst()).rows());
    }

    @Test
    void figurePlotBindingsAreIndexedWithoutUnwrappingFigure() {
        var plot = plot(List.of(new PlotSeries("bound", PlotSeriesKind.LINE, new DatasetPlotBinding("d1", "column", "column"))));
        var fragment = new DocumentFragment(new FragmentContent.Blocks(List.of(FigureBlock.emptyCaption("f1", plot))), List.of(dataset("d1")));
        assertEquals(Set.of(key(StableIdentityKind.DATASET, "d1")), fragment.identities().referenced());
        assertEquals(Set.of(key(StableIdentityKind.FIGURE, "f1"), key(StableIdentityKind.DATASET, "d1")), fragment.identities().provided());
        assertEquals(1, roots(fragment).size());
    }

    @Test
    void tableCellReferencesAreIndexedWithoutChangingCellAst() {
        var cell = new TableCell(new TableCellContent(inline(new CrossReference(CrossReferenceTargetKind.SECTION, "h1"))));
        var table = new TableBlock(List.of(new TableRow(List.of(cell))), 0);
        var fragment = fragment(table);
        assertSame(cell, ((TableBlock) roots(fragment).getFirst()).rows().getFirst().cells().getFirst());
        assertEquals(Set.of(key(StableIdentityKind.SECTION, "h1")), fragment.identities().referenced());
    }

    @Test
    void missingDatasetCanBeRepresentedButIsNotClaimedAsProvided() {
        var fragment = fragment(new TableBlock(new DatasetTableBinding("missing")));
        assertEquals(Set.of(key(StableIdentityKind.DATASET, "missing")), fragment.identities().referenced());
        assertTrue(fragment.identities().provided().isEmpty());
        assertTrue(fragment.resources().isEmpty());
    }

    @Test
    void sameSpellingAcrossGlobalNamespacesIsDistinct() {
        var fragment = fragment(new Heading("id", 1, inline()), new EquationBlock("id", new MathNumber("1")),
                TableBlock.empty(1, 1).withId("id"), FigureBlock.emptyCaption("id", plot(List.of())));
        assertEquals(4, fragment.identities().provided().size());
    }

    @Test
    void duplicateProvidedContentIdentityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> fragment(
                new Heading("h1", 1, inline()), new Heading("h1", 2, inline())));
    }

    @Test
    void repeatedDiagramLocalIdsRemainOwnedByIndependentRoots() {
        var first = diagram();
        var second = diagram();
        var fragment = fragment(first, second);
        assertEquals(first.definition().elements().getFirst().id(), second.definition().elements().getFirst().id());
        assertEquals(2, roots(fragment).size());
        assertTrue(fragment.identities().provided().isEmpty());
    }

    @Test
    void rootAndResourceInputListsCannotMutateSnapshot() {
        var sourceRoots = new ArrayList<BlockNode>(List.of(new Paragraph(inline())));
        var sourceResources = new ArrayList<>(List.of(dataset("d1")));
        var fragment = new DocumentFragment(new FragmentContent.Blocks(sourceRoots), sourceResources);
        sourceRoots.clear();
        sourceResources.clear();
        assertEquals(1, roots(fragment).size());
        assertEquals(1, fragment.resources().size());
        assertThrows(UnsupportedOperationException.class, () -> roots(fragment).clear());
        assertThrows(UnsupportedOperationException.class, () -> fragment.resources().clear());
    }

    @Test
    void inlineNodesMarksAndSegmentListsAreDefensivelyCopied() {
        var marks = new HashSet<>(Set.of(TextMark.BOLD));
        var nodes = new ArrayList<InlineNode>(List.of(new Text("abc", marks)));
        var content = new InlineContent(nodes);
        var segments = new ArrayList<>(List.of(content));
        var fragment = new DocumentFragment(new FragmentContent.InlineSegments(segments), List.of());
        marks.clear();
        nodes.clear();
        segments.clear();
        assertEquals(Set.of(TextMark.BOLD), ((Text) content.nodes().getFirst()).marks());
        assertEquals(1, ((FragmentContent.InlineSegments) fragment.content()).segments().size());
        assertThrows(UnsupportedOperationException.class, () -> content.nodes().clear());
        assertThrows(UnsupportedOperationException.class, () -> ((Text) content.nodes().getFirst()).marks().clear());
        assertThrows(UnsupportedOperationException.class, () -> ((FragmentContent.InlineSegments) fragment.content()).segments().clear());
    }

    @Test
    void nestedDatasetColumnsRowsAndValuesRemainImmutableAfterSourceReplacement() {
        var columns = new ArrayList<>(List.of(new DatasetColumn("column", "Column", DatasetColumnType.NUMBER)));
        var values = new ArrayList<>(List.of(DatasetValue.number("1")));
        var rows = new ArrayList<>(List.of(new DatasetRow("row", values)));
        var dataset = new ScientificDataset("d1", "Data", columns, rows);
        var fragment = new DocumentFragment(new FragmentContent.ResourcePrimary(Set.of(key(StableIdentityKind.DATASET, "d1"))), List.of(dataset));
        columns.clear();
        values.clear();
        rows.clear();
        var edited = dataset.withCell(0, "column", DatasetValue.number("9"));
        assertEquals("1", fragment.resources().getFirst().rows().getFirst().values().getFirst().displayText());
        assertEquals("9", edited.rows().getFirst().values().getFirst().displayText());
        assertThrows(UnsupportedOperationException.class, () -> dataset.columns().clear());
        assertThrows(UnsupportedOperationException.class, () -> dataset.rows().clear());
        assertThrows(UnsupportedOperationException.class, () -> dataset.rows().getFirst().values().clear());
    }

    @Test
    void nestedDiagramElementListIsSnapshottedByExistingAst() {
        var elements = new ArrayList<DiagramElement>(diagram().definition().elements());
        var diagram = new DiagramBlock(new DiagramDefinition("D", new DiagramCanvas(100, 100), elements, List.of()));
        var fragment = fragment(diagram);
        elements.clear();
        assertEquals(1, ((DiagramBlock) roots(fragment).getFirst()).definition().elements().size());
        assertThrows(UnsupportedOperationException.class, () -> diagram.definition().elements().clear());
    }

    @Test
    void diagramLocalPortsAndConnectivityStayInsideCompleteOwner() {
        var portId = new DiagramPortId("p");
        var ports = new ArrayList<>(List.of(new DiagramPort(portId, "Port", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5))));
        var first = new DiagramNode(new DiagramElementId("a"), new DiagramBounds(10, 10, 20, 20), "A", ports);
        var second = new DiagramNode(new DiagramElementId("b"), new DiagramBounds(60, 10, 20, 20), "B", ports);
        var connection = new DiagramConnection(new DiagramEndpoint(first.id(), portId), new DiagramEndpoint(second.id(), portId), "Wire");
        var connections = new ArrayList<>(List.of(connection));
        var owner = new DiagramBlock(new DiagramDefinition("Graph", new DiagramCanvas(100, 100), List.of(first, second), connections));
        var fragment = fragment(owner);
        ports.clear();
        connections.clear();
        assertSame(owner, roots(fragment).getFirst());
        assertEquals(List.of(connection), owner.definition().connections());
        assertEquals(portId, first.ports().getFirst().id());
        assertTrue(fragment.identities().provided().isEmpty());
        assertTrue(fragment.identities().referenced().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> first.ports().clear());
        assertThrows(UnsupportedOperationException.class, () -> owner.definition().connections().clear());
    }

    @Test
    void nestedMathSequenceListIsImmutableWithoutReparsingOrMergingTokens() {
        var atoms = new ArrayList<dev.rgcb.scholar.math.MathExpression>(List.of(new MathNumber("1"), new MathNumber("2")));
        var sequence = new MathSequence(atoms);
        var fraction = new MathFraction(sequence, new MathNumber("3"));
        var fragment = fragment(new EquationBlock(fraction));
        atoms.clear();
        assertSame(fraction, ((EquationBlock) roots(fragment).getFirst()).expression());
        assertEquals(2, sequence.expressions().size());
        assertThrows(UnsupportedOperationException.class, () -> sequence.expressions().clear());
    }

    @Test
    void tableRowAndCellListsRemainImmutable() {
        var cell = new TableCell(new TableCellContent(inline(new Text("cell", Set.of(TextMark.BOLD)))));
        var cells = new ArrayList<>(List.of(cell));
        var rows = new ArrayList<>(List.of(new TableRow(cells)));
        var table = new TableBlock(rows, 1);
        var fragment = fragment(table);
        cells.clear();
        rows.clear();
        assertSame(cell, ((TableBlock) roots(fragment).getFirst()).rows().getFirst().cells().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> table.rows().clear());
        assertThrows(UnsupportedOperationException.class, () -> table.rows().getFirst().cells().clear());
    }

    @Test
    void plotSeriesAndAuthoredPointsAreSnapshottedWithoutResolution() {
        var points = new ArrayList<>(List.of(new dev.rgcb.scholar.plot.DataPoint(1, 2)));
        var series = new ArrayList<>(List.of(new PlotSeries("authored", PlotSeriesKind.SCATTER, points)));
        var plot = plot(series);
        var fragment = fragment(plot);
        points.clear();
        series.clear();
        assertSame(plot, roots(fragment).getFirst());
        assertEquals(List.of(new dev.rgcb.scholar.plot.DataPoint(1, 2)), plot.definition().series().getFirst().points());
        assertThrows(UnsupportedOperationException.class, () -> plot.definition().series().clear());
        assertThrows(UnsupportedOperationException.class, () -> plot.definition().series().getFirst().points().clear());
    }

    @Test
    void emptyRootCategoriesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FragmentContent.Blocks(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new FragmentContent.InlineSegments(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new FragmentContent.ResourcePrimary(Set.of()));
    }

    @Test
    void nullRootsResourcesAndContentAreRejected() {
        assertThrows(NullPointerException.class, () -> new FragmentContent.Blocks(java.util.Arrays.asList((BlockNode) null)));
        assertThrows(NullPointerException.class, () -> new FragmentContent.InlineSegments(java.util.Arrays.asList((InlineContent) null)));
        assertThrows(NullPointerException.class, () -> new DocumentFragment(new FragmentContent.Blocks(List.of(TableBlock.empty(1, 1))),
                java.util.Arrays.asList((ScientificDataset) null)));
        assertThrows(NullPointerException.class, () -> new DocumentFragment(null, List.of()));
        assertThrows(NullPointerException.class, () -> new DocumentFragment(new FragmentContent.Blocks(List.of(TableBlock.empty(1, 1))), null));
    }

    @Test
    void resourcePrimaryRejectsNonResourceAndAbsentResourceKeys() {
        assertThrows(IllegalArgumentException.class, () -> new FragmentContent.ResourcePrimary(Set.of(key(StableIdentityKind.FIGURE, "f1"))));
        assertThrows(IllegalArgumentException.class, () -> new DocumentFragment(
                new FragmentContent.ResourcePrimary(Set.of(key(StableIdentityKind.DATASET, "missing"))), List.of()));
    }

    @Test
    void suppliedIndexMustExactlyMatchAstProvidedAndReferencedKeys() {
        var content = new FragmentContent.Blocks(List.of(new Heading("h1", 1, inline(new CrossReference(CrossReferenceTargetKind.FIGURE, "f1")))));
        var correct = new FragmentIdentityIndex(Set.of(key(StableIdentityKind.SECTION, "h1")), Set.of(key(StableIdentityKind.FIGURE, "f1")));
        assertEquals(correct, new DocumentFragment(content, List.of(), correct).identities());
        assertThrows(IllegalArgumentException.class, () -> new DocumentFragment(content, List.of(), new FragmentIdentityIndex(Set.of(), Set.of())));
        assertThrows(IllegalArgumentException.class, () -> new DocumentFragment(content, List.of(),
                new FragmentIdentityIndex(Set.of(key(StableIdentityKind.SECTION, "h1"), key(StableIdentityKind.FIGURE, "f1")), correct.referenced())));
        assertThrows(IllegalArgumentException.class, () -> new DocumentFragment(content, List.of(),
                new FragmentIdentityIndex(correct.provided(), Set.of())));
    }

    @Test
    void indexSetsAreImmutableAndDefensivelyCopied() {
        var provided = new HashSet<>(Set.of(key(StableIdentityKind.SECTION, "h1")));
        var referenced = new HashSet<>(Set.of(key(StableIdentityKind.FIGURE, "f1")));
        var index = new FragmentIdentityIndex(provided, referenced);
        provided.clear();
        referenced.clear();
        assertEquals(1, index.provided().size());
        assertEquals(1, index.referenced().size());
        assertThrows(UnsupportedOperationException.class, () -> index.provided().clear());
        assertThrows(UnsupportedOperationException.class, () -> index.referenced().clear());
    }

    @Test
    void unknownBlockAndInlineImplementationsAreNotSilentlyTreatedAsSupported() {
        assertThrows(IllegalArgumentException.class, () -> fragment(new BlockNode() {}));
        assertThrows(IllegalArgumentException.class, () -> fragment(new Paragraph(inline(new InlineNode() {}))));
    }

    private static void assertInlineMarks(Set<TextMark> marks) {
        var text = new Text("abc", marks);
        var content = inline(text);
        var fragment = new DocumentFragment(new FragmentContent.InlineSegments(List.of(content)), List.of());
        assertSame(content, ((FragmentContent.InlineSegments) fragment.content()).segments().getFirst());
        assertSame(text, content.nodes().getFirst());
        assertEquals(marks, text.marks());
    }

    private static DocumentFragment fragment(BlockNode... blocks) {
        return new DocumentFragment(new FragmentContent.Blocks(List.of(blocks)), List.of());
    }

    private static List<BlockNode> roots(DocumentFragment fragment) {
        return ((FragmentContent.Blocks) fragment.content()).roots();
    }

    private static InlineContent inline(InlineNode... nodes) {
        return new InlineContent(List.of(nodes));
    }

    private static StableIdentityKey key(StableIdentityKind kind, String id) {
        return new StableIdentityKey(kind, id);
    }

    private static ScientificDataset dataset(String id) {
        return new ScientificDataset(id, "Data", List.of(new DatasetColumn("column", "Column", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow("row", List.of(DatasetValue.number("1")))));
    }

    private static PlotBlock plot(List<PlotSeries> series) {
        return new PlotBlock(PlotDefinition.of("Plot", AxisDefinition.linear("x"), AxisDefinition.linear("y"), series));
    }

    private static DiagramBlock diagram() {
        var node = new DiagramNode(new DiagramElementId("local"), new DiagramBounds(10, 10, 20, 20), "Node", List.of());
        return new DiagramBlock(new DiagramDefinition("Diagram", new DiagramCanvas(100, 100), List.of(node), List.of()), 0.75);
    }
}
