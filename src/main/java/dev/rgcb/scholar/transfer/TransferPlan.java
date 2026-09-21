package dev.rgcb.scholar.transfer;

import java.util.List;
import java.util.Objects;
import java.util.HashSet;

/** Prepared decisions only; context captures original destination for later stale-state preflight. */
public record TransferPlan(DocumentFragment fragment, TransferContext context, boolean sameDocument,
                           IdentityRemapPlan identities, ResourceTransferPlan resources,
                           ReferenceDispositionPlan references, List<TransferDiagnostic> diagnostics) {
    public TransferPlan {
        fragment = Objects.requireNonNull(fragment, "fragment");
        context = Objects.requireNonNull(context, "context");
        identities = Objects.requireNonNull(identities, "identities");
        resources = Objects.requireNonNull(resources, "resources");
        references = Objects.requireNonNull(references, "references");
        diagnostics = List.copyOf(diagnostics);
        if (sameDocument != context.hasSameDocumentToken()
                || diagnostics.stream().anyMatch(d -> d.severity() == TransferDiagnostic.Severity.ERROR)) {
            throw new IllegalArgumentException("Plan proof/diagnostics must agree with successful context.");
        }
        if (!TransferPlanner.compatible(fragment.content(), context.scope())) {
            throw new IllegalArgumentException("Plan must match destination scope.");
        }
        var required = new HashSet<>(FragmentIdentityIndex.requiredDatasets(fragment.content()));
        if (fragment.content() instanceof FragmentContent.ResourcePrimary primary) { required.addAll(primary.selectedResources()); }
        if (!resources.decisions().keySet().equals(required)) {
            throw new IllegalArgumentException("Plan must account for every required resource, without unrelated entries.");
        }
        var expectedKeys = new HashSet<>(fragment.identities().provided());
        expectedKeys.addAll(required);
        if (!identities.decisions().keySet().equals(expectedKeys)) {
            throw new IllegalArgumentException("Plan must account for every traveling/reused identity.");
        }
        var inventory = TransferPlanner.inventory(context);
        for (var decision : identities.decisions().values()) {
            if (decision.disposition() != IdentityRemapPlan.Disposition.REUSED_EXISTING
                    && inventory.occupied().contains(decision.destination())) {
                throw new IllegalArgumentException("Planned new identity collides with a survivor.");
            }
            if (decision.disposition() == IdentityRemapPlan.Disposition.REUSED_EXISTING
                    && !resources.decisions().containsKey(decision.source())) {
                throw new IllegalArgumentException("Only resource dependencies can reuse identity.");
            }
        }
        for (var entry : resources.decisions().entrySet()) {
            var decision = entry.getValue();
            var identity = identities.decisions().get(entry.getKey());
            var reuse = decision.disposition() == ResourceTransferPlan.Disposition.REUSE_EXISTING_SAME_DOCUMENT;
            if (!decision.destination().equals(identity.destination())
                    || reuse != (identity.disposition() == IdentityRemapPlan.Disposition.REUSED_EXISTING)) {
                throw new IllegalArgumentException("Resource and identity decisions must agree.");
            }
            if (reuse) {
                if (!sameDocument || decision.sourceValue() != context.sourceMetadata().datasetWitnesses().get(entry.getKey())
                        || decision.sourceValue() != inventory.datasets().get(entry.getKey())
                        || fragment.resources().stream().anyMatch(value -> value.id().equals(decision.sourceValue().id())
                        && value != decision.sourceValue())
                        || fragment.content() instanceof FragmentContent.ResourcePrimary) {
                    throw new IllegalArgumentException("Resource reuse requires exact applicable proof, not primary import.");
                }
            } else if (fragment.resources().stream().noneMatch(value -> value == decision.sourceValue())) {
                throw new IllegalArgumentException("New resource decision must retain its carried source snapshot.");
            }
        }
        var occurrences = TransferPlanner.occurrences(fragment.content());
        if (occurrences.size() != references.decisions().size()) { throw new IllegalArgumentException("Incomplete reference plan."); }
        for (var i = 0; i < occurrences.size(); i++) {
            var occurrence = occurrences.get(i);
            var decision = references.decisions().get(i);
            var sourceKey = StableIdentityKey.targetOf(occurrence.reference());
            if (!decision.location().equals(occurrence.location()) || decision.source() != occurrence.reference()) {
                throw new IllegalArgumentException("Reference decision must match its exact source occurrence.");
            }
            if (fragment.identities().provided().contains(sourceKey)) {
                if (decision.disposition() != ReferenceDispositionPlan.Disposition.REMAP_INTERNAL
                        || !decision.destinationTarget().orElseThrow().equals(identities.destinationOf(sourceKey))) {
                    throw new IllegalArgumentException("Internal reference must use shared remap.");
                }
            } else if (decision.disposition() == ReferenceDispositionPlan.Disposition.REMAP_INTERNAL
                    || decision.disposition() == ReferenceDispositionPlan.Disposition.PRESERVE_EXTERNAL_SAME_DOCUMENT
                    && (!sameDocument || !inventory.targets().containsKey(sourceKey)
                    || inventory.targets().get(sourceKey) != context.sourceMetadata().targetWitnesses().get(sourceKey))) {
                throw new IllegalArgumentException("External active reference requires exact applicable proof.");
            }
        }
    }
}
