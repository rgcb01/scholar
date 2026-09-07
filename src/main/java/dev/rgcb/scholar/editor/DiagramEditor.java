package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.document.DiagramBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure immutable editing operations for DiagramBlock semantic state. */
public final class DiagramEditor {
    private static final double DEFAULT_NODE_WIDTH = 24.0;
    private static final double DEFAULT_NODE_HEIGHT = 16.0;
    private static final double MIN_WORKSPACE_ASPECT_RATIO = 0.25;
    private static final double MAX_WORKSPACE_ASPECT_RATIO = 1.50;

    public DiagramEditTarget firstTarget(DiagramBlock diagram) {
        Objects.requireNonNull(diagram, "diagram");
        if (!diagram.definition().elements().isEmpty()) {
            var element = diagram.definition().elements().get(0);
            return new DiagramElementTarget(0, element.id());
        }
        return new DiagramPropertyTarget(DiagramProperty.TITLE);
    }

    public void validateSelection(DiagramBlock diagram, DiagramEditTarget target) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        if (target instanceof DiagramPropertyTarget) {
            return;
        }
        if (target instanceof DiagramElementTarget elementTarget) {
            requireElement(diagram, elementTarget.elementIndex(), elementTarget.elementId());
            return;
        }
        if (target instanceof DiagramPortTarget portTarget) {
            requirePort(diagram, portTarget);
            return;
        }
        if (target instanceof DiagramConnectionTarget connectionTarget) {
            if (connectionTarget.connectionIndex() >= diagram.definition().connections().size()) {
                throw new IllegalArgumentException("Diagram connection selection is outside the diagram.");
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported diagram edit target: " + target.getClass().getName());
    }

    public DiagramEditTarget nextTarget(DiagramBlock diagram, DiagramEditTarget current) {
        return adjacentTarget(diagram, current, 1);
    }

    public DiagramEditTarget previousTarget(DiagramBlock diagram, DiagramEditTarget current) {
        return adjacentTarget(diagram, current, -1);
    }

    public boolean canEditText(DiagramBlock diagram, DiagramEditTarget target) {
        validateSelection(diagram, target);
        if (target instanceof DiagramPropertyTarget propertyTarget) {
            return propertyTarget.property() == DiagramProperty.TITLE;
        }
        if (target instanceof DiagramElementTarget elementTarget) {
            return requireElement(diagram, elementTarget.elementIndex(), elementTarget.elementId()) instanceof DiagramNode;
        }
        if (target instanceof DiagramPortTarget portTarget) {
            return requireElement(diagram, portTarget.elementIndex(), portTarget.elementId()) instanceof DiagramNode;
        }
        return target instanceof DiagramConnectionTarget;
    }

    public String textValue(DiagramBlock diagram, DiagramEditTarget target) {
        if (!canEditText(diagram, target)) {
            throw new IllegalArgumentException("Selected diagram target has no editable text value.");
        }
        if (target instanceof DiagramPropertyTarget) {
            return diagram.definition().title();
        }
        if (target instanceof DiagramElementTarget elementTarget) {
            var element = requireElement(diagram, elementTarget.elementIndex(), elementTarget.elementId());
            if (element instanceof DiagramNode node) {
                return node.label();
            }
        }
        if (target instanceof DiagramPortTarget portTarget) {
            return requirePort(diagram, portTarget).label();
        }
        if (target instanceof DiagramConnectionTarget connectionTarget) {
            return diagram.definition().connections().get(connectionTarget.connectionIndex()).label();
        }
        throw new IllegalArgumentException("Unsupported diagram text target: " + target.getClass().getName());
    }

    public DiagramEditResult setText(DiagramBlock diagram, DiagramEditTarget target, String text) {
        Objects.requireNonNull(text, "text");
        validateSelection(diagram, target);
        if (!canEditText(diagram, target)) {
            return new DiagramEditResult(diagram, target, false);
        }
        if (Objects.equals(textValue(diagram, target), text)) {
            return new DiagramEditResult(diagram, target, false);
        }

        var definition = diagram.definition();
        if (target instanceof DiagramPropertyTarget propertyTarget && propertyTarget.property() == DiagramProperty.TITLE) {
            return changed(diagram, target, new DiagramDefinition(
                    text, definition.canvas(), definition.elements(), definition.connections()));
        }
        if (target instanceof DiagramElementTarget elementTarget) {
            var element = requireElement(diagram, elementTarget.elementIndex(), elementTarget.elementId());
            if (element instanceof DiagramNode node) {
                var replacement = new DiagramNode(node.id(), node.bounds(), text, node.ports());
                var elements = new ArrayList<>(definition.elements());
                elements.set(elementTarget.elementIndex(), replacement);
                return changed(diagram, target, new DiagramDefinition(
                        definition.title(), definition.canvas(), elements, definition.connections()));
            }
            return new DiagramEditResult(diagram, target, false);
        }
        if (target instanceof DiagramPortTarget portTarget) {
            var element = requireElement(diagram, portTarget.elementIndex(), portTarget.elementId());
            if (element instanceof DiagramNode node) {
                var oldPort = requirePort(diagram, portTarget);
                var ports = new ArrayList<>(node.ports());
                ports.set(portTarget.portIndex(), new DiagramPort(oldPort.id(), text, oldPort.placement()));
                var replacement = new DiagramNode(node.id(), node.bounds(), node.label(), ports);
                var elements = new ArrayList<>(definition.elements());
                elements.set(portTarget.elementIndex(), replacement);
                return changed(diagram, target, new DiagramDefinition(
                        definition.title(), definition.canvas(), elements, definition.connections()));
            }
            return new DiagramEditResult(diagram, target, false);
        }
        if (target instanceof DiagramConnectionTarget connectionTarget) {
            var old = definition.connections().get(connectionTarget.connectionIndex());
            var connections = new ArrayList<>(definition.connections());
            connections.set(connectionTarget.connectionIndex(), new DiagramConnection(old.source(), old.target(), text));
            return changed(diagram, target, new DiagramDefinition(
                    definition.title(), definition.canvas(), definition.elements(), connections));
        }
        return new DiagramEditResult(diagram, target, false);
    }

    /** Adds one generic node with four reusable perimeter ports. */
    public DiagramEditResult addNode(DiagramBlock diagram, DiagramEditTarget currentTarget) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(currentTarget, "currentTarget");
        validateSelection(diagram, currentTarget);

        var definition = diagram.definition();
        var ordinal = nextNodeOrdinal(definition);
        var id = new DiagramElementId("node-" + ordinal);
        var bounds = defaultNodeBounds(definition);
        var ports = List.of(
                new DiagramPort(new DiagramPortId("left"), "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5)),
                new DiagramPort(new DiagramPortId("right"), "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)),
                new DiagramPort(new DiagramPortId("top"), "", new DiagramPortPlacement(DiagramPortSide.TOP, 0.5)),
                new DiagramPort(new DiagramPortId("bottom"), "", new DiagramPortPlacement(DiagramPortSide.BOTTOM, 0.5)));
        var node = new DiagramNode(id, bounds, "Node " + ordinal, ports);
        var elements = new ArrayList<>(definition.elements());
        elements.add(node);
        var target = new DiagramElementTarget(elements.size() - 1, id);
        return changed(diagram, target, new DiagramDefinition(
                definition.title(), definition.canvas(), elements, definition.connections()));
    }

    public boolean canDeleteNode(DiagramBlock diagram, DiagramEditTarget target) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        if (!(target instanceof DiagramElementTarget elementTarget)) {
            return false;
        }
        validateSelection(diagram, elementTarget);
        return requireElement(diagram, elementTarget.elementIndex(), elementTarget.elementId()) instanceof DiagramNode;
    }

    /** Deletes the node and all incident connections in one valid immutable replacement. */
    public DiagramEditResult deleteNode(DiagramBlock diagram, DiagramEditTarget target) {
        if (!canDeleteNode(diagram, target)) {
            return new DiagramEditResult(diagram, target, false);
        }
        var elementTarget = (DiagramElementTarget) target;
        var definition = diagram.definition();
        var removedId = elementTarget.elementId();
        var elements = new ArrayList<>(definition.elements());
        elements.remove(elementTarget.elementIndex());
        var connections = definition.connections().stream()
                .filter(connection -> !connection.source().elementId().equals(removedId)
                        && !connection.target().elementId().equals(removedId))
                .toList();
        var nextTarget = targetAfterNodeDeletion(elements, elementTarget.elementIndex());
        return changed(diagram, nextTarget, new DiagramDefinition(
                definition.title(), definition.canvas(), elements, connections));
    }

    public boolean canAddConnection(DiagramBlock diagram, DiagramPortTarget source, DiagramPortTarget target) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        validateSelection(diagram, source);
        validateSelection(diagram, target);
        return !endpoint(source).equals(endpoint(target));
    }

