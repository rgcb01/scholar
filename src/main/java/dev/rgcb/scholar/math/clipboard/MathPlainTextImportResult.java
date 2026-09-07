package dev.rgcb.scholar.math.clipboard;

import dev.rgcb.scholar.math.MathSequence;
import java.util.Objects;

public sealed interface MathPlainTextImportResult
        permits MathPlainTextImportResult.Success, MathPlainTextImportResult.Failure {
    record Success(MathSequence fragment) implements MathPlainTextImportResult {
        public Success {
            Objects.requireNonNull(fragment, "fragment");
        }
    }

    record Failure(MathImportError error) implements MathPlainTextImportResult {
        public Failure {
            Objects.requireNonNull(error, "error");
        }
    }
}
