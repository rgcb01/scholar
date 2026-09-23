package dev.rgcb.scholar.transfer;

import static dev.rgcb.scholar.transfer.ExtractionFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.math.MathNumber;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TransferPlannerTest {
    private final TransferPlanner planner = new TransferPlanner();

    @ParameterizedTest
    @EnumSource(value = StableIdentityKind.class, names = {"SECTION", "EQUATION", "TABLE", "FIGURE"})
    void unusedContentIdsAreExplicitlyPreserved(StableIdentityKind kind) {
        var plan = plan(fragment(root(kind, "a")), new Document(List.of()));
        var decision = plan.identities().decisions().get(key(kind, "a"));
        assertEquals(key(kind, "a"), decision.destination());
        assertEquals(IdentityRemapPlan.Disposition.PRESERVED, decision.disposition());
    }

    @ParameterizedTest
    @EnumSource(value = StableIdentityKind.class, names = {"SECTION", "EQUATION", "TABLE", "FIGURE"})
    void collisionSkipsAllOccupiedSuffixes(StableIdentityKind kind) {
        var plan = plan(fragment(root(kind, "a")), new Document(List.of(root(kind, "a"), root(kind, "a-2"))));
        assertEquals(key(kind, "a-3"), plan.identities().destinationOf(key(kind, "a")));
    }

    @ParameterizedTest
    @EnumSource(value = StableIdentityKind.class, names = {"SECTION", "EQUATION", "TABLE", "FIGURE"})
    void internalReferencesUseSharedTargetRemapWithoutDegradation(StableIdentityKind kind) {
        var target = root(kind, "a");
        var reference = reference(kind, "a");
        var fragment = fragment(target, new Paragraph(inline(reference, reference)));
        var plan = plan(fragment, new Document(List.of(root(kind, "a"))));
        assertEquals(2, plan.references().decisions().size());
        for (var decision : plan.references().decisions()) {
            assertEquals(ReferenceDispositionPlan.Disposition.REMAP_INTERNAL, decision.disposition());
            assertEquals(key(kind, "a-2"), decision.destinationTarget().orElseThrow());
            assertSame(reference, decision.source());
            assertTrue(decision.fallbackText().isEmpty());
        }
        assertTrue(plan.diagnostics().isEmpty());
        assertEquals("a", reference.targetId());
    }

    @Test
    void preReservationDoesNotStealAnotherSourcesUnusedId() {
        var fragment = fragment(root(StableIdentityKind.FIGURE, "a"), root(StableIdentityKind.FIGURE, "a-2"));
        var plan = plan(fragment, new Document(List.of(root(StableIdentityKind.FIGURE, "a"))));
        assertEquals(key(StableIdentityKind.FIGURE, "a-3"), plan.identities().destinationOf(key(StableIdentityKind.FIGURE, "a")));
        assertEquals(key(StableIdentityKind.FIGURE, "a-2"), plan.identities().destinationOf(key(StableIdentityKind.FIGURE, "a-2")));
    }

    @Test
    void namespacesAreIndependentAndSourceIdsAreNotSanitized() {
        var plan = plan(fragment(root(StableIdentityKind.TABLE, "x"), root(StableIdentityKind.FIGURE, "Fig.A")),
                new Document(List.of(root(StableIdentityKind.SECTION, "x"), root(StableIdentityKind.FIGURE, "Fig.A"))));
        assertEquals(key(StableIdentityKind.TABLE, "x"), plan.identities().destinationOf(key(StableIdentityKind.TABLE, "x")));
        assertEquals(key(StableIdentityKind.FIGURE, "Fig.A-2"), plan.identities().destinationOf(key(StableIdentityKind.FIGURE, "Fig.A")));
    }

    @Test
    void matchingTokenDoesNotReuseTravelingContentIdentity() {
        var target = root(StableIdentityKind.SECTION, "h");
        var source = new Document(List.of(target));
        var token = RuntimeDocumentToken.create();
        var extracted = extract(source, token, 0);
        var plan = success(planner.plan(extracted.fragment(), new TransferContext(source, Optional.of(token), extracted.sourceMetadata())));
        assertTrue(plan.sameDocument());
        assertEquals(key(StableIdentityKind.SECTION, "h-2"), plan.identities().destinationOf(key(StableIdentityKind.SECTION, "h")));
    }

    @Test
    void approvedRemovedRootIsExcludedFromReservationsAndWitnessProof() {
        var target = root(StableIdentityKind.FIGURE, "f");
        var token = RuntimeDocumentToken.create();
        var source = new Document(List.of(target, new Paragraph(inline(reference(StableIdentityKind.FIGURE, "f")))));
        var content = extract(source, token, 0);
        var replacementContext = new TransferContext(source, Optional.of(token), content.sourceMetadata(), TransferContext.Scope.BLOCKS, Set.of(0));
        assertEquals(key(StableIdentityKind.FIGURE, "f"), success(planner.plan(content.fragment(), replacementContext)).identities()
                .destinationOf(key(StableIdentityKind.FIGURE, "f")));
        var prose = extract(source, token, 1);
        var proseContext = new TransferContext(source, Optional.of(token), prose.sourceMetadata(), TransferContext.Scope.BLOCKS, Set.of(0));
        assertEquals(ReferenceDispositionPlan.Disposition.DEGRADE_TO_TEXT, success(planner.plan(prose.fragment(), proseContext)).references().decisions().getFirst().disposition());
        assertEquals(2, source.blocks().size());
    }

    @Test
    void crossDocumentDatasetCollisionNeverReusesEvenExactSameObject() {
        var value = dataset("d");
        var source = new Document(List.of(boundTable("d")), List.of(value));
        var extracted = extract(source, RuntimeDocumentToken.create(), 0);
        var destination = new Document(List.of(), List.of(value));
        var plan = success(planner.plan(extracted.fragment(), new TransferContext(destination, Optional.of(RuntimeDocumentToken.create()), extracted.sourceMetadata())));
        assertFalse(plan.sameDocument());
        var decision = plan.resources().decisions().get(key(StableIdentityKind.DATASET, "d"));
        assertEquals(ResourceTransferPlan.Disposition.TRANSFER_AS_NEW, decision.disposition());
        assertEquals(key(StableIdentityKind.DATASET, "d-2"), decision.destination());
        assertSame(value, decision.sourceValue());
    }

    @Test
    void crossDocumentUnusedDatasetIdIsPreservedButStillTransferredAsNew() {
        var fragment = boundFragment(dataset("d"));
        var plan = plan(fragment, new Document(List.of()));
        assertEquals(ResourceTransferPlan.Disposition.TRANSFER_AS_NEW, plan.resources().decisions().get(key(StableIdentityKind.DATASET, "d")).disposition());
        assertEquals(key(StableIdentityKind.DATASET, "d"), plan.identities().destinationOf(key(StableIdentityKind.DATASET, "d")));
    }

    @Test
    void sharedTablePlotAndFigureConsumersHaveOneResourceDecision() {
        var value = dataset("d");
        var roots = List.<BlockNode>of(boundTable("d"), plot("d", "d"), FigureBlock.emptyCaption("f", plot("d")));
        var fragment = new DocumentFragment(new FragmentContent.Blocks(roots), List.of(value));
        var plan = plan(fragment, new Document(List.of(), List.of(value)));
        assertEquals(1, plan.resources().decisions().size());
        assertEquals(key(StableIdentityKind.DATASET, "d-2"), plan.resources().decisions().get(key(StableIdentityKind.DATASET, "d")).destination());
        assertEquals(plan.identities().destinationOf(key(StableIdentityKind.DATASET, "d")), plan.resources().decisions().get(key(StableIdentityKind.DATASET, "d")).destination());
        assertSame(roots.getFirst(), ((FragmentContent.Blocks) plan.fragment().content()).roots().getFirst());
        assertEquals("d", ((TableBlock) roots.getFirst()).datasetBinding().orElseThrow().datasetId());
        assertEquals("x", value.columns().getFirst().id());
    }

    @Test
    void sameDocumentIncludedDependencySnapshotCanReuseWithExactWitness() {
        var value = dataset("d");
        var source = new Document(List.of(boundTable("d")), List.of(value));
        var token = RuntimeDocumentToken.create();
        var extracted = extract(source, token, 0);
        var plan = success(planner.plan(extracted.fragment(), new TransferContext(source, Optional.of(token), extracted.sourceMetadata())));
        assertEquals(ResourceTransferPlan.Disposition.REUSE_EXISTING_SAME_DOCUMENT, plan.resources().decisions().get(key(StableIdentityKind.DATASET, "d")).disposition());
        assertEquals(IdentityRemapPlan.Disposition.REUSED_EXISTING, plan.identities().decisions().get(key(StableIdentityKind.DATASET, "d")).disposition());
        assertEquals(key(StableIdentityKind.DATASET, "d"), plan.identities().destinationOf(key(StableIdentityKind.DATASET, "d")));
    }

    @Test
    void sameDocumentExternalDatasetRequiresExactWitnessAndUnknownNeverProvesReuse() {
        var value = dataset("d");
        var fragment = fragment(boundTable("d"));
        var token = RuntimeDocumentToken.create();
        var metadata = new SourceTransferMetadata(Optional.of(token), Map.of(), Map.of(key(StableIdentityKind.DATASET, "d"), value), Map.of());
        var destination = new Document(List.of(), List.of(value));
        var plan = success(planner.plan(fragment, new TransferContext(destination, Optional.of(token), metadata)));
        assertEquals(ResourceTransferPlan.Disposition.REUSE_EXISTING_SAME_DOCUMENT, plan.resources().decisions().get(key(StableIdentityKind.DATASET, "d")).disposition());
        assertFailure(fragment, new TransferContext(destination, Optional.empty(), metadata), TransferDiagnostic.Code.MISSING_REQUIRED_RESOURCE);
        assertFailure(fragment, new TransferContext(destination, Optional.of(RuntimeDocumentToken.create()), metadata), TransferDiagnostic.Code.MISSING_REQUIRED_RESOURCE);
        assertFailure(fragment, new TransferContext(destination, Optional.of(token), SourceTransferMetadata.unknown()), TransferDiagnostic.Code.MISSING_REQUIRED_RESOURCE);
        assertFailure(fragment, new TransferContext(new Document(List.of()), Optional.of(token), metadata), TransferDiagnostic.Code.MISSING_REQUIRED_RESOURCE);
        assertFailure(fragment, new TransferContext(new Document(List.of(), List.of(dataset("d"))), Optional.of(token), metadata), TransferDiagnostic.Code.MISSING_REQUIRED_RESOURCE);
    }

    @Test
    void rebuiltDatasetLosesReuseProofButAvailableSnapshotCanStillTransfer() {
        var value = dataset("d");
        var token = RuntimeDocumentToken.create();
        var extracted = extract(new Document(List.of(boundTable("d")), List.of(value)), token, 0);
        var destination = new Document(List.of(), List.of(dataset("d")));
        var plan = success(planner.plan(extracted.fragment(), new TransferContext(destination, Optional.of(token), extracted.sourceMetadata())));
        assertEquals(ResourceTransferPlan.Disposition.TRANSFER_AS_NEW, plan.resources().decisions().get(key(StableIdentityKind.DATASET, "d")).disposition());
        assertEquals(key(StableIdentityKind.DATASET, "d-2"), plan.identities().destinationOf(key(StableIdentityKind.DATASET, "d")));
    }

    @Test
    void directResourcePrimaryImportDoesNotReuseSameDocumentDataset() {
        var value = dataset("d");
        var token = RuntimeDocumentToken.create();
        var source = new Document(List.of(), List.of(value));
        var extracted = assertInstanceOf(ExtractionResult.Success.class,
                new FragmentExtractor().extract(source, new FragmentExtractionRequest.Datasets(List.of("d")), Optional.of(token)));
        var plan = success(planner.plan(extracted.fragment(), new TransferContext(source, Optional.of(token), extracted.sourceMetadata())));
        assertEquals(ResourceTransferPlan.Disposition.TRANSFER_AS_NEW, plan.resources().decisions().get(key(StableIdentityKind.DATASET, "d")).disposition());
        assertEquals(key(StableIdentityKind.DATASET, "d-2"), plan.identities().destinationOf(key(StableIdentityKind.DATASET, "d")));
    }

    @Test
    void resourceEqualityNameAndSchemaNeverDeduplicateAndRepeatedImportsStayIndependent() {
        var value = dataset("d");
        var fragment = boundFragment(value);
        var equalOtherId = plan(fragment, new Document(List.of(), List.of(value.withId("other"))));
        assertEquals(key(StableIdentityKind.DATASET, "d"), equalOtherId.identities().destinationOf(key(StableIdentityKind.DATASET, "d")));
        var second = plan(fragment, new Document(List.of(), List.of(dataset("d"), value.withId("d-2"))));
        assertEquals(key(StableIdentityKind.DATASET, "d-3"), second.identities().destinationOf(key(StableIdentityKind.DATASET, "d")));
    }

    @Test
    void externalReferencePreservesOnlyExactApplicableWitnessInSameDocument() {
        var figure = root(StableIdentityKind.FIGURE, "f");
        var reference = reference(StableIdentityKind.FIGURE, "f");
        var token = RuntimeDocumentToken.create();
        var source = new Document(List.of(figure, new Paragraph(inline(reference))));
        var extracted = extract(source, token, 1);
        var plan = success(planner.plan(extracted.fragment(), new TransferContext(source, Optional.of(token), extracted.sourceMetadata())));
        assertEquals(ReferenceDispositionPlan.Disposition.PRESERVE_EXTERNAL_SAME_DOCUMENT, plan.references().decisions().getFirst().disposition());
        assertEquals(key(StableIdentityKind.FIGURE, "f"), plan.references().decisions().getFirst().destinationTarget().orElseThrow());
        for (var destination : List.of(new Document(List.of()), new Document(List.of(root(StableIdentityKind.FIGURE, "f"))))) {
            var degraded = success(planner.plan(extracted.fragment(), new TransferContext(destination, Optional.of(token), extracted.sourceMetadata())));
            assertEquals(ReferenceDispositionPlan.Disposition.DEGRADE_TO_TEXT, degraded.references().decisions().getFirst().disposition());
            assertEquals("Figure 1", degraded.references().decisions().getFirst().fallbackText().orElseThrow());
        }
        assertEquals("f", reference.targetId());
    }

    @Test
    void differentOrUnknownTokensPreventAccidentalRebindingToSameIdOrEqualValue() {
        var figure = root(StableIdentityKind.FIGURE, "f");
        var token = RuntimeDocumentToken.create();
        var source = new Document(List.of(figure, new Paragraph(inline(reference(StableIdentityKind.FIGURE, "f")))));
        var extracted = extract(source, token, 1);
        for (var destinationToken : List.of(Optional.<RuntimeDocumentToken>empty(), Optional.of(RuntimeDocumentToken.create()))) {
            var plan = success(planner.plan(extracted.fragment(), new TransferContext(source, destinationToken, extracted.sourceMetadata())));
            assertFalse(plan.sameDocument());
            assertEquals(ReferenceDispositionPlan.Disposition.DEGRADE_TO_TEXT, plan.references().decisions().getFirst().disposition());
            assertTrue(plan.references().decisions().getFirst().destinationTarget().isEmpty());
        }
        var unknown = new SourceTransferMetadata(Optional.empty(), extracted.sourceMetadata().targetWitnesses(), Map.of(), extracted.sourceMetadata().referenceText());
        assertFalse(success(planner.plan(extracted.fragment(), new TransferContext(source, Optional.of(token), unknown))).sameDocument());
    }

    @Test
    void sourceDisplayAbsentUsesMissingTextAndWarningsWithoutAnySentinelId() {
        var reference = reference(StableIdentityKind.FIGURE, "f");
        var fragment = fragment(new Paragraph(inline(reference)));
        var plan = plan(fragment, new Document(List.of(root(StableIdentityKind.FIGURE, "f"))));
        var decision = plan.references().decisions().getFirst();
        assertSame(reference, decision.source());
        assertEquals("f", reference.targetId());
        assertEquals("[Missing reference]", decision.fallbackText().orElseThrow());
        assertTrue(decision.destinationTarget().isEmpty());
        assertTrue(plan.identities().decisions().isEmpty());
        assertTrue(plan.diagnostics().stream().allMatch(d -> d.severity() == TransferDiagnostic.Severity.WARNING));
        assertTrue(plan.diagnostics().stream().anyMatch(d -> d.code() == TransferDiagnostic.Code.EXTERNAL_REFERENCE_DEGRADED
                && d.sourceIdentity().orElseThrow().equals(key(StableIdentityKind.FIGURE, "f")) && d.fragmentLocation().isPresent()));
    }

    @Test
    void repeatedReferenceOccurrencesInTableCaptionAndInlineGetDistinctPaths() {
        var reference = reference(StableIdentityKind.FIGURE, "f");
        var table = new TableBlock(List.of(new TableRow(List.of(new TableCell(new TableCellContent(inline(reference, reference)))))), 0);
        var figure = new FigureBlock("owner", diagram(), inline(reference));
        var plan = plan(fragment(new Paragraph(inline(reference)), table, figure), new Document(List.of()));
        assertEquals(List.of("roots[0].content.nodes[0]", "roots[1].rows[0].cells[0].nodes[0]",
                "roots[1].rows[0].cells[0].nodes[1]", "roots[2].caption.nodes[0]"), plan.references().decisions().stream().map(ReferenceDispositionPlan.Decision::location).toList());
        var inlineFragment = new DocumentFragment(new FragmentContent.InlineSegments(List.of(inline(reference), inline(reference))), List.of());
        assertEquals(List.of("segments[0].nodes[0]", "segments[1].nodes[0]"), plan(inlineFragment, new Document(List.of())).references().decisions().stream().map(ReferenceDispositionPlan.Decision::location).toList());
    }

    @Test
    void figureDiagramAndTocDoNotCreateNewGlobalOrDerivedIdentities() {
        var diagram = diagram();
        var marker = new TableOfContentsBlock();
        var plan = plan(fragment(FigureBlock.emptyCaption("f", diagram), marker), new Document(List.of(new Heading("h", 1, inline()))));
        assertEquals(Set.of(key(StableIdentityKind.FIGURE, "f")), plan.identities().decisions().keySet());
        assertTrue(plan.resources().decisions().isEmpty());
        assertSame(diagram, ((FigureBlock) ((FragmentContent.Blocks) plan.fragment().content()).roots().getFirst()).content());
        assertEquals("a", diagram.definition().elements().getFirst().id().value());
    }

    @Test
    void failureHasNoPartialPlanAndNeverChangesInputs() {
        var fragment = fragment(boundTable("missing"));
        var destination = new Document(List.of(new Paragraph(inline())));
        var context = context(destination);
        var failure = assertInstanceOf(PlanningResult.Failure.class, planner.plan(fragment, context));
        assertEquals(1, PlanningResult.Failure.class.getRecordComponents().length);
        assertSame(destination, context.destination());
        assertTrue(fragment.resources().isEmpty());
        assertEquals(1, destination.blocks().size());
        assertFailure(fragment, context, TransferDiagnostic.Code.MISSING_REQUIRED_RESOURCE);
        assertInstanceOf(PlanningResult.Failure.class, planner.plan(null, context));
        assertInstanceOf(PlanningResult.Failure.class, planner.plan(fragment, null));
    }

    @Test
    void unrelatedResourceEntriesUnsupportedScopeAndDuplicateDestinationIdsReject() {
        var extra = new DocumentFragment(new FragmentContent.Blocks(List.of(new Paragraph(inline()))), List.of(dataset("extra")));
        assertFailure(extra, context(new Document(List.of())), TransferDiagnostic.Code.INVALID_RESOURCE_CLOSURE);
        var fragment = fragment(new Heading("h", 1, inline()));
        assertFailure(fragment, new TransferContext(new Document(List.of()), Optional.empty(), SourceTransferMetadata.unknown(), TransferContext.Scope.RESOURCES, Set.of()), TransferDiagnostic.Code.UNSUPPORTED_DESTINATION);
        assertFailure(fragment, context(new Document(List.of(new Heading("h", 1, inline()), new Heading("h", 2, inline())))), TransferDiagnostic.Code.MALFORMED_FRAGMENT);
    }

    @Test
    void planningTwiceProducesEqualPlansAndRetainsUnmodifiedOriginalInputs() {
        var fragment = fragment(new Heading("h", 1, inline()), new Paragraph(inline(reference(StableIdentityKind.SECTION, "h"))));
        var destination = new Document(List.of(new Heading("h", 1, inline())));
        var context = context(destination);
        var first = success(planner.plan(fragment, context));
        assertEquals(first, success(planner.plan(fragment, context)));
        assertSame(fragment, first.fragment());
        assertSame(destination, first.context().destination());
        assertEquals("h", ((Heading) ((FragmentContent.Blocks) fragment.content()).roots().getFirst()).id().orElseThrow());
        assertEquals("h", ((Heading) destination.blocks().getFirst()).id().orElseThrow());
    }

    @Test
    void fixedSeedCollisionPlanningIsDeterministicUniqueAndCollisionFree() {
        var random = new java.util.Random(25003);
        for (var run = 0; run < 75; run++) {
            var roots = new ArrayList<BlockNode>();
            var existing = new ArrayList<BlockNode>();
            for (var i = 0; i < 12; i++) {
                var id = i == 0 ? "h" : "h-" + (i + 1);
                roots.add(new Heading(id, 1, inline()));
                if (random.nextBoolean()) { existing.add(new Heading(id, 2, inline())); }
            }
            var fragment = new DocumentFragment(new FragmentContent.Blocks(roots), List.of());
            var destination = new Document(existing);
            var first = plan(fragment, destination);
            assertEquals(first, plan(fragment, destination));
            var allocated = new HashSet<StableIdentityKey>();
            var occupied = new HashSet<String>();
            existing.forEach(block -> occupied.add(((Heading) block).id().orElseThrow()));
            first.identities().decisions().values().forEach(decision -> {
                assertTrue(allocated.add(decision.destination()));
                assertFalse(occupied.contains(decision.destination().id()));
            });
            assertEquals(roots.size(), allocated.size());
        }
    }

    private TransferPlan plan(DocumentFragment fragment, Document destination) { return success(planner.plan(fragment, context(destination))); }
    private void assertFailure(DocumentFragment fragment, TransferContext context, TransferDiagnostic.Code code) {
        assertEquals(code, assertInstanceOf(PlanningResult.Failure.class, planner.plan(fragment, context)).diagnostics().getFirst().code());
    }
    static TransferPlan success(PlanningResult result) { return assertInstanceOf(PlanningResult.Success.class, result).plan(); }
    static TransferContext context(Document destination) { return new TransferContext(destination, Optional.empty(), SourceTransferMetadata.unknown()); }
    static DocumentFragment fragment(BlockNode... roots) { return new DocumentFragment(new FragmentContent.Blocks(List.of(roots)), List.of()); }
    static DocumentFragment boundFragment(ScientificDataset value) { return new DocumentFragment(new FragmentContent.Blocks(List.of(boundTable(value.id()))), List.of(value)); }
    static StableIdentityKey key(StableIdentityKind kind, String id) { return new StableIdentityKey(kind, id); }
    private static ExtractionResult.Success extract(Document source, RuntimeDocumentToken token, Integer... indices) {
        return assertInstanceOf(ExtractionResult.Success.class, new FragmentExtractor().extract(source,
                new FragmentExtractionRequest.Blocks(List.of(indices)), Optional.of(token)));
    }
    private static CrossReference reference(StableIdentityKind kind, String id) {
        var targetKind = switch (kind) {
            case SECTION -> CrossReferenceTargetKind.SECTION;
            case FIGURE -> CrossReferenceTargetKind.FIGURE;
            case TABLE -> CrossReferenceTargetKind.TABLE;
            case EQUATION -> CrossReferenceTargetKind.EQUATION;
            case DATASET -> throw new IllegalArgumentException("Dataset is not a cross-reference target.");
            case VARIABLE -> throw new IllegalArgumentException("Variable is not a cross-reference target.");
            case ANALYSIS -> throw new IllegalArgumentException("Analysis is not a cross-reference target.");
        };
        return new CrossReference(targetKind, id);
    }
    private static BlockNode root(StableIdentityKind kind, String id) {
        return switch (kind) {
            case SECTION -> new Heading(id, 1, inline());
            case EQUATION -> new EquationBlock(id, new MathNumber("1"));
            case TABLE -> TableBlock.empty(1, 1).withId(id);
            case FIGURE -> FigureBlock.emptyCaption(id, plot());
            case DATASET -> throw new IllegalArgumentException("Dataset is not a block root.");
            case VARIABLE -> throw new IllegalArgumentException("Variable requires computation content.");
            case ANALYSIS -> throw new IllegalArgumentException("Analysis requires dataset content.");
        };
    }
}
