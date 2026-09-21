package dev.rgcb.scholar.quantity;

public sealed interface QuantityValue permits Quantity, MeasuredQuantity {
    Quantity nominal();
}
