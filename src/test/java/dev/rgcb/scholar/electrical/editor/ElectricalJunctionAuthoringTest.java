package dev.rgcb.scholar.electrical.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.editor.DiagramProperty;
import dev.rgcb.scholar.editor.DiagramPropertyTarget;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectricalJunctionAuthoringTest {
    private final ElectricalDiagramEditor editor = new ElectricalDiagramEditor();

    @Test
    void addJunctionCreatesDeterministicFourWaySplice() {
        var result = editor.addJunction(empty(), new DiagramPropertyTarget(DiagramProperty.TITLE));
        var junction = (ElectricalJunction) result.diagram().definition().elements().get(0);

        assertTrue(result.changed());
        assertEquals("junction-1", junction.id().value());
        assertEquals(List.of("left", "right", "top", "bottom"),
                junction.ports().stream().map(port -> port.id().value()).toList());
        assertEquals(new DiagramElementTarget(0, junction.id()), result.target());
    }

    @Test
    void junctionNetLabelIsEditableWithoutChangingIdentityOrPorts() {
        var added = editor.addJunction(empty(), new DiagramPropertyTarget(DiagramProperty.TITLE));
        var before = (ElectricalJunction) added.diagram().definition().elements().get(0);
        var edited = editor.setJunctionNetLabel(added.diagram(), added.target(), "VOUT");
        var after = (ElectricalJunction) edited.diagram().definition().elements().get(0);

        assertEquals(before.id(), after.id());
        assertEquals(before.ports(), after.ports());
        assertEquals("VOUT", after.netLabel());
        assertEquals("VOUT", editor.junctionNetLabel(edited.diagram(), edited.target()).orElseThrow());
    }

    @Test
    void deletingJunctionAlsoDeletesEveryIncidentWireSegment() {
        var first = editor.addJunction(empty(), new DiagramPropertyTarget(DiagramProperty.TITLE));
        var second = editor.addJunction(first.diagram(), first.target());
        var j1 = (ElectricalJunction) second.diagram().definition().elements().get(0);
        var j2 = (ElectricalJunction) second.diagram().definition().elements().get(1);
        var wire = new DiagramConnection(
                new DiagramEndpoint(j1.id(), new DiagramPortId("right")),
                new DiagramEndpoint(j2.id(), new DiagramPortId("left")), "");
        var wired = new DiagramBlock(new DiagramDefinition("j", new DiagramCanvas(100, 50), List.of(j1, j2), List.of(wire)));

        var result = editor.deleteJunction(wired, new DiagramElementTarget(0, j1.id()));

        assertEquals(List.of(j2), result.diagram().definition().elements());
        assertTrue(result.diagram().definition().connections().isEmpty());
    }

    @Test
    void componentOperationsDoNotClaimJunctionTargets() {
        var added = editor.addJunction(empty(), new DiagramPropertyTarget(DiagramProperty.TITLE));

        assertFalse(editor.canRotate(added.diagram(), added.target()));
        assertFalse(editor.canDelete(added.diagram(), added.target()));
        assertTrue(editor.draft(added.diagram(), added.target()).isEmpty());
        assertTrue(editor.canDeleteJunction(added.diagram(), added.target()));
    }

    private static DiagramBlock empty() {
        return new DiagramBlock(new DiagramDefinition("Electrical", new DiagramCanvas(100, 50), List.of(), List.of()));
    }
}
