package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolution;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.StableIdAllocator;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.validation.DocumentDiagnosticCode;
import dev.rgcb.scholar.validation.DocumentDiagnosticSeverity;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Pure, operation-local planning; no semantic AST rewrites, insertion or committed reservations. */
public final class TransferPlanner {
    public PlanningResult plan(DocumentFragment fragment, TransferContext context) {
        if (fragment == null || context == null) {
            return failure(TransferDiagnostic.Code.MALFORMED_FRAGMENT, "Fragment/context must not be null.");
        }
        try {
            return checkedPlan(fragment, context);
        } catch (IllegalArgumentException exception) {
            return failure(TransferDiagnostic.Code.MALFORMED_FRAGMENT, exception.getMessage());
        } catch (IllegalStateException exception) {
            return failure(TransferDiagnostic.Code.IDENTITY_REMAP_FAILURE, exception.getMessage());
        }
    }

    private PlanningResult checkedPlan(DocumentFragment fragment, TransferContext context) {
        var content = fragment.content();
        if (!compatible(content, context.scope())) {
            return failure(TransferDiagnostic.Code.UNSUPPORTED_DESTINATION, "Fragment does not match destination semantic scope.");
        }
        var inventory = inventory(context);
        var diagnostics = new ArrayList<TransferDiagnostic>();
        var carried = new LinkedHashMap<StableIdentityKey, ScientificDataset>();
        fragment.resources().forEach(value -> carried.put(datasetKey(value.id()), value));
        var needed = new LinkedHashSet<>(FragmentIdentityIndex.requiredDatasets(content));
        var primary = content instanceof FragmentContent.ResourcePrimary resource ? resource.selectedResources() : Set.<StableIdentityKey>of();
        needed.addAll(primary);
        if (!needed.containsAll(carried.keySet())) {
            return failure(TransferDiagnostic.Code.INVALID_RESOURCE_CLOSURE, "Fragment contains unrelated resource entries.");
        }

        var reused = new HashSet<StableIdentityKey>();
        var resourceValues = new LinkedHashMap<StableIdentityKey, ScientificDataset>();
        // Resource list order controls carried identity allocation; absent dependencies follow discovery order.
        var resourceOrder = new LinkedHashSet<>(carried.keySet());
        resourceOrder.addAll(needed);
        for (var key : resourceOrder) {
            var snapshot = carried.get(key);
            var witness = context.sourceMetadata().datasetWitnesses().get(key);
            var current = inventory.datasets().get(key);
            var applicable = !primary.contains(key) && context.hasSameDocumentToken() && witness != null
                    && current == witness && (snapshot == null || snapshot == witness);
            if (applicable) {
                reused.add(key);
                resourceValues.put(key, current);
            } else if (snapshot != null) {
                resourceValues.put(key, snapshot);
            } else {
                diagnostics.add(diagnostic(TransferDiagnostic.Severity.ERROR, TransferDiagnostic.Code.MISSING_REQUIRED_RESOURCE,
                        "Required resource lacks a snapshot or applicable same-document witness.", Optional.of(key), Optional.empty()));
            }
        }
        if (hasErrors(diagnostics)) { return new PlanningResult.Failure(diagnostics); }

        List<BlockNode> validationRoots = content instanceof FragmentContent.Blocks blocks ? blocks.roots()
                : content instanceof FragmentContent.InlineSegments inline
                ? inline.segments().stream().<BlockNode>map(Paragraph::new).toList() : List.of();
        var validation = DocumentValidator.validate(new Document(validationRoots, new ArrayList<>(resourceValues.values())));
        for (var issue : validation.diagnostics()) {
            if (issue.code() == DocumentDiagnosticCode.MISSING_CROSS_REFERENCE_TARGET) { continue; }
            var error = issue.severity() == DocumentDiagnosticSeverity.ERROR;
            diagnostics.add(diagnostic(error ? TransferDiagnostic.Severity.ERROR : TransferDiagnostic.Severity.WARNING,
                    error ? TransferDiagnostic.Code.MALFORMED_FRAGMENT : TransferDiagnostic.Code.SOURCE_VALIDATION_WARNING,
                    issue.code() + ": " + issue.message(), Optional.empty(), Optional.empty()));
        }
        if (hasErrors(diagnostics)) { return new PlanningResult.Failure(diagnostics); }

        var orderedKeys = new LinkedHashSet<StableIdentityKey>();
        if (content instanceof FragmentContent.Blocks blocks) {
            blocks.roots().forEach(block -> FragmentIdentityIndex.blockIdentity(block).ifPresent(orderedKeys::add));
        }
        orderedKeys.addAll(resourceOrder);
        var reserved = new HashSet<>(inventory.occupied());
        var preserve = new HashSet<StableIdentityKey>();
        for (var key : orderedKeys) {
            if (!reused.contains(key) && !inventory.occupied().contains(key)) {
                preserve.add(key);
                reserved.add(key);
            }
        }
        var identities = new LinkedHashMap<StableIdentityKey, IdentityRemapPlan.Decision>();
        for (var key : orderedKeys) {
            StableIdentityKey destination;
            IdentityRemapPlan.Disposition disposition;
            if (reused.contains(key)) {
                destination = key;
                disposition = IdentityRemapPlan.Disposition.REUSED_EXISTING;
            } else if (preserve.contains(key)) {
                destination = key;
                disposition = IdentityRemapPlan.Disposition.PRESERVED;
            } else {
                var id = StableIdAllocator.firstFree(key.id(), candidate -> reserved.contains(new StableIdentityKey(key.kind(), candidate)));
                destination = new StableIdentityKey(key.kind(), id);
                reserved.add(destination);
                disposition = IdentityRemapPlan.Disposition.REMAPPED_NEW;
            }
            identities.put(key, new IdentityRemapPlan.Decision(key, destination, disposition));
        }
        var remap = new IdentityRemapPlan(identities);
        var resources = new LinkedHashMap<StableIdentityKey, ResourceTransferPlan.Decision>();
        resourceValues.forEach((key, value) -> resources.put(key, new ResourceTransferPlan.Decision(value, remap.destinationOf(key),
                reused.contains(key) ? ResourceTransferPlan.Disposition.REUSE_EXISTING_SAME_DOCUMENT : ResourceTransferPlan.Disposition.TRANSFER_AS_NEW)));

        var references = new ArrayList<ReferenceDispositionPlan.Decision>();
        for (var occurrence : occurrences(content)) {
            var reference = occurrence.reference();
            var key = StableIdentityKey.targetOf(reference);
            if (fragment.identities().provided().contains(key)) {
                references.add(new ReferenceDispositionPlan.Decision(occurrence.location(), reference,
                        ReferenceDispositionPlan.Disposition.REMAP_INTERNAL, Optional.of(remap.destinationOf(key)), Optional.empty()));
            } else if (context.hasSameDocumentToken() && inventory.targets().containsKey(key)
                    && inventory.targets().get(key) == context.sourceMetadata().targetWitnesses().get(key)) {
                references.add(new ReferenceDispositionPlan.Decision(occurrence.location(), reference,
                        ReferenceDispositionPlan.Disposition.PRESERVE_EXTERNAL_SAME_DOCUMENT, Optional.of(key), Optional.empty()));
            } else {
                var text = context.sourceMetadata().referenceText().get(key);
                if (text == null) {
                    text = CrossReferenceResolution.MISSING_REFERENCE_TEXT;
                    diagnostics.add(diagnostic(TransferDiagnostic.Severity.WARNING, TransferDiagnostic.Code.SOURCE_DISPLAY_UNAVAILABLE,
                            "Source textual reference export is unavailable.", Optional.of(key), Optional.of(occurrence.location())));
                }
                references.add(new ReferenceDispositionPlan.Decision(occurrence.location(), reference,
                        ReferenceDispositionPlan.Disposition.DEGRADE_TO_TEXT, Optional.empty(), Optional.of(text)));
                diagnostics.add(diagnostic(TransferDiagnostic.Severity.WARNING, TransferDiagnostic.Code.EXTERNAL_REFERENCE_DEGRADED,
                        "Unproven external reference is planned as non-reference text.", Optional.of(key), Optional.of(occurrence.location())));
            }
        }
        return new PlanningResult.Success(new TransferPlan(fragment, context, context.hasSameDocumentToken(), remap,
                new ResourceTransferPlan(resources), new ReferenceDispositionPlan(references), diagnostics));
    }

