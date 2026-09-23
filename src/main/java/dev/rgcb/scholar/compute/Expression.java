package dev.rgcb.scholar.compute;

import dev.rgcb.scholar.document.VariableDependencyReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Authored computation tree; ordinary MathExpression has no evaluator semantics. */
public sealed interface Expression permits Expression.NumberLiteral, Expression.ValueLiteral,
        Expression.Variable, Expression.UnresolvedName, Expression.Unary, Expression.Binary,
        Expression.Power, Expression.Group, Expression.Invalid {
    enum Operator { ADD, SUBTRACT, MULTIPLY, DIVIDE }
    enum UnaryOperator { PLUS, MINUS }

    record NumberLiteral(BigDecimal value) implements Expression {
        public NumberLiteral { value = Objects.requireNonNull(value); }
    }

    record ValueLiteral(ScientificValue value) implements Expression {
        public ValueLiteral { value = Objects.requireNonNull(value); }
    }

    record Variable(VariableDependencyReference reference, String authoredName) implements Expression {
        public Variable {
            reference = Objects.requireNonNull(reference);
            authoredName = Objects.requireNonNull(authoredName);
            if (authoredName.isBlank()) throw new IllegalArgumentException("Variable name must not be blank.");
        }
    }

    record UnresolvedName(String name) implements Expression {
        public UnresolvedName {
            name = Objects.requireNonNull(name);
            if (name.isBlank()) throw new IllegalArgumentException("Unresolved name must not be blank.");
        }
    }

    record Unary(UnaryOperator operator, Expression operand) implements Expression {
        public Unary { operator = Objects.requireNonNull(operator); operand = Objects.requireNonNull(operand); }
    }

    record Binary(Operator operator, Expression left, Expression right) implements Expression {
        public Binary {
            operator = Objects.requireNonNull(operator);
            left = Objects.requireNonNull(left);
            right = Objects.requireNonNull(right);
        }
    }

    record Power(Expression base, int exponent) implements Expression {
        public Power { base = Objects.requireNonNull(base); }
    }

    record Group(Expression inner) implements Expression {
        public Group { inner = Objects.requireNonNull(inner); }
    }

    record Invalid(String source, ComputationDiagnostic.Code code) implements Expression {
        public Invalid {
            source = Objects.requireNonNull(source);
            code = Objects.requireNonNull(code);
        }
        public Invalid(String source) { this(source, ComputationDiagnostic.Code.INVALID_EXPRESSION); }
    }

    default List<VariableDependencyReference> dependencies() {
        var result = new ArrayList<VariableDependencyReference>();
        collect(this, result);
        return List.copyOf(result);
    }

    private static void collect(Expression expression, List<VariableDependencyReference> result) {
        if (expression instanceof Variable variable) result.add(variable.reference());
        else if (expression instanceof Unary unary) collect(unary.operand(), result);
        else if (expression instanceof Binary binary) {
            collect(binary.left(), result);
            collect(binary.right(), result);
        } else if (expression instanceof Power power) collect(power.base(), result);
        else if (expression instanceof Group group) collect(group.inner(), result);
    }

    default Expression mapVariableIds(Function<VariableDependencyReference, VariableDependencyReference> mapper) {
        Objects.requireNonNull(mapper);
        if (this instanceof Variable variable) return new Variable(mapper.apply(variable.reference()), variable.authoredName());
        if (this instanceof Unary unary) return new Unary(unary.operator(), unary.operand().mapVariableIds(mapper));
        if (this instanceof Binary binary) return new Binary(binary.operator(), binary.left().mapVariableIds(mapper), binary.right().mapVariableIds(mapper));
        if (this instanceof Power power) return new Power(power.base().mapVariableIds(mapper), power.exponent());
        if (this instanceof Group group) return new Group(group.inner().mapVariableIds(mapper));
        return this;
    }
}
