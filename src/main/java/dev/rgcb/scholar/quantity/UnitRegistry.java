package dev.rgcb.scholar.quantity;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UnitRegistry {
    public static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;
    private static final UnitRegistry BUILT_INS = new UnitRegistry(builtIns());
    private final Map<String, Unit> byId;
    private final Map<String, Unit> bySymbol;

    public UnitRegistry(List<Unit> units) {
        var ids = new LinkedHashMap<String, Unit>();
        var symbols = new LinkedHashMap<String, Unit>();
        for (var unit : units) {
            if (ids.put(unit.id(), unit) != null || symbols.put(unit.symbol(), unit) != null) {
                throw new IllegalArgumentException("unit IDs and symbols must be unique");
            }
        }
        byId = Map.copyOf(ids);
        bySymbol = Map.copyOf(symbols);
    }

    public static UnitRegistry builtIn() { return BUILT_INS; }
    public Optional<Unit> findById(String id) { return Optional.ofNullable(byId.get(id)); }
    public Optional<Unit> findBySymbol(String symbol) { return Optional.ofNullable(bySymbol.get(normalize(symbol))); }
    public List<Unit> units() { return List.copyOf(byId.values()); }

    public PhysicalDimension dimension(UnitExpression expression) {
        var result = PhysicalDimension.DIMENSIONLESS;
        for (var factor : expression.factors()) result = result.multiply(unit(factor).dimension().pow(factor.exponent()));
        return result;
    }

    public BigDecimal scale(UnitExpression expression) {
        var result = BigDecimal.ONE;
        for (var factor : expression.factors()) {
            var base = unit(factor).scaleToSi().multiply(factor.prefix().map(MetricPrefix::factor).orElse(BigDecimal.ONE), MATH_CONTEXT);
            result = factor.exponent() > 0
                    ? result.multiply(base.pow(factor.exponent(), MATH_CONTEXT), MATH_CONTEXT)
                    : result.divide(base.pow(-factor.exponent(), MATH_CONTEXT), MATH_CONTEXT);
        }
        return result;
    }

    public boolean affine(UnitExpression expression) { return simpleUnit(expression).filter(Unit::affine).isPresent(); }
    public BigDecimal offset(UnitExpression expression) { return simpleUnit(expression).map(Unit::offsetToSi).orElse(BigDecimal.ZERO); }

    public Optional<Unit> simpleUnit(UnitExpression expression) {
        if (expression.factors().size() != 1 || expression.factors().getFirst().exponent() != 1
                || expression.factors().getFirst().prefix().isPresent()) return Optional.empty();
        return findById(expression.factors().getFirst().unitId());
    }

    public String displaySymbol(UnitExpression expression, boolean unicode) {
        var numerator = new ArrayList<String>();
        var denominator = new ArrayList<String>();
        for (var factor : expression.factors()) {
            var text = factor.prefix().map(MetricPrefix::symbol).orElse("") + unit(factor).symbol();
            var exponent = Math.abs(factor.exponent());
            if (exponent != 1) text += unicode ? superscript(exponent) : "^" + exponent;
            (factor.exponent() > 0 ? numerator : denominator).add(text);
        }
        var multiplication = unicode ? "·" : "*";
        var top = numerator.isEmpty() ? "1" : String.join(multiplication, numerator);
        if (denominator.isEmpty()) return top;
        var bottom = String.join(multiplication, denominator);
        return top + "/" + (denominator.size() > 1 ? "(" + bottom + ")" : bottom);
    }

    Unit unit(UnitFactor factor) {
        var unit = findById(factor.unitId()).orElseThrow(() -> new IllegalArgumentException("unknown unit: " + factor.unitId()));
        if (factor.prefix().isPresent() && !unit.prefixAllowed()) throw new IllegalArgumentException("unit does not accept prefixes: " + unit.symbol());
        return unit;
    }

    UnitFactor factorForSymbol(String symbol, int exponent) {
        var normalized = normalize(symbol);
        var exact = findBySymbol(normalized);
        if (exact.isPresent()) return new UnitFactor(exact.orElseThrow().id(), exponent);
        for (var prefix : java.util.Arrays.stream(MetricPrefix.values())
                .sorted(java.util.Comparator.comparingInt((MetricPrefix value) -> value.symbol().length()).reversed()).toList()) {
            if (!normalized.startsWith(prefix.symbol()) || normalized.length() == prefix.symbol().length()) continue;
            var unit = findBySymbol(normalized.substring(prefix.symbol().length()));
            if (unit.isPresent() && unit.orElseThrow().prefixAllowed()) {
                return new UnitFactor(unit.orElseThrow().id(), Optional.of(prefix), exponent);
            }
        }
        throw new IllegalArgumentException("unknown unit symbol: " + symbol);
    }

    private static String normalize(String value) { return Objects.requireNonNull(value, "symbol").replace('μ', 'µ'); }

    private static String superscript(int value) {
        return Integer.toString(value).chars().mapToObj(c -> String.valueOf("⁰¹²³⁴⁵⁶⁷⁸⁹".charAt(c - '0')))
                .collect(java.util.stream.Collectors.joining());
    }

    private static List<Unit> builtIns() {
        var l = PhysicalDimension.LENGTH; var m = PhysicalDimension.MASS; var t = PhysicalDimension.TIME;
        var i = PhysicalDimension.CURRENT; var k = PhysicalDimension.TEMPERATURE;
        return List.of(
                unit("one", "dimensionless", "1", PhysicalDimension.DIMENSIONLESS, "1", false),
                unit("metre", "metre", "m", l, "1", true),
                unit("kilogram", "kilogram", "kg", m, "1", false),
                unit("gram", "gram", "g", m, "0.001", true),
                unit("second", "second", "s", t, "1", true),
                unit("ampere", "ampere", "A", i, "1", true),
                unit("kelvin", "kelvin", "K", k, "1", true),
                new Unit("celsius", "degree Celsius", "°C", k, BigDecimal.ONE, new BigDecimal("273.15"), false),
                unit("mole", "mole", "mol", PhysicalDimension.AMOUNT, "1", true),
                unit("candela", "candela", "cd", PhysicalDimension.LUMINOUS_INTENSITY, "1", true),
                unit("minute", "minute", "min", t, "60", false), unit("hour", "hour", "h", t, "3600", false),
                unit("litre", "litre", "L", l.pow(3), "0.001", true),
                unit("hertz", "hertz", "Hz", t.pow(-1), "1", true),
                unit("newton", "newton", "N", m.multiply(l).divide(t.pow(2)), "1", true),
                unit("pascal", "pascal", "Pa", m.divide(l).divide(t.pow(2)), "1", true),
                unit("joule", "joule", "J", m.multiply(l.pow(2)).divide(t.pow(2)), "1", true),
                unit("watt", "watt", "W", m.multiply(l.pow(2)).divide(t.pow(3)), "1", true),
                unit("coulomb", "coulomb", "C", t.multiply(i), "1", true),
                unit("volt", "volt", "V", m.multiply(l.pow(2)).divide(t.pow(3)).divide(i), "1", true),
                unit("farad", "farad", "F", m.pow(-1).multiply(l.pow(-2)).multiply(t.pow(4)).multiply(i.pow(2)), "1", true),
                unit("ohm", "ohm", "Ω", m.multiply(l.pow(2)).divide(t.pow(3)).divide(i.pow(2)), "1", true),
                unit("siemens", "siemens", "S", m.pow(-1).multiply(l.pow(-2)).multiply(t.pow(3)).multiply(i.pow(2)), "1", true),
                unit("weber", "weber", "Wb", m.multiply(l.pow(2)).divide(t.pow(2)).divide(i), "1", true),
                unit("tesla", "tesla", "T", m.divide(t.pow(2)).divide(i), "1", true),
                unit("henry", "henry", "H", m.multiply(l.pow(2)).divide(t.pow(2)).divide(i.pow(2)), "1", true));
    }

    private static Unit unit(String id, String name, String symbol, PhysicalDimension dimension, String scale, boolean prefix) {
        return new Unit(id, name, symbol, dimension, new BigDecimal(scale), BigDecimal.ZERO, prefix);
    }
}
