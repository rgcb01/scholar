package dev.rgcb.scholar.electrical.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramEditor;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.editor.DiagramProperty;
import dev.rgcb.scholar.editor.DiagramPropertyTarget;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectricalDiagramAuthoringTest {
    private final ElectricalDiagramEditor electricalEditor = new ElectricalDiagramEditor();
    private final DiagramEditor diagramEditor = new DiagramEditor();

    @Test
    void addResistorCreatesDeterministicSemanticComponent() {
        var result = electricalEditor.addComponent(emptyDiagram(), new DiagramPropertyTarget(DiagramProperty.TITLE), ElectricalComponentKind.RESISTOR);
        var component = (ElectricalComponent) result.diagram().definition().elements().get(0);

        assertTrue(result.changed());
        assertEquals(ElectricalComponentKind.RESISTOR, component.kind());
        assertEquals(ElectricalOrientation.DEG_0, component.orientation());
        assertEquals("R1", component.referenceDesignator());
        assertEquals("", component.valueLabel());
        assertEquals("resistor-1", component.id().value());
        assertEquals(new DiagramElementTarget(0, component.id()), result.target());
    }

    @Test
    void repeatedInsertionUsesNextReferenceAndElementIdentity() {
        var first = electricalEditor.addComponent(emptyDiagram(), new DiagramPropertyTarget(DiagramProperty.TITLE), ElectricalComponentKind.RESISTOR);
        var second = electricalEditor.addComponent(first.diagram(), first.target(), ElectricalComponentKind.RESISTOR);
        var components = second.diagram().definition().elements().stream().map(ElectricalComponent.class::cast).toList();

        assertEquals(List.of("R1", "R2"), components.stream().map(ElectricalComponent::referenceDesignator).toList());
        assertEquals(List.of("resistor-1", "resistor-2"), components.stream().map(c -> c.id().value()).toList());
    }

    @Test
    void diodeAndLedShareConventionalDReferenceNamespace() {
        var diode = electricalEditor.addComponent(emptyDiagram(), new DiagramPropertyTarget(DiagramProperty.TITLE), ElectricalComponentKind.DIODE);
        var led = electricalEditor.addComponent(diode.diagram(), diode.target(), ElectricalComponentKind.LED);
        var components = led.diagram().definition().elements().stream().map(ElectricalComponent.class::cast).toList();

        assertEquals("D1", components.get(0).referenceDesignator());
        assertEquals("D2", components.get(1).referenceDesignator());
        assertEquals("diode-1", components.get(0).id().value());
        assertEquals("led-1", components.get(1).id().value());
    }

    @Test
    void groundUsesReadableFirstDesignatorThenDeterministicOrdinal() {
        var first = electricalEditor.addComponent(emptyDiagram(), new DiagramPropertyTarget(DiagramProperty.TITLE), ElectricalComponentKind.GROUND);
        var second = electricalEditor.addComponent(first.diagram(), first.target(), ElectricalComponentKind.GROUND);
        var components = second.diagram().definition().elements().stream().map(ElectricalComponent.class::cast).toList();

        assertEquals("GND", components.get(0).referenceDesignator());
        assertEquals("GND1", components.get(1).referenceDesignator());
    }

    @Test
    void insertedComponentBoundsRemainInsideSmallCanvas() {
        var block = new DiagramBlock(new DiagramDefinition("small", new DiagramCanvas(10, 7), List.of(), List.of()));
        var result = electricalEditor.addComponent(block, new DiagramPropertyTarget(DiagramProperty.TITLE), ElectricalComponentKind.DC_VOLTAGE_SOURCE);
        var component = (ElectricalComponent) result.diagram().definition().elements().get(0);

        assertEquals(10.0, component.bounds().width());
        assertEquals(7.0, component.bounds().height());
        assertEquals(0.0, component.bounds().x());
        assertEquals(0.0, component.bounds().y());
    }

    @Test
    void clockwiseRotationSwapsBoundsAroundCenterAndPreservesAnnotations() {
        var component = component("r1", new DiagramBounds(30, 20, 28, 12), ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R7", "47 kΩ");
        var diagram = diagram(component);
        var target = new DiagramElementTarget(0, component.id());
        var result = electricalEditor.rotateClockwise(diagram, target);
        var rotated = (ElectricalComponent) result.diagram().definition().elements().get(0);

        assertEquals(ElectricalOrientation.DEG_90, rotated.orientation());
        assertEquals(12.0, rotated.bounds().width());
        assertEquals(28.0, rotated.bounds().height());
        assertEquals(38.0, rotated.bounds().x());
        assertEquals(12.0, rotated.bounds().y());
        assertEquals("R7", rotated.referenceDesignator());
        assertEquals("47 kΩ", rotated.valueLabel());
    }

    @Test
    void rotationNearCanvasEdgeClampsWholeComponentInsideCanvas() {
        var component = component("r1", new DiagramBounds(88, 38, 12, 28), ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_90, "R1", "");
        var diagram = new DiagramBlock(new DiagramDefinition("edge", new DiagramCanvas(100, 70), List.of(component), List.of()));
        var result = electricalEditor.rotateClockwise(diagram, new DiagramElementTarget(0, component.id()));
        var rotated = (ElectricalComponent) result.diagram().definition().elements().get(0);

        assertEquals(ElectricalOrientation.DEG_180, rotated.orientation());
        assertTrue(rotated.bounds().x() >= 0.0);
        assertTrue(rotated.bounds().y() >= 0.0);
        assertTrue(rotated.bounds().right() <= 100.0);
        assertTrue(rotated.bounds().bottom() <= 70.0);
    }

    @Test
    void counterClockwiseRotationUsesPreviousQuarterTurn() {
        var component = component("c1", new DiagramBounds(20, 20, 28, 12), ElectricalComponentKind.CAPACITOR, ElectricalOrientation.DEG_0, "C1", "1 uF");
        var result = electricalEditor.rotateCounterClockwise(diagram(component), new DiagramElementTarget(0, component.id()));
        var rotated = (ElectricalComponent) result.diagram().definition().elements().get(0);

        assertEquals(ElectricalOrientation.DEG_270, rotated.orientation());
        assertEquals(12.0, rotated.bounds().width());
        assertEquals(28.0, rotated.bounds().height());
    }

    @Test
    void rotationPreservesTerminalIdsAndExistingConnectionEndpoints() {
        var resistor = component("r1", new DiagramBounds(20, 10, 28, 12), ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "1 kΩ");
        var diode = component("d1", new DiagramBounds(65, 10, 28, 12), ElectricalComponentKind.DIODE, ElectricalOrientation.DEG_0, "D1", "");
        var connection = new DiagramConnection(
                new DiagramEndpoint(resistor.id(), new DiagramPortId("b")),
                new DiagramEndpoint(diode.id(), new DiagramPortId("anode")),
                "");
        var block = new DiagramBlock(new DiagramDefinition("wire", new DiagramCanvas(120, 60), List.of(resistor, diode), List.of(connection)));
        var beforeIds = resistor.ports().stream().map(DiagramPort::id).toList();

        var result = electricalEditor.rotateClockwise(block, new DiagramElementTarget(0, resistor.id()));
        var rotated = (ElectricalComponent) result.diagram().definition().elements().get(0);

        assertEquals(beforeIds, rotated.ports().stream().map(DiagramPort::id).toList());
        assertEquals(connection, result.diagram().definition().connections().get(0));
    }

    @Test
    void annotationsAreUpdatedTogetherWithoutChangingElectricalIdentity() {
        var component = component("r1", new DiagramBounds(20, 10, 28, 12), ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "1 kΩ");
        var target = new DiagramElementTarget(0, component.id());
        var result = electricalEditor.setAnnotations(diagram(component), target, "R42", "2.2 MΩ");
        var updated = (ElectricalComponent) result.diagram().definition().elements().get(0);

        assertTrue(result.changed());
        assertEquals("R42", updated.referenceDesignator());
        assertEquals("2.2 MΩ", updated.valueLabel());
        assertEquals(component.id(), updated.id());
        assertEquals(component.kind(), updated.kind());
        assertEquals(component.orientation(), updated.orientation());
    }

    @Test
    void unchangedAnnotationsProduceNoEdit() {
        var component = component("r1", new DiagramBounds(20, 10, 28, 12), ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "1 kΩ");
        var target = new DiagramElementTarget(0, component.id());
        var result = electricalEditor.setAnnotations(diagram(component), target, "R1", "1 kΩ");

        assertFalse(result.changed());
        assertEquals(diagram(component), result.diagram());
    }

    @Test
    void deletingElectricalComponentRemovesIncidentConnectionsOnly() {
        var resistor = component("r1", new DiagramBounds(10, 10, 28, 12), ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "");
        var diode = component("d1", new DiagramBounds(55, 10, 28, 12), ElectricalComponentKind.DIODE, ElectricalOrientation.DEG_0, "D1", "");
        var ground = component("gnd", new DiagramBounds(90, 35, 18, 12), ElectricalComponentKind.GROUND, ElectricalOrientation.DEG_0, "GND", "");
        var incident = new DiagramConnection(new DiagramEndpoint(resistor.id(), new DiagramPortId("b")), new DiagramEndpoint(diode.id(), new DiagramPortId("anode")), "a");
        var surviving = new DiagramConnection(new DiagramEndpoint(diode.id(), new DiagramPortId("cathode")), new DiagramEndpoint(ground.id(), new DiagramPortId("ground")), "b");
        var block = new DiagramBlock(new DiagramDefinition("delete", new DiagramCanvas(120, 60), List.of(resistor, diode, ground), List.of(incident, surviving)));

        var result = electricalEditor.deleteComponent(block, new DiagramElementTarget(0, resistor.id()));

        assertEquals(List.of(diode, ground), result.diagram().definition().elements());
        assertEquals(List.of(surviving), result.diagram().definition().connections());
    }

    @Test
    void electricalOperationsDoNotClaimGenericNodeTargets() {
        var node = new DiagramNode(
                new DiagramElementId("node"),
                new DiagramBounds(10, 10, 20, 12),
                "Node",
                List.of(new DiagramPort(new DiagramPortId("out"), "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5))));
        var block = new DiagramBlock(new DiagramDefinition("node", new DiagramCanvas(100, 50), List.of(node), List.of()));
        var target = new DiagramElementTarget(0, node.id());

        assertFalse(electricalEditor.canRotate(block, target));
        assertFalse(electricalEditor.canDelete(block, target));
        assertTrue(electricalEditor.draft(block, target).isEmpty());
    }

    @Test
    void genericDiagramDragNowMovesElectricalElementsWithoutDomainBranching() {
        var component = component("r1", new DiagramBounds(10, 10, 28, 12), ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "10 kΩ");
        var target = new DiagramElementTarget(0, component.id());
        var result = diagramEditor.moveElement(diagram(component), target, 50, 30);
        var moved = (ElectricalComponent) result.diagram().definition().elements().get(0);

        assertEquals(new DiagramBounds(50, 30, 28, 12), moved.bounds());
        assertEquals(component.kind(), moved.kind());
        assertEquals(component.orientation(), moved.orientation());
        assertEquals(component.referenceDesignator(), moved.referenceDesignator());
        assertEquals(component.valueLabel(), moved.valueLabel());
    }

    private static DiagramBlock emptyDiagram() {
        return new DiagramBlock(new DiagramDefinition("Electrical", new DiagramCanvas(120, 70), List.of(), List.of()));
    }

    private static DiagramBlock diagram(ElectricalComponent component) {
        return new DiagramBlock(new DiagramDefinition("Electrical", new DiagramCanvas(120, 70), List.of(component), List.of()));
    }

    private static ElectricalComponent component(
            String id,
            DiagramBounds bounds,
            ElectricalComponentKind kind,
            ElectricalOrientation orientation,
            String reference,
            String value
    ) {
        return new ElectricalComponent(new DiagramElementId(id), bounds, kind, orientation, reference, value);
    }
}
