package dev.rgcb.scholar.electrical.layout;

/** Positioned pure-Java schematic geometry consumed by the Minecraft renderer. */
public sealed interface LaidOutElectricalPrimitive
        permits LaidOutElectricalLine, LaidOutElectricalPolyline, LaidOutElectricalCircle {
}
