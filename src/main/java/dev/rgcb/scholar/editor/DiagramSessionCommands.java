package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.editor.ElectricalComponentDraft;
import dev.rgcb.scholar.electrical.editor.ElectricalDiagramEditor;
import dev.rgcb.scholar.mechanical.MechanicalConstraintKind;
import dev.rgcb.scholar.mechanical.editor.MechanicalDiagramEditor;
import java.util.Objects;
import java.util.Optional;

/** Structural electrical and mechanical commands for the active diagram selection. */
final class DiagramSessionCommands {
    private final EditorSession session;
    private final DiagramEditor diagramEditor;
    private final ElectricalDiagramEditor electricalEditor;
    private final MechanicalDiagramEditor mechanicalEditor;
    private DiagramPortTarget connectionSource;
    private DiagramElementTarget mechanicalConstraintSource;
    private MechanicalConstraintKind pendingMechanicalConstraintKind;

    DiagramSessionCommands(
            EditorSession session,
            DiagramEditor diagramEditor,
            ElectricalDiagramEditor electricalEditor,
            MechanicalDiagramEditor mechanicalEditor
    ) {
        this.session = Objects.requireNonNull(session, "session");
        this.diagramEditor = Objects.requireNonNull(diagramEditor, "diagramEditor");
        this.electricalEditor = Objects.requireNonNull(electricalEditor, "electricalEditor");
        this.mechanicalEditor = Objects.requireNonNull(mechanicalEditor, "mechanicalEditor");
    }

    void resetTransientState() {
        connectionSource = null;
        clearMechanicalConstraint();
    }

    void clearConnection() {
        connectionSource = null;
    }

    void clearMechanicalConstraint() {
        mechanicalConstraintSource = null;
        pendingMechanicalConstraintKind = null;
    }

    boolean supportsAddMechanicalUnaryConstraint(MechanicalConstraintKind kind) {
        return session.current().isDiagramEditingSelection()
                && mechanicalEditor.canApplyUnaryConstraint(
                        session.currentDiagramForCommands(),
                        session.current().diagramEditingSelection().target(), kind);
    }

