package dev.rgcb.scholar.compute;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.quantity.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure dimensional evaluator. No document mutation or rendering dependency. */
public final class ExpressionEvaluator {
    private static final UnitRegistry UNITS = UnitRegistry.builtIn();
    private static final UnitConverter CONVERTER = new UnitConverter();

    public ComputationResult evaluate(Expression expression, Document document) {
        try {
            return ComputationResult.success(evaluateValue(expression, document));
        } catch (EvaluationFailure failure) {
            return ComputationResult.failure(failure.code, failure.getMessage());
        } catch (IllegalArgumentException failure) {
            return ComputationResult.failure(ComputationDiagnostic.Code.UNSUPPORTED_OPERATION, failure.getMessage());
        }
    }

    private ScientificValue evaluateValue(Expression expression, Document document) {
        if (expression instanceof Expression.NumberLiteral number) return new ScientificValue.Scalar(number.value());
        if (expression instanceof Expression.ValueLiteral literal) return literal.value();
        if (expression instanceof Expression.Group group) return evaluateValue(group.inner(), document);
        if (expression instanceof Expression.Invalid invalid) throw fail(invalid.code(), "Malformed expression");
        if (expression instanceof Expression.UnresolvedName unknown) {
            var count = document.blocks().stream().filter(VariableDefinition.class::isInstance)
                    .map(VariableDefinition.class::cast).filter(variable -> variable.name().equals(unknown.name())).count();
            throw fail(count > 1 ? ComputationDiagnostic.Code.AMBIGUOUS_VARIABLE
                    : ComputationDiagnostic.Code.UNKNOWN_VARIABLE, "Unbound variable: " + unknown.name());
        }
        if (expression instanceof Expression.Variable reference) {
            var matches = document.blocks().stream().filter(VariableDefinition.class::isInstance)
                    .map(VariableDefinition.class::cast)
                    .filter(variable -> variable.id().equals(reference.reference().variableId())).toList();
            if (matches.size() != 1) throw fail(ComputationDiagnostic.Code.UNKNOWN_VARIABLE,
                    "Missing or duplicate variable identity: " + reference.reference().variableId());
            return matches.getFirst().value();
        }
        if (expression instanceof Expression.Unary unary) {
            var value = evaluateValue(unary.operand(), document);
            if (unary.operator() == Expression.UnaryOperator.PLUS) return value;
            if (value instanceof ScientificValue.Scalar scalar) return new ScientificValue.Scalar(scalar.value().negate());
            var physical = ((ScientificValue.Physical) value).value();
            if (physical instanceof MeasuredQuantity) throw fail(ComputationDiagnostic.Code.UNSUPPORTED_OPERATION,
                    "Uncertainty propagation is not supported.");
            var quantity = physical.nominal();
            if (quantity.semantics().isAbsoluteTemperature()) throw fail(ComputationDiagnostic.Code.INVALID_TEMPERATURE_OPERATION,
                    "An absolute temperature cannot be negated.");
            return new ScientificValue.Physical(new Quantity(quantity.value().negate(), quantity.unit(), quantity.semantics()));
        }
        if (expression instanceof Expression.Power power) return power(evaluateValue(power.base(), document), power.exponent());
        var binary = (Expression.Binary) expression;
        return binary(binary.operator(), evaluateValue(binary.left(), document), evaluateValue(binary.right(), document));
    }

    private ScientificValue binary(Expression.Operator operator, ScientificValue left, ScientificValue right) {
        if (left instanceof ScientificValue.Physical l && l.value() instanceof MeasuredQuantity
                || right instanceof ScientificValue.Physical r && r.value() instanceof MeasuredQuantity) {
            throw fail(ComputationDiagnostic.Code.UNSUPPORTED_OPERATION, "Uncertainty propagation is not supported.");
        }
        if (left instanceof ScientificValue.Scalar l && right instanceof ScientificValue.Scalar r) {
            return new ScientificValue.Scalar(arithmetic(operator, l.value(), r.value()));
        }
        var l = quantity(left);
        var r = quantity(right);
        var policyOperation = switch (operator) {
            case ADD -> QuantityArithmeticPolicy.BinaryOperation.ADD;
            case SUBTRACT -> QuantityArithmeticPolicy.BinaryOperation.SUBTRACT;
            case MULTIPLY -> QuantityArithmeticPolicy.BinaryOperation.MULTIPLY;
            case DIVIDE -> QuantityArithmeticPolicy.BinaryOperation.DIVIDE;
        };
        final QuantitySemantics semantics;
        try { semantics = QuantityArithmeticPolicy.result(policyOperation, l, r); }
        catch (IllegalArgumentException failure) {
            throw fail(l.semantics().isTemperature() || r.semantics().isTemperature()
                    ? ComputationDiagnostic.Code.INVALID_TEMPERATURE_OPERATION
                    : ComputationDiagnostic.Code.INCOMPATIBLE_DIMENSIONS, failure.getMessage());
        }
        if (operator == Expression.Operator.ADD || operator == Expression.Operator.SUBTRACT) {
            var resultUnit = l.semantics() == QuantitySemantics.TEMPERATURE_DIFFERENCE
                    && r.semantics() == QuantitySemantics.ABSOLUTE_TEMPERATURE ? r.unit() : l.unit();
            var lv = CONVERTER.convert(l, resultUnit).value();
            var rv = CONVERTER.convert(r, resultUnit).value();
            return physical(new Quantity(operator == Expression.Operator.ADD ? lv.add(rv) : lv.subtract(rv), resultUnit, semantics));
        }
        if (operator == Expression.Operator.DIVIDE && r.value().signum() == 0) {
            throw fail(ComputationDiagnostic.Code.DIVIDE_BY_ZERO, "Division by zero");
        }
        var normalizedLeft = compoundTemperature(l);
        var normalizedRight = compoundTemperature(r);
        var combined = combine(normalizedLeft.unit(), normalizedRight.unit(), operator == Expression.Operator.DIVIDE ? -1 : 1);
        var amount = arithmetic(operator, normalizedLeft.value(), normalizedRight.value());
        var result = new Quantity(amount, combined, semantics);
        if (semantics == QuantitySemantics.LINEAR && combined.dimension(UNITS).equals(UnitExpression.of("joule").dimension(UNITS))) {
            result = CONVERTER.convert(result, UnitExpression.of("joule"));
        }
        return physical(result);
    }

