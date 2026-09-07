package dev.rgcb.scholar.diagram;

import java.util.List;

/** Semantic element placed on a diagram's logical canvas. */
public interface DiagramElement {
    DiagramElementId id();

    DiagramBounds bounds();

    /** Immutable replacement used by domain-neutral diagram dragging. */
    DiagramElement withBounds(DiagramBounds bounds);

    /** Semantic connection points owned by this element. */
    List<DiagramPort> ports();
}
