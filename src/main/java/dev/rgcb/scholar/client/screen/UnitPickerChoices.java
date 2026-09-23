package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.quantity.MetricPrefix;
import dev.rgcb.scholar.quantity.PhysicalDimension;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitConverter;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitParser;
import dev.rgcb.scholar.quantity.UnitRegistry;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/** Picker presentation derived from the M31 registry, never a second unit definition. */
final class UnitPickerChoices {
    private static final UnitRegistry REGISTRY = UnitRegistry.builtIn();
    private static final UnitParser PARSER = new UnitParser();
    private static final List<MetricPrefix> COMMON_PREFIXES = List.of(
            MetricPrefix.MILLI, MetricPrefix.CENTI, MetricPrefix.KILO);

    private UnitPickerChoices() { }

    public record Choice(String label, Optional<UnitExpression> unit, QuantitySemantics semantics) {
        public Choice {
            if (label == null || label.isBlank() || unit == null || semantics == null) {
                throw new IllegalArgumentException("Invalid unit picker choice");
            }
        }
    }

    public static List<Choice> available() {
        var choices = new LinkedHashMap<String, Choice>();
        add(choices, "Dimensionless / No unit", Optional.empty(), QuantitySemantics.LINEAR);
        var units = REGISTRY.units().stream()
                .sorted(Comparator.comparing(unit -> unit.symbol()))
                .toList();
        for (var unit : units) {
            if (unit.id().equals("one")) continue;
            addSymbol(choices, unit.symbol());
            if (unit.prefixAllowed()) {
                for (var prefix : COMMON_PREFIXES) addSymbol(choices, prefix.symbol() + unit.symbol());
            }
        }
        var simpleChoices = List.copyOf(choices.values());
        var lengths = simpleChoices.stream().filter(choice -> choice.unit().isPresent()
                && choice.unit().orElseThrow().dimension(REGISTRY).equals(PhysicalDimension.LENGTH))
                .map(choice -> choice.unit().orElseThrow().displaySymbol(REGISTRY)).toList();
        var times = simpleChoices.stream().filter(choice -> choice.unit().isPresent()
                && choice.unit().orElseThrow().dimension(REGISTRY).equals(PhysicalDimension.TIME))
                .map(choice -> choice.unit().orElseThrow().displaySymbol(REGISTRY)).toList();
        for (var length : lengths) {
            for (var time : times) {
                addSymbol(choices, length + "/" + time);
                addSymbol(choices, length + "/" + time + "²");
            }
        }
        return List.copyOf(choices.values());
    }

    private static void addSymbol(LinkedHashMap<String, Choice> choices, String symbol) {
        var expression = PARSER.parseRequired(symbol);
        if (expression.dimension(REGISTRY).equals(PhysicalDimension.TEMPERATURE)) {
            add(choices, expression.displaySymbol(REGISTRY) + " · absolute", Optional.of(expression),
                    QuantitySemantics.ABSOLUTE_TEMPERATURE);
            add(choices, "Δ" + expression.displaySymbol(REGISTRY) + " · difference", Optional.of(expression),
                    QuantitySemantics.TEMPERATURE_DIFFERENCE);
        } else {
            add(choices, expression.displaySymbol(REGISTRY), Optional.of(expression), QuantitySemantics.LINEAR);
        }
    }

    private static void add(LinkedHashMap<String, Choice> choices, String label,
                            Optional<UnitExpression> expression, QuantitySemantics semantics) {
        choices.putIfAbsent(label, new Choice(label, expression, semantics));
    }

    public static Optional<Choice> matching(Optional<UnitExpression> unit, QuantitySemantics semantics) {
        return available().stream().filter(choice -> choice.unit().equals(unit) && choice.semantics() == semantics)
                .findFirst();
    }

    public static BigDecimal convertedValue(BigDecimal value, Optional<UnitExpression> from,
                                             QuantitySemantics fromSemantics, Choice to) {
        if (from.isPresent() && to.unit().isPresent() && fromSemantics == to.semantics()
                && from.orElseThrow().compatibleWith(to.unit().orElseThrow(), REGISTRY)) {
            return new UnitConverter(REGISTRY).convert(
                    new Quantity(value, from.orElseThrow(), fromSemantics), to.unit().orElseThrow()).value();
        }
        return value;
    }
}
