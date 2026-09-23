package dev.rgcb.scholar.quantity;

import java.util.Objects;

/** Semantic interpretation of a physical quantity, independent of its display unit. */
public enum QuantitySemantics {
    LINEAR,
    ABSOLUTE_TEMPERATURE,
    TEMPERATURE_DIFFERENCE;

    public static QuantitySemantics defaultFor(UnitExpression unit) {
        return isTemperature(Objects.requireNonNull(unit)) ? ABSOLUTE_TEMPERATURE : LINEAR;
    }

    public void validate(UnitExpression unit) {
        var temperature = isTemperature(Objects.requireNonNull(unit));
        if (temperature != (this != LINEAR)) {
            throw new IllegalArgumentException(temperature
                    ? "thermodynamic-temperature quantities require explicit absolute or difference semantics"
                    : "temperature semantics require thermodynamic-temperature dimension");
        }
    }

    public boolean isTemperature() {
        return this != LINEAR;
    }

    public boolean isAbsoluteTemperature() {
        return this == ABSOLUTE_TEMPERATURE;
    }

    public String valuePrefix(boolean unicode) {
        return this == TEMPERATURE_DIFFERENCE ? (unicode ? "Δ" : "delta ") : "";
    }

    private static boolean isTemperature(UnitExpression unit) {
        return unit.dimension(UnitRegistry.builtIn()).equals(PhysicalDimension.TEMPERATURE);
    }
}
