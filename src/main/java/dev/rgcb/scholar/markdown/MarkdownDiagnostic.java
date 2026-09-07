package dev.rgcb.scholar.markdown;

import java.util.Objects;

/**
 * A recoverable concern found while parsing Scholar Markdown.
 */
public record MarkdownDiagnostic(MarkdownDiagnosticKind kind, String message) {
    public MarkdownDiagnostic {
        kind = Objects.requireNonNull(kind, "kind");
        message = Objects.requireNonNull(message, "message");
    }
}
