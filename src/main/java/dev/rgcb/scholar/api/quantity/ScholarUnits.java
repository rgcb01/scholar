package dev.rgcb.scholar.api.quantity;

import java.util.List;

/** Read-only access to Scholar's built-in M31 units and dimensional conversions. */
public interface ScholarUnits {
    record Unit(String symbol, String name) {}
    record Dimension(int length, int mass, int time, int current, int temperature, int amount, int luminosity) {}
    List<Unit> builtIns();
    Dimension dimension(String expression);
    ScholarQuantity convert(ScholarQuantity source, String targetUnit);
}
