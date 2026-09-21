package dev.rgcb.scholar.persistence;

import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.math.*;
import dev.rgcb.scholar.mechanical.*;
import dev.rgcb.scholar.plot.AxisScale;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.Map;
import static java.util.Map.entry;

/** Closed V1 wire vocabulary, deliberately independent of enum names/ordinals. */
final class V1WireLabels {
    private V1WireLabels() { }
    private static final Map<Enum<?>, String> VALUES = Map.ofEntries(
            entry(TextMark.BOLD, "bold"), entry(TextMark.ITALIC, "italic"),
            entry(CrossReferenceTargetKind.FIGURE, "figure"), entry(CrossReferenceTargetKind.TABLE, "table"),
            entry(CrossReferenceTargetKind.EQUATION, "equation"), entry(CrossReferenceTargetKind.SECTION, "section"),
            entry(DatasetColumnType.NUMBER, "number"), entry(DatasetColumnType.TEXT, "text"),
            entry(MathDelimiter.PARENTHESES, "parentheses"), entry(MathDelimiter.BRACKETS, "brackets"), entry(MathDelimiter.BRACES, "braces"),
            entry(MathOperatorRole.RELATION, "relation"), entry(MathOperatorRole.BINARY, "binary"),
            entry(MathOperatorRole.UNARY, "unary"), entry(MathOperatorRole.PUNCTUATION, "punctuation"), entry(MathOperatorRole.OTHER, "other"),
            entry(MathSymbolKind.GREEK, "greek"), entry(MathSymbolKind.CONSTANT, "constant"),
            entry(MathSymbolKind.INFINITY, "infinity"), entry(MathSymbolKind.CALCULUS, "calculus"), entry(MathSymbolKind.OTHER, "other"),
            entry(AxisScale.LINEAR, "linear"), entry(PlotSeriesKind.LINE, "line"), entry(PlotSeriesKind.SCATTER, "scatter"),
            entry(DiagramPortSide.LEFT, "left"), entry(DiagramPortSide.RIGHT, "right"), entry(DiagramPortSide.TOP, "top"), entry(DiagramPortSide.BOTTOM, "bottom"),
            entry(ElectricalComponentKind.RESISTOR, "resistor"), entry(ElectricalComponentKind.CAPACITOR, "capacitor"),
            entry(ElectricalComponentKind.DC_VOLTAGE_SOURCE, "dc-voltage-source"), entry(ElectricalComponentKind.GROUND, "ground"),
            entry(ElectricalComponentKind.DIODE, "diode"), entry(ElectricalComponentKind.LED, "led"), entry(ElectricalComponentKind.SWITCH_SPST, "switch-spst"),
            entry(ElectricalOrientation.DEG_0, "deg-0"), entry(ElectricalOrientation.DEG_90, "deg-90"),
            entry(ElectricalOrientation.DEG_180, "deg-180"), entry(ElectricalOrientation.DEG_270, "deg-270"),
            entry(MechanicalPrimitiveKind.LINE, "line"), entry(MechanicalPrimitiveKind.CENTERLINE, "centerline"),
            entry(MechanicalPrimitiveKind.RECTANGLE, "rectangle"), entry(MechanicalPrimitiveKind.CIRCLE, "circle"),
            entry(MechanicalPrimitiveKind.ARC, "arc"), entry(MechanicalPrimitiveKind.ARROW, "arrow"), entry(MechanicalPrimitiveKind.REFERENCE_POINT, "reference-point"),
            entry(MechanicalOrientation.DEG_0, "deg-0"), entry(MechanicalOrientation.DEG_90, "deg-90"),
            entry(MechanicalSymbolKind.SHAFT, "shaft"), entry(MechanicalSymbolKind.GEAR, "gear"), entry(MechanicalSymbolKind.BEARING, "bearing"),
            entry(MechanicalSymbolKind.SPRING, "spring"), entry(MechanicalSymbolKind.PISTON, "piston"), entry(MechanicalSymbolKind.BOLT, "bolt"),
            entry(MechanicalDimensionKind.HORIZONTAL, "horizontal"), entry(MechanicalDimensionKind.VERTICAL, "vertical"),
            entry(MechanicalDimensionKind.ALIGNED, "aligned"), entry(MechanicalDimensionKind.RADIUS, "radius"),
            entry(MechanicalDimensionKind.DIAMETER, "diameter"), entry(MechanicalDimensionKind.ANGLE, "angle"),
            entry(MechanicalAnnotationKind.PART_LABEL, "part-label"), entry(MechanicalAnnotationKind.NOTE, "note"), entry(MechanicalAnnotationKind.LEADER, "leader"),
            entry(MechanicalConstraintKind.HORIZONTAL, "horizontal"), entry(MechanicalConstraintKind.VERTICAL, "vertical"),
            entry(MechanicalConstraintKind.COINCIDENT, "coincident"), entry(MechanicalConstraintKind.PARALLEL, "parallel"),
            entry(MechanicalConstraintKind.PERPENDICULAR, "perpendicular"), entry(MechanicalConstraintKind.CONCENTRIC, "concentric"));

    static String label(Enum<?> value) {
        var label = VALUES.get(value);
        if (label == null) throw new IllegalArgumentException("Enum value is not supported by Scholar V1.");
        return label;
    }
}
