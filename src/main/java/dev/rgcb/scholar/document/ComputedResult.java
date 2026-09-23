package dev.rgcb.scholar.document;

import dev.rgcb.scholar.compute.Expression;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.UnitExpression;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Authored expression and presentation preferences; evaluated value is derived. */
public record ComputedResult(Expression expression, String authoredSource, Optional<String> label,
                             Optional<UnitExpression> displayUnit, NumberNotation notation) implements ComputationTransferBlock {
    public ComputedResult {
        expression = Objects.requireNonNull(expression, "expression");
        authoredSource = Objects.requireNonNull(authoredSource, "authoredSource");
        label = Objects.requireNonNull(label, "label");
        displayUnit = Objects.requireNonNull(displayUnit, "displayUnit");
        notation = Objects.requireNonNull(notation, "notation");
    }

    public ComputedResult(Expression expression, String authoredSource) {
        this(expression, authoredSource, Optional.empty(), Optional.empty(), NumberNotation.DECIMAL);
    }

    public ComputedResult withExpression(Expression replacement, String source) {
        return new ComputedResult(replacement, source, label, displayUnit, notation);
    }

    public ComputedResult withDisplayUnit(Optional<UnitExpression> replacement) {
        return new ComputedResult(expression, authoredSource, label, replacement, notation);
    }

    public ComputedResult withNotation(NumberNotation replacement) {
        return new ComputedResult(expression, authoredSource, label, displayUnit, replacement);
    }

    @Override public Optional<String> definedVariableId() { return Optional.empty(); }
    @Override public List<VariableDependencyReference> variableDependencies() { return expression.dependencies(); }
    @Override public ComputedResult withVariableTransferIds(Optional<String> definedId,
                                                             List<VariableDependencyReference> dependencies) {
        if (definedId.isPresent() || dependencies.size() != variableDependencies().size()) {
            throw new IllegalArgumentException("Computed result transfer identities do not match its expression.");
        }
        var index = new int[1];
        var rewritten = expression.mapVariableIds(source -> dependencies.get(index[0]++));
        return new ComputedResult(rewritten, authoredSource, label, displayUnit, notation);
    }
}
