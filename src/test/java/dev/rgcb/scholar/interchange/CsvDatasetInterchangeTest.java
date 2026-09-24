package dev.rgcb.scholar.interchange;

import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetValueKind;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsvDatasetInterchangeTest {
    private final CsvDatasetInterchange interchange = new CsvDatasetInterchange();

    @Test void importsQuotedFieldsBomLineEndingsAndMissingValues() {
        var preview = interchange.preview("\uFEFFTime [s],Note,Distance [m]\r\n"
                + "0,\"first, \"\"quoted\"\"\",0\r\n1,,1.5e-3\n");
        assertEquals(2, preview.rows().size());
        assertEquals("first, \"quoted\"", preview.rows().getFirst().values().get(1).text().orElseThrow());
        assertEquals(DatasetValueKind.MISSING, preview.rows().get(1).values().get(1).kind());
        assertEquals(DatasetColumnType.NUMBER, preview.columns().get(2).type());
        assertEquals("0.0015", preview.rows().get(1).values().get(2).number().orElseThrow().toPlainString());
        assertEquals("Time", preview.columns().getFirst().displayName());
    }

    @Test void temperatureSemanticsAndRoundTrip() {
        var preview = interchange.preview("Temperature [°C],Temperature Difference [Δ°C],Note\n20,5,café\n,,-1\n");
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE, preview.columns().get(0).quantitySemantics());
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE, preview.columns().get(1).quantitySemantics());
        var roundTrip = interchange.preview(interchange.export(preview.dataset("d", "Imported")));
        assertEquals(preview.columns().stream().map(c -> c.displayName()).toList(),
                roundTrip.columns().stream().map(c -> c.displayName()).toList());
        assertEquals(preview.columns().stream().map(c -> c.quantitySemantics()).toList(),
                roundTrip.columns().stream().map(c -> c.quantitySemantics()).toList());
        assertEquals(preview.rows(), roundTrip.rows());
    }

    @Test void invalidUnitRemainsInHeaderAndMalformedCsvFails() {
        var preview = interchange.preview("Speed [furlongs],Value\n1,2\n");
        assertEquals("Speed [furlongs]", preview.columns().getFirst().displayName());
        assertFalse(preview.warnings().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> interchange.preview("A,B\n1\n"));
        assertThrows(IllegalArgumentException.class, () -> interchange.preview("A\n\"unfinished"));
        assertThrows(IllegalArgumentException.class, () -> interchange.preview("A\n\"x\"oops"));
    }

    @Test void quotedEmptyTextRemainsDistinctFromMissing() {
        var imported = interchange.preview("Name\nvalue\n\"\"\n\n");
        assertEquals(DatasetColumnType.TEXT, imported.columns().getFirst().type());
        assertEquals(DatasetValueKind.TEXT, imported.rows().get(1).values().getFirst().kind());
        assertEquals("", imported.rows().get(1).values().getFirst().text().orElseThrow());
        assertEquals(DatasetValueKind.MISSING, imported.rows().get(2).values().getFirst().kind());
        var exported = interchange.export(imported.dataset("d", "D"));
        assertEquals(imported.rows(), interchange.preview(exported).rows());
    }

    @Test void kelvinDifferenceAndScientificNotationAreLocaleIndependent() {
        var original = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY);
            var imported = interchange.preview("Absolute [K],Difference [ΔK],Current [mA]\n300,5,6.022e23\n");
            assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE, imported.columns().get(0).quantitySemantics());
            assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE, imported.columns().get(1).quantitySemantics());
            assertEquals("6.022E+23", imported.rows().getFirst().values().get(2).number().orElseThrow().toString());
            assertEquals(imported.rows(), interchange.preview(interchange.export(imported.dataset("d", "D"))).rows());
        } finally { java.util.Locale.setDefault(original); }
    }

    @Test void quotedEmbeddedNewlineRoundTrips() {
        var imported = interchange.preview("Name,Comment\nfirst,\"line one\nline two\"\n");
        assertEquals("line one\nline two", imported.rows().getFirst().values().get(1).text().orElseThrow());
        assertEquals(imported.rows(), interchange.preview(interchange.export(imported.dataset("d", "D"))).rows());
    }
}
