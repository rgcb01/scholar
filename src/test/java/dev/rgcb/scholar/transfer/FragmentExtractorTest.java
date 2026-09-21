package dev.rgcb.scholar.transfer;

import static dev.rgcb.scholar.transfer.ExtractionFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.LayoutSectionBreak;
import dev.rgcb.scholar.document.ColumnLayout;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathNumber;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class FragmentExtractorTest {
    private final FragmentExtractor extractor = new FragmentExtractor();

    static Stream<BlockNode> roots() {
        return Stream.of(new Paragraph(inline(new Text("abc", Set.of()))), new Heading("h", 2, inline()),
                new EquationBlock("eq", new MathFraction(new MathNumber("1"), new MathNumber("2"))),
                TableBlock.empty(2, 3).withId("table"), plot(), diagram(), FigureBlock.emptyCaption("fig", plot()),
                FigureBlock.emptyCaption("fig", diagram()), new TableOfContentsBlock(), new LayoutSectionBreak(ColumnLayout.two()));
    }

    @ParameterizedTest
    @MethodSource("roots")
    void wholeRootsPreserveExactSemanticValues(BlockNode root) {
        var success = success(extractor.extract(new Document(List.of(root)), blocks(0)));
        assertSame(root, ((FragmentContent.Blocks) success.fragment().content()).roots().getFirst());
        assertTrue(success.fragment().resources().isEmpty());
        assertEquals(FragmentIdentityIndex.derive(success.fragment().content(), List.of()), success.fragment().identities());
    }

    @Test
    void explicitBlockCollectionExtractsInDocumentOrder() {
        var source = new Document(List.of(new Heading("h", 1, inline()), new Paragraph(inline()), new EquationBlock("eq", new MathNumber("1"))));
        var fragment = success(extractor.extract(source, blocks(2, 0))).fragment();
        assertEquals(List.of(source.blocks().get(0), source.blocks().get(2)), ((FragmentContent.Blocks) fragment.content()).roots());
        assertEquals(Set.of(key(StableIdentityKind.SECTION, "h"), key(StableIdentityKind.EQUATION, "eq")), fragment.identities().provided());
    }

    @Test
    void boundTableCarriesWholeDatasetWithoutResolvingStoredCells() {
        var table = boundTable("d");
        var dataset = dataset("d");
        var success = success(extractor.extract(new Document(List.of(table), List.of(dataset, dataset("unrelated"))), blocks(0)));
        assertSame(table, ((FragmentContent.Blocks) success.fragment().content()).roots().getFirst());
        assertEquals(List.of(dataset), success.fragment().resources());
        assertSame(dataset, success.sourceMetadata().datasetWitnesses().get(key(StableIdentityKind.DATASET, "d")));
        assertEquals(Set.of(key(StableIdentityKind.TABLE, "table"), key(StableIdentityKind.DATASET, "d")), success.fragment().identities().provided());
    }

    @Test
    void plotDiscoversMultipleDatasetsInFirstDiscoveryOrderAndDeduplicatesSeries() {
        var plot = plot("d2", "d1", "d2");
        var d1 = dataset("d1");
        var d2 = dataset("d2");
        var fragment = success(extractor.extract(new Document(List.of(plot), List.of(d1, d2)), blocks(0))).fragment();
        assertEquals(List.of(d2, d1), fragment.resources());
        assertTrue(plot.definition().series().getFirst().points().isEmpty());
        assertEquals(Set.of(key(StableIdentityKind.DATASET, "d1"), key(StableIdentityKind.DATASET, "d2")), fragment.identities().provided());
    }

    @Test
    void tableAndPlotShareOneResourceEntry() {
        var source = new Document(List.of(boundTable("d"), plot("d")), List.of(dataset("d")));
        var fragment = success(extractor.extract(source, blocks(0, 1))).fragment();
        assertEquals(source.datasets(), fragment.resources());
        assertEquals(1, fragment.resources().size());
    }

    @Test
    void figurePlotAndTableShareOneResourceWithoutDuplicatingFigureContentRoot() {
        var caption = inline(new CrossReference(CrossReferenceTargetKind.SECTION, "outside"));
        var figure = new FigureBlock("fig", plot("d"), caption);
        var source = new Document(List.of(figure, boundTable("d")), List.of(dataset("d")));
        var fragment = success(extractor.extract(source, blocks(0, 1))).fragment();
        assertEquals(2, ((FragmentContent.Blocks) fragment.content()).roots().size());
        assertSame(figure, ((FragmentContent.Blocks) fragment.content()).roots().getFirst());
        assertSame(caption, figure.caption());
        assertEquals(1, fragment.resources().size());
        assertTrue(fragment.identities().provided().contains(key(StableIdentityKind.FIGURE, "fig")));
    }

    @Test
    void missingTableDatasetFailsWithoutPartialFragment() {
        assertMissing(new Document(List.of(boundTable("missing"))), blocks(0));
    }

    @Test
    void missingOneOfSeveralPlotDatasetsFailsEntireExtraction() {
        assertMissing(new Document(List.of(plot("present", "missing")), List.of(dataset("present"))), blocks(0));
    }

    @Test
    void missingFigurePlotDatasetFailsEntireExtraction() {
        assertMissing(new Document(List.of(FigureBlock.emptyCaption("fig", plot("missing")))), blocks(0));
    }

    @Test
    void resourcePrimaryCopiesOnlyExplicitDatasetsAndNeverConsumers() {
        var source = new Document(List.of(boundTable("d"), plot("d")), List.of(dataset("d"), dataset("other")));
        var fragment = success(extractor.extract(source, new FragmentExtractionRequest.Datasets(List.of("d")))).fragment();
        assertInstanceOf(FragmentContent.ResourcePrimary.class, fragment.content());
        assertEquals(List.of(source.datasets().getFirst()), fragment.resources());
        assertEquals(Set.of(key(StableIdentityKind.DATASET, "d")), fragment.identities().provided());
    }

    @Test
    void resourcePrimaryMissingDatasetFails() {
        assertMissing(new Document(List.of()), new FragmentExtractionRequest.Datasets(List.of("missing")));
    }

    @Test
    void referenceAndTargetTravelUnchangedWhileIndexRecordsActualInclusion() {
        var reference = new CrossReference(CrossReferenceTargetKind.SECTION, "h");
        var heading = new Heading("h", 1, inline());
        var paragraph = new Paragraph(inline(reference));
        var source = new Document(List.of(heading, paragraph));
        var together = success(extractor.extract(source, blocks(0, 1)));
        var alone = success(extractor.extract(source, blocks(1)));
        assertTrue(together.fragment().identities().provided().contains(key(StableIdentityKind.SECTION, "h")));
        assertFalse(alone.fragment().identities().provided().contains(key(StableIdentityKind.SECTION, "h")));
        assertSame(reference, paragraph.content().nodes().getFirst());
        assertSame(heading, alone.sourceMetadata().targetWitnesses().get(key(StableIdentityKind.SECTION, "h")));
        assertEquals("Section 1", alone.sourceMetadata().referenceText().get(key(StableIdentityKind.SECTION, "h")));
        assertTrue(alone.fragment().resources().isEmpty());
    }

    @Test
    void missingSourceReferenceRemainsRawWithMissingExportAndNoWitness() {
        var reference = new CrossReference(CrossReferenceTargetKind.FIGURE, "fig-a");
        var paragraph = new Paragraph(inline(reference));
        var result = success(extractor.extract(new Document(List.of(paragraph)), blocks(0)));
        assertSame(reference, paragraph.content().nodes().getFirst());
        assertEquals("[Missing reference]", result.sourceMetadata().referenceText().get(key(StableIdentityKind.FIGURE, "fig-a")));
        assertTrue(result.sourceMetadata().targetWitnesses().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code() == TransferDiagnostic.Code.SOURCE_DISPLAY_UNAVAILABLE));
    }

    @Test
    void tableCellReferenceIsPreservedAndSourceExportCaptured() {
        var reference = new CrossReference(CrossReferenceTargetKind.EQUATION, "eq");
        var cell = new TableCell(new TableCellContent(inline(reference)));
        var table = new TableBlock(List.of(new TableRow(List.of(cell))), 0);
        var source = new Document(List.of(table, new EquationBlock("eq", new MathNumber("1"))));
        var result = success(extractor.extract(source, blocks(0)));
        assertSame(reference, cell.content().content().nodes().getFirst());
        assertTrue(result.sourceMetadata().referenceText().containsKey(key(StableIdentityKind.EQUATION, "eq")));
    }

    @Test
    void tocDoesNotPullHeadingsOrDerivedEntries() {
        var marker = new TableOfContentsBlock();
        var source = new Document(List.of(new Heading("h", 1, inline()), marker));
        var result = success(extractor.extract(source, blocks(1)));
        assertEquals(List.of(marker), ((FragmentContent.Blocks) result.fragment().content()).roots());
        assertTrue(result.fragment().identities().provided().isEmpty());
        assertTrue(result.sourceMetadata().targetWitnesses().isEmpty());
    }

    @Test
    void diagramGraphLocalIdsPortsConnectivityAndWorkspaceRemainUnchanged() {
        var diagram = diagram();
        var result = success(extractor.extract(new Document(List.of(diagram, diagram())), blocks(0, 1)));
        assertSame(diagram, ((FragmentContent.Blocks) result.fragment().content()).roots().getFirst());
        assertEquals("a", diagram.definition().connections().getFirst().source().elementId().value());
        assertEquals("port", diagram.definition().connections().getFirst().source().portId().value());
        assertEquals(0.6, diagram.workspaceAspectRatio());
        assertTrue(result.fragment().identities().provided().isEmpty());
        assertTrue(result.fragment().identities().referenced().isEmpty());
    }

    @Test
    void knownTokenAndWitnessInstancesRemainOutsideFragmentAndUnknownIsSupported() {
        var token = RuntimeDocumentToken.create();
        var source = new Document(List.of(new Heading("h", 1, inline())));
        var result = success(extractor.extract(source, blocks(0), Optional.of(token)));
        assertSame(token, result.sourceMetadata().documentToken().orElseThrow());
        assertSame(source.blocks().getFirst(), result.sourceMetadata().targetWitnesses().get(key(StableIdentityKind.SECTION, "h")));
        assertTrue(new TransferContext(source, Optional.of(token), result.sourceMetadata()).hasSameDocumentToken());
        assertFalse(new TransferContext(source, Optional.of(RuntimeDocumentToken.create()), result.sourceMetadata()).hasSameDocumentToken());
        assertTrue(success(extractor.extract(source, blocks(0))).sourceMetadata().documentToken().isEmpty());
    }

    @Test
    void malformedRequestsFailCleanly() {
        var source = new Document(List.of(new Paragraph(inline())));
        assertFailure(extractor.extract(source, blocks(-1)));
        assertFailure(extractor.extract(source, blocks(1)));
        assertFailure(extractor.extract(source, blocks(0, 0)));
        assertFailure(extractor.extract(source, blocks()));
        assertFailure(extractor.extract(source, new FragmentExtractionRequest.InlineSegments(List.of())));
        assertFailure(extractor.extract(source, new FragmentExtractionRequest.Datasets(List.of("d", "d"))));
        assertFailure(extractor.extract(source, new FragmentExtractionRequest.Datasets(List.of(" "))));
        assertFailure(extractor.extract(null, blocks(0)));
        assertFailure(extractor.extract(source, null));
        assertFailure(extractor.extract(source, blocks(0), null));
    }

    @Test
    void ambiguousSourceIdsFailEvenIfOnlyOneDuplicateTargetIsSelected() {
        var source = new Document(List.of(new Heading("h", 1, inline()), new Heading("h", 2, inline())));
        assertFailure(extractor.extract(source, blocks(0)));
    }

    @Test
    void unsupportedSelectedModelFailsButUnrelatedSourceErrorsDoNotBlockSafeExtraction() {
        var unknown = new BlockNode() {};
        var source = new Document(List.of(new Paragraph(inline()), unknown));
        assertFailure(extractor.extract(source, blocks(1)));
        assertInstanceOf(ExtractionResult.Success.class, extractor.extract(source, blocks(0)));
        assertFailure(extractor.extract(new Document(List.of(new Paragraph(inline(new InlineNode() {})))), blocks(0)));
    }

    @Test
    void missingColumnWarningDoesNotRepairOrRejectAvailableDatasetClosure() {
        var table = new TableBlock(new dev.rgcb.scholar.data.DatasetTableBinding("d", List.of("absent")));
        var source = new Document(List.of(table), List.of(dataset("d")));
        var result = success(extractor.extract(source, blocks(0)));
        assertSame(table, ((FragmentContent.Blocks) result.fragment().content()).roots().getFirst());
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code() == TransferDiagnostic.Code.SOURCE_VALIDATION_WARNING));
        var invalidTable = new TableBlock(new dev.rgcb.scholar.data.DatasetTableBinding("d", List.of("x", "x")));
        assertFailure(extractor.extract(new Document(List.of(invalidTable), List.of(dataset("d"))), blocks(0)));
        var safeRoot = new Paragraph(inline());
        assertInstanceOf(ExtractionResult.Success.class, extractor.extract(
                new Document(List.of(safeRoot, invalidTable), List.of(dataset("d"))), blocks(0)));
    }

    @Test
    void fragmentRemainsOriginalAfterImmutableSourceReplacement() {
        var paragraph = new Paragraph(inline(new Text("old", Set.of(TextMark.BOLD))));
        var source = new Document(List.of(paragraph), List.of(dataset("d")));
        var result = success(extractor.extract(source, blocks(0)));
        var replacement = new Document(List.of(new Paragraph(inline(new Text("new", Set.of())))));
        assertNotEquals(source, replacement);
        assertSame(paragraph, ((FragmentContent.Blocks) result.fragment().content()).roots().getFirst());
        assertEquals("old", ((Text) paragraph.content().nodes().getFirst()).content());
    }

    @Test
    void fixedSeedMixedRootExtractionHasConsistentIdentityAndClosure() {
        var random = new java.util.Random(25002);
        for (var run = 0; run < 75; run++) {
            var nodes = new ArrayList<BlockNode>();
            nodes.add(new Heading("h", 1, inline()));
            nodes.add(new Paragraph(inline(new CrossReference(CrossReferenceTargetKind.SECTION, "h"))));
            nodes.add(boundTable("d"));
            nodes.add(plot("d", "d"));
            nodes.add(FigureBlock.emptyCaption("fig", plot("d")));
            nodes.add(diagram());
            var indices = new ArrayList<Integer>();
            for (var i = 0; i < nodes.size(); i++) { if (random.nextBoolean()) { indices.add(i); } }
            if (indices.isEmpty()) { indices.add(0); }
            var result = success(extractor.extract(new Document(nodes, List.of(dataset("d"))), new FragmentExtractionRequest.Blocks(indices)));
            var fragment = result.fragment();
            assertEquals(FragmentIdentityIndex.derive(fragment.content(), fragment.resources()), fragment.identities());
            assertTrue(fragment.resources().size() <= 1);
            if (fragment.identities().referenced().contains(key(StableIdentityKind.DATASET, "d"))) {
                assertEquals(List.of(dataset("d")), fragment.resources());
            }
            for (var index : indices) { assertSame(nodes.get(index), ((FragmentContent.Blocks) fragment.content()).roots().get(indices.indexOf(index))); }
        }
    }

    private void assertMissing(Document source, FragmentExtractionRequest request) {
        var failure = assertInstanceOf(ExtractionResult.Failure.class, extractor.extract(source, request));
        assertTrue(failure.diagnostics().stream().anyMatch(d -> d.code() == TransferDiagnostic.Code.MISSING_REQUIRED_RESOURCE
                && d.sourceIdentity().orElseThrow().kind() == StableIdentityKind.DATASET));
    }

    private static void assertFailure(ExtractionResult result) {
        var failure = assertInstanceOf(ExtractionResult.Failure.class, result);
        assertTrue(failure.diagnostics().stream().anyMatch(d -> d.severity() == TransferDiagnostic.Severity.ERROR));
    }

    static ExtractionResult.Success success(ExtractionResult result) { return assertInstanceOf(ExtractionResult.Success.class, result); }
    static FragmentExtractionRequest.Blocks blocks(Integer... indices) { return new FragmentExtractionRequest.Blocks(List.of(indices)); }
    static StableIdentityKey key(StableIdentityKind kind, String id) { return new StableIdentityKey(kind, id); }
}