    static boolean compatible(FragmentContent content, TransferContext.Scope scope) {
        return scope == TransferContext.Scope.DOCUMENT_CONTENT
                || scope == TransferContext.Scope.BLOCKS && content instanceof FragmentContent.Blocks
                || scope == TransferContext.Scope.INLINE && content instanceof FragmentContent.InlineSegments
                || scope == TransferContext.Scope.RESOURCES && content instanceof FragmentContent.ResourcePrimary;
    }

    record Inventory(Map<StableIdentityKey, BlockNode> targets, Map<StableIdentityKey, ScientificDataset> datasets,
                             Set<StableIdentityKey> occupied) {}

    static Inventory inventory(TransferContext context) {
        var targets = new HashMap<StableIdentityKey, BlockNode>();
        var datasets = new HashMap<StableIdentityKey, ScientificDataset>();
        var occupied = new HashSet<StableIdentityKey>();
        for (var index = 0; index < context.destination().blocks().size(); index++) {
            if (context.removedBlockIndices().contains(index)) { continue; }
            var block = context.destination().blocks().get(index);
            FragmentIdentityIndex.blockIdentity(block).ifPresent(key -> {
                if (!occupied.add(key)) { throw new IllegalArgumentException("Ambiguous surviving destination identity: " + key); }
                targets.put(key, block);
            });
        }
        context.destination().datasets().forEach(value -> {
            var key = datasetKey(value.id());
            occupied.add(key);
            datasets.put(key, value);
        });
        return new Inventory(targets, datasets, occupied);
    }

