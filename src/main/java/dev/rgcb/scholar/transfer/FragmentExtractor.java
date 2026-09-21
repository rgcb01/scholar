package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolver;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.validation.DocumentDiagnosticCode;
import dev.rgcb.scholar.validation.DocumentDiagnosticSeverity;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Source-only extraction. No destination, ID allocator, editor, or clipboard dependency. */
public final class FragmentExtractor {
    public ExtractionResult extract(Document source, FragmentExtractionRequest request) {
        return extract(source, request, Optional.empty());
    }

    public ExtractionResult extract(Document source, FragmentExtractionRequest request, Optional<RuntimeDocumentToken> token) {
        if (source == null || request == null || token == null) {
            return fail(TransferDiagnostic.Code.MALFORMED_FRAGMENT, "Source, request and token optional must not be null.");
        }
        if (request instanceof FragmentExtractionRequest.Rejected rejected) {
            return fail(rejected.code(), rejected.reason());
        }
        try {
            return extractChecked(source, request, token);
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            return fail(TransferDiagnostic.Code.MALFORMED_FRAGMENT, "Invalid source request/content: " + exception.getMessage());
        }
    }

    private ExtractionResult extractChecked(Document source, FragmentExtractionRequest request, Optional<RuntimeDocumentToken> token) {
        FragmentContent content;
        List<StableIdentityKey> required;
        if (request instanceof FragmentExtractionRequest.Blocks blocks) {
            if (blocks.blockIndices().isEmpty() || new HashSet<>(blocks.blockIndices()).size() != blocks.blockIndices().size()) {
                return fail(TransferDiagnostic.Code.MALFORMED_FRAGMENT, "Block request must contain unique source indices.");
            }
            var roots = blocks.blockIndices().stream().sorted().map(index -> source.blocks().get(index)).toList();
            content = new FragmentContent.Blocks(roots);
            required = FragmentIdentityIndex.requiredDatasets(content);
        } else if (request instanceof FragmentExtractionRequest.InlineSegments inline) {
            content = new FragmentContent.InlineSegments(inline.segments());
            required = FragmentIdentityIndex.requiredDatasets(content);
        } else if (request instanceof FragmentExtractionRequest.Datasets datasets) {
            if (datasets.datasetIds().isEmpty() || new HashSet<>(datasets.datasetIds()).size() != datasets.datasetIds().size()) {
                return fail(TransferDiagnostic.Code.MALFORMED_FRAGMENT, "Dataset request must contain unique source IDs.");
            }
            required = datasets.datasetIds().stream().map(id -> new StableIdentityKey(StableIdentityKind.DATASET, id)).toList();
            content = new FragmentContent.ResourcePrimary(new LinkedHashSet<>(required));
        } else {
            return fail(TransferDiagnostic.Code.UNSUPPORTED_SOURCE_SELECTION, "Unsupported extraction request.");
        }

        var resources = new ArrayList<ScientificDataset>();
        var diagnostics = new ArrayList<TransferDiagnostic>();
        var sourceResources = new HashMap<String, ScientificDataset>();
        source.datasets().forEach(dataset -> sourceResources.put(dataset.id(), dataset));
        for (var key : required) {
            var resource = sourceResources.get(key.id());
            if (resource == null) {
                diagnostics.add(diagnostic(TransferDiagnostic.Severity.ERROR, TransferDiagnostic.Code.MISSING_REQUIRED_RESOURCE,
                        "Required source dataset is missing: " + key.id(), Optional.of(key)));
            } else {
                resources.add(resource);
            }
        }
        if (!diagnostics.isEmpty()) { return new ExtractionResult.Failure(diagnostics); }

        var fragment = new DocumentFragment(content, resources);
        // Validate selected owned values, not unrelated document errors or projected external links.
        List<BlockNode> validationRoots = content instanceof FragmentContent.Blocks blocks ? blocks.roots()
                : content instanceof FragmentContent.InlineSegments inline
                ? inline.segments().stream().<BlockNode>map(Paragraph::new).toList() : List.of();
        var validation = DocumentValidator.validate(new Document(validationRoots, resources));
        for (var issue : validation.diagnostics()) {
            if (issue.code() == DocumentDiagnosticCode.MISSING_CROSS_REFERENCE_TARGET) { continue; }
            var fatal = issue.severity() == DocumentDiagnosticSeverity.ERROR;
            diagnostics.add(diagnostic(fatal ? TransferDiagnostic.Severity.ERROR : TransferDiagnostic.Severity.WARNING,
                    fatal ? TransferDiagnostic.Code.MALFORMED_FRAGMENT : TransferDiagnostic.Code.SOURCE_VALIDATION_WARNING,
                    issue.code() + ": " + issue.message(), Optional.empty()));
        }
        if (!validation.isValid()) { return new ExtractionResult.Failure(diagnostics); }

        var targets = new HashMap<StableIdentityKey, BlockNode>();
        var ambiguous = new HashSet<StableIdentityKey>();
        source.blocks().forEach(block -> FragmentIdentityIndex.blockIdentity(block).ifPresent(key -> {
            if (targets.putIfAbsent(key, block) != null) { ambiguous.add(key); }
        }));
        var involved = new HashSet<>(fragment.identities().provided());
        involved.addAll(fragment.identities().referenced());
        if (ambiguous.stream().anyMatch(involved::contains)) {
            return fail(TransferDiagnostic.Code.MALFORMED_FRAGMENT, "Source identity is ambiguous for extracted content/reference.");
        }

        var witnesses = new HashMap<StableIdentityKey, BlockNode>();
        var resourceWitnesses = new HashMap<StableIdentityKey, ScientificDataset>();
        resources.forEach(resource -> resourceWitnesses.put(new StableIdentityKey(StableIdentityKind.DATASET, resource.id()), resource));
        involved.forEach(key -> { if (targets.containsKey(key)) { witnesses.put(key, targets.get(key)); } });
        var exports = new HashMap<StableIdentityKey, String>();
        var resolver = new CrossReferenceResolver();
        var orderedReferences = fragment.identities().referenced().stream()
                .filter(key -> key.kind() != StableIdentityKind.DATASET)
                .sorted(java.util.Comparator.comparing((StableIdentityKey key) -> key.kind().ordinal()).thenComparing(StableIdentityKey::id)).toList();
        for (var key : orderedReferences) {
            var kind = switch (key.kind()) {
                case SECTION -> CrossReferenceTargetKind.SECTION;
                case EQUATION -> CrossReferenceTargetKind.EQUATION;
                case TABLE -> CrossReferenceTargetKind.TABLE;
                case FIGURE -> CrossReferenceTargetKind.FIGURE;
                case DATASET -> throw new IllegalArgumentException("Dataset is not a CrossReference target.");
            };
            exports.put(key, resolver.resolve(source, new CrossReference(kind, key.id())).displayText());
            if (!targets.containsKey(key)) {
                diagnostics.add(diagnostic(TransferDiagnostic.Severity.WARNING, TransferDiagnostic.Code.SOURCE_DISPLAY_UNAVAILABLE,
                        "Source reference target is missing: " + key.id(), Optional.of(key)));
            }
        }
        var metadata = new SourceTransferMetadata(token, witnesses, resourceWitnesses, exports);
        return new ExtractionResult.Success(fragment, metadata, diagnostics);
    }

    private static ExtractionResult.Failure fail(TransferDiagnostic.Code code, String message) {
        return new ExtractionResult.Failure(List.of(diagnostic(TransferDiagnostic.Severity.ERROR, code, message, Optional.empty())));
    }

    private static TransferDiagnostic diagnostic(TransferDiagnostic.Severity severity, TransferDiagnostic.Code code,
                                                  String message, Optional<StableIdentityKey> key) {
        return new TransferDiagnostic(severity, code, message, key, Optional.empty());
    }
}
