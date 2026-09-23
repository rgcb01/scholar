package dev.rgcb.scholar.compute;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.document.VariableDependencyReference;
import java.util.HashMap;
import java.util.Map;

/** Binds only uniquely named, still-unresolved authored operands to stable IDs. */
public final class ExpressionBinder {
    public Expression bindAvailable(Expression expression, Document document) {
        var byName = new HashMap<String, VariableDefinition>();
        var counts = new HashMap<String, Integer>();
        document.blocks().stream().filter(VariableDefinition.class::isInstance).map(VariableDefinition.class::cast)
                .forEach(variable -> {
                    byName.put(variable.name(), variable);
                    counts.merge(variable.name(), 1, Integer::sum);
                });
        return bind(expression, byName, counts);
    }

    private Expression bind(Expression expression, Map<String, VariableDefinition> byName, Map<String, Integer> counts) {
        if (expression instanceof Expression.UnresolvedName name && counts.getOrDefault(name.name(), 0) == 1) {
            return new Expression.Variable(new VariableDependencyReference(byName.get(name.name()).id()), name.name());
        }
        if (expression instanceof Expression.Unary unary) {
            return new Expression.Unary(unary.operator(), bind(unary.operand(), byName, counts));
        }
        if (expression instanceof Expression.Binary binary) {
            return new Expression.Binary(binary.operator(), bind(binary.left(), byName, counts), bind(binary.right(), byName, counts));
        }
        if (expression instanceof Expression.Power power) return new Expression.Power(bind(power.base(), byName, counts), power.exponent());
        if (expression instanceof Expression.Group group) return new Expression.Group(bind(group.inner(), byName, counts));
        return expression;
    }
}
