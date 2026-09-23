package dev.rgcb.scholar.transfer;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.ComputationTransferBlock;
import dev.rgcb.scholar.document.VariableDependencyReference;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VariableDependencyTransferTest {
    private final FragmentExtractor extractor = new FragmentExtractor();
    private final TransferPlanner planner = new TransferPlanner();
    private final TransferMaterializer materializer = new TransferMaterializer();

    private record FixtureBlock(Optional<String> definedVariableId, String authoredName,
                                List<VariableDependencyReference> variableDependencies) implements ComputationTransferBlock {
        private FixtureBlock {
            definedVariableId = java.util.Objects.requireNonNull(definedVariableId);
            authoredName = java.util.Objects.requireNonNull(authoredName);
            variableDependencies = List.copyOf(variableDependencies);
        }

        @Override
        public FixtureBlock withVariableTransferIds(Optional<String> definedId, List<VariableDependencyReference> dependencies) {
            return new FixtureBlock(definedId, authoredName, dependencies);
        }
    }

    private static FixtureBlock variable(String id, String name) {
        return new FixtureBlock(Optional.of(id), name, List.of());
    }

    private static FixtureBlock computed(String name, String... dependencies) {
        return new FixtureBlock(Optional.empty(), name,
                java.util.Arrays.stream(dependencies).map(VariableDependencyReference::new).toList());
    }

    private static StableIdentityKey key(String id) {
        return new StableIdentityKey(StableIdentityKind.VARIABLE, id);
    }

    private ExtractionResult.Success extract(Document source, RuntimeDocumentToken token, Integer... indices) {
        return assertInstanceOf(ExtractionResult.Success.class, extractor.extract(source,
                new FragmentExtractionRequest.Blocks(List.of(indices)), Optional.of(token)));
    }

    private TransferPlan plan(ExtractionResult.Success extracted, Document destination, RuntimeDocumentToken destinationToken) {
        return assertInstanceOf(PlanningResult.Success.class, planner.plan(extracted.fragment(),
                new TransferContext(destination, Optional.of(destinationToken), extracted.sourceMetadata()))).plan();
    }

    private List<FixtureBlock> materialized(ExtractionResult.Success extracted, TransferPlan plan) {
        var result = assertInstanceOf(MaterializationResult.Success.class,
                materializer.materialize(extracted.fragment(), plan));
        return assertInstanceOf(FragmentContent.Blocks.class, result.transfer().content()).roots().stream()
                .map(block -> assertInstanceOf(FixtureBlock.class, block)).toList();
    }

    @Test
    void travelingVariableRemapsEveryDependencyOccurrenceUsingStableIdentity() {
        var source = new Document(List.of(variable("var-A", "mass"), computed("energy", "var-A", "var-A")));
        var extracted = extract(source, RuntimeDocumentToken.create(), 0, 1);
        var destination = new Document(List.of(variable("var-A", "different-name")));
        var plan = plan(extracted, destination, RuntimeDocumentToken.create());

        assertEquals(key("var-A-2"), plan.identities().destinationOf(key("var-A")));
        assertEquals(List.of(VariableDependencyPlan.Disposition.REMAP_TRAVELING_TARGET,
                VariableDependencyPlan.Disposition.REMAP_TRAVELING_TARGET),
                plan.variableDependencies().decisions().stream().map(VariableDependencyPlan.Decision::disposition).toList());
        var roots = materialized(extracted, plan);
        assertEquals(Optional.of("var-A-2"), roots.get(0).definedVariableId());
        assertEquals(List.of(new VariableDependencyReference("var-A-2"), new VariableDependencyReference("var-A-2")),
                roots.get(1).variableDependencies());
        assertEquals("mass", roots.get(0).authoredName());
        assertEquals(Optional.of("var-A"), ((FixtureBlock) source.blocks().getFirst()).definedVariableId());
    }

    @Test
    void provenSameDocumentExternalVariableKeepsOriginalIdentity() {
        var source = new Document(List.of(variable("var-A", "mass"), computed("energy", "var-A")));
        var token = RuntimeDocumentToken.create();
        var extracted = extract(source, token, 1);
        var plan = plan(extracted, source, token);

        assertEquals(VariableDependencyPlan.Disposition.PRESERVE_PROVEN_SAME_DOCUMENT_TARGET,
                plan.variableDependencies().decisions().getFirst().disposition());
        assertEquals("var-A", materialized(extracted, plan).getFirst().variableDependencies().getFirst().variableId());
    }

    @Test
    void nameOrIdCollisionWithoutProofRejectsBeforeMaterialization() {
        var source = new Document(List.of(variable("var-A", "mass"), computed("energy", "var-A")));
        var extracted = extract(source, RuntimeDocumentToken.create(), 1);
        for (var destination : List.of(new Document(List.of(variable("var-Z", "mass"))),
                new Document(List.of(variable("var-A", "mass"))), new Document(List.of()))) {
            var before = destination.blocks();
            var failure = assertInstanceOf(PlanningResult.Failure.class, planner.plan(extracted.fragment(),
                    new TransferContext(destination, Optional.of(RuntimeDocumentToken.create()), extracted.sourceMetadata())));
            assertEquals(TransferDiagnostic.Code.UNRESOLVED_EXTERNAL_VARIABLE_DEPENDENCY,
                    failure.diagnostics().getFirst().code());
            assertEquals(Optional.of(key("var-A")), failure.diagnostics().getFirst().sourceIdentity());
            assertSame(before, destination.blocks());
        }
    }

    @Test
    void matchingTokenWithoutTargetWitnessAlsoRejects() {
        var source = new Document(List.of(variable("var-A", "mass"), computed("energy", "var-A")));
        var token = RuntimeDocumentToken.create();
        var extracted = extract(source, token, 1);
        var changedTarget = new Document(List.of(variable("var-A", "renamed"), source.blocks().get(1)));
        var failure = assertInstanceOf(PlanningResult.Failure.class, planner.plan(extracted.fragment(),
                new TransferContext(changedTarget, Optional.of(token), extracted.sourceMetadata())));
        assertEquals(TransferDiagnostic.Code.UNRESOLVED_EXTERNAL_VARIABLE_DEPENDENCY,
                failure.diagnostics().getFirst().code());
    }

    @Test
    void removedExternalVariableIsNotAProvenSurvivor() {
        var source = new Document(List.of(variable("var-A", "mass"), computed("energy", "var-A")));
        var token = RuntimeDocumentToken.create();
        var extracted = extract(source, token, 1);
        var context = new TransferContext(source, Optional.of(token), extracted.sourceMetadata(),
                TransferContext.Scope.BLOCKS, java.util.Set.of(0));
        var failure = assertInstanceOf(PlanningResult.Failure.class, planner.plan(extracted.fragment(), context));
        assertEquals(TransferDiagnostic.Code.UNRESOLVED_EXTERNAL_VARIABLE_DEPENDENCY,
                failure.diagnostics().getFirst().code());
    }

    @Test
    void successfulPlanCannotBeRecastAsUnprovenExternalLink() {
        var source = new Document(List.of(variable("var-A", "mass"), computed("energy", "var-A")));
        var extracted = extract(source, RuntimeDocumentToken.create(), 0, 1);
        var plan = plan(extracted, new Document(List.of(variable("var-A", "other"))), RuntimeDocumentToken.create());
        var occurrence = plan.variableDependencies().decisions().getFirst();
        var forged = new VariableDependencyPlan(List.of(new VariableDependencyPlan.Decision(
                occurrence.location(), occurrence.source(),
                VariableDependencyPlan.Disposition.PRESERVE_PROVEN_SAME_DOCUMENT_TARGET, key("var-A"))));
        assertThrows(IllegalArgumentException.class, () -> new TransferPlan(plan.fragment(), plan.context(),
                plan.sameDocument(), plan.identities(), plan.resources(), plan.references(), forged, plan.diagnostics()));
    }

    @Test
    void oneUnsafeDependencyRejectsEntireCompositeAndDoesNotReserveIds() {
        var source = new Document(List.of(variable("var-A", "mass"), variable("var-B", "height"),
                computed("energy", "var-A", "var-B")));
        var extracted = extract(source, RuntimeDocumentToken.create(), 0, 2);
        var destination = new Document(List.of(variable("var-A", "other")));
        var failure = assertInstanceOf(PlanningResult.Failure.class, planner.plan(extracted.fragment(),
                new TransferContext(destination, Optional.of(RuntimeDocumentToken.create()), extracted.sourceMetadata())));

        assertEquals(TransferDiagnostic.Code.UNRESOLVED_EXTERNAL_VARIABLE_DEPENDENCY,
                failure.diagnostics().getFirst().code());
        assertEquals(Optional.of(key("var-B")), failure.diagnostics().getFirst().sourceIdentity());
        assertEquals(1, destination.blocks().size());
        var safe = extract(source, RuntimeDocumentToken.create(), 0);
        assertEquals(key("var-A-2"), plan(safe, destination, RuntimeDocumentToken.create()).identities().destinationOf(key("var-A")));
    }

    @Test
    void sameInputsProduceSameRemapWithoutNamesAsKeys() {
        var source = new Document(List.of(variable("var-A", "mass"), computed("energy", "var-A")));
        var extracted = extract(source, RuntimeDocumentToken.create(), 0, 1);
        var destination = new Document(List.of(variable("var-A", "mass")));
        var token = RuntimeDocumentToken.create();
        var first = plan(extracted, destination, token);
        var second = plan(extracted, destination, token);
        assertEquals(first.identities(), second.identities());
        assertEquals(first.variableDependencies(), second.variableDependencies());
    }
}
