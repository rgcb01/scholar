package dev.rgcb.scholar.quantity;

import java.math.BigDecimal;
import java.util.Objects;

public final class UnitConverter {
    private final UnitRegistry registry;
    public UnitConverter() { this(UnitRegistry.builtIn()); }
    public UnitConverter(UnitRegistry registry) { this.registry = Objects.requireNonNull(registry); }

    public Quantity convert(Quantity source, UnitExpression target) {
        Objects.requireNonNull(source); Objects.requireNonNull(target);
        if (!source.unit().compatibleWith(target, registry)) throw new IllegalArgumentException("incompatible unit dimensions");
        source.semantics().validate(target);
        var useOffset = source.semantics().isAbsoluteTemperature();
        var sourceOffset = useOffset ? registry.offset(source.unit()) : BigDecimal.ZERO;
        var targetOffset = useOffset ? registry.offset(target) : BigDecimal.ZERO;
        var canonical = source.value().multiply(registry.scale(source.unit()), UnitRegistry.MATH_CONTEXT)
                .add(sourceOffset, UnitRegistry.MATH_CONTEXT);
        var converted = canonical.subtract(targetOffset, UnitRegistry.MATH_CONTEXT)
                .divide(registry.scale(target), UnitRegistry.MATH_CONTEXT);
        return new Quantity(normalize(converted), target, source.semantics());
    }

    public MeasuredQuantity convert(MeasuredQuantity source, UnitExpression target) {
        var nominal = convert(source.nominal(), target);
        var uncertainty = source.absoluteUncertainty().multiply(registry.scale(source.nominal().unit()), UnitRegistry.MATH_CONTEXT)
                .divide(registry.scale(target), UnitRegistry.MATH_CONTEXT);
        return new MeasuredQuantity(nominal, normalize(uncertainty));
    }

    private static BigDecimal normalize(BigDecimal value) { return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros(); }
}