    private static ScientificValue power(ScientificValue source, int exponent) {
        if (exponent < -12 || exponent > 12) throw fail(ComputationDiagnostic.Code.INVALID_POWER, "Power magnitude exceeds 12.");
        if (source instanceof ScientificValue.Physical p && p.value() instanceof MeasuredQuantity) {
            throw fail(ComputationDiagnostic.Code.UNSUPPORTED_OPERATION, "Uncertainty propagation is not supported.");
        }
        if (source instanceof ScientificValue.Scalar scalar) return new ScientificValue.Scalar(pow(scalar.value(), exponent));
        var quantity = ((ScientificValue.Physical) source).value().nominal();
        final QuantitySemantics semantics;
        try { semantics = QuantityArithmeticPolicy.integerPower(quantity, exponent); }
        catch (IllegalArgumentException failure) { throw fail(ComputationDiagnostic.Code.INVALID_TEMPERATURE_OPERATION, failure.getMessage()); }
        var factors = new ArrayList<UnitFactor>();
        for (var factor : quantity.unit().factors()) {
            var next = factor.exponent() * exponent;
            if (next != 0) factors.add(new UnitFactor(factor.unitId(), factor.prefix(), next));
        }
        var unit = new UnitExpression(factors.isEmpty() ? List.of(new UnitFactor("one", 1)) : factors);
        return physical(new Quantity(pow(quantity.value(), exponent), unit, semantics));
    }

    private static BigDecimal pow(BigDecimal value, int exponent) {
        if (exponent < 0 && value.signum() == 0) throw fail(ComputationDiagnostic.Code.DIVIDE_BY_ZERO, "Zero cannot have a negative power.");
        return exponent >= 0 ? value.pow(exponent, UnitRegistry.MATH_CONTEXT)
                : BigDecimal.ONE.divide(value.pow(-exponent, UnitRegistry.MATH_CONTEXT), UnitRegistry.MATH_CONTEXT);
    }

    private static BigDecimal arithmetic(Expression.Operator operator, BigDecimal left, BigDecimal right) {
        return switch (operator) {
            case ADD -> left.add(right, UnitRegistry.MATH_CONTEXT);
            case SUBTRACT -> left.subtract(right, UnitRegistry.MATH_CONTEXT);
            case MULTIPLY -> left.multiply(right, UnitRegistry.MATH_CONTEXT);
            case DIVIDE -> {
                if (right.signum() == 0) throw fail(ComputationDiagnostic.Code.DIVIDE_BY_ZERO, "Division by zero");
                yield left.divide(right, UnitRegistry.MATH_CONTEXT);
            }
        };
    }

    private static Quantity quantity(ScientificValue value) {
        return value instanceof ScientificValue.Scalar scalar
                ? new Quantity(scalar.value(), UnitExpression.of("one"))
                : ((ScientificValue.Physical) value).value().nominal();
    }

    private static ScientificValue physical(Quantity value) {
        return value.unit().dimension(UNITS).isDimensionless() ? new ScientificValue.Scalar(
                value.value().multiply(UNITS.scale(value.unit()), UnitRegistry.MATH_CONTEXT))
                : new ScientificValue.Physical(value);
    }

    private static Quantity compoundTemperature(Quantity value) {
        return value.semantics() == QuantitySemantics.TEMPERATURE_DIFFERENCE
                ? CONVERTER.convert(value, UnitExpression.of("kelvin")) : value;
    }

    private static UnitExpression combine(UnitExpression left, UnitExpression right, int rightSign) {
        var exponents = new LinkedHashMap<UnitFactor, Integer>();
        for (var factor : left.factors()) {
            if (!factor.unitId().equals("one")) exponents.merge(new UnitFactor(factor.unitId(), factor.prefix(), 1), factor.exponent(), Integer::sum);
        }
        for (var factor : right.factors()) {
            if (!factor.unitId().equals("one")) exponents.merge(new UnitFactor(factor.unitId(), factor.prefix(), 1), factor.exponent() * rightSign, Integer::sum);
        }
        var result = exponents.entrySet().stream().filter(entry -> entry.getValue() != 0)
                .map(entry -> new UnitFactor(entry.getKey().unitId(), entry.getKey().prefix(), entry.getValue())).toList();
        return new UnitExpression(result.isEmpty() ? List.of(new UnitFactor("one", 1)) : result);
    }

    private static EvaluationFailure fail(ComputationDiagnostic.Code code, String message) {
        return new EvaluationFailure(code, message);
    }

    private static final class EvaluationFailure extends RuntimeException {
        private final ComputationDiagnostic.Code code;
        private EvaluationFailure(ComputationDiagnostic.Code code, String message) { super(message); this.code = code; }
    }
}
