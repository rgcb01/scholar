package dev.rgcb.scholar.electrical.symbol;

import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import java.util.List;
import java.util.Objects;

/** Canonical DEG_0 normalized geometry for Scholar's built-in schematic symbols. */
public final class ElectricalSymbolLibrary {
    public ElectricalSymbol symbol(ElectricalComponentKind kind) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case RESISTOR -> resistor();
            case CAPACITOR -> capacitor();
            case DC_VOLTAGE_SOURCE -> dcVoltageSource();
            case GROUND -> ground();
            case DIODE -> diode();
            case LED -> led();
            case SWITCH_SPST -> switchSpst();
        };
    }

    private static ElectricalSymbol resistor() {
        // M18H.1: logical ports stay on the authored perimeter, while the
        // visible resistor body is brought closer to them. The drawn terminal
        // legs are shorter without moving anchors or interaction geometry.
        return new ElectricalSymbol(List.of(
                line(0.0, 0.5, 0.16, 0.5),
                polyline(
                        point(0.16, 0.5),
                        point(0.22, 0.38),
                        point(0.30, 0.62),
                        point(0.38, 0.38),
                        point(0.46, 0.62),
                        point(0.54, 0.38),
                        point(0.62, 0.62),
                        point(0.70, 0.38),
                        point(0.84, 0.5)),
                line(0.84, 0.5, 1.0, 0.5)));
    }

    private static ElectricalSymbol capacitor() {
        return new ElectricalSymbol(List.of(
                line(0.0, 0.5, 0.25, 0.5),
                line(0.25, 0.31, 0.25, 0.69),
                line(0.75, 0.31, 0.75, 0.69),
                line(0.75, 0.5, 1.0, 0.5)));
    }

    private static ElectricalSymbol dcVoltageSource() {
        // The actual source circle/lead layout is specialized in
        // ElectricalSymbolLayoutEngine so the circle stays round under
        // rectangular authored bounds. This canonical form remains the semantic
        // reference shape used by library-level tests and future tooling.
        return new ElectricalSymbol(List.of(
                line(0.0, 0.5, 0.31, 0.5),
                new ElectricalSymbolCircle(point(0.5, 0.5), 0.19),
                line(0.69, 0.5, 1.0, 0.5),
                // Positive terminal marker near canonical left side.
                line(0.40, 0.45, 0.47, 0.45),
                line(0.435, 0.415, 0.435, 0.485),
                // Negative terminal marker near canonical right side.
                line(0.53, 0.55, 0.60, 0.55)));
    }

    private static ElectricalSymbol ground() {
        return new ElectricalSymbol(List.of(
                line(0.5, 0.0, 0.5, 0.25),
                line(0.20, 0.25, 0.80, 0.25),
                line(0.31, 0.40, 0.69, 0.40),
                line(0.41, 0.55, 0.59, 0.55)));
    }

    private static ElectricalSymbol diode() {
        return new ElectricalSymbol(List.of(
                line(0.0, 0.5, 0.20, 0.5),
                polyline(
                        point(0.20, 0.30),
                        point(0.60, 0.5),
                        point(0.20, 0.70),
                        point(0.20, 0.30)),
                line(0.68, 0.28, 0.68, 0.72),
                line(0.68, 0.5, 1.0, 0.5)));
    }

    private static ElectricalSymbol led() {
        return new ElectricalSymbol(List.of(
                line(0.0, 0.5, 0.20, 0.5),
                polyline(
                        point(0.20, 0.30),
                        point(0.60, 0.5),
                        point(0.20, 0.70),
                        point(0.20, 0.30)),
                line(0.68, 0.28, 0.68, 0.72),
                line(0.68, 0.5, 1.0, 0.5),
                // Light-emission arrows live in a dedicated gutter above the
                // canonical diode body. Keeping every arrow point above y=0.24
                // gives the rotated DEG_90 symbol a visible gap to the cathode/body
                // instead of letting the arrows merge with the lower terminal lead.
                line(0.53, 0.19, 0.63, 0.09),
                line(0.63, 0.09, 0.57, 0.10),
                line(0.63, 0.09, 0.62, 0.15),
                line(0.61, 0.20, 0.71, 0.10),
                line(0.71, 0.10, 0.65, 0.11),
                line(0.71, 0.10, 0.70, 0.16)));
    }

    private static ElectricalSymbol switchSpst() {
        return new ElectricalSymbol(List.of(
                line(0.0, 0.5, 0.22, 0.5),
                line(0.23, 0.5, 0.72, 0.34),
                line(0.76, 0.5, 1.0, 0.5),
                // Short contact marks keep the open state readable at Minecraft GUI scale.
                line(0.22, 0.44, 0.22, 0.56),
                line(0.76, 0.44, 0.76, 0.56)));
    }

    private static ElectricalSymbolLine line(double x1, double y1, double x2, double y2) {
        return new ElectricalSymbolLine(point(x1, y1), point(x2, y2));
    }

    private static ElectricalSymbolPolyline polyline(NormalizedElectricalPoint... points) {
        return new ElectricalSymbolPolyline(List.of(points));
    }

    private static NormalizedElectricalPoint point(double x, double y) {
        return new NormalizedElectricalPoint(x, y);
    }
}
