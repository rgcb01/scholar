package dev.rgcb.scholar.electrical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.electrical.symbol.ElectricalSymbolCircle;
import dev.rgcb.scholar.electrical.symbol.ElectricalSymbolLibrary;
import dev.rgcb.scholar.electrical.symbol.ElectricalSymbolLine;
import dev.rgcb.scholar.electrical.symbol.ElectricalSymbolPolyline;
import org.junit.jupiter.api.Test;

class ElectricalSymbolLibraryTest {
    private final ElectricalSymbolLibrary library = new ElectricalSymbolLibrary();

    @Test
    void everyApprovedBasicKindHasDeterministicDerivedGeometry() {
        for (var kind : ElectricalComponentKind.values()) {
            var first = library.symbol(kind);
            var second = library.symbol(kind);
            assertTrue(!first.primitives().isEmpty());
            assertEquals(first, second);
        }
    }

    @Test
    void resistorUsesLeadsAndARecognizableZigzagPolyline() {
        var symbol = library.symbol(ElectricalComponentKind.RESISTOR);

        assertEquals(3, symbol.primitives().size());
        assertInstanceOf(ElectricalSymbolLine.class, symbol.primitives().get(0));
        assertInstanceOf(ElectricalSymbolPolyline.class, symbol.primitives().get(1));
        assertInstanceOf(ElectricalSymbolLine.class, symbol.primitives().get(2));
    }

    @Test
    void capacitorUsesTwoParallelPlatesBetweenLeads() {
        var symbol = library.symbol(ElectricalComponentKind.CAPACITOR);

        assertEquals(4, symbol.primitives().size());
        assertTrue(symbol.primitives().stream().allMatch(ElectricalSymbolLine.class::isInstance));
    }

    @Test
    void dcVoltageSourceContainsCircleAndPolarityMarks() {
        var symbol = library.symbol(ElectricalComponentKind.DC_VOLTAGE_SOURCE);

        assertTrue(symbol.primitives().stream().anyMatch(ElectricalSymbolCircle.class::isInstance));
        assertTrue(symbol.primitives().size() >= 6);
    }

    @Test
    void diodeUsesDirectionalBodyCathodeBarAndTerminalLeads() {
        var symbol = library.symbol(ElectricalComponentKind.DIODE);

        assertEquals(4, symbol.primitives().size());
        assertInstanceOf(ElectricalSymbolLine.class, symbol.primitives().get(0));
        assertInstanceOf(ElectricalSymbolPolyline.class, symbol.primitives().get(1));
        assertInstanceOf(ElectricalSymbolLine.class, symbol.primitives().get(2));
        assertInstanceOf(ElectricalSymbolLine.class, symbol.primitives().get(3));
    }


    @Test
    void diodeBodyUsesShortTerminalLeadsAcrossTheFullFootprint() {
        var symbol = library.symbol(ElectricalComponentKind.DIODE);
        var firstLead = assertInstanceOf(ElectricalSymbolLine.class, symbol.primitives().get(0));
        var body = assertInstanceOf(ElectricalSymbolPolyline.class, symbol.primitives().get(1));
        var cathodeBar = assertInstanceOf(ElectricalSymbolLine.class, symbol.primitives().get(2));
        var lastLead = assertInstanceOf(ElectricalSymbolLine.class, symbol.primitives().get(3));

        assertEquals(0.0, firstLead.start().x(), 1.0e-12);
        assertEquals(1.0, lastLead.end().x(), 1.0e-12);
        assertTrue(firstLead.end().x() <= 0.22);
        assertTrue(lastLead.start().x() >= 0.66);
        var minBodyX = body.points().stream().mapToDouble(point -> point.x()).min().orElseThrow();
        var maxBodyX = Math.max(
                body.points().stream().mapToDouble(point -> point.x()).max().orElseThrow(),
                cathodeBar.start().x());
        assertTrue(maxBodyX - minBodyX >= 0.45);
    }

    @Test
    void ledExtendsDiodeBodyWithEmissionArrowGeometry() {
        var diode = library.symbol(ElectricalComponentKind.DIODE);
        var led = library.symbol(ElectricalComponentKind.LED);

        assertTrue(led.primitives().size() > diode.primitives().size());
        assertTrue(led.primitives().stream().allMatch(primitive ->
                primitive instanceof ElectricalSymbolLine || primitive instanceof ElectricalSymbolPolyline));
    }

    @Test
    void ledEmissionArrowsStayInDedicatedGutterAwayFromDiodeBody() {
        var led = library.symbol(ElectricalComponentKind.LED);

        // Primitive 0..3 are the diode itself. Every arrow primitive after that
        // must remain above the canonical body so quarter-turn rotation keeps a
        // visible lateral gap instead of merging arrows with the cathode/lead.
        for (var primitive : led.primitives().subList(4, led.primitives().size())) {
            var line = assertInstanceOf(ElectricalSymbolLine.class, primitive);
            assertTrue(line.start().y() <= 0.24);
            assertTrue(line.end().y() <= 0.24);
        }
    }

    @Test
    void spstSwitchUsesOpenLeverAndContactGeometry() {
        var symbol = library.symbol(ElectricalComponentKind.SWITCH_SPST);

        assertEquals(5, symbol.primitives().size());
        assertTrue(symbol.primitives().stream().allMatch(ElectricalSymbolLine.class::isInstance));
    }

    @Test
    void groundUsesOnlyLineGeometry() {
        var symbol = library.symbol(ElectricalComponentKind.GROUND);

        assertEquals(4, symbol.primitives().size());
        assertTrue(symbol.primitives().stream().allMatch(ElectricalSymbolLine.class::isInstance));
    }
}
