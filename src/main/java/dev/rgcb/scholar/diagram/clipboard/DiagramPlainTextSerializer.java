package dev.rgcb.scholar.diagram.clipboard;

import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentCatalog;
import dev.rgcb.scholar.electrical.ElectricalTerminalDefinition;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import dev.rgcb.scholar.mechanical.MechanicalPrimitive;
import dev.rgcb.scholar.mechanical.MechanicalDimension;
import dev.rgcb.scholar.mechanical.MechanicalConstraint;
import dev.rgcb.scholar.mechanical.MechanicalSymbol;
import dev.rgcb.scholar.mechanical.MechanicalAnnotation;
import dev.rgcb.scholar.mechanical.MechanicalPartReference;
import java.util.Objects;

/**
 * Deterministic, readable plain-text fallback for whole-diagram clipboard operations.
 * The format is intentionally descriptive only: Scholar does not infer DiagramBlock AST
 * from external text. Lossless in-process transfer uses {@link DiagramClipboardPayload}.
 */
public final class DiagramPlainTextSerializer {
    public String serialize(DiagramBlock diagram) {
        Objects.requireNonNull(diagram, "diagram");
        var definition = diagram.definition();
        var output = new StringBuilder();
        output.append("Diagram: ").append(sanitize(definition.title())).append('\n');
        output.append("Canvas: ")
                .append(format(definition.canvas().width()))
                .append(" x ")
                .append(format(definition.canvas().height()));
        var defaultWorkspaceAspectRatio = DiagramBlock.defaultWorkspaceAspectRatio(definition);
        if (Math.abs(diagram.workspaceAspectRatio() - defaultWorkspaceAspectRatio) > 1.0e-12) {
            output.append("\nWorkspace Aspect Ratio: ")
                    .append(format(diagram.workspaceAspectRatio()));
        }

        for (var element : definition.elements()) {
            output.append("\n\n");
            if (element instanceof DiagramNode node) {
                appendNode(output, node);
            } else if (element instanceof ElectricalComponent component) {
                appendElectricalComponent(output, component);
            } else if (element instanceof ElectricalJunction junction) {
                appendElectricalJunction(output, junction);
            } else if (element instanceof MechanicalPrimitive primitive) {
                appendMechanicalPrimitive(output, primitive);
            } else if (element instanceof MechanicalSymbol symbol) {
                output.append("Mechanical Symbol: ").append(symbol.kind().name()).append(" [").append(sanitize(symbol.id().value())).append("]\n");
                output.append("Bounds: [").append(format(symbol.bounds().x())).append(", ").append(format(symbol.bounds().y())).append(", ").append(format(symbol.bounds().width())).append(", ").append(format(symbol.bounds().height())).append(']');
            } else if (element instanceof MechanicalPartReference reference) {
                output.append("Part Reference: ").append(reference.itemNumber()).append(" [").append(sanitize(reference.id().value())).append("]\n");
                output.append("Target: ").append(sanitize(reference.targetId().value())).append("\nPart: ").append(sanitize(reference.partName())).append("\nQuantity: ").append(reference.quantity()).append("\nDescription: ").append(sanitize(reference.description()));
            } else if (element instanceof MechanicalAnnotation annotation) {
                output.append("Mechanical Annotation: ").append(annotation.kind().name()).append(" [").append(sanitize(annotation.id().value())).append("]\n");
                output.append("Text: ").append(sanitize(annotation.text())).append("\nBounds: [").append(format(annotation.bounds().x())).append(", ").append(format(annotation.bounds().y())).append(", ").append(format(annotation.bounds().width())).append(", ").append(format(annotation.bounds().height())).append(']');
            } else if (element instanceof MechanicalDimension dimension) {
                appendMechanicalDimension(output, dimension);
            } else if (element instanceof MechanicalConstraint constraint) {
                appendMechanicalConstraint(output, constraint);
            } else {
                output.append("Element: [").append(sanitize(element.id().value())).append(']');
            }
        }

        if (!definition.connections().isEmpty()) {
            output.append("\n\nConnections:");
            for (var connection : definition.connections()) {
                output.append('\n');
                appendConnection(output, connection);
            }
        }
        return output.toString();
    }

    private static void appendNode(StringBuilder output, DiagramNode node) {
        output.append("Node: ")
                .append(sanitize(node.label()))
                .append(" [")
                .append(sanitize(node.id().value()))
                .append("]\n");
        output.append("Bounds: [")
                .append(format(node.bounds().x())).append(", ")
                .append(format(node.bounds().y())).append(", ")
                .append(format(node.bounds().width())).append(", ")
                .append(format(node.bounds().height())).append(']');
        for (var port : node.ports()) {
            output.append('\n');
            appendPort(output, port);
        }
    }

