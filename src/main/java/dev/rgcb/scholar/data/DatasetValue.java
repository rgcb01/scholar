package dev.rgcb.scholar.data;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record DatasetValue(DatasetValueKind kind, Optional<BigDecimal> number, Optional<String> text) {
    public DatasetValue {
        kind = Objects.requireNonNull(kind, "kind");
        number = Objects.requireNonNull(number, "number");
        text = Objects.requireNonNull(text, "text");
        switch (kind) {
            case NUMBER -> {
                if (number.isEmpty() || text.isPresent()) {
                    throw new IllegalArgumentException("number values require only numeric content.");
                }
            }
            case TEXT -> {
                if (text.isEmpty() || number.isPresent()) {
                    throw new IllegalArgumentException("text values require only text content.");
                }
            }
            case MISSING -> {
                if (number.isPresent() || text.isPresent()) {
                    throw new IllegalArgumentException("missing values must not contain content.");
                }
            }
        }
    }

    public static DatasetValue number(String value) {
        return number(new BigDecimal(Objects.requireNonNull(value, "value")));
    }

    public static DatasetValue number(BigDecimal value) {
        return new DatasetValue(DatasetValueKind.NUMBER, Optional.of(value), Optional.empty());
    }

    public static DatasetValue text(String value) {
        return new DatasetValue(DatasetValueKind.TEXT, Optional.empty(), Optional.of(Objects.requireNonNull(value, "value")));
    }

    public static DatasetValue missing() {
        return new DatasetValue(DatasetValueKind.MISSING, Optional.empty(), Optional.empty());
    }

    public Optional<Double> asDouble() {
        return number.map(BigDecimal::doubleValue);
    }

    public String displayText() {
        return switch (kind) {
            case NUMBER -> number.orElseThrow().stripTrailingZeros().toPlainString();
            case TEXT -> text.orElseThrow();
            case MISSING -> "";
        };
    }
}
