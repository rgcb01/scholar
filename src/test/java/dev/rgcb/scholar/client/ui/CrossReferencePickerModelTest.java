package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.document.CrossReferenceTarget;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrossReferencePickerModelTest {
    @Test void allTargetsRemainReachableWithoutExposingStableIdsAsLabels() {
        var picker = new CrossReferencePickerModel();
        picker.setTargets(IntStream.range(0, 19).mapToObj(index -> new CrossReferenceTarget(
                CrossReferenceTargetKind.SECTION, "section-" + index, index, index + 1,
                "Section " + (index + 1), "Results " + index)).toList());

        assertEquals(3, picker.pageCount());
        assertEquals(8, picker.visibleTargets().size());
        picker.movePage(1);
        assertEquals("Section 9 - Results 8", picker.selected().orElseThrow().pickerLabel());
        picker.movePage(1);
        assertEquals(3, picker.visibleTargets().size());
        assertEquals("section-18", picker.selectVisibleRow(2).orElseThrow().targetId());
        assertEquals("section-18", picker.selected().orElseThrow().targetId());
        picker.moveSelection(5);
        assertEquals("section-18", picker.selected().orElseThrow().targetId());
        picker.moveSelection(-18);
        assertEquals("section-0", picker.selected().orElseThrow().targetId());
    }

    @Test void clearingPickerRemovesTransientSelection() {
        var picker = new CrossReferencePickerModel();
        picker.setTargets(java.util.List.of(new CrossReferenceTarget(CrossReferenceTargetKind.SECTION,
                "intro", 0, 1, "Section 1", "Introduction")));
        picker.setTargets(java.util.List.of());
        assertTrue(picker.selected().isEmpty());
        assertTrue(picker.visibleTargets().isEmpty());
    }
}
