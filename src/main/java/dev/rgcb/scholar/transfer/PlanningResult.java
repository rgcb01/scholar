package dev.rgcb.scholar.transfer;

import java.util.List;
import java.util.Objects;

public sealed interface PlanningResult {
    List<TransferDiagnostic> diagnostics();

    record Success(TransferPlan plan) implements PlanningResult {
        public Success { plan = Objects.requireNonNull(plan, "plan"); }
        public List<TransferDiagnostic> diagnostics() { return plan.diagnostics(); }
    }

    record Failure(List<TransferDiagnostic> diagnostics) implements PlanningResult {
        public Failure {
            diagnostics = List.copyOf(diagnostics);
            if (diagnostics.stream().noneMatch(d -> d.severity() == TransferDiagnostic.Severity.ERROR)) {
                throw new IllegalArgumentException("Failure requires a fatal diagnostic.");
            }
        }
    }
}
