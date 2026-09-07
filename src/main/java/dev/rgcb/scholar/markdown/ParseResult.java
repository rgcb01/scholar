package dev.rgcb.scholar.markdown;

import dev.rgcb.scholar.document.Document;
import java.util.List;
import java.util.Objects;

/**
 * The result of parsing Scholar Markdown into the document model.
 */
public record ParseResult(Document document, List<MarkdownDiagnostic> diagnostics) {
    public ParseResult {
        document = Objects.requireNonNull(document, "document");
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean hasDiagnostics() {
        return !diagnostics.isEmpty();
    }
}
