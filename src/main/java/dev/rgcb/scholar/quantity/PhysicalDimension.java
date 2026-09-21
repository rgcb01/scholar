package dev.rgcb.scholar.quantity;

/** Immutable SI base-dimension exponent vector. */
public record PhysicalDimension(int length, int mass, int time, int electricCurrent,
                                int temperature, int amount, int luminousIntensity) {
    public static final PhysicalDimension DIMENSIONLESS = new PhysicalDimension(0, 0, 0, 0, 0, 0, 0);
    public static final PhysicalDimension LENGTH = new PhysicalDimension(1, 0, 0, 0, 0, 0, 0);
    public static final PhysicalDimension MASS = new PhysicalDimension(0, 1, 0, 0, 0, 0, 0);
    public static final PhysicalDimension TIME = new PhysicalDimension(0, 0, 1, 0, 0, 0, 0);
    public static final PhysicalDimension CURRENT = new PhysicalDimension(0, 0, 0, 1, 0, 0, 0);
    public static final PhysicalDimension TEMPERATURE = new PhysicalDimension(0, 0, 0, 0, 1, 0, 0);
    public static final PhysicalDimension AMOUNT = new PhysicalDimension(0, 0, 0, 0, 0, 1, 0);
    public static final PhysicalDimension LUMINOUS_INTENSITY = new PhysicalDimension(0, 0, 0, 0, 0, 0, 1);

    public PhysicalDimension multiply(PhysicalDimension other) {
        return new PhysicalDimension(length + other.length, mass + other.mass, time + other.time,
                electricCurrent + other.electricCurrent, temperature + other.temperature,
                amount + other.amount, luminousIntensity + other.luminousIntensity);
    }

    public PhysicalDimension divide(PhysicalDimension other) { return multiply(other.pow(-1)); }

    public PhysicalDimension pow(int exponent) {
        return new PhysicalDimension(length * exponent, mass * exponent, time * exponent,
                electricCurrent * exponent, temperature * exponent, amount * exponent,
                luminousIntensity * exponent);
    }

    public boolean isDimensionless() { return equals(DIMENSIONLESS); }
}