    private static void appendMechanicalPrimitive(StringBuilder output, MechanicalPrimitive primitive) {
        output.append("Mechanical: ")
                .append(primitive.kind().name())
                .append(" [")
                .append(sanitize(primitive.id().value()))
                .append("]\n");
        output.append("Bounds: [")
                .append(format(primitive.bounds().x())).append(", ")
                .append(format(primitive.bounds().y())).append(", ")
                .append(format(primitive.bounds().width())).append(", ")
                .append(format(primitive.bounds().height())).append(']');
        if (primitive.directional()) {
            output.append("\nOrientation: ").append(primitive.orientation().name());
        }
    }

    private static void appendMechanicalDimension(StringBuilder output, MechanicalDimension dimension) {
        output.append("Mechanical Dimension: ")
                .append(dimension.kind().name())
                .append(" [")
                .append(sanitize(dimension.id().value()))
                .append("]\n");
        output.append("Bounds: [")
                .append(format(dimension.bounds().x())).append(", ")
                .append(format(dimension.bounds().y())).append(", ")
                .append(format(dimension.bounds().width())).append(", ")
                .append(format(dimension.bounds().height())).append("]\n");
        output.append("Measured Value: ").append(sanitize(dimension.displayText()));
    }

    private static void appendMechanicalConstraint(StringBuilder output, MechanicalConstraint constraint) {
        output.append("Mechanical Constraint: ")
                .append(constraint.kind().name())
                .append(" [")
                .append(sanitize(constraint.id().value()))
                .append("]\n");
        output.append("Subject: ").append(sanitize(constraint.subjectId().value()));
        constraint.peerId().ifPresent(peer ->
                output.append("\nPeer: ").append(sanitize(peer.value())));
    }

    private static void appendElectricalComponent(StringBuilder output, ElectricalComponent component) {
        output.append("Component: ")
                .append(component.kind().name())
                .append(' ')
                .append(sanitize(component.referenceDesignator()))
                .append(" [")
                .append(sanitize(component.id().value()))
                .append("]\n");
        output.append("Bounds: [")
                .append(format(component.bounds().x())).append(", ")
                .append(format(component.bounds().y())).append(", ")
                .append(format(component.bounds().width())).append(", ")
                .append(format(component.bounds().height())).append("]\n");
        output.append("Orientation: ").append(component.orientation().name());
        if (!component.valueLabel().isEmpty()) {
            output.append("\nValue: ").append(sanitize(component.valueLabel()));
        }
        var terminalDefinitions = ElectricalComponentCatalog.definition(component.kind()).terminals();
        var ports = component.ports();
        for (var index = 0; index < terminalDefinitions.size(); index++) {
            output.append('\n');
            appendElectricalTerminal(output, terminalDefinitions.get(index), ports.get(index));
        }
    }

    private static void appendElectricalTerminal(
            StringBuilder output,
            ElectricalTerminalDefinition terminal,
            DiagramPort port
    ) {
        output.append("Terminal: ")
                .append(sanitize(port.id().value()))
                .append(" [")
                .append(terminal.role().name())
                .append(", ")
                .append(port.placement().side().name())
                .append(" @ ")
                .append(format(port.placement().offset()))
                .append(']');
    }

    private static void appendElectricalJunction(StringBuilder output, ElectricalJunction junction) {
        output.append("Junction: [")
                .append(sanitize(junction.id().value()))
                .append("]\n");
        output.append("Bounds: [")
                .append(format(junction.bounds().x())).append(", ")
                .append(format(junction.bounds().y())).append(", ")
                .append(format(junction.bounds().width())).append(", ")
                .append(format(junction.bounds().height())).append(']');
        if (!junction.netLabel().isBlank()) {
            output.append("\nNet Label: ").append(sanitize(junction.netLabel()));
        }
        for (var port : junction.ports()) {
            output.append('\n');
            appendPort(output, port);
        }
    }

    private static void appendPort(StringBuilder output, DiagramPort port) {
        output.append("Port: ")
                .append(sanitize(port.id().value()))
                .append(" [")
                .append(port.placement().side().name())
                .append(" @ ")
                .append(format(port.placement().offset()))
                .append(']');
        if (!port.label().isEmpty()) {
            output.append(" - ").append(sanitize(port.label()));
        }
    }

    private static void appendConnection(StringBuilder output, DiagramConnection connection) {
        output.append(endpoint(connection.source().elementId().value(), connection.source().portId().value()))
                .append(" -> ")
                .append(endpoint(connection.target().elementId().value(), connection.target().portId().value()));
        if (!connection.label().isEmpty()) {
            output.append(" : ").append(sanitize(connection.label()));
        }
    }

    private static String endpoint(String elementId, String portId) {
        return sanitize(elementId) + "/" + sanitize(portId);
    }

    private static String format(double value) {
        return Double.toString(value);
    }

    private static String sanitize(String text) {
        return Objects.requireNonNull(text, "text")
                .replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
    }
}
