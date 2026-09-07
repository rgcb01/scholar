package dev.rgcb.scholar.electrical.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.layout.DiagramCoordinateTransform;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import org.junit.jupiter.api.Test;

class ElectricalSymbolLayoutEngineTest {
    private final ElectricalSymbolLayoutEngine engine = new ElectricalSymbolLayoutEngine();

    @Test
    void resistorGeometrySpansAuthoredBoundsAtCanonicalTerminals() {
        var component = component(ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, new DiagramBounds(10, 10, 30, 10));
        var transform = new DiagramCoordinateTransform(new DiagramCanvas(100, 50), 2.0, 8, 12);
        var primitives = engine.layout(component, transform, 60, 20);
        var firstLead = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(0));
        var lastLead = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(2));

        assertEquals((int) Math.round(transform.mapX(10)), firstLead.start().x());
        assertEquals((int) Math.round(transform.mapY(15)), firstLead.start().y());
        assertEquals((int) Math.round(transform.mapX(40)), lastLead.end().x());
        assertEquals((int) Math.round(transform.mapY(15)), lastLead.end().y());
    }

    @Test
    void quarterTurnRotatesCanonicalResistorGeometry() {
        var component = component(ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_90, new DiagramBounds(10, 5, 10, 30));
        var transform = new DiagramCoordinateTransform(new DiagramCanvas(50, 50), 2.0, 0, 0);
        var primitives = engine.layout(component, transform, 20, 60);
        var firstLead = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(0));
        var lastLead = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(2));

        assertEquals((int) Math.round(transform.mapX(15)), firstLead.start().x());
        assertEquals((int) Math.round(transform.mapY(5)), firstLead.start().y());
        assertEquals((int) Math.round(transform.mapX(15)), lastLead.end().x());
        assertEquals((int) Math.round(transform.mapY(35)), lastLead.end().y());
    }

    @Test
    void voltageSourceCircleGetsPositiveResponsivePixelRadius() {
        var component = component(ElectricalComponentKind.DC_VOLTAGE_SOURCE, ElectricalOrientation.DEG_0, new DiagramBounds(10, 10, 20, 20));
        var transform = new DiagramCoordinateTransform(new DiagramCanvas(100, 100), 1.5, 0, 0);
        var circle = engine.layout(component, transform, 30, 30).stream()
                .filter(LaidOutElectricalCircle.class::isInstance)
                .map(LaidOutElectricalCircle.class::cast)
                .findFirst()
                .orElseThrow();

        assertTrue(circle.radius() > 0);
        assertEquals((int) Math.round(transform.mapX(20)), circle.center().x());
        assertEquals((int) Math.round(transform.mapY(20)), circle.center().y());
    }


    @Test
    void verticalVoltageSourceLeadsTouchCircleAndPolarityMarksStayUpright() {
        var component = component(
                ElectricalComponentKind.DC_VOLTAGE_SOURCE,
                ElectricalOrientation.DEG_90,
                new DiagramBounds(10, 10, 20, 40));
        var transform = new DiagramCoordinateTransform(new DiagramCanvas(100, 100), 1.0, 0, 0);
        var primitives = engine.layout(component, transform, 20, 40);

        var positiveLead = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(0));
        var circle = assertInstanceOf(LaidOutElectricalCircle.class, primitives.get(1));
        var negativeLead = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(2));
        var plusHorizontal = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(3));
        var plusVertical = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(4));
        var minus = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(5));

        assertEquals(circle.center().x(), positiveLead.end().x());
        assertEquals(circle.center().y() - circle.radius(), positiveLead.end().y());
        assertEquals(circle.center().x(), negativeLead.start().x());
        assertEquals(circle.center().y() + circle.radius(), negativeLead.start().y());

        assertEquals(plusHorizontal.start().y(), plusHorizontal.end().y());
        assertEquals(plusVertical.start().x(), plusVertical.end().x());
        assertEquals(minus.start().y(), minus.end().y());
        assertTrue(minus.end().x() > minus.start().x());
    }

    @Test
    void rotatedLedGeometryKeepsBothTerminalLeadsOnRotatedPerimeter() {
        var component = component(
                ElectricalComponentKind.LED,
                ElectricalOrientation.DEG_90,
                new DiagramBounds(10, 5, 12, 30));
        var transform = new DiagramCoordinateTransform(new DiagramCanvas(50, 50), 2.0, 0, 0);
        var primitives = engine.layout(component, transform, 24, 60);
        var firstLead = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(0));
        var rightLead = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(3));

        assertEquals((int) Math.round(transform.mapX(16)), firstLead.start().x());
        assertEquals((int) Math.round(transform.mapY(5)), firstLead.start().y());
        assertEquals((int) Math.round(transform.mapX(16)), rightLead.end().x());
        assertEquals((int) Math.round(transform.mapY(35)), rightLead.end().y());
    }

    @Test
    void rotatedLedEmissionArrowsKeepVisibleGapFromSymbolBody() {
        var component = component(
                ElectricalComponentKind.LED,
                ElectricalOrientation.DEG_90,
                new DiagramBounds(10, 5, 12, 30));
        var transform = new DiagramCoordinateTransform(new DiagramCanvas(50, 50), 4.0, 0, 0);
        var primitives = engine.layout(component, transform, 48, 120);

        var body = assertInstanceOf(LaidOutElectricalPolyline.class, primitives.get(1));
        var cathodeBar = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(2));
        var bodyRight = Math.max(
                body.points().stream().mapToInt(point -> point.x()).max().orElseThrow(),
                Math.max(cathodeBar.start().x(), cathodeBar.end().x()));

        for (var index = 4; index < primitives.size(); index++) {
            var arrow = assertInstanceOf(LaidOutElectricalLine.class, primitives.get(index));
            assertTrue(Math.min(arrow.start().x(), arrow.end().x()) >= bodyRight + 3);
        }
    }

    @Test
    void switchGeometryRotatesDeterministicallyAtOneEightyDegrees() {
        var zero = component(
                ElectricalComponentKind.SWITCH_SPST,
                ElectricalOrientation.DEG_0,
                new DiagramBounds(10, 10, 30, 10));
        var oneEighty = component(
                ElectricalComponentKind.SWITCH_SPST,
                ElectricalOrientation.DEG_180,
                new DiagramBounds(10, 10, 30, 10));
        var transform = new DiagramCoordinateTransform(new DiagramCanvas(60, 40), 1.0, 0, 0);
        var zeroLead = assertInstanceOf(LaidOutElectricalLine.class, engine.layout(zero, transform, 30, 10).get(0));
        var rotatedLead = assertInstanceOf(LaidOutElectricalLine.class, engine.layout(oneEighty, transform, 30, 10).get(0));

        assertEquals((int) Math.round(transform.mapX(40)), rotatedLead.start().x());
        assertEquals(zeroLead.start().y(), rotatedLead.start().y());
        assertTrue(rotatedLead.end().x() < rotatedLead.start().x());
    }

    @Test
    void wideningResponsiveScaleExpandsDerivedSymbolGeometry() {
        var component = component(ElectricalComponentKind.CAPACITOR, ElectricalOrientation.DEG_0, new DiagramBounds(10, 10, 20, 10));
        var narrow = engine.layout(component, new DiagramCoordinateTransform(new DiagramCanvas(100, 50), 1.0, 0, 0), 20, 10);
        var wide = engine.layout(component, new DiagramCoordinateTransform(new DiagramCanvas(100, 50), 2.0, 0, 0), 40, 20);
        var narrowLead = assertInstanceOf(LaidOutElectricalLine.class, narrow.get(0));
        var wideLead = assertInstanceOf(LaidOutElectricalLine.class, wide.get(0));

        var expected = (narrowLead.end().x() - narrowLead.start().x()) * 2;
        var actual = wideLead.end().x() - wideLead.start().x();
        assertTrue(Math.abs(expected - actual) <= 1);
    }

    private static ElectricalComponent component(
            ElectricalComponentKind kind,
            ElectricalOrientation orientation,
            DiagramBounds bounds
    ) {
        return new ElectricalComponent(new DiagramElementId("x"), bounds, kind, orientation, "X1", "value");
    }
}