    boolean addMechanicalUnaryConstraint(MechanicalConstraintKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!supportsAddMechanicalUnaryConstraint(kind)) return false;
        resetTransientState();
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(mechanicalEditor.addUnaryConstraint(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target(), kind));
    }

    boolean supportsStartMechanicalConstraint(MechanicalConstraintKind kind) {
        return session.current().isDiagramEditingSelection()
                && mechanicalEditor.canStartBinaryConstraint(
                        session.currentDiagramForCommands(),
                        session.current().diagramEditingSelection().target(), kind);
    }

    boolean startMechanicalConstraint(MechanicalConstraintKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!supportsStartMechanicalConstraint(kind)
                || !(session.current().diagramEditingSelection().target() instanceof DiagramElementTarget source)) {
            return false;
        }
        connectionSource = null;
        session.cancelDiagramElementDrag();
        mechanicalConstraintSource = source;
        pendingMechanicalConstraintKind = kind;
        return true;
    }

    boolean supportsFinishMechanicalConstraint() {
        return session.current().isDiagramEditingSelection()
                && mechanicalConstraintSource != null
                && pendingMechanicalConstraintKind != null
                && mechanicalEditor.canFinishBinaryConstraint(
                        session.currentDiagramForCommands(),
                        mechanicalConstraintSource,
                        session.current().diagramEditingSelection().target(),
                        pendingMechanicalConstraintKind);
    }

    boolean finishMechanicalConstraint() {
        if (!supportsFinishMechanicalConstraint()) return false;
        var source = mechanicalConstraintSource;
        var kind = pendingMechanicalConstraintKind;
        clearMechanicalConstraint();
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(mechanicalEditor.addBinaryConstraint(
                session.currentDiagramForCommands(), source,
                session.current().diagramEditingSelection().target(), kind));
    }

    boolean cancelMechanicalConstraint() {
        var active = hasPendingMechanicalConstraint();
        clearMechanicalConstraint();
        return active;
    }

    boolean hasPendingMechanicalConstraint() {
        return mechanicalConstraintSource != null && pendingMechanicalConstraintKind != null;
    }

    boolean supportsDeleteMechanicalConstraint() {
        return session.current().isDiagramEditingSelection()
                && mechanicalEditor.canDeleteConstraint(
                        session.currentDiagramForCommands(),
                        session.current().diagramEditingSelection().target());
    }

    boolean deleteMechanicalConstraint() {
        if (!supportsDeleteMechanicalConstraint()) return false;
        clearMechanicalConstraint();
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(mechanicalEditor.deleteConstraint(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target()));
    }

    boolean addElectricalJunction() {
        if (!session.supportsDiagramEditingAction()) return false;
        connectionSource = null;
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(electricalEditor.addJunction(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target()));
    }

    boolean supportsDeleteElectricalJunction() {
        return session.current().isDiagramEditingSelection()
                && electricalEditor.canDeleteJunction(
                        session.currentDiagramForCommands(),
                        session.current().diagramEditingSelection().target());
    }

    boolean deleteElectricalJunction() {
        if (!supportsDeleteElectricalJunction()) return false;
        connectionSource = null;
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(electricalEditor.deleteJunction(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target()));
    }

    boolean addElectricalComponent(ElectricalComponentKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!session.supportsDiagramEditingAction()) return false;
        connectionSource = null;
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(electricalEditor.addComponent(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target(), kind));
    }

    Optional<ElectricalComponent> selectedElectricalComponent() {
        if (!session.current().isDiagramEditingSelection()) return Optional.empty();
        return electricalEditor.selectedComponent(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target());
    }

    Optional<ElectricalComponentDraft> electricalComponentDraft() {
        if (!session.current().isDiagramEditingSelection()) return Optional.empty();
        return electricalEditor.draft(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target());
    }

    boolean applyElectricalComponentAnnotations(String referenceDesignator, String valueLabel) {
        Objects.requireNonNull(referenceDesignator, "referenceDesignator");
        Objects.requireNonNull(valueLabel, "valueLabel");
        if (!session.current().isDiagramEditingSelection()) return false;
        connectionSource = null;
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(electricalEditor.setAnnotations(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target(),
                referenceDesignator, valueLabel));
    }

    boolean supportsRotateElectricalComponent() {
        return session.current().isDiagramEditingSelection()
                && electricalEditor.canRotate(
                        session.currentDiagramForCommands(),
                        session.current().diagramEditingSelection().target());
    }

    boolean rotateElectricalComponentClockwise() {
        if (!supportsRotateElectricalComponent()) return false;
        connectionSource = null;
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(electricalEditor.rotateClockwise(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target()));
    }

    boolean rotateElectricalComponentCounterClockwise() {
        if (!supportsRotateElectricalComponent()) return false;
        connectionSource = null;
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(electricalEditor.rotateCounterClockwise(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target()));
    }

    boolean supportsDeleteElectricalComponent() {
        return session.current().isDiagramEditingSelection()
                && electricalEditor.canDelete(
                        session.currentDiagramForCommands(),
                        session.current().diagramEditingSelection().target());
    }

    boolean deleteElectricalComponent() {
        if (!supportsDeleteElectricalComponent()) return false;
        connectionSource = null;
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(electricalEditor.deleteComponent(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target()));
    }

    boolean supportsBeginConnection() {
        return session.current().isDiagramEditingSelection()
                && connectionSource == null
                && session.current().diagramEditingSelection().target() instanceof DiagramPortTarget;
    }

    boolean beginConnection() {
        if (!supportsBeginConnection()) return false;
        connectionSource = (DiagramPortTarget) session.current().diagramEditingSelection().target();
        session.cancelDiagramElementDrag();
        return true;
    }

    boolean connectionInProgress() {
        return connectionSource != null;
    }

    Optional<DiagramPortTarget> connectionSourceTarget() {
        return Optional.ofNullable(connectionSource);
    }

    boolean supportsCompleteConnection() {
        if (!session.current().isDiagramEditingSelection()
                || connectionSource == null
                || !(session.current().diagramEditingSelection().target() instanceof DiagramPortTarget target)) {
            return false;
        }
        return diagramEditor.canAddConnection(session.currentDiagramForCommands(), connectionSource, target);
    }

    boolean completeConnection() {
        if (!supportsCompleteConnection()) return false;
        var source = connectionSource;
        var target = (DiagramPortTarget) session.current().diagramEditingSelection().target();
        connectionSource = null;
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(
                diagramEditor.addConnection(session.currentDiagramForCommands(), source, target));
    }

    boolean cancelConnection() {
        var active = connectionSource != null;
        connectionSource = null;
        return active;
    }

    boolean supportsDeleteConnection() {
        return session.current().isDiagramEditingSelection()
                && diagramEditor.canDeleteConnection(
                        session.currentDiagramForCommands(),
                        session.current().diagramEditingSelection().target());
    }

    boolean deleteConnection() {
        if (!supportsDeleteConnection()) return false;
        connectionSource = null;
        session.cancelDiagramElementDrag();
        return session.applyDiagramCommand(diagramEditor.deleteConnection(
                session.currentDiagramForCommands(),
                session.current().diagramEditingSelection().target()));
    }
}
