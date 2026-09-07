package dev.rgcb.scholar.diagram.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** M18G regressions for transient viewport state across immutable document snapshots. */
class DiagramViewportStoreTest {
    @Test
    void semanticReplacementAtSameBlockIndexKeepsTransientViewport() {
        var first = diagram("A", 100, 50);
        var initial = new Document(List.of(paragraph("before"), first));
        var store = new DiagramViewportStore();
        var viewport = new DiagramViewport(2.0, 70.0, 25.0);
        store.reconcile(initial);
        store.put(1, viewport);

        var replacement = first.withDefinition(new DiagramDefinition(
                "A edited", first.definition().canvas(), List.of(), List.of()));
        var edited = new Document(List.of(initial.blocks().get(0), replacement));
        store.reconcile(edited);

        assertEquals(viewport, store.viewportFor(1, replacement));
    }

    @Test
    void insertingBlockBeforeDiagramRebasesViewportToShiftedIndex() {
        var diagram = diagram("A", 100, 50);
        var before = paragraph("before");
        var initial = new Document(List.of(before, diagram));
        var store = new DiagramViewportStore();
        var viewport = new DiagramViewport(3.0, 60.0, 20.0);
        store.reconcile(initial);
        store.put(1, viewport);

        var shifted = new Document(List.of(paragraph("new"), before, diagram));
        store.reconcile(shifted);

        assertTrue(store.storedViewport(1).isEmpty());
        assertEquals(viewport, store.viewportFor(2, diagram));
    }

    @Test
    void deletingPrecedingBlockRebasesViewportBackToNewIndex() {
        var diagram = diagram("A", 100, 50);
        var keep = paragraph("keep");
        var initial = new Document(List.of(paragraph("remove"), keep, diagram));
        var store = new DiagramViewportStore();
        var viewport = new DiagramViewport(1.5, 55.0, 22.0);
        store.reconcile(initial);
        store.put(2, viewport);

        var shifted = new Document(List.of(keep, diagram));
        store.reconcile(shifted);

        assertTrue(store.storedViewport(2).isEmpty());
        assertEquals(viewport, store.viewportFor(1, diagram));
    }

    @Test
    void deletingDiagramDoesNotLeakItsViewportOntoNeighborShiftedIntoSlot() {
        var first = diagram("A", 100, 50);
        var second = diagram("B", 120, 60);
        var initial = new Document(List.of(paragraph("before"), first, second));
        var store = new DiagramViewportStore();
        store.reconcile(initial);
        store.put(1, new DiagramViewport(4.0, 80.0, 20.0));

        var deleted = new Document(List.of(initial.blocks().get(0), second));
        store.reconcile(deleted);

        assertTrue(store.storedViewport(1).isEmpty());
        assertEquals(DiagramViewport.fit(second.definition().canvas()), store.viewportFor(1, second));
    }

    @Test
    void neighboringDiagramKeepsItsOwnViewportWhenEarlierDiagramIsDeleted() {
        var first = diagram("A", 100, 50);
        var second = diagram("B", 120, 60);
        var initial = new Document(List.of(paragraph("before"), first, second));
        var store = new DiagramViewportStore();
        var firstViewport = new DiagramViewport(2.0, 70.0, 20.0);
        var secondViewport = new DiagramViewport(3.0, 90.0, 30.0);
        store.reconcile(initial);
        store.put(1, firstViewport);
        store.put(2, secondViewport);

        var deleted = new Document(List.of(initial.blocks().get(0), second));
        store.reconcile(deleted);

        assertEquals(secondViewport, store.viewportFor(1, second));
    }

    @Test
    void losslessDuplicateStartsAtFitInsteadOfSharingSourceViewport() {
        var diagram = diagram("A", 100, 50);
        var initial = new Document(List.of(paragraph("before"), diagram));
        var store = new DiagramViewportStore();
        var viewport = new DiagramViewport(2.5, 65.0, 25.0);
        store.reconcile(initial);
        store.put(1, viewport);

        // Native whole-block clipboard may reuse the same immutable DiagramBlock
        // instance. Identity therefore appears twice after paste.
        var duplicated = new Document(List.of(initial.blocks().get(0), diagram, diagram));
        store.reconcile(duplicated);

        assertEquals(viewport, store.viewportFor(1, diagram));
        assertTrue(store.storedViewport(2).isEmpty());
        assertEquals(DiagramViewport.fit(diagram.definition().canvas()), store.viewportFor(2, diagram));
    }

    private static DiagramBlock diagram(String title, double width, double height) {
        return new DiagramBlock(new DiagramDefinition(
                title, new DiagramCanvas(width, height), List.of(), List.of()));
    }

    private static Paragraph paragraph(String value) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(value, Set.of()))));
    }
}
