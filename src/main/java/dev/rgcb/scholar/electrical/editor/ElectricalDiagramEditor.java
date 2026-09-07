package dev.rgcb.scholar.electrical.editor;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramEditResult;
import dev.rgcb.scholar.editor.DiagramEditTarget;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.editor.DiagramProperty;
import dev.rgcb.scholar.editor.DiagramPropertyTarget;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentCatalog;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Pure immutable authoring operations layered over the generic M17 DiagramBlock model. */
public final class ElectricalDiagramEditor {
    private static final double PLACEMENT_GAP = 2.0;


    public Optional<ElectricalJunction> selectedJunction(DiagramBlock diagram, DiagramEditTarget target) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        if (!(target instanceof DiagramElementTarget elementTarget)) {
            return Optional.empty();
        }
        if (elementTarget.elementIndex() < 0 || elementTarget.elementIndex() >= diagram.definition().elements().size()) {
            return Optional.empty();
        }
        var element = diagram.definition().elements().get(elementTarget.elementIndex());
        if (!element.id().equals(elementTarget.elementId()) || !(element instanceof ElectricalJunction junction)) {
            return Optional.empty();
        }
        return Optional.of(junction);
    }

    public DiagramEditResult addJunction(DiagramBlock diagram, DiagramEditTarget currentTarget) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(currentTarget, "currentTarget");
        var definition = diagram.definition();
        var ordinal = 1;
        while (containsElementId(definition.elements(), "junction-" + ordinal)) {
            ordinal++;
        }
        var size = 4.0;
        var width = Math.min(size, definition.canvas().width());
        var height = Math.min(size, definition.canvas().height());
        var x = Math.max(0.0, (definition.canvas().width() - width) / 2.0);
        var y = Math.max(0.0, (definition.canvas().height() - height) / 2.0);
        var id = new DiagramElementId("junction-" + ordinal);
        var junction = new ElectricalJunction(id, new DiagramBounds(x, y, width, height), "");
        var elements = new ArrayList<>(definition.elements());
        elements.add(junction);
        var target = new DiagramElementTarget(elements.size() - 1, id);
        return changed(diagram, target, definition, elements, definition.connections());
    }

    public boolean canDeleteJunction(DiagramBlock diagram, DiagramEditTarget target) {
        return selectedJunction(diagram, target).isPresent();
    }

    /** Deletes one explicit junction and all wire segments incident to it. */
    public DiagramEditResult deleteJunction(DiagramBlock diagram, DiagramEditTarget target) {
        var selected = selectedJunction(diagram, target);
        if (selected.isEmpty()) {
            return new DiagramEditResult(diagram, target, false);
        }
        var elementTarget = (DiagramElementTarget) target;
        var definition = diagram.definition();
        var removedId = selected.orElseThrow().id();
        var elements = new ArrayList<>(definition.elements());
        elements.remove(elementTarget.elementIndex());
        var connections = definition.connections().stream()
                .filter(connection -> !connection.source().elementId().equals(removedId)
                        && !connection.target().elementId().equals(removedId))
                .toList();
        var nextTarget = targetAfterDeletion(elements, elementTarget.elementIndex());
        return changed(diagram, nextTarget, definition, elements, connections);
    }

    public Optional<String> junctionNetLabel(DiagramBlock diagram, DiagramEditTarget target) {
        return selectedJunction(diagram, target).map(ElectricalJunction::netLabel);
    }

    public DiagramEditResult setJunctionNetLabel(DiagramBlock diagram, DiagramEditTarget target, String label) {
        Objects.requireNonNull(label, "label");
        var selected = selectedJunction(diagram, target);
        if (selected.isEmpty()) {
            return new DiagramEditResult(diagram, target, false);
        }
        var old = selected.orElseThrow();
        if (old.netLabel().equals(label)) {
            return new DiagramEditResult(diagram, target, false);
        }
        var elementTarget = (DiagramElementTarget) target;
        var definition = diagram.definition();
        var elements = new ArrayList<>(definition.elements());
        elements.set(elementTarget.elementIndex(), old.withNetLabel(label));
        return changed(diagram, target, definition, elements, definition.connections());
    }

    public Optional<ElectricalComponent> selectedComponent(DiagramBlock diagram, DiagramEditTarget target) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        if (!(target instanceof DiagramElementTarget elementTarget)) {
            return Optional.empty();
        }
        if (elementTarget.elementIndex() < 0 || elementTarget.elementIndex() >= diagram.definition().elements().size()) {
            return Optional.empty();
        }
        var element = diagram.definition().elements().get(elementTarget.elementIndex());
        if (!element.id().equals(elementTarget.elementId()) || !(element instanceof ElectricalComponent component)) {
            return Optional.empty();
        }
        return Optional.of(component);
    }

    public DiagramEditResult addComponent(
            DiagramBlock diagram,
            DiagramEditTarget currentTarget,
            ElectricalComponentKind kind
    ) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(currentTarget, "currentTarget");
        Objects.requireNonNull(kind, "kind");

        var definition = diagram.definition();
        var id = nextElementId(definition, kind);
        var reference = nextReferenceDesignator(definition, kind);
        var bounds = defaultBounds(definition, kind);
        var component = new ElectricalComponent(
                id,
                bounds,
                kind,
                ElectricalOrientation.DEG_0,
                reference,
                "");
        var elements = new ArrayList<>(definition.elements());
        elements.add(component);
        var target = new DiagramElementTarget(elements.size() - 1, id);
        return changed(diagram, target, definition, elements, definition.connections());
    }

    public boolean canRotate(DiagramBlock diagram, DiagramEditTarget target) {
        return selectedComponent(diagram, target).isPresent();
    }

    public DiagramEditResult rotateClockwise(DiagramBlock diagram, DiagramEditTarget target) {
        return rotate(diagram, target, true);
    }

    public DiagramEditResult rotateCounterClockwise(DiagramBlock diagram, DiagramEditTarget target) {
        return rotate(diagram, target, false);
    }

    public Optional<ElectricalComponentDraft> draft(DiagramBlock diagram, DiagramEditTarget target) {
        return selectedComponent(diagram, target)
                .map(component -> new ElectricalComponentDraft(
                        component.referenceDesignator(),
                        component.valueLabel()));
    }

    /** Updates both annotations in one immutable/history-friendly edit. */
    public DiagramEditResult setAnnotations(
            DiagramBlock diagram,
            DiagramEditTarget target,
            String referenceDesignator,
            String valueLabel
    ) {
        Objects.requireNonNull(referenceDesignator, "referenceDesignator");
        Objects.requireNonNull(valueLabel, "valueLabel");
        var component = selectedComponent(diagram, target);
        if (component.isEmpty()) {
            return new DiagramEditResult(diagram, target, false);
        }
        var old = component.orElseThrow();
        if (old.referenceDesignator().equals(referenceDesignator) && old.valueLabel().equals(valueLabel)) {
            return new DiagramEditResult(diagram, target, false);
        }
        var replacement = new ElectricalComponent(
                old.id(), old.bounds(), old.kind(), old.orientation(), referenceDesignator, valueLabel);
        return replaceElement(diagram, (DiagramElementTarget) target, replacement, target);
    }

    public boolean canDelete(DiagramBlock diagram, DiagramEditTarget target) {
        return selectedComponent(diagram, target).isPresent();
    }

    /** Deletes one component plus every incident semantic wire in one valid replacement. */
    public DiagramEditResult deleteComponent(DiagramBlock diagram, DiagramEditTarget target) {
        var selected = selectedComponent(diagram, target);
        if (selected.isEmpty()) {
            return new DiagramEditResult(diagram, target, false);
        }
        var elementTarget = (DiagramElementTarget) target;
        var definition = diagram.definition();
        var removedId = selected.orElseThrow().id();
        var elements = new ArrayList<>(definition.elements());
        elements.remove(elementTarget.elementIndex());
        var connections = definition.connections().stream()
                .filter(connection -> !connection.source().elementId().equals(removedId)
                        && !connection.target().elementId().equals(removedId))
                .toList();
        var nextTarget = targetAfterDeletion(elements, elementTarget.elementIndex());
        return changed(diagram, nextTarget, definition, elements, connections);
    }

    public boolean canScaleAllComponents(DiagramBlock diagram) {
        Objects.requireNonNull(diagram, "diagram");
        return diagram.definition().elements().stream().anyMatch(ElectricalComponent.class::isInstance);
    }

    /**
     * Uniformly scales all electrical component bounds about their centers.
     * Logical terminal schemas, orientation, annotations, connections, generic nodes,
     * and explicit junctions remain untouched.
     */
    public DiagramEditResult scaleAllComponents(
            DiagramBlock diagram,
            DiagramEditTarget target,
            double factor
    ) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(factor) || factor <= 0.0) {
            throw new IllegalArgumentException("Electrical symbol scale factor must be finite and positive.");
        }
        if (!canScaleAllComponents(diagram) || Math.abs(factor - 1.0) < 1.0e-9) {
            return new DiagramEditResult(diagram, target, false);
        }

        var canvas = diagram.definition().canvas();
        var elements = new ArrayList<DiagramElement>(diagram.definition().elements().size());
        var changed = false;
        for (var element : diagram.definition().elements()) {
            if (!(element instanceof ElectricalComponent component)) {
                elements.add(element);
                continue;
            }
            var old = component.bounds();
            var width = Math.min(canvas.width(), Math.max(2.0, old.width() * factor));
            var height = Math.min(canvas.height(), Math.max(2.0, old.height() * factor));
            var centerX = old.x() + old.width() / 2.0;
            var centerY = old.y() + old.height() / 2.0;
            var x = clamp(centerX - width / 2.0, 0.0, Math.max(0.0, canvas.width() - width));
            var y = clamp(centerY - height / 2.0, 0.0, Math.max(0.0, canvas.height() - height));
            var bounds = new DiagramBounds(x, y, width, height);
            if (!bounds.equals(old)) {
                changed = true;
            }
            elements.add(new ElectricalComponent(
                    component.id(), bounds, component.kind(), component.orientation(),
                    component.referenceDesignator(), component.valueLabel()));
        }
        if (!changed) {
            return new DiagramEditResult(diagram, target, false);
        }
        return changed(diagram, target, diagram.definition(), elements, diagram.definition().connections());
    }

    private DiagramEditResult rotate(DiagramBlock diagram, DiagramEditTarget target, boolean clockwise) {
        var selected = selectedComponent(diagram, target);
        if (selected.isEmpty()) {
            return new DiagramEditResult(diagram, target, false);
        }
        var old = selected.orElseThrow();
        var orientation = clockwise
                ? old.orientation().rotateClockwise()
                : old.orientation().rotateCounterClockwise();
        var parityChanged = (old.orientation().quarterTurnsClockwise() & 1)
                != (orientation.quarterTurnsClockwise() & 1);
        var width = parityChanged ? old.bounds().height() : old.bounds().width();
        var height = parityChanged ? old.bounds().width() : old.bounds().height();
        var centerX = old.bounds().x() + old.bounds().width() / 2.0;
        var centerY = old.bounds().y() + old.bounds().height() / 2.0;
        var canvas = diagram.definition().canvas();
        width = Math.min(width, canvas.width());
        height = Math.min(height, canvas.height());
        var x = clamp(centerX - width / 2.0, 0.0, Math.max(0.0, canvas.width() - width));
        var y = clamp(centerY - height / 2.0, 0.0, Math.max(0.0, canvas.height() - height));
        var replacement = new ElectricalComponent(
                old.id(),
                new DiagramBounds(x, y, width, height),
                old.kind(),
                orientation,
                old.referenceDesignator(),
                old.valueLabel());
        return replaceElement(diagram, (DiagramElementTarget) target, replacement, target);
    }

    private static DiagramEditResult replaceElement(
            DiagramBlock diagram,
            DiagramElementTarget selected,
            ElectricalComponent replacement,
            DiagramEditTarget nextTarget
    ) {
        var definition = diagram.definition();
        var elements = new ArrayList<>(definition.elements());
        elements.set(selected.elementIndex(), replacement);
        return changed(diagram, nextTarget, definition, elements, definition.connections());
    }

    private static DiagramEditResult changed(
            DiagramBlock original,
            DiagramEditTarget target,
            DiagramDefinition definition,
            List<DiagramElement> elements,
            List<dev.rgcb.scholar.diagram.DiagramConnection> connections
    ) {
        return new DiagramEditResult(
                original.withDefinition(new DiagramDefinition(
                        definition.title(), definition.canvas(), elements, connections)),
                target,
                true);
    }

    private static DiagramElementId nextElementId(DiagramDefinition definition, ElectricalComponentKind kind) {
        var stem = idStem(kind);
        var ordinal = 1;
        while (containsElementId(definition.elements(), stem + "-" + ordinal)) {
            ordinal++;
        }
        return new DiagramElementId(stem + "-" + ordinal);
    }

    private static String nextReferenceDesignator(DiagramDefinition definition, ElectricalComponentKind kind) {
        var prefix = ElectricalComponentCatalog.definition(kind).defaultReferencePrefix();
        if (kind == ElectricalComponentKind.GROUND
                && definition.elements().stream()
                        .filter(ElectricalComponent.class::isInstance)
                        .map(ElectricalComponent.class::cast)
                        .noneMatch(component -> component.referenceDesignator().equals(prefix))) {
            return prefix;
        }
        var ordinal = 1;
        while (containsReferenceDesignator(definition.elements(), prefix + ordinal)) {
            ordinal++;
        }
        return prefix + ordinal;
    }

    private static boolean containsReferenceDesignator(List<DiagramElement> elements, String value) {
        return elements.stream()
                .filter(ElectricalComponent.class::isInstance)
                .map(ElectricalComponent.class::cast)
                .anyMatch(component -> component.referenceDesignator().equals(value));
    }

    private static boolean containsElementId(List<DiagramElement> elements, String value) {
        return elements.stream().anyMatch(element -> element.id().value().equals(value));
    }

    private static DiagramBounds defaultBounds(DiagramDefinition definition, ElectricalComponentKind kind) {
        var canvas = definition.canvas();
        var canonical = canonicalSize(kind);
        var width = Math.min(canonical.width(), canvas.width());
        var height = Math.min(canonical.height(), canvas.height());
        var maxX = Math.max(0.0, canvas.width() - width);
        var maxY = Math.max(0.0, canvas.height() - height);

        var centered = new DiagramBounds(maxX / 2.0, maxY / 2.0, width, height);
        if (definition.elements().stream().noneMatch(element -> overlaps(centered, element.bounds()))) {
            return centered;
        }

        var stepX = Math.max(1.0, width + PLACEMENT_GAP);
        var stepY = Math.max(1.0, height + PLACEMENT_GAP);
        for (double y = 0.0; y <= maxY + 1.0e-9; y += stepY) {
            for (double x = 0.0; x <= maxX + 1.0e-9; x += stepX) {
                var candidate = new DiagramBounds(Math.min(x, maxX), Math.min(y, maxY), width, height);
                if (definition.elements().stream().noneMatch(element -> overlaps(candidate, element.bounds()))) {
                    return candidate;
                }
            }
        }
        return centered;
    }

    private static Size canonicalSize(ElectricalComponentKind kind) {
        return switch (kind) {
            case RESISTOR, CAPACITOR, DIODE, LED, SWITCH_SPST -> new Size(28.0, 12.0);
            case DC_VOLTAGE_SOURCE -> new Size(30.0, 18.0);
            case GROUND -> new Size(18.0, 12.0);
        };
    }

    private static String idStem(ElectricalComponentKind kind) {
        return switch (kind) {
            case RESISTOR -> "resistor";
            case CAPACITOR -> "capacitor";
            case DC_VOLTAGE_SOURCE -> "voltage-source";
            case GROUND -> "ground";
            case DIODE -> "diode";
            case LED -> "led";
            case SWITCH_SPST -> "switch";
        };
    }

    private static DiagramEditTarget targetAfterDeletion(List<DiagramElement> elements, int removedIndex) {
        if (elements.isEmpty()) {
            return new DiagramPropertyTarget(DiagramProperty.TITLE);
        }
        var index = Math.min(removedIndex, elements.size() - 1);
        return new DiagramElementTarget(index, elements.get(index).id());
    }

    private static boolean overlaps(DiagramBounds first, DiagramBounds second) {
        return first.x() < second.right() && first.right() > second.x()
                && first.y() < second.bottom() && first.bottom() > second.y();
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private record Size(double width, double height) {
    }
}
