package dev.rgcb.scholar.transfer;

import static dev.rgcb.scholar.transfer.ExtractionFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.math.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TransferMaterializerTest {
    @Test void internalReferencesAndAllGlobalContentIdsFollowOnePlan() {
        var reference = new CrossReference(CrossReferenceTargetKind.FIGURE, "f");
        var math = new MathFraction(new MathNumber("1"), new MathRoot(new MathIdentifier("x"), Optional.empty()));
        var roots = List.<BlockNode>of(new Heading("h", 1, inline()), new EquationBlock("e", math),
                TableBlock.empty(1, 1).withId("t"), new FigureBlock("f", diagram(), inline(reference)), new Paragraph(inline(reference)));
        var fragment = new DocumentFragment(new FragmentContent.Blocks(roots), List.of());
        var plan = plan(fragment, new TransferContext(new Document(roots), Optional.empty(), SourceTransferMetadata.unknown()));
        var transfer = materialize(fragment, plan);
        var actual = ((FragmentContent.Blocks) transfer.content()).roots();
        assertEquals("h-2", ((Heading) actual.get(0)).id().orElseThrow());
        assertEquals("e-2", ((EquationBlock) actual.get(1)).id().orElseThrow());
        assertSame(math, ((EquationBlock) actual.get(1)).expression());
        assertEquals("t-2", ((TableBlock) actual.get(2)).id().orElseThrow());
        var figure = (FigureBlock) actual.get(3);
        assertEquals("f-2", figure.id());
        assertSame(((FigureBlock) roots.get(3)).content(), figure.content());
        assertEquals(new CrossReference(CrossReferenceTargetKind.FIGURE, "f-2"), figure.caption().nodes().get(0));
        assertEquals(figure.caption(), ((Paragraph) actual.get(4)).content());
        assertEquals("f", reference.targetId());
    }

    @Test void sharedDatasetMaterializesOnceAndBindingsRemainCanonical() {
        var value = dataset("d");
        var fragment = new DocumentFragment(new FragmentContent.Blocks(List.of(boundTable("d"), plot("d", "d"),
                new FigureBlock("f", plot("d"), inline()))), List.of(value));
        var context = new TransferContext(new Document(List.of(), List.of(value)), Optional.empty(), SourceTransferMetadata.unknown());
        var transfer = materialize(fragment, plan(fragment, context));
        assertEquals(1, transfer.resourceAdditions().size());
        assertEquals("d-2", transfer.resourceAdditions().get(0).id());
        assertEquals(value.columns(), transfer.resourceAdditions().get(0).columns());
        assertEquals(value.rows(), transfer.resourceAdditions().get(0).rows());
        var roots = ((FragmentContent.Blocks) transfer.content()).roots();
        assertEquals("d-2", ((TableBlock) roots.get(0)).datasetBinding().orElseThrow().datasetId());
        for (var series : ((PlotBlock) roots.get(1)).definition().series()) {
            assertEquals("d-2", series.datasetBinding().orElseThrow().datasetId());
            assertTrue(series.points().isEmpty());
        }
        assertEquals("d-2", ((PlotBlock) ((FigureBlock) roots.get(2)).content()).definition().series().get(0).datasetBinding().orElseThrow().datasetId());
        assertThrows(UnsupportedOperationException.class, () -> transfer.resourceAdditions().clear());
    }

    @Test void exactSameDocumentDependencyReuseCreatesNoResourceAddition() {
        var value = dataset("d");
        var token = RuntimeDocumentToken.create();
        var fragment = new DocumentFragment(new FragmentContent.Blocks(List.of(boundTable("d"))), List.of(value));
        var key = new StableIdentityKey(StableIdentityKind.DATASET, "d");
        var metadata = new SourceTransferMetadata(Optional.of(token), Map.of(), Map.of(key, value), Map.of());
        var transfer = materialize(fragment, plan(fragment, new TransferContext(new Document(List.of(), List.of(value)), Optional.of(token), metadata)));
        assertTrue(transfer.resourceAdditions().isEmpty());
        assertEquals("d", ((TableBlock) ((FragmentContent.Blocks) transfer.content()).roots().get(0)).datasetBinding().orElseThrow().datasetId());
    }

    @Test void referenceDegradationPreservesSurroundingRunsAndUsesCapturedExport() {
        var left = new Text("left", Set.of(TextMark.BOLD));
        var right = new Text("right", Set.of(TextMark.ITALIC));
        var ref = new CrossReference(CrossReferenceTargetKind.FIGURE, "f");
        var content = inline(left, ref, right);
        var fragment = new DocumentFragment(new FragmentContent.InlineSegments(List.of(content)), List.of());
        var metadata = new SourceTransferMetadata(Optional.empty(), Map.of(), Map.of(), Map.of(StableIdentityKey.targetOf(ref), "Figure 7"));
        var transfer = materialize(fragment, plan(fragment, new TransferContext(new Document(List.of()), Optional.empty(), metadata)));
        var nodes = ((FragmentContent.InlineSegments) transfer.content()).segments().get(0).nodes();
        assertSame(left, nodes.get(0));
        assertEquals(new Text("Figure 7", Set.of()), nodes.get(1));
        assertSame(right, nodes.get(2));
        assertSame(ref, content.nodes().get(1));
        assertTrue(transfer.plan().diagnostics().stream().anyMatch(d -> d.code() == TransferDiagnostic.Code.EXTERNAL_REFERENCE_DEGRADED));
    }

    @Test void authoredCellReferencesUseTheirOccurrenceDecision() {
        var ref = new CrossReference(CrossReferenceTargetKind.SECTION, "h");
        var table = new TableBlock(List.of(new TableRow(List.of(new TableCell(new TableCellContent(inline(ref, ref)))))), 0);
        var fragment = new DocumentFragment(new FragmentContent.Blocks(List.of(table)), List.of());
        var transfer = materialize(fragment, plan(fragment, new TransferContext(new Document(List.of()), Optional.empty(), SourceTransferMetadata.unknown())));
        var actual = (TableBlock) ((FragmentContent.Blocks) transfer.content()).roots().get(0);
        assertEquals(List.of(new Text("[Missing reference]", Set.of()), new Text("[Missing reference]", Set.of())), actual.rows().get(0).cells().get(0).content().content().nodes());
    }

    @Test void resourcePrimaryRewritesSelectedKeyAndDoesNotCreateAView() {
        var value = dataset("d");
        var fragment = new DocumentFragment(new FragmentContent.ResourcePrimary(Set.of(new StableIdentityKey(StableIdentityKind.DATASET, "d"))), List.of(value));
        var transfer = materialize(fragment, plan(fragment, new TransferContext(new Document(List.of(), List.of(value)), Optional.empty(), SourceTransferMetadata.unknown())));
        assertEquals(Set.of(new StableIdentityKey(StableIdentityKind.DATASET, "d-2")), ((FragmentContent.ResourcePrimary) transfer.content()).selectedResources());
        assertEquals(1, transfer.resourceAdditions().size());
    }

    @Test void tocAndDiagramRemainExactOwnedValues() {
        var graph = diagram();
        var toc = new TableOfContentsBlock();
        var fragment = new DocumentFragment(new FragmentContent.Blocks(List.of(graph, toc)), List.of());
        var transfer = materialize(fragment, plan(fragment, new TransferContext(new Document(List.of()), Optional.empty(), SourceTransferMetadata.unknown())));
        var roots = ((FragmentContent.Blocks) transfer.content()).roots();
        assertSame(graph, roots.get(0));
        assertSame(toc, roots.get(1));
        assertTrue(transfer.plan().identities().decisions().isEmpty());
    }

    @Test void mismatchedFragmentRejectsWithoutPartialOutput() {
        var fragment = new DocumentFragment(new FragmentContent.Blocks(List.of(new Paragraph(inline()))), List.of());
        var plan = plan(fragment, new TransferContext(new Document(List.of()), Optional.empty(), SourceTransferMetadata.unknown()));
        var copy = new DocumentFragment(fragment.content(), fragment.resources());
        var result = new TransferMaterializer().materialize(copy, plan);
        assertInstanceOf(MaterializationResult.Failure.class, result);
        assertInstanceOf(MaterializationResult.Failure.class, new TransferMaterializer().materialize(fragment, null));
    }

    private static TransferPlan plan(DocumentFragment fragment, TransferContext context) {
        return assertInstanceOf(PlanningResult.Success.class, new TransferPlanner().plan(fragment, context)).plan();
    }
    private static MaterializedTransfer materialize(DocumentFragment fragment, TransferPlan plan) {
        return assertInstanceOf(MaterializationResult.Success.class, new TransferMaterializer().materialize(fragment, plan)).transfer();
    }
}
