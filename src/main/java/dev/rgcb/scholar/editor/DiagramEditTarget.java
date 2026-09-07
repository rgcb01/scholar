package dev.rgcb.scholar.editor;

public sealed interface DiagramEditTarget permits DiagramPropertyTarget, DiagramElementTarget, DiagramPortTarget, DiagramConnectionTarget {
}
