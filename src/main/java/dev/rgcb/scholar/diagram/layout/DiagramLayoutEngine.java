package dev.rgcb.scholar.diagram.layout;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.layout.ElectricalSymbolLayoutEngine;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalComponent;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalJunction;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.mechanical.MechanicalPrimitive;
import dev.rgcb.scholar.mechanical.MechanicalDimension;
import dev.rgcb.scholar.mechanical.MechanicalConstraint;
import dev.rgcb.scholar.mechanical.MechanicalSymbol;
import dev.rgcb.scholar.mechanical.MechanicalAnnotation;
import dev.rgcb.scholar.mechanical.MechanicalPartReference;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalPrimitive;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalDimension;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalConstraint;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalSymbol;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalAnnotation;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalPartReference;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Pure-Java responsive layout for semantic DiagramBlock content. */
public final class DiagramLayoutEngine {
    public static final int OUTER_PADDING = 8;
    public static final int TITLE_GAP = 6;
    public static final int PORT_LABEL_GAP = 3;
    public static final int PORT_HIT_RADIUS = 5;
    public static final int ELECTRICAL_LABEL_GAP = 4;
    public static final double ELECTRICAL_ANNOTATION_FOOTPRINT_SCALE = 0.62;
    /** Maximum derived straight run outside an electrical terminal before routing may turn. */
    public static final int ELECTRICAL_PORT_EXIT_LENGTH = 8;

    private final DiagramConnectionRouter connectionRouter = new DiagramConnectionRouter();
    private final ElectricalSymbolLayoutEngine electricalSymbolLayoutEngine = new ElectricalSymbolLayoutEngine();

    public LaidOutDiagram layout(
            DiagramBlock block,
            int sourceBlockIndex,
            int x,
            int y,
            int width,
            TextMeasurer textMeasurer
    ) {
        Objects.requireNonNull(block, "block");
        return layout(
                block,
                sourceBlockIndex,
                x,
                y,
                width,
                textMeasurer,
                DiagramViewport.fit(block.definition().canvas()));
    }

    /**
     * Lays out one diagram through a bounded internal workspace viewport. The
     * authored logical canvas may be larger than the visible workspace; zoom and
     * pan only change the derived transform and never rewrite semantic geometry.
     */
    public LaidOutDiagram layout(
            DiagramBlock block,
            int sourceBlockIndex,
            int x,
            int y,
            int width,
            TextMeasurer textMeasurer,
            DiagramViewport requestedViewport
    ) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        Objects.requireNonNull(requestedViewport, "requestedViewport");
        if (sourceBlockIndex < 0) {
            throw new IllegalArgumentException("sourceBlockIndex must not be negative.");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive.");
        }

        var definition = block.definition();
        var titleStyle = TextStyle.paragraph(Set.of(TextMark.BOLD));
        var labelStyle = TextStyle.paragraph();
        var referenceStyle = TextStyle.paragraph(Set.of(TextMark.BOLD));
        var title = layoutCenteredLabel(definition.title(), x, y + OUTER_PADDING, width, titleStyle, textMeasurer);
        var titleReservation = title.map(item -> item.height() + TITLE_GAP).orElse(0);

        var horizontalPadding = Math.min(OUTER_PADDING, Math.max(0, (width - 1) / 2));
        var workspaceX = x + horizontalPadding;
        var workspaceWidth = Math.max(1, width - horizontalPadding * 2);
        var workspaceHeight = Math.max(1, (int) Math.round(workspaceWidth * block.workspaceAspectRatio()));
        var workspaceY = y + OUTER_PADDING + titleReservation;
        var canvas = definition.canvas();
        var fitScale = Math.min(
                workspaceWidth / canvas.width(),
                workspaceHeight / canvas.height());
        // Preserve the exact pre-M18E.5 width-fit transform for ordinary blocks
        // whose authored workspace still matches the logical canvas aspect ratio.
        if (Math.abs(block.workspaceAspectRatio() - canvas.height() / canvas.width()) <= 1.0e-12) {
            fitScale = workspaceWidth / canvas.width();
        }
        var scale = fitScale * requestedViewport.zoom();
        var centerX = clampViewportCenter(requestedViewport.centerX(), canvas.width(), workspaceWidth / scale);
        var centerY = clampViewportCenter(requestedViewport.centerY(), canvas.height(), workspaceHeight / scale);
        var effectiveViewport = new DiagramViewport(requestedViewport.zoom(), centerX, centerY);

        var canvasX = (int) Math.round(workspaceX + workspaceWidth / 2.0 - centerX * scale);
        var canvasY = (int) Math.round(workspaceY + workspaceHeight / 2.0 - centerY * scale);
        var canvasWidth = Math.max(1, (int) Math.round(canvas.width() * scale));
        var canvasHeight = Math.max(1, (int) Math.round(canvas.height() * scale));
        var transform = new DiagramCoordinateTransform(canvas, scale, canvasX, canvasY);
        var canvasRect = new LaidOutDiagramRect(canvasX, canvasY, canvasWidth, canvasHeight);

