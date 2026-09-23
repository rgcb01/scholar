package dev.rgcb.scholar.compute;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitParser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExpressionEvaluatorTest {
    private final ExpressionParser parser = new ExpressionParser();
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private final UnitParser units = new UnitParser();

    private VariableDefinition value(String id, String name, String number, String unit) {
        return new VariableDefinition(id, name, new ScientificValue.Physical(new Quantity(number, units.parseRequired(unit))));
    }

    private ComputationResult evaluate(String source, Document document) {
        return evaluator.evaluate(parser.parse(source, document).expression(), document);
    }

    private Quantity physical(ComputationResult result) {
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        return ((ScientificValue.Physical) result.value().orElseThrow()).value().nominal();
    }

    @Test
    void goldenEnergyRecomputesAfterValueChangeAndUsesDerivedUnit() {
        var mass = value("var-m", "m", "2.5", "kg");
        var gravity = value("var-g", "g", "9.81", "m/s²");
        var height = value("var-h", "h", "1.2", "m");
        var document = new Document(List.of(mass, gravity, height));
        var parsed = parser.parse("m*g*h", document);
        assertEquals(List.of("var-m", "var-g", "var-h"), parsed.expression().dependencies().stream()
                .map(ref -> ref.variableId()).toList());
        var before = physical(evaluator.evaluate(parsed.expression(), document));
        assertEquals(0, before.value().compareTo(new BigDecimal("29.43")));
        assertEquals("J", before.unit().asciiSymbol(dev.rgcb.scholar.quantity.UnitRegistry.builtIn()));
        var updated = new Document(List.of(mass, gravity, height.withValue(new ScientificValue.Physical(
                new Quantity("2", UnitExpression.of("metre"))))));
        assertEquals(0, physical(evaluator.evaluate(parsed.expression(), updated)).value().compareTo(new BigDecimal("49.05")));
        var renamed = new Document(List.of(mass.withName("mass"), gravity, height));
        assertEquals(0, physical(evaluator.evaluate(parsed.expression(), renamed)).value().compareTo(new BigDecimal("29.43")));
    }

    @Test
    void precedenceGroupingUnaryAndPowersRetainDimensions() {
        var document = new Document(List.of(value("d", "distance", "10", "m"), value("t", "time", "2", "s")));
        var velocity = physical(evaluate("distance/time", document));
        assertEquals(0, velocity.value().compareTo(new BigDecimal("5")));
        assertEquals("m/s", velocity.unit().asciiSymbol(dev.rgcb.scholar.quantity.UnitRegistry.builtIn()));
        assertEquals(new BigDecimal("9"), ((ScientificValue.Scalar) evaluate("-2^2 + (a+b)/2", new Document(List.of(
                new VariableDefinition("a", "a", new ScientificValue.Scalar(new BigDecimal("20"))),
                new VariableDefinition("b", "b", new ScientificValue.Scalar(new BigDecimal("6"))))))
                .value().orElseThrow()).value());
        assertEquals(ComputationDiagnostic.Code.INVALID_POWER, evaluate("2^1.5", document).diagnostics().getFirst().code());
    }

    @Test
    void compatibleUnitsAndScientificErrors() {
        assertEquals(0, physical(evaluate("5 m + 30 cm", new Document(List.of()))).value().compareTo(new BigDecimal("5.3")));
        assertEquals(ComputationDiagnostic.Code.INCOMPATIBLE_DIMENSIONS,
                evaluate("5 m + 3 s", new Document(List.of())).diagnostics().getFirst().code());
        assertEquals(ComputationDiagnostic.Code.DIVIDE_BY_ZERO,
                evaluate("10 m / 0", new Document(List.of())).diagnostics().getFirst().code());
        assertEquals(ComputationDiagnostic.Code.UNKNOWN_VARIABLE,
                evaluate("missing + 1", new Document(List.of())).diagnostics().getFirst().code());
        var duplicate = new Document(List.of(new VariableDefinition("a", "x", new ScientificValue.Scalar(BigDecimal.ONE)),
                new VariableDefinition("b", "x", new ScientificValue.Scalar(BigDecimal.TEN))));
        assertEquals(ComputationDiagnostic.Code.AMBIGUOUS_VARIABLE, evaluate("x + 1", duplicate).diagnostics().getFirst().code());
    }

    @Test
    void thermalPolicyControlsEvaluationAndKelvinDifference() {
        var room = value("room", "room", "20", "°C");
        var increase = new VariableDefinition("increase", "increase", new ScientificValue.Physical(
                new Quantity("5", UnitExpression.of("celsius"), QuantitySemantics.TEMPERATURE_DIFFERENCE)));
        var document = new Document(List.of(room, increase));
        assertEquals(0, physical(evaluate("room+increase", document)).value().compareTo(new BigDecimal("25")));
        assertEquals(0, physical(evaluate("increase+room", document)).value().compareTo(new BigDecimal("25")));
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE, physical(evaluate("30 °C-20 °C", document)).semantics());
        assertEquals(0, physical(evaluate("30 °C-20 °C", document)).value().compareTo(new BigDecimal("10")));
        assertEquals(ComputationDiagnostic.Code.INVALID_TEMPERATURE_OPERATION,
                evaluate("20 °C+10 °C", document).diagnostics().getFirst().code());
        assertEquals(ComputationDiagnostic.Code.INVALID_TEMPERATURE_OPERATION,
                evaluate("20 °C*2", document).diagnostics().getFirst().code());
        assertEquals(0, physical(evaluate("298.15 K-20 °C", document)).value().compareTo(new BigDecimal("5")));
    }

    @Test
    void parserPreservesRecoverableInvalidInputAndBindsForwardDefinitionsById() {
        var definitions = new Document(List.of(value("stable-x", "x_1", "2", "m")));
        var bound = parser.parse("(x_1 + 3 m)*2", definitions);
        assertTrue(bound.diagnostics().isEmpty());
        assertEquals("stable-x", bound.expression().dependencies().getFirst().variableId());
        var malformed = parser.parse("x_1 + (", definitions);
        assertInstanceOf(Expression.Invalid.class, malformed.expression());
        assertEquals(ComputationDiagnostic.Code.INVALID_EXPRESSION, malformed.diagnostics().getFirst().code());
        assertEquals(ComputationDiagnostic.Code.INVALID_EXPRESSION,
                evaluator.evaluate(malformed.expression(), definitions).diagnostics().getFirst().code());
        var missing = parser.parse("not_defined + 1", definitions);
        assertInstanceOf(Expression.UnresolvedName.class, ((Expression.Binary) missing.expression()).left());
    }

    @Test
    void measuredVariableCannotEnterArithmeticWithoutUncertaintyPropagation() {
        var nominal = new Quantity("5", units.parseRequired("m"));
        var measured = new VariableDefinition("measured-id", "length", new ScientificValue.Physical(
                new dev.rgcb.scholar.quantity.MeasuredQuantity(nominal, new BigDecimal("0.1"))));
        var document = new Document(List.of(measured));
        assertEquals(ComputationDiagnostic.Code.UNSUPPORTED_OPERATION,
                evaluate("length*2", document).diagnostics().getFirst().code());
        assertTrue(evaluate("length", document).value().isPresent());
    }
}
