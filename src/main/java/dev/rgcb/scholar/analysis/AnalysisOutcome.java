package dev.rgcb.scholar.analysis;

import java.util.List;
import java.util.Optional;

public record AnalysisOutcome(Optional<AnalysisResult> result, List<AnalysisDiagnostic> diagnostics) {
    public AnalysisOutcome {
        result = result == null ? Optional.empty() : result;
        diagnostics = List.copyOf(diagnostics);
    }
    public static AnalysisOutcome success(AnalysisResult result) { return new AnalysisOutcome(Optional.of(result), List.of()); }
    public static AnalysisOutcome failure(AnalysisDiagnostic.Code code, String message) {
        return new AnalysisOutcome(Optional.empty(), List.of(new AnalysisDiagnostic(code, message)));
    }
}
