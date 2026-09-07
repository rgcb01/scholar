package dev.rgcb.scholar.mechanical.layout;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.layout.*;
public record LaidOutMechanicalPartReference(int elementIndex, DiagramElementId elementId, LaidOutDiagramRect balloonBounds, int targetX, int targetY, LaidOutDiagramLabel itemLabel) {}
