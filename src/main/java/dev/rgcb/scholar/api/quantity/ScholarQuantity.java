package dev.rgcb.scholar.api.quantity;

import java.math.BigDecimal;
import java.util.Objects;

/** Authored magnitude and M31 unit expression; temperature meaning is explicit. */
public record ScholarQuantity(BigDecimal value, String unit, Semantics semantics) {
    public enum Semantics { LINEAR, ABSOLUTE_TEMPERATURE, TEMPERATURE_DIFFERENCE }
    public ScholarQuantity {
        Objects.requireNonNull(value); Objects.requireNonNull(unit); Objects.requireNonNull(semantics);
    }
    public static ScholarQuantity linear(BigDecimal value, String unit) {
        return new ScholarQuantity(value, unit, Semantics.LINEAR);
    }
}
