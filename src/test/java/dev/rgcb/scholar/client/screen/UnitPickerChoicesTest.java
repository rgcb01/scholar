package dev.rgcb.scholar.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitParser;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UnitPickerChoicesTest {
    @Test void commonChoicesAreRegistryBackedAndDimensionlessIsExplicit() {
        var choices = UnitPickerChoices.available();
        for (var symbol : new String[] {"m", "cm", "mm", "km", "kg", "g", "s", "min", "h",
                "m/s", "km/h", "m/s²", "Hz", "N", "Pa", "J", "W", "V", "A", "mA", "Ω", "kΩ", "L", "mL"}) {
            assertTrue(choices.stream().anyMatch(choice -> choice.unit().isPresent()
                    && choice.unit().orElseThrow().equals(new UnitParser().parseRequired(symbol))), symbol);
        }
        assertTrue(choices.getFirst().unit().isEmpty());
        assertEquals(QuantitySemantics.LINEAR, choices.getFirst().semantics());
    }

    @Test void temperatureChoicesCarrySemanticsWithoutTextInference() {
        var celsius = Optional.of(new UnitParser().parseRequired("°C"));
        var kelvin = Optional.of(new UnitParser().parseRequired("K"));
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE,
                UnitPickerChoices.matching(celsius, QuantitySemantics.ABSOLUTE_TEMPERATURE).orElseThrow().semantics());
        assertTrue(UnitPickerChoices.matching(celsius, QuantitySemantics.TEMPERATURE_DIFFERENCE)
                .orElseThrow().label().startsWith("Δ"));
        assertTrue(UnitPickerChoices.matching(kelvin, QuantitySemantics.TEMPERATURE_DIFFERENCE)
                .orElseThrow().label().startsWith("Δ"));
    }

    @Test void editingUnitConvertsExistingPhysicalValue() {
        var metre = Optional.of(new UnitParser().parseRequired("m"));
        var centimetre = UnitPickerChoices.matching(Optional.of(new UnitParser().parseRequired("cm")),
                QuantitySemantics.LINEAR).orElseThrow();
        assertEquals(0, new BigDecimal("200").compareTo(UnitPickerChoices.convertedValue(
                new BigDecimal("2"), metre, QuantitySemantics.LINEAR, centimetre)));
        var deltaCelsius = UnitPickerChoices.matching(Optional.of(new UnitParser().parseRequired("°C")),
                QuantitySemantics.TEMPERATURE_DIFFERENCE).orElseThrow();
        assertEquals(new BigDecimal("20"), UnitPickerChoices.convertedValue(new BigDecimal("20"),
                Optional.of(new UnitParser().parseRequired("°C")),
                QuantitySemantics.ABSOLUTE_TEMPERATURE, deltaCelsius));
        assertFalse(UnitPickerChoices.matching(metre, QuantitySemantics.LINEAR).isEmpty());
    }
}
