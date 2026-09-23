package dev.rgcb.scholar.quantity;

import java.util.Objects;

/** Central thermal arithmetic contract for future computation consumers. */
public final class QuantityArithmeticPolicy {
    public enum BinaryOperation { ADD, SUBTRACT, MULTIPLY, DIVIDE }

    private QuantityArithmeticPolicy() { }

    public static QuantitySemantics result(BinaryOperation operation, Quantity left, Quantity right) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        return switch (operation) {
            case ADD -> additive(left, right, false);
            case SUBTRACT -> additive(left, right, true);
            case MULTIPLY -> multiplicative(left, right, false);
            case DIVIDE -> multiplicative(left, right, true);
        };
    }

    public static QuantitySemantics integerPower(Quantity value, int exponent) {
        Objects.requireNonNull(value);
        if (value.semantics().isAbsoluteTemperature()) {
            throw new IllegalArgumentException("absolute temperatures cannot be raised to a power");
        }
        if (exponent == 1) return value.semantics();
        return QuantitySemantics.LINEAR;
    }

    private static QuantitySemantics additive(Quantity left, Quantity right, boolean subtract) {
        if (!left.unit().compatibleWith(right.unit(), UnitRegistry.builtIn())) {
            throw new IllegalArgumentException("addition and subtraction require compatible dimensions");
        }
        var l = left.semantics();
        var r = right.semantics();
        if (l == QuantitySemantics.LINEAR || r == QuantitySemantics.LINEAR) {
            if (l != r) throw new IllegalArgumentException("quantity semantics are incompatible");
            return QuantitySemantics.LINEAR;
        }
        if (!subtract) {
            if (l == QuantitySemantics.ABSOLUTE_TEMPERATURE && r == QuantitySemantics.ABSOLUTE_TEMPERATURE) {
                throw new IllegalArgumentException("absolute temperatures cannot be added");
            }
            return l == QuantitySemantics.ABSOLUTE_TEMPERATURE || r == QuantitySemantics.ABSOLUTE_TEMPERATURE
                    ? QuantitySemantics.ABSOLUTE_TEMPERATURE
                    : QuantitySemantics.TEMPERATURE_DIFFERENCE;
        }
        if (l == QuantitySemantics.TEMPERATURE_DIFFERENCE && r == QuantitySemantics.ABSOLUTE_TEMPERATURE) {
            throw new IllegalArgumentException("an absolute temperature cannot be subtracted from a temperature difference");
        }
        if (l == QuantitySemantics.ABSOLUTE_TEMPERATURE && r == QuantitySemantics.ABSOLUTE_TEMPERATURE) {
            return QuantitySemantics.TEMPERATURE_DIFFERENCE;
        }
        return l;
    }

    private static QuantitySemantics multiplicative(Quantity left, Quantity right, boolean divide) {
        if (left.semantics().isAbsoluteTemperature() || right.semantics().isAbsoluteTemperature()) {
            throw new IllegalArgumentException("absolute temperatures cannot be multiplied or divided");
        }
        var leftDimensionless = left.unit().dimension(UnitRegistry.builtIn()).isDimensionless();
        var rightDimensionless = right.unit().dimension(UnitRegistry.builtIn()).isDimensionless();
        if (left.semantics() == QuantitySemantics.TEMPERATURE_DIFFERENCE && rightDimensionless) {
            return QuantitySemantics.TEMPERATURE_DIFFERENCE;
        }
        if (!divide && right.semantics() == QuantitySemantics.TEMPERATURE_DIFFERENCE && leftDimensionless) {
            return QuantitySemantics.TEMPERATURE_DIFFERENCE;
        }
        return QuantitySemantics.LINEAR;
    }
}
