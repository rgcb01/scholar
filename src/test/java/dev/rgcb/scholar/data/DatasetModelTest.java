package dev.rgcb.scholar.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DatasetModelTest {
    @Test
    void datasetAndColumnsHaveStableIdsIndependentOfNames() {
        var dataset = projectile().withDisplayName("Projectile Trial");
        var renamed = dataset.withColumnDisplayName("height", "Vertical Position");

        assertEquals("projectile-test", renamed.id());
        assertEquals("height", renamed.columns().get(1).id());
        assertEquals("Vertical Position", renamed.columns().get(1).displayName());
    }

    @Test
    void storesNumericTextAndMissingValues() {
        var row = new DatasetRow(List.of(DatasetValue.number("1.25"), DatasetValue.text("ok"), DatasetValue.missing()));

        assertEquals(DatasetValueKind.NUMBER, row.values().get(0).kind());
        assertEquals(DatasetValueKind.TEXT, row.values().get(1).kind());
        assertEquals(DatasetValueKind.MISSING, row.values().get(2).kind());
        assertEquals("", row.values().get(2).displayText());
    }

    @Test
    void rowInsertionDeletionAndCellEditAreImmutable() {
        var dataset = projectile();
        var added = dataset.withAddedRow(new DatasetRow(List.of(DatasetValue.number("3"), DatasetValue.number("1"))));
        var edited = added.withCell(1, "height", DatasetValue.number("8"));
        var deleted = edited.withoutRow(0);

        assertEquals(3, dataset.rows().size());
        assertEquals(4, added.rows().size());
        assertEquals("8", edited.rows().get(1).values().get(1).displayText());
        assertEquals(3, deleted.rows().size());
    }

    @Test
    void columnInsertionDeletionPreservesOtherColumnIds() {
        var dataset = projectile();
        var withColumn = dataset.withAddedColumn(new DatasetColumn("note", "Note", DatasetColumnType.TEXT), DatasetValue.missing());
        var withoutTime = withColumn.withoutColumn("time");

        assertEquals(List.of("time", "height", "note"), withColumn.columns().stream().map(DatasetColumn::id).toList());
        assertEquals(List.of("height", "note"), withoutTime.columns().stream().map(DatasetColumn::id).toList());
        assertEquals(2, withoutTime.rows().get(0).values().size());
    }

    @Test
    void validatesDuplicateColumnIdsAndRowWidths() {
        assertThrows(IllegalArgumentException.class, () -> new ScientificDataset(
                "bad",
                "Bad",
                List.of(new DatasetColumn("x", "X", DatasetColumnType.NUMBER), new DatasetColumn("x", "X2", DatasetColumnType.NUMBER)),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ScientificDataset(
                "bad",
                "Bad",
                List.of(new DatasetColumn("x", "X", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("2"))))));
    }

    @Test
    void documentRejectsDuplicateDatasetIds() {
        assertThrows(IllegalArgumentException.class, () -> new dev.rgcb.scholar.document.Document(
                List.of(new dev.rgcb.scholar.document.Paragraph(new dev.rgcb.scholar.document.InlineContent(List.of()))),
                List.of(projectile(), projectile())));
    }

    @Test
    void tsvSerializerIsReadable() {
        var text = new DatasetTsvSerializer().serialize(projectile());

        assertTrue(text.startsWith("Dataset: Projectile Test"));
        assertTrue(text.contains("Time\tHeight"));
        assertTrue(text.contains("1\t5"));
    }

    @Test
    void tabularImporterCreatesDatasetAndDetectsNumericColumnsConservatively() {
        var dataset = new DatasetTabularImporter().importText("projectile-test", "Projectile Test", """
                time,height,note
                0,0,start
                1,5,
                """);

        assertEquals("projectile-test", dataset.id());
        assertEquals(List.of(DatasetColumnType.NUMBER, DatasetColumnType.NUMBER, DatasetColumnType.TEXT),
                dataset.columns().stream().map(DatasetColumn::type).toList());
        assertEquals(DatasetValueKind.MISSING, dataset.rows().get(1).values().get(2).kind());
    }

    private static ScientificDataset projectile() {
        return new ScientificDataset(
                "projectile-test",
                "Projectile Test",
                List.of(
                        new DatasetColumn("time", "Time", DatasetColumnType.NUMBER),
                        new DatasetColumn("height", "Height", DatasetColumnType.NUMBER)),
                List.of(
                        new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("0"))),
                        new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("5"))),
                        new DatasetRow(List.of(DatasetValue.number("2"), DatasetValue.number("0")))));
    }
}
