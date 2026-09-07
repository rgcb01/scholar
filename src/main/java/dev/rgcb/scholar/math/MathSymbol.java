package dev.rgcb.scholar.math;

import java.util.Objects;

/**
 * A special mathematical symbol that is not an ordinary identifier.
 */
public record MathSymbol(String symbol, MathSymbolKind kind) implements MathExpression {
    public MathSymbol {
        symbol = Objects.requireNonNull(symbol, "symbol");
        kind = Objects.requireNonNull(kind, "kind");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Math symbol must not be blank.");
        }
    }
}
