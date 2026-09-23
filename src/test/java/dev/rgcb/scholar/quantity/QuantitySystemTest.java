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
        var freezing = new Quantity("0", parser.parseRequired("°C"));
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE, freezing.semantics());
        assertEquals(new BigDecimal("273.15"), freezing.convertTo(parser.parseRequired("K")).value());
        assertEquals(BigDecimal.ZERO, new Quantity("273.15", parser.parseRequired("K"))
                .convertTo(parser.parseRequired("°C")).value());
        assertEquals(new BigDecimal("373.15"), new Quantity("100", parser.parseRequired("°C"))
                .convertTo(parser.parseRequired("K")).value());
    }

    @Test void temperatureDifferencesConvertWithScaleOnlyAndKelvinMeaningIsExplicit() {
        var celsiusDifference = new Quantity("10", parser.parseRequired("°C"), QuantitySemantics.TEMPERATURE_DIFFERENCE);
        var kelvinDifference = celsiusDifference.convertTo(parser.parseRequired("K"));
        assertEquals(0, new BigDecimal("10").compareTo(kelvinDifference.value()));
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE, kelvinDifference.semantics());
        assertEquals(BigDecimal.ONE, new Quantity("1", parser.parseRequired("°C"), QuantitySemantics.TEMPERATURE_DIFFERENCE)
                .convertTo(parser.parseRequired("K")).value());
        assertNotEquals(new Quantity("5", parser.parseRequired("K")),
                new Quantity("5", parser.parseRequired("K"), QuantitySemantics.TEMPERATURE_DIFFERENCE));
    }

    @Test void uncertaintyUsesScaleButNeverAffineOffset() {
        var converted = new MeasuredQuantity("25", "1", parser.parseRequired("°C"))
                .convertTo(parser.parseRequired("K"));
        assertEquals(new BigDecimal("298.15"), converted.nominal().value());
        assertEquals(BigDecimal.ONE, converted.absoluteUncertainty());
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE, converted.nominal().semantics());
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE, converted.uncertaintyQuantity().semantics());
    }

    @Test void quantitySemanticsRejectImpossibleDimensionsAndRawLinearTemperatures() {
        assertThrows(IllegalArgumentException.class, () -> new Quantity("5", parser.parseRequired("m"),
                QuantitySemantics.ABSOLUTE_TEMPERATURE));
        assertThrows(IllegalArgumentException.class, () -> new Quantity("5", parser.parseRequired("kg"),
                QuantitySemantics.TEMPERATURE_DIFFERENCE));
        assertThrows(IllegalArgumentException.class, () -> new Quantity("5", parser.parseRequired("K"),
                QuantitySemantics.LINEAR));
    }

    @Test void thermalArithmeticContractIsCentralAndDeterministic() {
        var absolute = new Quantity("25", parser.parseRequired("°C"));
        var difference = new Quantity("5", parser.parseRequired("K"), QuantitySemantics.TEMPERATURE_DIFFERENCE);
        var scalar = new Quantity("2", parser.parseRequired("1"));

        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE,
                QuantityArithmeticPolicy.result(QuantityArithmeticPolicy.BinaryOperation.SUBTRACT, absolute, absolute));
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE,
                QuantityArithmeticPolicy.result(QuantityArithmeticPolicy.BinaryOperation.ADD, absolute, difference));
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE,
                QuantityArithmeticPolicy.result(QuantityArithmeticPolicy.BinaryOperation.ADD, difference, absolute));
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE,
                QuantityArithmeticPolicy.result(QuantityArithmeticPolicy.BinaryOperation.SUBTRACT, absolute, difference));
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE,
                QuantityArithmeticPolicy.result(QuantityArithmeticPolicy.BinaryOperation.ADD, difference, difference));
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE,
                QuantityArithmeticPolicy.result(QuantityArithmeticPolicy.BinaryOperation.SUBTRACT, difference, difference));
        assertThrows(IllegalArgumentException.class, () -> QuantityArithmeticPolicy.result(
                QuantityArithmeticPolicy.BinaryOperation.ADD, absolute, absolute));
        assertThrows(IllegalArgumentException.class, () -> QuantityArithmeticPolicy.result(
                QuantityArithmeticPolicy.BinaryOperation.SUBTRACT, difference, absolute));
        assertThrows(IllegalArgumentException.class, () -> QuantityArithmeticPolicy.result(
                QuantityArithmeticPolicy.BinaryOperation.MULTIPLY, absolute, scalar));
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE,
                QuantityArithmeticPolicy.result(QuantityArithmeticPolicy.BinaryOperation.MULTIPLY, difference, scalar));
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE,
                QuantityArithmeticPolicy.result(QuantityArithmeticPolicy.BinaryOperation.DIVIDE, difference, scalar));
        assertEquals(QuantitySemantics.LINEAR,
                QuantityArithmeticPolicy.result(QuantityArithmeticPolicy.BinaryOperation.DIVIDE, scalar, difference));
        assertThrows(IllegalArgumentException.class, () -> QuantityArithmeticPolicy.integerPower(absolute, 2));
    }

    @Test void formatterMakesTemperatureDifferencesExplicitInUnicodeAndPlainText() {
        var difference = new Quantity("5", parser.parseRequired("°C"), QuantitySemantics.TEMPERATURE_DIFFERENCE);
        var formatter = new ScientificNumberFormatter();
        assertEquals("Δ5 °C", formatter.format(difference, NumberNotation.DECIMAL, true));
        assertEquals("delta 5 °C", formatter.format(difference, NumberNotation.DECIMAL, false));
        assertEquals("25 °C", formatter.format(new Quantity("25", parser.parseRequired("°C")), NumberNotation.DECIMAL, true));
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
