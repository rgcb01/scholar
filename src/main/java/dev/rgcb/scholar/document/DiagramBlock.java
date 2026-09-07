package dev.rgcb.scholar.document;

import dev.rgcb.scholar.diagram.DiagramDefinition;
import java.util.Objects;

/**
 * One authored scientific diagram plus its document-workspace presentation height.
 *
 * <p>The logical canvas remains semantic diagram data. {@code workspaceAspectRatio}
 * controls only how tall the embedded document workspace is relative to its laid-out
 * width; transient zoom/pan state is deliberately not stored here.</p>
 */
public record DiagramBlock(
        DiagramDefinition definition,
        double workspaceAspectRatio
) implements BlockNode {
    public DiagramBlock(DiagramDefinition definition) {
        this(definition, defaultWorkspaceAspectRatio(definition));
    }

    public DiagramBlock {
        definition = Objects.requireNonNull(definition, "definition");
        if (!Double.isFinite(workspaceAspectRatio) || workspaceAspectRatio <= 0.0) {
            throw new IllegalArgumentException("Diagram workspace aspect ratio must be finite and positive.");
        }
    }

    public DiagramBlock withDefinition(DiagramDefinition replacement) {
        return new DiagramBlock(Objects.requireNonNull(replacement, "replacement"), workspaceAspectRatio);
    }

    public DiagramBlock withWorkspaceAspectRatio(double replacement) {
        return new DiagramBlock(definition, replacement);
    }

    public DiagramBlock resetWorkspaceAspectRatio() {
        return withWorkspaceAspectRatio(defaultWorkspaceAspectRatio(definition));
    }

    public static double defaultWorkspaceAspectRatio(DiagramDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return definition.canvas().height() / definition.canvas().width();
    }
}
