package dev.rgcb.scholar.quantity;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuantitySystemTest {
    private final UnitParser parser = new UnitParser();
    private final UnitRegistry registry = UnitRegistry.builtIn();

    @Test void baseAndDerivedDimensionsAreStructurallyEquivalent() {
        assertEquals(parser.parseRequired("N").dimension(registry), parser.parseRequired("kg*m/s^2").dimension(registry));
        assertEquals(parser.parseRequired("J").dimension(registry), parser.parseRequired("N*m").dimension(registry));
        assertEquals(parser.parseRequired("Pa").dimension(registry), parser.parseRequired("N/m²").dimension(registry));
        assertTrue(parser.parseRequired("1").dimension(registry).isDimensionless());
        assertFalse(parser.parseRequired("m").compatibleWith(parser.parseRequired("s"), registry));
    }

    @Test void prefixesAndCompoundUnitsParseDeterministically() {
        for (var source : new String[] { "mm", "µm", "nm", "km", "mA", "kΩ", "MHz", "mV", "m/s²", "kg/m^3", "N·m", "J/(kg·K)", "V/A" }) {
            var expression = parser.parseRequired(source);
            assertInstanceOf(UnitParseResult.Success.class, parser.parse(expression.displaySymbol(registry)), source);
        }
        assertEquals("kg", parser.parseRequired("kg").displaySymbol(registry));
        assertEquals("m/s^2", parser.parseRequired("m/s²").asciiSymbol(registry));
        assertInstanceOf(UnitParseResult.Failure.class, parser.parse("not a unit"));
        assertInstanceOf(UnitParseResult.Failure.class, parser.parse("°C/s"));
    }

    @Test void metricConversionsUseDeterministicDecimalArithmetic() {
        assertEquals(new BigDecimal("0.001"), new Quantity("1", parser.parseRequired("mm"))
                .convertTo(parser.parseRequired("m")).value());
        assertEquals(0, new BigDecimal("1000").compareTo(new Quantity("1", parser.parseRequired("km"))
                .convertTo(parser.parseRequired("m")).value()));
        assertEquals(0, new Quantity("1", parser.parseRequired("N"))
                .compareTo(new Quantity("1", parser.parseRequired("kg*m/s^2"))));
        assertThrows(IllegalArgumentException.class, () -> new Quantity("1", parser.parseRequired("m"))
                .convertTo(parser.parseRequired("s")));
    }

    @Test void celsiusKelvinConversionIsAffine() {
        assertEquals(new BigDecimal("273.15"), new Quantity("0", parser.parseRequired("°C"))
                .convertTo(parser.parseRequired("K")).value());
        assertEquals(BigDecimal.ZERO, new Quantity("273.15", parser.parseRequired("K"))
                .convertTo(parser.parseRequired("°C")).value());
        assertEquals(new BigDecimal("373.15"), new Quantity("100", parser.parseRequired("°C"))
                .convertTo(parser.parseRequired("K")).value());
    }

    @Test void uncertaintyUsesScaleButNeverAffineOffset() {
        var converted = new MeasuredQuantity("25", "1", parser.parseRequired("°C"))
                .convertTo(parser.parseRequired("K"));
        assertEquals(new BigDecimal("298.15"), converted.nominal().value());
        assertEquals(BigDecimal.ONE, converted.absoluteUncertainty());
    }

    @Test void scientificAndEngineeringFormattingAreExplicitPresentationChoices() {
        var formatter = new ScientificNumberFormatter();
        assertEquals("1.2 × 10⁻³ A", formatter.format(new Quantity("0.0012", parser.parseRequired("A")), NumberNotation.SCIENTIFIC, true));
        assertEquals("1.2e-3 A", formatter.format(new Quantity("0.0012", parser.parseRequired("A")), NumberNotation.ENGINEERING, false));
        assertEquals("1.2 ± 0.1 mm", formatter.format(new MeasuredQuantity("1.2", "0.1", parser.parseRequired("mm")), NumberNotation.DECIMAL, true));
    }

    @Test void zeroExponentNeverEmitsRedundantScientificFactor() {
        var formatter = new ScientificNumberFormatter();
        assertEquals("2.4 mA", formatter.format(new Quantity("2.4", parser.parseRequired("mA")), NumberNotation.SCIENTIFIC, true));
        assertEquals("5 V", formatter.format(new Quantity("5", parser.parseRequired("V")), NumberNotation.ENGINEERING, true));
        assertEquals("3.3 kΩ", formatter.format(new Quantity("3.3", parser.parseRequired("kΩ")), NumberNotation.SCIENTIFIC, true));
    }

    @Test void meaningfulPositiveAndNegativeExponentsRemainVisible() {
        var formatter = new ScientificNumberFormatter();
        assertEquals("1.2 × 10³ A", formatter.format(new Quantity("1200", parser.parseRequired("A")), NumberNotation.SCIENTIFIC, true));
        assertEquals("1.2 × 10⁻³ A", formatter.format(new Quantity("0.0012", parser.parseRequired("A")), NumberNotation.SCIENTIFIC, true));
        assertEquals("12 × 10³ A", formatter.format(new Quantity("12000", parser.parseRequired("A")), NumberNotation.ENGINEERING, true));
        assertEquals("1.2 × 10⁻³ A", formatter.format(new Quantity("0.0012", parser.parseRequired("A")), NumberNotation.ENGINEERING, true));
    }
}