        var nodes = new ArrayList<LaidOutDiagramNode>();
        var electricalComponents = new ArrayList<LaidOutElectricalComponent>();
        var electricalJunctions = new ArrayList<LaidOutElectricalJunction>();
        var mechanicalPrimitives = new ArrayList<LaidOutMechanicalPrimitive>();
        var mechanicalDimensions = new ArrayList<LaidOutMechanicalDimension>();
        var mechanicalConstraints = new ArrayList<LaidOutMechanicalConstraint>();
        var mechanicalSymbols = new ArrayList<LaidOutMechanicalSymbol>();
        var mechanicalAnnotations = new ArrayList<LaidOutMechanicalAnnotation>();
        var mechanicalPartReferences = new ArrayList<LaidOutMechanicalPartReference>();
        var portsByEndpoint = new HashMap<DiagramEndpoint, LaidOutDiagramPort>();
        var electricalEndpoints = new HashSet<DiagramEndpoint>();
        var junctionEndpoints = new HashSet<DiagramEndpoint>();
        for (var elementIndex = 0; elementIndex < definition.elements().size(); elementIndex++) {
            var element = definition.elements().get(elementIndex);
            List<LaidOutDiagramPort> laidOutPorts;
            if (element instanceof DiagramNode node) {
                var laidOut = layoutNode(node, elementIndex, transform, canvasRect, labelStyle, textMeasurer);
                nodes.add(laidOut);
                laidOutPorts = laidOut.ports();
            } else if (element instanceof ElectricalComponent component) {
                var laidOut = layoutElectricalComponent(
                        component,
                        elementIndex,
                        transform,
                        canvasRect,
                        referenceStyle,
                        labelStyle,
                        textMeasurer);
                electricalComponents.add(laidOut);
                laidOutPorts = laidOut.ports();
            } else if (element instanceof ElectricalJunction junction) {
                var laidOut = layoutElectricalJunction(
                        junction, elementIndex, transform, canvasRect, labelStyle, textMeasurer);
                electricalJunctions.add(laidOut);
                laidOutPorts = laidOut.ports();
            } else if (element instanceof MechanicalPrimitive primitive) {
                var rect = layoutElementRect(primitive.bounds(), transform, canvasRect);
                mechanicalPrimitives.add(new LaidOutMechanicalPrimitive(
                        elementIndex, primitive.id(), primitive.kind(), primitive.orientation(), rect));
                laidOutPorts = List.of();
            } else if (element instanceof MechanicalSymbol symbol) {
                var rect = layoutElementRect(symbol.bounds(), transform, canvasRect);
                mechanicalSymbols.add(new LaidOutMechanicalSymbol(elementIndex, symbol.id(), symbol.kind(), symbol.orientation(), rect));
                laidOutPorts = List.of();
            } else if (element instanceof MechanicalPartReference reference) {
                var balloon=layoutElementRect(reference.bounds(),transform,canvasRect);
                var target=definition.elements().stream().filter(candidate -> candidate.id().equals(reference.targetId())).findFirst();
                var targetRect=target.map(candidate -> layoutElementRect(candidate.bounds(),transform,canvasRect)).orElse(balloon);
                var text=Integer.toString(reference.itemNumber()); var lw=textMeasurer.measureWidth(text,labelStyle); var lh=textMeasurer.lineHeight(labelStyle);
                mechanicalPartReferences.add(new LaidOutMechanicalPartReference(elementIndex,reference.id(),balloon,targetRect.x()+targetRect.width()/2,targetRect.y()+targetRect.height()/2,new LaidOutDiagramLabel(text,balloon.x()+Math.max(1,(balloon.width()-lw)/2),balloon.y()+Math.max(1,(balloon.height()-lh)/2),lw,lh,labelStyle)));
                laidOutPorts=List.of();
            } else if (element instanceof MechanicalAnnotation annotation) {
                var authoredRect = layoutElementRect(annotation.bounds(), transform, canvasRect);
                var labelWidth = textMeasurer.measureWidth(annotation.text(), labelStyle);
                var labelHeight = textMeasurer.lineHeight(labelStyle);
                var rect = layoutMechanicalAnnotationRect(
                        annotation.kind(), authoredRect, labelWidth, labelHeight, canvasRect);
                var labelX = switch (annotation.kind()) {
                    case PART_LABEL -> rect.x() + Math.max(4, (rect.width() - labelWidth) / 2);
                    case NOTE -> rect.x() + 2;
                    case LEADER -> rect.x() + Math.max(10, rect.width() / 3);
                };
                var labelY = switch (annotation.kind()) {
                    case PART_LABEL -> rect.y() + Math.max(3, (rect.height() - labelHeight) / 2);
                    case NOTE -> rect.y() + 2;
                    case LEADER -> rect.y() + 2;
                };
                mechanicalAnnotations.add(new LaidOutMechanicalAnnotation(
                        elementIndex,
                        annotation.id(),
                        annotation.kind(),
                        rect,
                        new LaidOutDiagramLabel(
                                annotation.text(),
                                labelX,
                                labelY,
                                labelWidth,
                                labelHeight,
                                labelStyle)));
                laidOutPorts = List.of();
            } else if (element instanceof MechanicalDimension dimension) {
                var rect = layoutElementRect(dimension.bounds(), transform, canvasRect);
                var dimensionText = dimension.displayText();
                var labelWidth = textMeasurer.measureWidth(dimensionText, labelStyle);
                var labelHeight = textMeasurer.lineHeight(labelStyle);
                var label = new LaidOutDiagramLabel(
                        dimensionText,
                        rect.x() + (rect.width() - labelWidth) / 2,
                        rect.y() + (rect.height() - labelHeight) / 2,
                        labelWidth,
                        labelHeight,
                        labelStyle);
                mechanicalDimensions.add(new LaidOutMechanicalDimension(
                        elementIndex, dimension.id(), dimension.kind(), rect, label));
                laidOutPorts = List.of();
            } else if (element instanceof MechanicalConstraint constraint) {
                var marker = layoutMechanicalConstraintMarker(constraint, definition, transform, canvasRect);
                mechanicalConstraints.add(new LaidOutMechanicalConstraint(
                        elementIndex, constraint.id(), constraint.kind(), marker));
                laidOutPorts = List.of();
            } else {
                throw new IllegalArgumentException("Unsupported diagram element: " + element.getClass().getName());
            }
            registerPorts(portsByEndpoint, laidOutPorts);
            if (element instanceof ElectricalComponent) {
                for (var port : laidOutPorts) {
                    electricalEndpoints.add(new DiagramEndpoint(port.elementId(), port.portId()));
                }
            } else if (element instanceof ElectricalJunction) {
                for (var port : laidOutPorts) {
                    junctionEndpoints.add(new DiagramEndpoint(port.elementId(), port.portId()));
                }
            }
        }

