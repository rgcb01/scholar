package dev.rgcb.scholar.transfer;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.document.Paragraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ExtractionResultTest {
    @Test
    void diagnosticsAreImmutableAndFailureHasNoFragmentOrMetadataField() {
        var diagnostic = diagnostic(TransferDiagnostic.Severity.ERROR);
        var diagnostics = new ArrayList<>(List.of(diagnostic));
        var failure = new ExtractionResult.Failure(diagnostics);
        diagnostics.clear();
        assertEquals(List.of(diagnostic), failure.diagnostics());
        assertThrows(UnsupportedOperationException.class, () -> failure.diagnostics().clear());
        assertEquals(1, ExtractionResult.Failure.class.getRecordComponents().length);
    }

    @Test
    void successAndFailureCannotContradictFatalDiagnostics() {
        var fragment = new DocumentFragment(new FragmentContent.Blocks(List.of(new Paragraph(ExtractionFixtures.inline()))), List.of());
        assertThrows(IllegalArgumentException.class, () -> new ExtractionResult.Success(fragment, SourceTransferMetadata.unknown(),
                List.of(diagnostic(TransferDiagnostic.Severity.ERROR))));
        assertThrows(IllegalArgumentException.class, () -> new ExtractionResult.Failure(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ExtractionResult.Failure(List.of(diagnostic(TransferDiagnostic.Severity.WARNING))));
        var warnings = new ArrayList<>(List.of(diagnostic(TransferDiagnostic.Severity.WARNING)));
        var success = new ExtractionResult.Success(fragment, SourceTransferMetadata.unknown(), warnings);
        warnings.clear();
        assertEquals(1, success.diagnostics().size());
        assertThrows(UnsupportedOperationException.class, () -> success.diagnostics().clear());
    }

    @Test
    void requestCollectionsAreDefensivelyCopied() {
        var indices = new ArrayList<>(List.of(0));
        var request = new FragmentExtractionRequest.Blocks(indices);
        indices.clear();
        assertEquals(List.of(0), request.blockIndices());
        assertThrows(UnsupportedOperationException.class, () -> request.blockIndices().clear());
        var segments = new ArrayList<>(List.of(ExtractionFixtures.inline()));
        var inline = new FragmentExtractionRequest.InlineSegments(segments);
        segments.clear();
        assertEquals(1, inline.segments().size());
        var ids = new ArrayList<>(List.of("d"));
        var resources = new FragmentExtractionRequest.Datasets(ids);
        ids.clear();
        assertEquals(List.of("d"), resources.datasetIds());
    }

    private static TransferDiagnostic diagnostic(TransferDiagnostic.Severity severity) {
        return new TransferDiagnostic(severity, TransferDiagnostic.Code.MALFORMED_FRAGMENT, "Diagnostic", Optional.empty(), Optional.empty());
    }
}