    /** Adds a semantic port-to-port connection; routed geometry remains derived. */
    public DiagramEditResult addConnection(DiagramBlock diagram, DiagramPortTarget source, DiagramPortTarget target) {
        if (!canAddConnection(diagram, source, target)) {
            return new DiagramEditResult(diagram, target, false);
        }
        var definition = diagram.definition();
        var connections = new ArrayList<>(definition.connections());
        connections.add(new DiagramConnection(endpoint(source), endpoint(target), ""));
        var selected = new DiagramConnectionTarget(connections.size() - 1);
        return changed(diagram, selected, new DiagramDefinition(
                definition.title(), definition.canvas(), definition.elements(), connections));
    }

    public boolean canDeleteConnection(DiagramBlock diagram, DiagramEditTarget target) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        if (!(target instanceof DiagramConnectionTarget connectionTarget)) {
            return false;
        }
        validateSelection(diagram, connectionTarget);
        return true;
    }

    public DiagramEditResult deleteConnection(DiagramBlock diagram, DiagramEditTarget target) {
        if (!canDeleteConnection(diagram, target)) {
            return new DiagramEditResult(diagram, target, false);
        }
        var connectionTarget = (DiagramConnectionTarget) target;
        var definition = diagram.definition();
        var connections = new ArrayList<>(definition.connections());
        connections.remove(connectionTarget.connectionIndex());
        var nextTarget = targetAfterConnectionDeletion(definition.elements(), connections, connectionTarget.connectionIndex());
        return changed(diagram, nextTarget, new DiagramDefinition(
                definition.title(), definition.canvas(), definition.elements(), connections));
    }

    /**
     * Moves the selected element by assigning its logical top-left coordinate.
     * The final position is clamped so the complete element remains inside the
     * authored logical canvas.
     */
    public DiagramEditResult moveElement(
            DiagramBlock diagram,
            DiagramElementTarget target,
            double logicalX,
            double logicalY
    ) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(logicalX) || !Double.isFinite(logicalY)) {
            throw new IllegalArgumentException("Diagram drag coordinates must be finite.");
        }
        validateSelection(diagram, target);

        var definition = diagram.definition();
        var element = requireElement(diagram, target.elementIndex(), target.elementId());

        var canvas = definition.canvas();
        var x = clamp(logicalX, 0.0, Math.max(0.0, canvas.width() - element.bounds().width()));
        var y = clamp(logicalY, 0.0, Math.max(0.0, canvas.height() - element.bounds().height()));
        var replacementBounds = new DiagramBounds(x, y, element.bounds().width(), element.bounds().height());
        if (replacementBounds.equals(element.bounds())) {
            return new DiagramEditResult(diagram, target, false);
        }

        var replacement = element.withBounds(replacementBounds);
        var elements = new ArrayList<>(definition.elements());
        elements.set(target.elementIndex(), replacement);
        var updated = new DiagramDefinition(
                definition.title(),
                definition.canvas(),
                elements,
                definition.connections());
        return new DiagramEditResult(diagram.withDefinition(updated), target, true);
    }

    /**
     * Resizes the authored logical canvas. Existing elements keep their size and
     * are translated inward only when an edge would otherwise fall outside the
     * new canvas. A canvas smaller than an existing element is rejected without
     * changing the document.
     */
    public DiagramEditResult resizeCanvas(
            DiagramBlock diagram,
            DiagramEditTarget target,
            double width,
            double height
    ) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        validateSelection(diagram, target);
        var replacementCanvas = new DiagramCanvas(width, height);
        var definition = diagram.definition();

        for (var element : definition.elements()) {
            if (element.bounds().width() > width || element.bounds().height() > height) {
                return new DiagramEditResult(diagram, target, false);
            }
        }

        var elements = new ArrayList<DiagramElement>();
        for (var element : definition.elements()) {
            var bounds = element.bounds();
            var x = clamp(bounds.x(), 0.0, Math.max(0.0, width - bounds.width()));
            var y = clamp(bounds.y(), 0.0, Math.max(0.0, height - bounds.height()));
            elements.add(element.withBounds(new DiagramBounds(x, y, bounds.width(), bounds.height())));
        }
        var updated = new DiagramDefinition(
                definition.title(),
                replacementCanvas,
                elements,
                definition.connections());
        if (updated.equals(definition)) {
            return new DiagramEditResult(diagram, target, false);
        }
        return new DiagramEditResult(diagram.withDefinition(updated), target, true);
    }

    public DiagramEditResult scaleWorkspaceHeight(
            DiagramBlock diagram,
            DiagramEditTarget target,
            double factor
    ) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(factor) || factor <= 0.0) {
            throw new IllegalArgumentException("Workspace height scale factor must be finite and positive.");
        }
        validateSelection(diagram, target);
        var desired = clamp(
                diagram.workspaceAspectRatio() * factor,
                MIN_WORKSPACE_ASPECT_RATIO,
                MAX_WORKSPACE_ASPECT_RATIO);
        var replacement = diagram.withWorkspaceAspectRatio(desired);
        return new DiagramEditResult(replacement, target, !replacement.equals(diagram));
    }

    public DiagramEditResult resetWorkspaceHeight(DiagramBlock diagram, DiagramEditTarget target) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        validateSelection(diagram, target);
        var replacement = diagram.resetWorkspaceAspectRatio();
        return new DiagramEditResult(replacement, target, !replacement.equals(diagram));
    }

    private DiagramEditTarget adjacentTarget(DiagramBlock diagram, DiagramEditTarget current, int direction) {
        validateSelection(diagram, current);
        var targets = targets(diagram);
        var index = targets.indexOf(current);
        if (index < 0) {
            return firstTarget(diagram);
        }
        var next = Math.max(0, Math.min(targets.size() - 1, index + direction));
        return targets.get(next);
    }

    private List<DiagramEditTarget> targets(DiagramBlock diagram) {
        var targets = new ArrayList<DiagramEditTarget>();
        targets.add(new DiagramPropertyTarget(DiagramProperty.TITLE));
        for (var elementIndex = 0; elementIndex < diagram.definition().elements().size(); elementIndex++) {
            var element = diagram.definition().elements().get(elementIndex);
            targets.add(new DiagramElementTarget(elementIndex, element.id()));
            for (var portIndex = 0; portIndex < element.ports().size(); portIndex++) {
                var port = element.ports().get(portIndex);
                targets.add(new DiagramPortTarget(elementIndex, portIndex, element.id(), port.id()));
            }
        }
        for (var connectionIndex = 0; connectionIndex < diagram.definition().connections().size(); connectionIndex++) {
            targets.add(new DiagramConnectionTarget(connectionIndex));
        }
        targets.add(new DiagramPropertyTarget(DiagramProperty.CANVAS));
        return List.copyOf(targets);
    }

    private static DiagramEditResult changed(
            DiagramBlock original,
            DiagramEditTarget target,
            DiagramDefinition definition
    ) {
        return new DiagramEditResult(original.withDefinition(definition), target, true);
    }

    private static int nextNodeOrdinal(DiagramDefinition definition) {
        var ordinal = 1;
        while (containsElementId(definition.elements(), "node-" + ordinal)) {
            ordinal++;
        }
        return ordinal;
    }

    private static boolean containsElementId(List<DiagramElement> elements, String value) {
        return elements.stream().anyMatch(element -> element.id().value().equals(value));
    }

    private static DiagramBounds defaultNodeBounds(DiagramDefinition definition) {
        var canvas = definition.canvas();
        var width = Math.min(DEFAULT_NODE_WIDTH, canvas.width());
        var height = Math.min(DEFAULT_NODE_HEIGHT, canvas.height());
        var maxX = Math.max(0.0, canvas.width() - width);
        var maxY = Math.max(0.0, canvas.height() - height);
        var candidates = List.of(
                new DiagramBounds(maxX / 2.0, 0.0, width, height),
                new DiagramBounds(maxX / 2.0, maxY, width, height),
                new DiagramBounds(0.0, maxY / 2.0, width, height),
                new DiagramBounds(maxX, maxY / 2.0, width, height),
                new DiagramBounds(maxX / 2.0, maxY / 2.0, width, height),
                new DiagramBounds(0.0, 0.0, width, height),
                new DiagramBounds(maxX, 0.0, width, height),
                new DiagramBounds(0.0, maxY, width, height),
                new DiagramBounds(maxX, maxY, width, height));
        for (var candidate : candidates) {
            if (definition.elements().stream().noneMatch(element -> overlaps(candidate, element.bounds()))) {
                return candidate;
            }
        }
        return candidates.get(4);
    }

    private static boolean overlaps(DiagramBounds first, DiagramBounds second) {
        return first.x() < second.right() && first.right() > second.x()
                && first.y() < second.bottom() && first.bottom() > second.y();
    }

    private static DiagramEditTarget targetAfterNodeDeletion(List<DiagramElement> elements, int removedIndex) {
        if (elements.isEmpty()) {
            return new DiagramPropertyTarget(DiagramProperty.TITLE);
        }
        var index = Math.min(removedIndex, elements.size() - 1);
        return new DiagramElementTarget(index, elements.get(index).id());
    }

    private static DiagramEditTarget targetAfterConnectionDeletion(
            List<DiagramElement> elements,
            List<DiagramConnection> connections,
            int removedIndex
    ) {
        if (!connections.isEmpty()) {
            return new DiagramConnectionTarget(Math.min(removedIndex, connections.size() - 1));
        }
        if (!elements.isEmpty()) {
            return new DiagramElementTarget(0, elements.get(0).id());
        }
        return new DiagramPropertyTarget(DiagramProperty.TITLE);
    }

    private static DiagramEndpoint endpoint(DiagramPortTarget target) {
        return new DiagramEndpoint(target.elementId(), target.portId());
    }

    private static DiagramPort requirePort(DiagramBlock diagram, DiagramPortTarget target) {
        var element = requireElement(diagram, target.elementIndex(), target.elementId());
        if (target.portIndex() >= element.ports().size()) {
            throw new IllegalArgumentException("Diagram port selection is outside the selected element.");
        }
        var port = element.ports().get(target.portIndex());
        if (!port.id().equals(target.portId())) {
            throw new IllegalArgumentException("Diagram port selection id does not match the selected port index.");
        }
        return port;
    }

    private static DiagramElement requireElement(
            DiagramBlock diagram,
            int elementIndex,
            DiagramElementId elementId
    ) {
        if (elementIndex >= diagram.definition().elements().size()) {
            throw new IllegalArgumentException("Diagram element selection is outside the diagram.");
        }
        var element = diagram.definition().elements().get(elementIndex);
        if (!element.id().equals(elementId)) {
            throw new IllegalArgumentException("Diagram element selection id does not match the selected element index.");
        }
        return element;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