        var connections = new ArrayList<LaidOutDiagramConnection>();
        for (var connectionIndex = 0; connectionIndex < definition.connections().size(); connectionIndex++) {
            connections.add(layoutConnection(
                    definition.connections().get(connectionIndex),
                    connectionIndex,
                    portsByEndpoint,
                    electricalEndpoints,
                    junctionEndpoints,
                    canvasRect,
                    labelStyle,
                    textMeasurer));
        }

        var blockHeight = OUTER_PADDING + titleReservation + workspaceHeight + OUTER_PADDING;
        return new LaidOutDiagram(
                sourceBlockIndex,
                x,
                y,
                width,
                blockHeight,
                workspaceX,
                workspaceY,
                workspaceWidth,
                workspaceHeight,
                canvasX,
                canvasY,
                canvasWidth,
                canvasHeight,
                title,
                nodes,
                electricalComponents,
                electricalJunctions,
                mechanicalPrimitives,
                mechanicalDimensions,
                mechanicalConstraints,
                mechanicalSymbols,
                mechanicalAnnotations,
                mechanicalPartReferences,
                connections,
                transform,
                effectiveViewport);
    }

    private static LaidOutDiagramRect layoutMechanicalAnnotationRect(
            dev.rgcb.scholar.mechanical.MechanicalAnnotationKind kind,
            LaidOutDiagramRect authored,
            int labelWidth,
            int labelHeight,
            LaidOutDiagramRect canvasRect
    ) {
        var desiredWidth = switch (kind) {
            case PART_LABEL -> Math.max(authored.width(), labelWidth + 16);
            case NOTE -> Math.max(authored.width(), labelWidth + 4);
            case LEADER -> Math.max(authored.width(), labelWidth + 30);
        };
        var desiredHeight = switch (kind) {
            case PART_LABEL -> Math.max(authored.height(), labelHeight + 10);
            case NOTE -> Math.max(authored.height(), labelHeight + 4);
            case LEADER -> Math.max(authored.height(), labelHeight + 14);
        };

        desiredWidth = Math.min(desiredWidth, canvasRect.width());
        desiredHeight = Math.min(desiredHeight, canvasRect.height());

        var centerX = authored.x() + authored.width() / 2;
        var centerY = authored.y() + authored.height() / 2;
        var left = clamp(
                centerX - desiredWidth / 2,
                canvasRect.x(),
                Math.max(canvasRect.x(), canvasRect.right() - desiredWidth));
        var top = clamp(
                centerY - desiredHeight / 2,
                canvasRect.y(),
                Math.max(canvasRect.y(), canvasRect.bottom() - desiredHeight));
        return new LaidOutDiagramRect(left, top, desiredWidth, desiredHeight);
    }

    private static LaidOutDiagramRect layoutMechanicalConstraintMarker(
            MechanicalConstraint constraint,
            dev.rgcb.scholar.diagram.DiagramDefinition definition,
            DiagramCoordinateTransform transform,
            LaidOutDiagramRect canvasRect
    ) {
        var subject = definition.elements().stream()
                .filter(item -> item.id().equals(constraint.subjectId()))
                .findFirst()
                .orElse(null);
        if (subject == null) {
            return layoutElementRect(constraint.bounds(), transform, canvasRect);
        }
        var logicalX = subject.bounds().x() + subject.bounds().width() / 2.0;
        var logicalY = subject.bounds().y() + subject.bounds().height() / 2.0;
        if (constraint.peerId().isPresent()) {
            var peerId = constraint.peerId().orElseThrow();
            var peer = definition.elements().stream()
                    .filter(item -> item.id().equals(peerId))
                    .findFirst()
                    .orElse(null);
            if (peer != null) {
                logicalX = (logicalX + peer.bounds().x() + peer.bounds().width() / 2.0) / 2.0;
                logicalY = (logicalY + peer.bounds().y() + peer.bounds().height() / 2.0) / 2.0;
            }
        }
        var centerX = clamp((int) Math.round(transform.mapX(logicalX)),
                canvasRect.x(), Math.max(canvasRect.x(), canvasRect.right() - 1));
        var centerY = clamp((int) Math.round(transform.mapY(logicalY)),
                canvasRect.y(), Math.max(canvasRect.y(), canvasRect.bottom() - 1));
        var size = Math.max(9, Math.min(13, (int) Math.round(10 * transform.scale())));
        var left = clamp(centerX - size / 2, canvasRect.x(), Math.max(canvasRect.x(), canvasRect.right() - 1));
        var top = clamp(centerY - size / 2, canvasRect.y(), Math.max(canvasRect.y(), canvasRect.bottom() - 1));
        var right = clamp(left + size, left + 1, canvasRect.right());
        var bottom = clamp(top + size, top + 1, canvasRect.bottom());
        return new LaidOutDiagramRect(left, top, right - left, bottom - top);
    }

    private static double clampViewportCenter(double requested, double canvasExtent, double visibleExtent) {
        if (visibleExtent >= canvasExtent) {
            return canvasExtent / 2.0;
        }
        var halfVisible = visibleExtent / 2.0;
        return Math.max(halfVisible, Math.min(requested, canvasExtent - halfVisible));
    }

    private static void registerPorts(
            Map<DiagramEndpoint, LaidOutDiagramPort> portsByEndpoint,
            List<LaidOutDiagramPort> ports
    ) {
        for (var port : ports) {
            var endpoint = new DiagramEndpoint(port.elementId(), port.portId());
            if (portsByEndpoint.putIfAbsent(endpoint, port) != null) {
                throw new IllegalStateException("Duplicate laid-out diagram endpoint: " + endpoint);
            }
        }
    }

    private static LaidOutDiagramNode layoutNode(
            DiagramNode node,
            int elementIndex,
            DiagramCoordinateTransform transform,
            LaidOutDiagramRect canvasRect,
            TextStyle labelStyle,
            TextMeasurer textMeasurer
    ) {
        var rect = layoutElementRect(node.bounds(), transform, canvasRect);

        var label = Optional.<LaidOutDiagramLabel>empty();
        if (!node.label().isBlank()) {
            var labelWidth = textMeasurer.measureWidth(node.label(), labelStyle);
            var labelHeight = textMeasurer.lineHeight(labelStyle);
            var labelX = rect.x() + (rect.width() - labelWidth) / 2;
            var labelY = rect.y() + (rect.height() - labelHeight) / 2;
            if (labelWidth <= canvasRect.width()) {
                labelX = clamp(labelX, canvasRect.x(), canvasRect.right() - labelWidth);
            }
            if (labelHeight <= canvasRect.height()) {
                labelY = clamp(labelY, canvasRect.y(), canvasRect.bottom() - labelHeight);
            }
            label = Optional.of(new LaidOutDiagramLabel(
                    node.label(),
                    labelX,
                    labelY,
                    labelWidth,
                    labelHeight,
                    labelStyle));
        }

        var ports = layoutPorts(
                node.id(), node.bounds(), node.ports(), elementIndex, transform, canvasRect, labelStyle, textMeasurer);
        return new LaidOutDiagramNode(
                elementIndex,
                node.id(),
                rect.x(),
                rect.y(),
                rect.width(),
                rect.height(),
                label,
                ports);
    }

    private LaidOutElectricalComponent layoutElectricalComponent(
            ElectricalComponent component,
            int elementIndex,
            DiagramCoordinateTransform transform,
            LaidOutDiagramRect canvasRect,
            TextStyle referenceStyle,
            TextStyle valueStyle,
            TextMeasurer textMeasurer
    ) {
        var rect = layoutElementRect(component.bounds(), transform, canvasRect);
        var ports = layoutPorts(
                component.id(),
                component.bounds(),
                component.ports(),
                elementIndex,
                transform,
                canvasRect,
                valueStyle,
                textMeasurer);
        var annotationRect = electricalAnnotationRect(rect);
        var labelPolicy = electricalLabelPolicy(component);
        var reference = layoutElectricalLabel(
                component.referenceDesignator(), annotationRect, canvasRect, labelPolicy.referenceSides(), referenceStyle, textMeasurer);
        var value = layoutElectricalLabel(
                component.valueLabel(), annotationRect, canvasRect, labelPolicy.valueSides(), valueStyle, textMeasurer);
        var primitives = electricalSymbolLayoutEngine.layout(component, transform, rect.width(), rect.height());
        return new LaidOutElectricalComponent(
                elementIndex,
                component.id(),
                component.kind(),
                rect.x(),
                rect.y(),
                rect.width(),
                rect.height(),
                reference,
                value,
                ports,
                primitives);
    }

    private static LaidOutElectricalJunction layoutElectricalJunction(
            ElectricalJunction junction,
            int elementIndex,
            DiagramCoordinateTransform transform,
            LaidOutDiagramRect canvasRect,
            TextStyle labelStyle,
            TextMeasurer textMeasurer
    ) {
        var rect = layoutElementRect(junction.bounds(), transform, canvasRect);
        var ports = layoutPorts(
                junction.id(), junction.bounds(), junction.ports(), elementIndex, transform, canvasRect, labelStyle, textMeasurer);
        var label = Optional.<LaidOutDiagramLabel>empty();
        if (!junction.netLabel().isBlank()) {
            var width = textMeasurer.measureWidth(junction.netLabel(), labelStyle);
            var height = textMeasurer.lineHeight(labelStyle);
            var x = rect.right() + 4;
            var y = rect.y() + (rect.height() - height) / 2;
            if (width <= canvasRect.width()) {
                x = clamp(x, canvasRect.x(), canvasRect.right() - width);
            }
            if (height <= canvasRect.height()) {
                y = clamp(y, canvasRect.y(), canvasRect.bottom() - height);
            }
            label = Optional.of(new LaidOutDiagramLabel(junction.netLabel(), x, y, width, height, labelStyle));
        }
        return new LaidOutElectricalJunction(
                elementIndex, junction.id(), rect.x(), rect.y(), rect.width(), rect.height(), label, ports);
    }

    private static LaidOutDiagramRect layoutElementRect(
            DiagramBounds bounds,
            DiagramCoordinateTransform transform,
            LaidOutDiagramRect canvasRect
    ) {
        // Rounding can collapse a logically valid element when the document is very
        // narrow. Clamp derived pixels while preserving authored logical bounds.
        var x = clamp((int) Math.round(transform.mapX(bounds.x())),
                canvasRect.x(), Math.max(canvasRect.x(), canvasRect.right() - 1));
        var y = clamp((int) Math.round(transform.mapY(bounds.y())),
                canvasRect.y(), Math.max(canvasRect.y(), canvasRect.bottom() - 1));
        var right = clamp((int) Math.round(transform.mapX(bounds.right())),
                x + 1, canvasRect.right());
        var bottom = clamp((int) Math.round(transform.mapY(bounds.bottom())),
                y + 1, canvasRect.bottom());
        return new LaidOutDiagramRect(x, y, right - x, bottom - y);
    }

    private static List<LaidOutDiagramPort> layoutPorts(
            DiagramElementId elementId,
            DiagramBounds bounds,
            List<DiagramPort> authoredPorts,
            int elementIndex,
            DiagramCoordinateTransform transform,
            LaidOutDiagramRect canvasRect,
            TextStyle labelStyle,
            TextMeasurer textMeasurer
    ) {
        var ports = new ArrayList<LaidOutDiagramPort>();
        for (var portIndex = 0; portIndex < authoredPorts.size(); portIndex++) {
            var port = authoredPorts.get(portIndex);
            var center = portCenter(bounds, port.placement(), transform);
            var hitBounds = new LaidOutDiagramRect(
                    center.x() - PORT_HIT_RADIUS,
                    center.y() - PORT_HIT_RADIUS,
                    PORT_HIT_RADIUS * 2,
                    PORT_HIT_RADIUS * 2);
            ports.add(new LaidOutDiagramPort(
                    elementIndex,
                    portIndex,
                    elementId,
                    port.id(),
                    port.placement().side(),
                    center.x(),
                    center.y(),
                    hitBounds,
                    layoutPortLabel(port, center, canvasRect, labelStyle, textMeasurer)));
        }
        return List.copyOf(ports);
    }

    /** Maps the authored perimeter point directly instead of accumulating rounded element dimensions. */
    private static LaidOutDiagramPoint portCenter(
            DiagramBounds bounds,
            DiagramPortPlacement placement,
            DiagramCoordinateTransform transform
    ) {
        double logicalX;
        double logicalY;
        switch (placement.side()) {
            case LEFT -> {
                logicalX = bounds.x();
                logicalY = bounds.y() + bounds.height() * placement.offset();
            }
            case RIGHT -> {
                logicalX = bounds.right();
                logicalY = bounds.y() + bounds.height() * placement.offset();
            }
            case TOP -> {
                logicalX = bounds.x() + bounds.width() * placement.offset();
                logicalY = bounds.y();
            }
            case BOTTOM -> {
                logicalX = bounds.x() + bounds.width() * placement.offset();
                logicalY = bounds.bottom();
            }
            default -> throw new IllegalStateException("Unexpected diagram port side: " + placement.side());
        }
        return new LaidOutDiagramPoint(
                (int) Math.round(transform.mapX(logicalX)),
                (int) Math.round(transform.mapY(logicalY)));
    }

    private static Optional<LaidOutDiagramLabel> layoutPortLabel(
            DiagramPort port,
            LaidOutDiagramPoint center,
            LaidOutDiagramRect canvasRect,
            TextStyle style,
            TextMeasurer textMeasurer
    ) {
        if (port.label().isBlank()) {
            return Optional.empty();
        }
        var width = textMeasurer.measureWidth(port.label(), style);
        var height = textMeasurer.lineHeight(style);
        var x = center.x();
        var y = center.y();
        switch (port.placement().side()) {
            case LEFT -> {
                x -= PORT_LABEL_GAP + width;
                y -= height / 2;
            }
            case RIGHT -> {
                x += PORT_LABEL_GAP;
                y -= height / 2;
            }
            case TOP -> {
                x -= width / 2;
                y -= PORT_LABEL_GAP + height;
            }
            case BOTTOM -> {
                x -= width / 2;
                y += PORT_LABEL_GAP;
            }
        }

        if (width <= canvasRect.width()) {
            x = clamp(x, canvasRect.x(), canvasRect.right() - width);
        }
        if (height <= canvasRect.height()) {
            y = clamp(y, canvasRect.y(), canvasRect.bottom() - height);
        }
        return Optional.of(new LaidOutDiagramLabel(port.label(), x, y, width, height, style));
    }

    /**
     * Labels track the compact visible symbol body rather than the larger authored
     * interaction/terminal footprint. This keeps annotations visually attached to
     * the schematic symbol while preserving forgiving hit targets and wire leads.
     */
    private static LaidOutDiagramRect electricalAnnotationRect(LaidOutDiagramRect interactionRect) {
        var width = Math.max(1, (int) Math.round(interactionRect.width() * ELECTRICAL_ANNOTATION_FOOTPRINT_SCALE));
        var height = Math.max(1, (int) Math.round(interactionRect.height() * ELECTRICAL_ANNOTATION_FOOTPRINT_SCALE));
        return new LaidOutDiagramRect(
                interactionRect.x() + (interactionRect.width() - width) / 2,
                interactionRect.y() + (interactionRect.height() - height) / 2,
                width,
                height);
    }

    private static ElectricalLabelPolicy electricalLabelPolicy(ElectricalComponent component) {
        if (component.kind() == ElectricalComponentKind.GROUND) {
            // Ground is fed from its top terminal; a side annotation stays clear of the conductor.
            return new ElectricalLabelPolicy(
                    List.of(ElectricalLabelSide.RIGHT, ElectricalLabelSide.LEFT,
                            ElectricalLabelSide.BOTTOM, ElectricalLabelSide.TOP),
                    List.of(ElectricalLabelSide.LEFT, ElectricalLabelSide.RIGHT,
                            ElectricalLabelSide.BOTTOM, ElectricalLabelSide.TOP));
        }

        var vertical = component.orientation().quarterTurnsClockwise() % 2 != 0;
        if (vertical) {
            // Vertical two-terminal parts use their left/right gutters so labels do not sit on terminal leads.
            return new ElectricalLabelPolicy(
                    List.of(ElectricalLabelSide.LEFT, ElectricalLabelSide.RIGHT,
                            ElectricalLabelSide.TOP, ElectricalLabelSide.BOTTOM),
                    List.of(ElectricalLabelSide.RIGHT, ElectricalLabelSide.LEFT,
                            ElectricalLabelSide.BOTTOM, ElectricalLabelSide.TOP));
        }
        return new ElectricalLabelPolicy(
                List.of(ElectricalLabelSide.TOP, ElectricalLabelSide.BOTTOM,
                        ElectricalLabelSide.LEFT, ElectricalLabelSide.RIGHT),
                List.of(ElectricalLabelSide.BOTTOM, ElectricalLabelSide.TOP,
                        ElectricalLabelSide.RIGHT, ElectricalLabelSide.LEFT));
    }

    private static Optional<LaidOutDiagramLabel> layoutElectricalLabel(
            String text,
            LaidOutDiagramRect componentRect,
            LaidOutDiagramRect canvasRect,
            List<ElectricalLabelSide> candidateSides,
            TextStyle style,
            TextMeasurer textMeasurer
    ) {
        if (text.isBlank()) {
            return Optional.empty();
        }
        var width = textMeasurer.measureWidth(text, style);
        var height = textMeasurer.lineHeight(style);

        for (var side : candidateSides) {
            var position = electricalLabelPosition(componentRect, side, width, height);
            if (fitsInside(position.x(), position.y(), width, height, canvasRect)) {
                return Optional.of(new LaidOutDiagramLabel(
                        text, position.x(), position.y(), width, height, style));
            }
        }

        // Extremely constrained diagrams still get deterministic, clipped-safe text.
        var fallback = electricalLabelPosition(componentRect, candidateSides.get(0), width, height);
        var x = fallback.x();
        var y = fallback.y();
        if (width <= canvasRect.width()) {
            x = clamp(x, canvasRect.x(), canvasRect.right() - width);
        }
        if (height <= canvasRect.height()) {
            y = clamp(y, canvasRect.y(), canvasRect.bottom() - height);
        }
        return Optional.of(new LaidOutDiagramLabel(text, x, y, width, height, style));
    }

    private static LaidOutDiagramPoint electricalLabelPosition(
            LaidOutDiagramRect componentRect,
            ElectricalLabelSide side,
            int width,
            int height
    ) {
        return switch (side) {
            case TOP -> new LaidOutDiagramPoint(
                    componentRect.x() + (componentRect.width() - width) / 2,
                    componentRect.y() - ELECTRICAL_LABEL_GAP - height);
            case BOTTOM -> new LaidOutDiagramPoint(
                    componentRect.x() + (componentRect.width() - width) / 2,
                    componentRect.bottom() + ELECTRICAL_LABEL_GAP);
            case LEFT -> new LaidOutDiagramPoint(
                    componentRect.x() - ELECTRICAL_LABEL_GAP - width,
                    componentRect.y() + (componentRect.height() - height) / 2);
            case RIGHT -> new LaidOutDiagramPoint(
                    componentRect.right() + ELECTRICAL_LABEL_GAP,
                    componentRect.y() + (componentRect.height() - height) / 2);
        };
    }

    private static boolean fitsInside(
            int x,
            int y,
            int width,
            int height,
            LaidOutDiagramRect canvasRect
    ) {
        return x >= canvasRect.x()
                && y >= canvasRect.y()
                && x + width <= canvasRect.right()
                && y + height <= canvasRect.bottom();
    }

    private enum ElectricalLabelSide {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }

    private record ElectricalLabelPolicy(
            List<ElectricalLabelSide> referenceSides,
            List<ElectricalLabelSide> valueSides
    ) {
        private ElectricalLabelPolicy {
            referenceSides = List.copyOf(referenceSides);
            valueSides = List.copyOf(valueSides);
            if (referenceSides.isEmpty() || valueSides.isEmpty()) {
                throw new IllegalArgumentException("Electrical label side preferences must not be empty.");
            }
        }
    }

    private LaidOutDiagramConnection layoutConnection(
            DiagramConnection connection,
            int connectionIndex,
            Map<DiagramEndpoint, LaidOutDiagramPort> ports,
            Set<DiagramEndpoint> electricalEndpoints,
            Set<DiagramEndpoint> junctionEndpoints,
            LaidOutDiagramRect canvasRect,
            TextStyle style,
            TextMeasurer textMeasurer
    ) {
        var source = requirePort(ports, connection.source());
        var target = requirePort(ports, connection.target());
        var sourceElectrical = electricalEndpoints.contains(connection.source());
        var targetElectrical = electricalEndpoints.contains(connection.target());
        var sourceJunction = junctionEndpoints.contains(connection.source());
        var targetJunction = junctionEndpoints.contains(connection.target());
        var sourceExitLength = sourceJunction
                ? 0
                : sourceElectrical
                        ? electricalPortExitLength(source, target)
                        : DiagramConnectionRouter.PORT_EXIT_LENGTH;
        var targetExitLength = targetJunction
                ? 0
                : targetElectrical
                        ? electricalPortExitLength(target, source)
                        : DiagramConnectionRouter.PORT_EXIT_LENGTH;
        var path = connectionRouter.route(source, target, canvasRect, sourceExitLength, targetExitLength);
        if (sourceElectrical || targetElectrical || sourceJunction || targetJunction) {
            path = simplifyElectricalRoute(path);
        }
        var bounds = connectionRouter.bounds(path);
        var label = layoutConnectionLabel(connection.label(), path, canvasRect, style, textMeasurer);
        return new LaidOutDiagramConnection(connectionIndex, path, bounds, label);
    }


    /**
     * Returns a compact presentation-only electrical exit length. The logical
     * terminal remains exactly on the authored perimeter; only the first routed
     * wire segment is shortened. If the other endpoint is not in the terminal's
     * outward half-plane, no artificial exit stub is added because it would
     * necessarily create a visible spike past the connection point.
     */
    private static int electricalPortExitLength(LaidOutDiagramPort terminal, LaidOutDiagramPort other) {
        var available = switch (terminal.side()) {
            case LEFT -> terminal.centerX() - other.centerX();
            case RIGHT -> other.centerX() - terminal.centerX();
            case TOP -> terminal.centerY() - other.centerY();
            case BOTTOM -> other.centerY() - terminal.centerY();
        };
        return clamp(available, 0, ELECTRICAL_PORT_EXIT_LENGTH);
    }

    /**
     * Removes redundant collinear route vertices for electrical presentation.
     * Besides making ordinary straight runs simpler, this collapses the
     * out-and-back spikes that can occur when an endpoint's preferred outward
     * direction conflicts with the actual route. First/last terminal anchors are
     * preserved exactly. Coincident semantic endpoints retain the two-point route
     * contract required by layout/render/hit testing.
     */
    private static List<LaidOutDiagramPoint> simplifyElectricalRoute(List<LaidOutDiagramPoint> path) {
        var simplified = new ArrayList<LaidOutDiagramPoint>();
        for (var point : path) {
            if (!simplified.isEmpty() && simplified.get(simplified.size() - 1).equals(point)) {
                continue;
            }
            simplified.add(point);
            var changed = true;
            while (changed && simplified.size() >= 3) {
                changed = false;
                var last = simplified.size() - 1;
                var first = simplified.get(last - 2);
                var middle = simplified.get(last - 1);
                var end = simplified.get(last);
                if (collinear(first, middle, end)) {
                    simplified.remove(last - 1);
                    changed = true;
                    if (simplified.size() >= 2
                            && simplified.get(simplified.size() - 1).equals(simplified.get(simplified.size() - 2))) {
                        simplified.remove(simplified.size() - 1);
                    }
                }
            }
        }
        if (simplified.isEmpty()) {
            throw new IllegalArgumentException("Electrical route must contain at least one point.");
        }
        if (simplified.size() == 1) {
            simplified.add(simplified.get(0));
        }
        return List.copyOf(simplified);
    }

    private static boolean collinear(
            LaidOutDiagramPoint first,
            LaidOutDiagramPoint middle,
            LaidOutDiagramPoint end
    ) {
        return (first.x() == middle.x() && middle.x() == end.x())
                || (first.y() == middle.y() && middle.y() == end.y());
    }

    private static Optional<LaidOutDiagramLabel> layoutConnectionLabel(
            String text,
            List<LaidOutDiagramPoint> path,
            LaidOutDiagramRect canvasRect,
            TextStyle style,
            TextMeasurer textMeasurer
    ) {
        if (text.isBlank()) {
            return Optional.empty();
        }
        var anchor = connectionLabelAnchor(path);
        var width = textMeasurer.measureWidth(text, style);
        var height = textMeasurer.lineHeight(style);
        var x = anchor.x() - width / 2;
        var y = anchor.y() - height - 2;

        if (y < canvasRect.y()) {
            y = anchor.y() + 2;
        }
        if (width <= canvasRect.width()) {
            x = clamp(x, canvasRect.x(), canvasRect.right() - width);
        }
        if (height <= canvasRect.height()) {
            y = clamp(y, canvasRect.y(), canvasRect.bottom() - height);
        }
        return Optional.of(new LaidOutDiagramLabel(text, x, y, width, height, style));
    }

    /**
     * Places a connection label at the midpoint of the most useful routed
     * segment rather than at a path vertex. The longest segment wins;
     * horizontal segments win deterministic ties because they provide the
     * most readable baseline for text.
     */
    private static LaidOutDiagramPoint connectionLabelAnchor(List<LaidOutDiagramPoint> path) {
        if (path.size() < 2) {
            throw new IllegalArgumentException("Diagram connection path must contain at least two points.");
        }

        LaidOutDiagramPoint bestStart = path.get(0);
        LaidOutDiagramPoint bestEnd = path.get(1);
        var bestLength = -1;
        var bestHorizontal = false;

        for (var index = 1; index < path.size(); index++) {
            var start = path.get(index - 1);
            var end = path.get(index);
            var horizontal = start.y() == end.y();
            var length = Math.abs(end.x() - start.x()) + Math.abs(end.y() - start.y());
            if (length > bestLength || (length == bestLength && horizontal && !bestHorizontal)) {
                bestStart = start;
                bestEnd = end;
                bestLength = length;
                bestHorizontal = horizontal;
            }
        }

        return new LaidOutDiagramPoint(
                bestStart.x() + (bestEnd.x() - bestStart.x()) / 2,
                bestStart.y() + (bestEnd.y() - bestStart.y()) / 2);
    }

    private static LaidOutDiagramPort requirePort(Map<DiagramEndpoint, LaidOutDiagramPort> ports, DiagramEndpoint endpoint) {
        var result = ports.get(endpoint);
        if (result == null) {
            throw new IllegalArgumentException("Unable to resolve laid-out diagram endpoint: " + endpoint);
        }
        return result;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static Optional<LaidOutDiagramLabel> layoutCenteredLabel(
            String text,
            int x,
            int y,
            int width,
            TextStyle style,
            TextMeasurer textMeasurer
    ) {
        if (text.isBlank()) {
            return Optional.empty();
        }
        var labelWidth = textMeasurer.measureWidth(text, style);
        var labelHeight = textMeasurer.lineHeight(style);
        return Optional.of(new LaidOutDiagramLabel(
                text,
                x + (width - labelWidth) / 2,
                y,
                labelWidth,
                labelHeight,
                style));
    }
}
