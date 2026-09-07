package dev.rgcb.scholar.diagram;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DiagramDefinition(
        String title,
        DiagramCanvas canvas,
        List<DiagramElement> elements,
        List<DiagramConnection> connections
) {
    public DiagramDefinition {
        title = Objects.requireNonNull(title, "title");
        canvas = Objects.requireNonNull(canvas, "canvas");
        elements = List.copyOf(Objects.requireNonNull(elements, "elements"));
        connections = List.copyOf(Objects.requireNonNull(connections, "connections"));

        var byId = new HashMap<DiagramElementId, DiagramElement>();
        for (var element : elements) {
            Objects.requireNonNull(element, "element");
            if (byId.putIfAbsent(element.id(), element) != null) {
                throw new IllegalArgumentException("Diagram element ids must be unique: " + element.id().value());
            }
            validateInsideCanvas(element.bounds(), canvas);
        }
        for (var connection : connections) {
            Objects.requireNonNull(connection, "connection");
            requireEndpoint(byId, connection.source());
            requireEndpoint(byId, connection.target());
        }
    }

    private static void validateInsideCanvas(DiagramBounds bounds, DiagramCanvas canvas) {
        var epsilon = 1.0e-9;
        if (bounds.x() < -epsilon || bounds.y() < -epsilon
                || bounds.right() > canvas.width() + epsilon
                || bounds.bottom() > canvas.height() + epsilon) {
            throw new IllegalArgumentException("Diagram element bounds must remain inside the authored canvas.");
        }
    }

    private static void requireEndpoint(Map<DiagramElementId, DiagramElement> byId, DiagramEndpoint endpoint) {
        var element = byId.get(endpoint.elementId());
        if (element == null) {
            throw new IllegalArgumentException("Diagram endpoint references an unknown element: " + endpoint.elementId().value());
        }
        if (element.ports().stream().noneMatch(port -> port.id().equals(endpoint.portId()))) {
            throw new IllegalArgumentException("Diagram endpoint references an unknown port: "
                    + endpoint.elementId().value() + "/" + endpoint.portId().value());
        }
    }
}