    record Occurrence(String location, CrossReference reference) {}

    static List<Occurrence> occurrences(FragmentContent content) {
        var result = new ArrayList<Occurrence>();
        if (content instanceof FragmentContent.Blocks blocks) {
            for (var i = 0; i < blocks.roots().size(); i++) { collectBlock(blocks.roots().get(i), "roots[" + i + "]", result); }
        } else if (content instanceof FragmentContent.InlineSegments inline) {
            for (var i = 0; i < inline.segments().size(); i++) { collectInline(inline.segments().get(i), "segments[" + i + "]", result); }
        }
        return List.copyOf(result);
    }

    private static void collectBlock(BlockNode block, String path, List<Occurrence> result) {
        if (block instanceof Paragraph paragraph) { collectInline(paragraph.content(), path + ".content", result); }
        else if (block instanceof Heading heading) { collectInline(heading.content(), path + ".content", result); }
        else if (block instanceof FigureBlock figure) { collectInline(figure.caption(), path + ".caption", result); }
        else if (block instanceof TableBlock table) {
            for (var row = 0; row < table.rows().size(); row++) {
                for (var column = 0; column < table.columnCount(); column++) {
                    collectInline(table.rows().get(row).cells().get(column).content().content(),
                            path + ".rows[" + row + "].cells[" + column + "]", result);
                }
            }
        }
    }

    private static void collectInline(InlineContent content, String path, List<Occurrence> result) {
        for (var i = 0; i < content.nodes().size(); i++) {
            if (content.nodes().get(i) instanceof CrossReference reference) { result.add(new Occurrence(path + ".nodes[" + i + "]", reference)); }
        }
    }

    private static StableIdentityKey datasetKey(String id) { return new StableIdentityKey(StableIdentityKind.DATASET, id); }
    private static boolean hasErrors(List<TransferDiagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(d -> d.severity() == TransferDiagnostic.Severity.ERROR);
    }
    private static PlanningResult failure(TransferDiagnostic.Code code, String message) {
        return new PlanningResult.Failure(List.of(diagnostic(TransferDiagnostic.Severity.ERROR, code, message, Optional.empty(), Optional.empty())));
    }
    private static TransferDiagnostic diagnostic(TransferDiagnostic.Severity severity, TransferDiagnostic.Code code, String message,
                                                  Optional<StableIdentityKey> key, Optional<String> location) {
        return new TransferDiagnostic(severity, code, message, key, location);
    }
}
