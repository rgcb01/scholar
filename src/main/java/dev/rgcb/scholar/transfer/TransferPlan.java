package dev.rgcb.scholar.transfer;

import java.util.List;
import java.util.Objects;
import java.util.HashSet;

/** Prepared decisions only; context captures original destination for later stale-state preflight. */
public record TransferPlan(DocumentFragment fragment, TransferContext context, boolean sameDocument,
                           IdentityRemapPlan identities, ResourceTransferPlan resources,
                           ReferenceDispositionPlan references, VariableDependencyPlan variableDependencies,
                           AnalysisDependencyPlan analysisDependencies,
                           List<TransferDiagnostic> diagnostics) {
    public TransferPlan(DocumentFragment fragment, TransferContext context, boolean sameDocument,
                        IdentityRemapPlan identities, ResourceTransferPlan resources,
                        ReferenceDispositionPlan references, VariableDependencyPlan variableDependencies,
                        List<TransferDiagnostic> diagnostics) {
        this(fragment, context, sameDocument, identities, resources, references, variableDependencies,
                new AnalysisDependencyPlan(List.of()), diagnostics);
    }

    public TransferPlan {
        fragment = Objects.requireNonNull(fragment, "fragment");
        context = Objects.requireNonNull(context, "context");
        identities = Objects.requireNonNull(identities, "identities");
        resources = Objects.requireNonNull(resources, "resources");
        references = Objects.requireNonNull(references, "references");
        variableDependencies = Objects.requireNonNull(variableDependencies, "variableDependencies");
        analysisDependencies = Objects.requireNonNull(analysisDependencies, "analysisDependencies");
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
        var variableOccurrences = TransferPlanner.variableOccurrences(fragment.content());
        if (variableOccurrences.size() != variableDependencies.decisions().size()) {
            throw new IllegalArgumentException("Incomplete variable dependency plan.");
        }
        for (var i = 0; i < variableOccurrences.size(); i++) {
            var occurrence = variableOccurrences.get(i);
            var decision = variableDependencies.decisions().get(i);
            var sourceKey = new StableIdentityKey(StableIdentityKind.VARIABLE, occurrence.reference().variableId());
            if (!decision.location().equals(occurrence.location()) || !decision.source().equals(occurrence.reference())) {
                throw new IllegalArgumentException("Variable decision must match its exact source occurrence.");
            }
            if (fragment.identities().provided().contains(sourceKey)) {
                if (decision.disposition() != VariableDependencyPlan.Disposition.REMAP_TRAVELING_TARGET
                        || !decision.destination().equals(identities.destinationOf(sourceKey))) {
                    throw new IllegalArgumentException("Traveling variable dependency must use shared remap.");
                }
            } else if (decision.disposition() != VariableDependencyPlan.Disposition.PRESERVE_PROVEN_SAME_DOCUMENT_TARGET
                    || !sameDocument || !inventory.targets().containsKey(sourceKey)
                    || inventory.targets().get(sourceKey) != context.sourceMetadata().targetWitnesses().get(sourceKey)) {
                throw new IllegalArgumentException("External variable dependency requires exact applicable proof.");
            }
        }
        var fitOccurrences = TransferPlanner.analysisOccurrences(fragment.content());
        if (fitOccurrences.size() != analysisDependencies.decisions().size()) {
            throw new IllegalArgumentException("Incomplete fit analysis dependency plan.");
        }
        for (var i = 0; i < fitOccurrences.size(); i++) {
            var occurrence = fitOccurrences.get(i);
            var decision = analysisDependencies.decisions().get(i);
            var key = new StableIdentityKey(StableIdentityKind.ANALYSIS, occurrence.analysisId());
            if (!decision.location().equals(occurrence.location()) || !decision.sourceId().equals(occurrence.analysisId())) {
                throw new IllegalArgumentException("Fit decision must match source occurrence.");
            }
            if (fragment.identities().provided().contains(key)) {
                if (decision.disposition() != AnalysisDependencyPlan.Disposition.REMAP_TRAVELING_TARGET
                        || !decision.destination().equals(identities.destinationOf(key))) {
                    throw new IllegalArgumentException("Traveling fit analysis must use shared remap.");
                }
            } else if (decision.disposition() != AnalysisDependencyPlan.Disposition.PRESERVE_PROVEN_SAME_DOCUMENT_TARGET
                    || !sameDocument || inventory.targets().get(key) == null
                    || inventory.targets().get(key) != context.sourceMetadata().targetWitnesses().get(key)) {
                throw new IllegalArgumentException("External fit analysis requires exact applicable proof.");
            }
        }
    }
}
