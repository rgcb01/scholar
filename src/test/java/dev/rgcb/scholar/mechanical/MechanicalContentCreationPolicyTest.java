package dev.rgcb.scholar.mechanical;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.document.Text;
import java.util.List;
import org.junit.jupiter.api.Test;

class MechanicalContentCreationPolicyTest {
    @Test
    void billOfMaterialsHasStableColumnsAndSortedRows() {
        var target = new DiagramElementId("part");
        var second = new MechanicalPartReference(
                new DiagramElementId("ref-2"), new DiagramBounds(0, 0, 10, 10),
                target, 2, "Bearing", 1, "Support");
        var first = new MechanicalPartReference(
                new DiagramElementId("ref-1"), new DiagramBounds(0, 0, 10, 10),
                target, 1, "Shaft", 2, "Drive");

        var table = MechanicalContentCreationPolicy.billOfMaterials(List.of(second, first));

        assertEquals(1, table.headerRowCount());
        assertEquals(3, table.rows().size());
        assertEquals(List.of("ITEM", "PART", "QTY", "DESCRIPTION"), values(table.rows().get(0)));
        assertEquals(List.of("1", "Shaft", "2", "Drive"), values(table.rows().get(1)));
        assertEquals(List.of("2", "Bearing", "1", "Support"), values(table.rows().get(2)));
    }

    private static List<String> values(dev.rgcb.scholar.document.TableRow row) {
        return row.cells().stream()
                .map(cell -> ((Text) cell.content().content().nodes().getFirst()).content())
                .toList();
    }
}
