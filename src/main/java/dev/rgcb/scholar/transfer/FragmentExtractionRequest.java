package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.document.InlineContent;
import java.util.List;
import java.util.Objects;

/** Semantic source request. Editor adapters slice inline ranges using existing editor helpers. */
public sealed interface FragmentExtractionRequest {
    record Blocks(List<Integer> blockIndices) implements FragmentExtractionRequest {
        public Blocks { blockIndices = List.copyOf(blockIndices); }
    }

    record InlineSegments(List<InlineContent> segments) implements FragmentExtractionRequest {
        public InlineSegments { segments = List.copyOf(segments); }
    }

    record Datasets(List<String> datasetIds) implements FragmentExtractionRequest {
        public Datasets { datasetIds = List.copyOf(datasetIds); }
    }

    record Rejected(TransferDiagnostic.Code code, String reason) implements FragmentExtractionRequest {
        public Rejected {
            code = Objects.requireNonNull(code, "code");
            reason = Objects.requireNonNull(reason, "reason");
            if (reason.isBlank()) { throw new IllegalArgumentException("Rejection reason must not be blank."); }
        }
    }
}
