package dev.rgcb.scholar.compute;

import dev.rgcb.scholar.document.ComputedResult;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.quantity.ScientificNumberFormatter;
import dev.rgcb.scholar.quantity.UnitConverter;
import dev.rgcb.scholar.quantity.MeasuredQuantity;
import java.util.Objects;

/** Shared readable presentation for layout and interchange; never exposes stable IDs. */
public final class ComputationFormatter {
    private final ScientificNumberFormatter numbers = new ScientificNumberFormatter();

    public String value(ScientificValue value, dev.rgcb.scholar.quantity.NumberNotation notation, boolean unicode) {
        if (value instanceof ScientificValue.Scalar scalar) return numbers.formatNumber(scalar.value(), notation, unicode);
        return numbers.format(((ScientificValue.Physical) value).value(), notation, unicode);
    }

    public String expression(Expression expression, Document document, boolean unicode) {
        Objects.requireNonNull(expression);
        if (expression instanceof Expression.NumberLiteral number) return number.value().stripTrailingZeros().toPlainString();
        if (expression instanceof Expression.ValueLiteral literal) return value(literal.value(), dev.rgcb.scholar.quantity.NumberNotation.DECIMAL, unicode);
        if (expression instanceof Expression.Variable variable) return document.blocks().stream()
                .filter(VariableDefinition.class::isInstance).map(VariableDefinition.class::cast)
                .filter(definition -> definition.id().equals(variable.reference().variableId()))
                .map(VariableDefinition::name).findFirst().orElse(variable.authoredName());
        if (expression instanceof Expression.UnresolvedName name) return name.name();
        if (expression instanceof Expression.Invalid invalid) return invalid.source();
        if (expression instanceof Expression.Group group) return "(" + expression(group.inner(), document, unicode) + ")";
        if (expression instanceof Expression.Unary unary) return (unary.operator() == Expression.UnaryOperator.MINUS ? "-" : "+")
                + expression(unary.operand(), document, unicode);
        if (expression instanceof Expression.Power power) return expression(power.base(), document, unicode)
                + "^" + power.exponent();
        var binary = (Expression.Binary) expression;
        var operator = switch (binary.operator()) {
            case ADD -> " + ";
            case SUBTRACT -> " - ";
            case MULTIPLY -> unicode ? " × " : " * ";
            case DIVIDE -> " / ";
        };
        return expression(binary.left(), document, unicode) + operator + expression(binary.right(), document, unicode);
    }

    public String result(ComputedResult block, ComputationResult result, Document document, boolean unicode) {
        var prefix = block.label().filter(text -> !text.isBlank()).map(text -> text + ": ").orElse("");
        var authored = expression(block.expression(), document, unicode);
        if (result.value().isEmpty()) {
            return prefix + authored + " [" + result.diagnostics().getFirst().message() + "]";
        }
        var scientific = result.value().orElseThrow();
        if (block.displayUnit().isPresent()) {
            if (!(scientific instanceof ScientificValue.Physical physical)) {
                return prefix + authored + " [Display unit requires a physical result]";
            }
            try {
                var target = block.displayUnit().orElseThrow();
                scientific = new ScientificValue.Physical(physical.value() instanceof MeasuredQuantity measured
                        ? new UnitConverter().convert(measured, target)
                        : new UnitConverter().convert(physical.value().nominal(), target));
            } catch (IllegalArgumentException failure) {
                return prefix + authored + " [Incompatible display unit]";
            }
        }
        return prefix + authored + " = " + value(scientific, block.notation(), unicode);
    }
}
