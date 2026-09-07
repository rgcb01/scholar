package dev.rgcb.scholar.electrical.symbol;

/** Small internal vector vocabulary for derived schematic presentation. */
public sealed interface ElectricalSymbolPrimitive
        permits ElectricalSymbolLine, ElectricalSymbolPolyline, ElectricalSymbolCircle {
}
