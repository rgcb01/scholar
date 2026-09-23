package dev.rgcb.scholar.transfer;

import static org.junit.jupiter.api.Assertions.*;
import static dev.rgcb.scholar.transfer.ExtractionFixtures.*;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.StableIdAllocator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TransferPlanModelTest {
    private static StableIdentityKey key(String id) { return new StableIdentityKey(StableIdentityKind.DATASET, id); }

    @Test void identityMapIsDefensivelyCopiedAndUnmodifiable() {
        var entries = new HashMap<StableIdentityKey, IdentityRemapPlan.Decision>();
        entries.put(key("a"), new IdentityRemapPlan.Decision(key("a"), key("a"), IdentityRemapPlan.Disposition.PRESERVED));
        var plan = new IdentityRemapPlan(entries);
        entries.clear();
        assertEquals(key("a"), plan.destinationOf(key("a")));
        assertThrows(UnsupportedOperationException.class, () -> plan.decisions().clear());
    }

    @Test void identityDecisionRejectsWrongNamespaceAndDisposition() {
        assertThrows(IllegalArgumentException.class, () -> new IdentityRemapPlan.Decision(key("a"),
                new StableIdentityKey(StableIdentityKind.FIGURE, "a"), IdentityRemapPlan.Disposition.PRESERVED));
        assertThrows(IllegalArgumentException.class, () -> new IdentityRemapPlan.Decision(key("a"), key("a"), IdentityRemapPlan.Disposition.REMAPPED_NEW));
    }

    @Test void identityMapRejectsDuplicateDestinations() {
        assertThrows(IllegalArgumentException.class, () -> new IdentityRemapPlan(Map.of(
                key("a"), new IdentityRemapPlan.Decision(key("a"), key("c"), IdentityRemapPlan.Disposition.REMAPPED_NEW),
                key("b"), new IdentityRemapPlan.Decision(key("b"), key("c"), IdentityRemapPlan.Disposition.REMAPPED_NEW))));
    }

    @Test void resourceMapRetainsImmutableSourceAndCopiesContainer() {
        var value = dataset("a");
        var entries = new HashMap<StableIdentityKey, ResourceTransferPlan.Decision>();
        entries.put(key("a"), new ResourceTransferPlan.Decision(value, key("a-2"), ResourceTransferPlan.Disposition.TRANSFER_AS_NEW));
        var plan = new ResourceTransferPlan(entries);
        entries.clear();
        assertSame(value, plan.decisions().get(key("a")).sourceValue());
        assertThrows(UnsupportedOperationException.class, () -> plan.decisions().clear());
    }

    @Test void referenceContainerIsDefensivelyCopied() {
        var entries = new ArrayList<ReferenceDispositionPlan.Decision>();
        var plan = new ReferenceDispositionPlan(entries);
        entries.clear();
        assertThrows(UnsupportedOperationException.class, () -> plan.decisions().clear());
    }

    @Test void successPlanRetainsOriginalSnapshotsAndImmutableDiagnostics() {
        var destination = new Document(List.of());
        var fragment = new DocumentFragment(new FragmentContent.Blocks(List.of(new Paragraph(inline()))), List.of());
        var result = new TransferPlanner().plan(fragment, new TransferContext(destination, Optional.empty(), SourceTransferMetadata.unknown()));
        var plan = ((PlanningResult.Success) result).plan();
        assertSame(destination, plan.context().destination());
        assertSame(fragment, plan.fragment());
        assertThrows(UnsupportedOperationException.class, () -> plan.diagnostics().clear());
    }

    @Test void incompleteResourcePlanCannotBeConstructed() {
        var value = dataset("a");
        var fragment = new DocumentFragment(new FragmentContent.ResourcePrimary(Set.of(key("a"))), List.of(value));
        var context = new TransferContext(new Document(List.of()), Optional.empty(), SourceTransferMetadata.unknown());
        assertThrows(IllegalArgumentException.class, () -> new TransferPlan(fragment, context, false,
                new IdentityRemapPlan(Map.of()), new ResourceTransferPlan(Map.of()), new ReferenceDispositionPlan(List.of()),
                new VariableDependencyPlan(List.of()), List.of()));
    }

    @Test void allocatorPreservesBaseAndFindsFirstHoleWithoutMutatingReservations() {
        var occupied = Set.of("a", "a-2", "a-4");
        assertEquals("a-3", StableIdAllocator.firstFree("a", occupied));
        assertEquals("A", StableIdAllocator.firstFree("A", occupied));
        assertEquals("a-3", StableIdAllocator.firstFree("a", occupied::contains));
        assertEquals(Set.of("a", "a-2", "a-4"), occupied);
    }
}
