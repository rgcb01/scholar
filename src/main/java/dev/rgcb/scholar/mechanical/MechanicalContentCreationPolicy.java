package dev.rgcb.scholar.mechanical;

import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Deterministic authored-table policy for a mechanical bill of materials. */
public final class MechanicalContentCreationPolicy {
    private static final List<String> BOM_COLUMNS = List.of("ITEM", "PART", "QTY", "DESCRIPTION");

    private MechanicalContentCreationPolicy() {
    }

    public static TableBlock billOfMaterials(List<MechanicalPartReference> references) {
        Objects.requireNonNull(references, "references");
        var rows = new ArrayList<TableRow>();
        rows.add(row(BOM_COLUMNS));
        references.stream()
                .sorted(Comparator.comparingInt(MechanicalPartReference::itemNumber))
                .forEach(reference -> rows.add(row(List.of(
                        Integer.toString(reference.itemNumber()),
                        reference.partName(),
                        Integer.toString(reference.quantity()),
                        reference.description()))));
        return new TableBlock(rows, 1);
    }

    private static TableRow row(List<String> values) {
        return new TableRow(values.stream().map(MechanicalContentCreationPolicy::cell).toList());
    }

    private static TableCell cell(String value) {
        return new TableCell(new TableCellContent(new InlineContent(List.of(new Text(value, Set.of())))));
    }
}
