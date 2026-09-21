package dev.rgcb.scholar.quantity;

import java.math.BigDecimal;
import java.util.Objects;

public record Unit(String id, String name, String symbol, PhysicalDimension dimension,
                   BigDecimal scaleToSi, BigDecimal offsetToSi, boolean prefixAllowed) {
    public Unit {
        id = require(id, "id");
        name = require(name, "name");
        symbol = require(symbol, "symbol");
        dimension = Objects.requireNonNull(dimension, "dimension");
        scaleToSi = Objects.requireNonNull(scaleToSi, "scaleToSi");
        offsetToSi = Objects.requireNonNull(offsetToSi, "offsetToSi");
        if (scaleToSi.signum() <= 0) throw new IllegalArgumentException("unit scale must be positive");
    }

    public boolean affine() { return offsetToSi.signum() != 0; }

    private static String require(String value, String name) {
        var result = Objects.requireNonNull(value, name).trim();
        if (result.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return result;
    }
}
