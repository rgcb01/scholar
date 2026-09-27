package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EditorDialogCoordinatorTest {
    @Test
    void openingDialogReplacesCurrentModalOwner() {
        var coordinator = new EditorDialogCoordinator();

        coordinator.open(EditorDialogCoordinator.Dialog.SEMANTIC_TOKEN);
        coordinator.open(EditorDialogCoordinator.Dialog.CROSS_REFERENCE);

        assertEquals(EditorDialogCoordinator.Dialog.CROSS_REFERENCE, coordinator.active().orElseThrow());
        assertFalse(coordinator.isOpen(EditorDialogCoordinator.Dialog.SEMANTIC_TOKEN));
        assertTrue(coordinator.isOpen(EditorDialogCoordinator.Dialog.CROSS_REFERENCE));
    }

    @Test
    void onlyTheActiveDialogCanCloseModalOwnership() {
        var coordinator = new EditorDialogCoordinator();
        coordinator.open(EditorDialogCoordinator.Dialog.DIAGRAM_CANVAS);

        assertFalse(coordinator.close(EditorDialogCoordinator.Dialog.PLOT_VALUE));
        assertTrue(coordinator.anyOpen());
        assertTrue(coordinator.close(EditorDialogCoordinator.Dialog.DIAGRAM_CANVAS));
        assertFalse(coordinator.anyOpen());
    }

    @Test
    void closeAllClearsCurrentDialog() {
        var coordinator = new EditorDialogCoordinator();
        coordinator.open(EditorDialogCoordinator.Dialog.ELECTRICAL_COMPONENT);

        coordinator.closeAll();

        assertTrue(coordinator.active().isEmpty());
    }
}
