package dev.rgcb.scholar.mechanical.editor;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramEditResult;
import dev.rgcb.scholar.editor.DiagramEditTarget;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.mechanical.MechanicalConstraint;
import dev.rgcb.scholar.mechanical.MechanicalConstraintKind;
import dev.rgcb.scholar.mechanical.MechanicalDimension;
import dev.rgcb.scholar.mechanical.MechanicalDimensionKind;
import dev.rgcb.scholar.mechanical.MechanicalOrientation;
import dev.rgcb.scholar.mechanical.MechanicalPrimitive;
import dev.rgcb.scholar.mechanical.MechanicalPrimitiveKind;
import dev.rgcb.scholar.mechanical.MechanicalSymbol;
import dev.rgcb.scholar.mechanical.MechanicalSymbolKind;
import dev.rgcb.scholar.mechanical.MechanicalAnnotation;
import dev.rgcb.scholar.mechanical.MechanicalAnnotationKind;
import dev.rgcb.scholar.mechanical.MechanicalPartReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable authoring and reconciliation operations for M19 mechanical content. */
public final class MechanicalDiagramEditor {
    public DiagramEditResult addPrimitive(
            DiagramBlock diagram,
            DiagramEditTarget currentTarget,
            MechanicalPrimitiveKind kind
    ) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(currentTarget, "currentTarget");
        Objects.requireNonNull(kind, "kind");

        var definition = diagram.definition();
        var ordinal = nextOrdinal(definition);
        var id = new DiagramElementId("mechanical-" + ordinal);
        var bounds = defaultBounds(definition, kind);
        var elements = new ArrayList<>(definition.elements());
        elements.add(new MechanicalPrimitive(id, bounds, kind));
        var target = new DiagramElementTarget(elements.size() - 1, id);
        return changed(diagram, target, new DiagramDefinition(
                definition.title(), definition.canvas(), elements, definition.connections()));
    }

    public DiagramEditResult addSymbol(DiagramBlock diagram, DiagramEditTarget currentTarget, MechanicalSymbolKind kind) {
        Objects.requireNonNull(diagram, "diagram"); Objects.requireNonNull(currentTarget, "currentTarget"); Objects.requireNonNull(kind, "kind");
        var definition=diagram.definition(); var ordinal=nextSymbolOrdinal(definition); var id=new DiagramElementId("mechanical-symbol-"+ordinal);
        var elements=new ArrayList<>(definition.elements()); elements.add(new MechanicalSymbol(id, defaultSymbolBounds(definition, kind), kind));
        var target=new DiagramElementTarget(elements.size()-1,id);
        return changed(diagram,target,new DiagramDefinition(definition.title(),definition.canvas(),elements,definition.connections()));
    }

    public boolean canAddPartReference(DiagramBlock diagram, DiagramEditTarget target) { return referenceableTarget(diagram,target).isPresent(); }
    public DiagramEditResult addPartReference(DiagramBlock diagram, DiagramEditTarget target) {
        var selected=referenceableTarget(diagram,target); if(selected.isEmpty())return new DiagramEditResult(diagram,target,false);
        var d=diagram.definition();var item=nextPartItemNumber(d);var id=new DiagramElementId("part-reference-"+item);var targetElement=selected.orElseThrow();
        var partName=defaultPartName(targetElement);var b=targetElement.bounds();var bw=12.0;var bh=12.0;var x=Math.min(Math.max(0,b.x()+b.width()+10),Math.max(0,d.canvas().width()-bw));var y=Math.min(Math.max(0,b.y()-8),Math.max(0,d.canvas().height()-bh));
        var elements=new ArrayList<>(d.elements());elements.add(new MechanicalPartReference(id,new DiagramBounds(x,y,bw,bh),targetElement.id(),item,partName,1,partName));
        return changed(diagram,new DiagramElementTarget(elements.size()-1,id),new DiagramDefinition(d.title(),d.canvas(),elements,d.connections()));
    }
    public Optional<MechanicalPartReference> selectedPartReference(DiagramBlock diagram,DiagramEditTarget target){if(!(target instanceof DiagramElementTarget t)||t.elementIndex()<0||t.elementIndex()>=diagram.definition().elements().size())return Optional.empty();var e=diagram.definition().elements().get(t.elementIndex());return e.id().equals(t.elementId())&&e instanceof MechanicalPartReference ref?Optional.of(ref):Optional.empty();}
    public boolean canDeletePartReference(DiagramBlock d,DiagramEditTarget t){return selectedPartReference(d,t).isPresent();}
    public DiagramEditResult deletePartReference(DiagramBlock diagram,DiagramEditTarget target){if(!canDeletePartReference(diagram,target))return new DiagramEditResult(diagram,target,false);var t=(DiagramElementTarget)target;var d=diagram.definition();var elements=new ArrayList<>(d.elements());elements.remove(t.elementIndex());return changed(diagram,currentTargetAfterDeletion(elements,t.elementIndex()),new DiagramDefinition(d.title(),d.canvas(),elements,d.connections()));}
    public DiagramEditResult setPartReferenceName(DiagramBlock diagram,DiagramEditTarget target,String name){var ref=selectedPartReference(diagram,target);if(ref.isEmpty())return new DiagramEditResult(diagram,target,false);var t=(DiagramElementTarget)target;var d=diagram.definition();var elements=new ArrayList<>(d.elements());elements.set(t.elementIndex(),ref.orElseThrow().withPartName(name));return changed(diagram,target,new DiagramDefinition(d.title(),d.canvas(),elements,d.connections()));}

    public DiagramEditResult addAnnotation(DiagramBlock diagram, DiagramEditTarget currentTarget, MechanicalAnnotationKind kind) {
        Objects.requireNonNull(diagram); Objects.requireNonNull(currentTarget); Objects.requireNonNull(kind);
        var d=diagram.definition(); var id=new DiagramElementId("mechanical-annotation-"+nextAnnotationOrdinal(d));
        var text=switch(kind){case PART_LABEL->"PART A";case NOTE->"NOTE";case LEADER->"CALLOUT";};
        var elements=new ArrayList<>(d.elements()); elements.add(new MechanicalAnnotation(id,defaultAnnotationBounds(d,kind),kind,text));
        return changed(diagram,new DiagramElementTarget(elements.size()-1,id),new DiagramDefinition(d.title(),d.canvas(),elements,d.connections()));
    }
    public Optional<MechanicalAnnotation> selectedAnnotation(DiagramBlock diagram, DiagramEditTarget target) {
        if(!(target instanceof DiagramElementTarget t)||t.elementIndex()<0||t.elementIndex()>=diagram.definition().elements().size())return Optional.empty();
        var e=diagram.definition().elements().get(t.elementIndex()); return e.id().equals(t.elementId())&&e instanceof MechanicalAnnotation a?Optional.of(a):Optional.empty();
    }
    public DiagramEditResult setAnnotationText(DiagramBlock diagram, DiagramEditTarget target, String text) {
        var a=selectedAnnotation(diagram,target); if(a.isEmpty())return new DiagramEditResult(diagram,target,false);
        var t=(DiagramElementTarget)target;var d=diagram.definition();var elements=new ArrayList<>(d.elements());elements.set(t.elementIndex(),a.orElseThrow().withText(text));
        return changed(diagram,target,new DiagramDefinition(d.title(),d.canvas(),elements,d.connections()));
    }
    public boolean canDeleteAnnotation(DiagramBlock d,DiagramEditTarget t){return selectedAnnotation(d,t).isPresent();}
    public DiagramEditResult deleteAnnotation(DiagramBlock diagram,DiagramEditTarget target){
        if(!canDeleteAnnotation(diagram,target))return new DiagramEditResult(diagram,target,false);
        var t=(DiagramElementTarget)target;var d=diagram.definition();var elements=new ArrayList<>(d.elements());elements.remove(t.elementIndex());
        return changed(diagram,currentTargetAfterDeletion(elements,t.elementIndex()),new DiagramDefinition(d.title(),d.canvas(),elements,d.connections()));
    }

    public DiagramEditResult addDimension(
            DiagramBlock diagram,
            DiagramEditTarget currentTarget,
            MechanicalDimensionKind kind
    ) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(currentTarget, "currentTarget");
        Objects.requireNonNull(kind, "kind");

        var definition = diagram.definition();
        var ordinal = nextDimensionOrdinal(definition);
        var id = new DiagramElementId("dimension-" + ordinal);
        var bounds = defaultDimensionBounds(definition, kind);
        var elements = new ArrayList<>(definition.elements());
        elements.add(new MechanicalDimension(id, bounds, kind));
        var target = new DiagramElementTarget(elements.size() - 1, id);
        return changed(diagram, target, new DiagramDefinition(
                definition.title(), definition.canvas(), elements, definition.connections()));
    }

    public Optional<MechanicalSymbol> selectedSymbol(DiagramBlock diagram, DiagramEditTarget target) {
        Objects.requireNonNull(diagram); Objects.requireNonNull(target);
        if (!(target instanceof DiagramElementTarget t) || t.elementIndex()<0 || t.elementIndex()>=diagram.definition().elements().size()) return Optional.empty();
        var e=diagram.definition().elements().get(t.elementIndex());
        return e.id().equals(t.elementId()) && e instanceof MechanicalSymbol symbol ? Optional.of(symbol) : Optional.empty();
    }

    public boolean canDeleteSymbol(DiagramBlock diagram, DiagramEditTarget target) { return selectedSymbol(diagram,target).isPresent(); }
    public DiagramEditResult deleteSymbol(DiagramBlock diagram, DiagramEditTarget target) {
        if(!canDeleteSymbol(diagram,target)) return new DiagramEditResult(diagram,target,false);
        var selected=(DiagramElementTarget)target; var d=diagram.definition(); var removedId=selected.elementId(); var elements=new ArrayList<DiagramElement>();
        for(var element:d.elements()){if(element.id().equals(removedId))continue;if(element instanceof MechanicalPartReference reference&&reference.targetId().equals(removedId))continue;elements.add(element);}
        return changed(diagram,currentTargetAfterDeletion(elements,Math.min(selected.elementIndex(),elements.size())),new DiagramDefinition(d.title(),d.canvas(),elements,d.connections()));
    }

    public Optional<MechanicalPrimitive> selectedPrimitive(DiagramBlock diagram, DiagramEditTarget target) {
        Objects.requireNonNull(diagram, "diagram");
        Objects.requireNonNull(target, "target");
        if (!(target instanceof DiagramElementTarget elementTarget)) return Optional.empty();
        if (elementTarget.elementIndex() < 0 || elementTarget.elementIndex() >= diagram.definition().elements().size()) {
            return Optional.empty();
        }
        var element = diagram.definition().elements().get(elementTarget.elementIndex());
        if (!element.id().equals(elementTarget.elementId()) || !(element instanceof MechanicalPrimitive primitive)) {
            return Optional.empty();
        }
        return Optional.of(primitive);
    }

    public boolean canApplyUnaryConstraint(
            DiagramBlock diagram,
            DiagramEditTarget target,
            MechanicalConstraintKind kind
    ) {
        if (kind == null || kind.binary()) return false;
        return selectedPrimitive(diagram, target).filter(MechanicalPrimitive::directional).isPresent();
    }

    public DiagramEditResult addUnaryConstraint(
            DiagramBlock diagram,
            DiagramEditTarget target,
            MechanicalConstraintKind kind
    ) {
        if (!canApplyUnaryConstraint(diagram, target, kind)) {
            return new DiagramEditResult(diagram, target, false);
        }
        var selected = (DiagramElementTarget) target;
        if (hasConstraint(diagram.definition(), kind, selected.elementId(), Optional.empty())) {
            return new DiagramEditResult(diagram, target, false);
        }
        var definition = diagram.definition();
        var elements = new ArrayList<>(definition.elements());
        var id = new DiagramElementId("constraint-" + nextConstraintOrdinal(definition));
        elements.add(MechanicalConstraint.unary(
                id, markerBounds(definition, selected.elementId(), Optional.empty()), kind, selected.elementId()));
        var updated = new DiagramDefinition(definition.title(), definition.canvas(), elements, definition.connections());
        updated = reconcileDefinition(updated);
        var constraintIndex = indexOf(updated.elements(), id);
        return changed(diagram, new DiagramElementTarget(constraintIndex, id), updated);
    }

    public boolean canStartBinaryConstraint(
            DiagramBlock diagram,
            DiagramEditTarget target,
            MechanicalConstraintKind kind
    ) {
        if (kind == null || !kind.binary()) return false;
        var primitive = selectedPrimitive(diagram, target);
        if (primitive.isEmpty()) return false;
        return switch (kind) {
            case PARALLEL, PERPENDICULAR -> primitive.get().directional();
            case CONCENTRIC -> primitive.get().radial();
            case COINCIDENT -> true;
            default -> false;
        };
    }

    public boolean canFinishBinaryConstraint(
            DiagramBlock diagram,
            DiagramElementTarget source,
            DiagramEditTarget target,
            MechanicalConstraintKind kind
    ) {
        if (!canStartBinaryConstraint(diagram, source, kind)) return false;
        var peer = selectedPrimitive(diagram, target);
        if (peer.isEmpty() || !(target instanceof DiagramElementTarget peerTarget)) return false;
        if (source.elementId().equals(peerTarget.elementId())) return false;
        return switch (kind) {
            case PARALLEL, PERPENDICULAR -> peer.get().directional();
            case CONCENTRIC -> peer.get().radial();
            case COINCIDENT -> true;
            default -> false;
        };
    }

    public DiagramEditResult addBinaryConstraint(
            DiagramBlock diagram,
            DiagramElementTarget source,
            DiagramEditTarget target,
            MechanicalConstraintKind kind
    ) {
        if (!canFinishBinaryConstraint(diagram, source, target, kind)) {
            return new DiagramEditResult(diagram, target, false);
        }
        var peer = (DiagramElementTarget) target;
        if (hasConstraint(diagram.definition(), kind, source.elementId(), Optional.of(peer.elementId()))
                || hasConstraint(diagram.definition(), kind, peer.elementId(), Optional.of(source.elementId()))) {
            return new DiagramEditResult(diagram, target, false);
        }

        var definition = diagram.definition();
        var elements = new ArrayList<>(definition.elements());
        var id = new DiagramElementId("constraint-" + nextConstraintOrdinal(definition));
        elements.add(MechanicalConstraint.binary(
                id,
                markerBounds(definition, source.elementId(), Optional.of(peer.elementId())),
                kind,
                source.elementId(),
                peer.elementId()));
        var updated = new DiagramDefinition(definition.title(), definition.canvas(), elements, definition.connections());
        updated = reconcileDefinition(updated);
        var constraintIndex = indexOf(updated.elements(), id);
        return changed(diagram, new DiagramElementTarget(constraintIndex, id), updated);
    }

    public DiagramBlock reconcileConstraints(DiagramBlock diagram) {
        Objects.requireNonNull(diagram, "diagram");
        var definition = reconcileDefinition(diagram.definition());
        return definition.equals(diagram.definition()) ? diagram : diagram.withDefinition(definition);
    }

    public boolean canDeleteConstraint(DiagramBlock diagram, DiagramEditTarget target) {
        if (!(target instanceof DiagramElementTarget elementTarget)) return false;
        if (elementTarget.elementIndex() < 0 || elementTarget.elementIndex() >= diagram.definition().elements().size()) {
            return false;
        }
        var element = diagram.definition().elements().get(elementTarget.elementIndex());
        return element.id().equals(elementTarget.elementId()) && element instanceof MechanicalConstraint;
    }

    public DiagramEditResult deleteConstraint(DiagramBlock diagram, DiagramEditTarget target) {
        if (!canDeleteConstraint(diagram, target)) return new DiagramEditResult(diagram, target, false);
        var selected = (DiagramElementTarget) target;
        var definition = diagram.definition();
        var elements = new ArrayList<>(definition.elements());
        elements.remove(selected.elementIndex());
        var next = currentTargetAfterDeletion(elements, selected.elementIndex());
        return changed(diagram, next, new DiagramDefinition(
                definition.title(), definition.canvas(), elements, definition.connections()));
    }

    public boolean canDeleteDimension(DiagramBlock diagram, DiagramEditTarget target) {
        if (!(target instanceof DiagramElementTarget elementTarget)) return false;
        if (elementTarget.elementIndex() < 0 || elementTarget.elementIndex() >= diagram.definition().elements().size()) return false;
        var element = diagram.definition().elements().get(elementTarget.elementIndex());
        return element.id().equals(elementTarget.elementId()) && element instanceof MechanicalDimension;
    }

    public DiagramEditResult deleteDimension(DiagramBlock diagram, DiagramEditTarget target) {
        if (!canDeleteDimension(diagram, target)) return new DiagramEditResult(diagram, target, false);
        var selected = (DiagramElementTarget) target;
        var definition = diagram.definition();
        var elements = new ArrayList<>(definition.elements());
        elements.remove(selected.elementIndex());
        DiagramEditTarget next = currentTargetAfterDeletion(elements, selected.elementIndex());
        return changed(diagram, next, new DiagramDefinition(
                definition.title(), definition.canvas(), elements, definition.connections()));
    }

    public boolean canDelete(DiagramBlock diagram, DiagramEditTarget target) {
        return selectedPrimitive(diagram, target).isPresent();
    }

    public DiagramEditResult deletePrimitive(DiagramBlock diagram, DiagramEditTarget target) {
        if (!canDelete(diagram, target)) {
            return new DiagramEditResult(diagram, target, false);
        }
        var selected = (DiagramElementTarget) target;
        var definition = diagram.definition();
        var removedId = selected.elementId();
        var elements = new ArrayList<DiagramElement>();
        for (var element : definition.elements()) {
            if (element.id().equals(removedId)) continue;
            if (element instanceof MechanicalConstraint constraint && constraint.references(removedId)) continue;
            if (element instanceof MechanicalPartReference reference && reference.targetId().equals(removedId)) continue;
            elements.add(element);
        }
        DiagramEditTarget next = currentTargetAfterDeletion(elements, Math.min(selected.elementIndex(), elements.size()));
        return changed(diagram, next, new DiagramDefinition(
                definition.title(), definition.canvas(), elements, definition.connections()));
    }

    private static DiagramDefinition reconcileDefinition(DiagramDefinition definition) {
        var elements = new ArrayList<>(definition.elements());
        // Two deterministic passes are enough for the simple source->peer chains
        // authored in M19C while avoiding a general-purpose solver in this milestone.
        for (var pass = 0; pass < 2; pass++) {
            for (var element : List.copyOf(elements)) {
                if (element instanceof MechanicalConstraint constraint) {
                    applyConstraint(definition, elements, constraint);
                }
            }
        }
        return new DiagramDefinition(definition.title(), definition.canvas(), elements, definition.connections());
    }

    private static void applyConstraint(
            DiagramDefinition definition,
            ArrayList<DiagramElement> elements,
            MechanicalConstraint constraint
    ) {
        var subjectIndex = indexOf(elements, constraint.subjectId());
        if (subjectIndex < 0 || !(elements.get(subjectIndex) instanceof MechanicalPrimitive subject)) return;

        switch (constraint.kind()) {
            case HORIZONTAL -> {
                if (subject.directional() && subject.orientation() != MechanicalOrientation.DEG_0) {
                    elements.set(subjectIndex, orientPrimitive(definition, subject, MechanicalOrientation.DEG_0));
                }
            }
            case VERTICAL -> {
                if (subject.directional() && subject.orientation() != MechanicalOrientation.DEG_90) {
                    elements.set(subjectIndex, orientPrimitive(definition, subject, MechanicalOrientation.DEG_90));
                }
            }
            case PARALLEL, PERPENDICULAR, COINCIDENT, CONCENTRIC -> {
                var peerId = constraint.peerId().orElse(null);
                var peerIndex = indexOf(elements, peerId);
                if (peerIndex < 0 || !(elements.get(peerIndex) instanceof MechanicalPrimitive peer)) return;
                if (constraint.kind() == MechanicalConstraintKind.PARALLEL
                        && subject.directional() && peer.directional()) {
                    elements.set(peerIndex, orientPrimitive(definition, peer, subject.orientation()));
                } else if (constraint.kind() == MechanicalConstraintKind.PERPENDICULAR
                        && subject.directional() && peer.directional()) {
                    elements.set(peerIndex, orientPrimitive(definition, peer, subject.orientation().perpendicular()));
                } else if (constraint.kind() == MechanicalConstraintKind.COINCIDENT) {
                    elements.set(peerIndex, moveCenterTo(definition, peer, centerX(subject), centerY(subject)));
                } else if (constraint.kind() == MechanicalConstraintKind.CONCENTRIC
                        && subject.radial() && peer.radial()) {
                    elements.set(peerIndex, moveCenterTo(definition, peer, centerX(subject), centerY(subject)));
                }
            }
        }
    }

    private static MechanicalPrimitive orientPrimitive(
            DiagramDefinition definition,
            MechanicalPrimitive primitive,
            MechanicalOrientation orientation
    ) {
        if (primitive.orientation() == orientation) return primitive;
        var old = primitive.bounds();
        var width = old.height();
        var height = old.width();
        var centerX = old.x() + old.width() / 2.0;
        var centerY = old.y() + old.height() / 2.0;
        var x = clamp(centerX - width / 2.0, 0.0, Math.max(0.0, definition.canvas().width() - width));
        var y = clamp(centerY - height / 2.0, 0.0, Math.max(0.0, definition.canvas().height() - height));
        return new MechanicalPrimitive(
                primitive.id(),
                new DiagramBounds(x, y, width, height),
                primitive.kind(),
                orientation);
    }

    private static MechanicalPrimitive moveCenterTo(
            DiagramDefinition definition,
            MechanicalPrimitive primitive,
            double centerX,
            double centerY
    ) {
        var b = primitive.bounds();
        var x = clamp(centerX - b.width() / 2.0, 0.0, Math.max(0.0, definition.canvas().width() - b.width()));
        var y = clamp(centerY - b.height() / 2.0, 0.0, Math.max(0.0, definition.canvas().height() - b.height()));
        return primitive.withBounds(new DiagramBounds(x, y, b.width(), b.height()));
    }

    private static boolean hasConstraint(
            DiagramDefinition definition,
            MechanicalConstraintKind kind,
            DiagramElementId subject,
            Optional<DiagramElementId> peer
    ) {
        return definition.elements().stream().anyMatch(element ->
                element instanceof MechanicalConstraint constraint
                        && constraint.kind() == kind
                        && constraint.subjectId().equals(subject)
                        && constraint.peerId().equals(peer));
    }

    private static DiagramBounds markerBounds(
            DiagramDefinition definition,
            DiagramElementId subjectId,
            Optional<DiagramElementId> peerId
    ) {
        var subject = definition.elements().stream()
                .filter(element -> element.id().equals(subjectId))
                .findFirst().orElseThrow();
        var x = centerX(subject);
        var y = centerY(subject);
        if (peerId.isPresent()) {
            var peer = definition.elements().stream()
                    .filter(element -> element.id().equals(peerId.get()))
                    .findFirst().orElseThrow();
            x = (x + centerX(peer)) / 2.0;
            y = (y + centerY(peer)) / 2.0;
        }
        var size = Math.min(6.0, Math.min(definition.canvas().width(), definition.canvas().height()));
        return new DiagramBounds(
                clamp(x - size / 2.0, 0.0, Math.max(0.0, definition.canvas().width() - size)),
                clamp(y - size / 2.0, 0.0, Math.max(0.0, definition.canvas().height() - size)),
                Math.max(1.0, size),
                Math.max(1.0, size));
    }

    private static double centerX(DiagramElement element) {
        return element.bounds().x() + element.bounds().width() / 2.0;
    }

    private static double centerY(DiagramElement element) {
        return element.bounds().y() + element.bounds().height() / 2.0;
    }

    private static DiagramBounds defaultDimensionBounds(DiagramDefinition definition, MechanicalDimensionKind kind) {
        var canvas = definition.canvas();
        var w = switch (kind) {
            case HORIZONTAL -> Math.min(42.0, canvas.width() * 0.38);
            case VERTICAL -> Math.min(12.0, canvas.width() * 0.12);
            case ALIGNED, ANGLE -> Math.min(36.0, canvas.width() * 0.34);
            case RADIUS, DIAMETER -> Math.min(28.0, canvas.width() * 0.26);
        };
        var h = switch (kind) {
            case HORIZONTAL -> Math.min(14.0, canvas.height() * 0.18);
            case VERTICAL -> Math.min(38.0, canvas.height() * 0.42);
            case ALIGNED -> Math.min(24.0, canvas.height() * 0.30);
            case RADIUS, DIAMETER -> Math.min(28.0, canvas.height() * 0.34);
            case ANGLE -> Math.min(28.0, canvas.height() * 0.34);
        };
        w = Math.max(4.0, Math.min(w, canvas.width()));
        h = Math.max(4.0, Math.min(h, canvas.height()));
        return placeWithoutOverlap(definition, w, h);
    }

    private static DiagramBounds defaultBounds(DiagramDefinition definition, MechanicalPrimitiveKind kind) {
        var canvas = definition.canvas();
        var w = switch (kind) {
            case LINE, CENTERLINE, ARROW -> Math.min(34.0, canvas.width() * 0.32);
            case RECTANGLE -> Math.min(28.0, canvas.width() * 0.28);
            case CIRCLE, ARC -> Math.min(22.0, canvas.width() * 0.22);
            case REFERENCE_POINT -> Math.min(8.0, canvas.width() * 0.08);
        };
        var h = switch (kind) {
            case LINE, CENTERLINE, ARROW -> Math.min(8.0, canvas.height() * 0.12);
            case RECTANGLE -> Math.min(20.0, canvas.height() * 0.28);
            case CIRCLE, ARC -> Math.min(22.0, canvas.height() * 0.28);
            case REFERENCE_POINT -> Math.min(8.0, canvas.height() * 0.10);
        };
        w = Math.max(2.0, Math.min(w, canvas.width()));
        h = Math.max(2.0, Math.min(h, canvas.height()));
        return placeWithoutOverlap(definition, w, h);
    }

    private static Optional<DiagramElement> referenceableTarget(DiagramBlock diagram,DiagramEditTarget target){if(!(target instanceof DiagramElementTarget t)||t.elementIndex()<0||t.elementIndex()>=diagram.definition().elements().size())return Optional.empty();var e=diagram.definition().elements().get(t.elementIndex());if(!e.id().equals(t.elementId())||e instanceof MechanicalConstraint||e instanceof MechanicalDimension||e instanceof MechanicalAnnotation||e instanceof MechanicalPartReference)return Optional.empty();return (e instanceof MechanicalPrimitive||e instanceof MechanicalSymbol)?Optional.of(e):Optional.empty();}
    private static int nextPartItemNumber(DiagramDefinition d){return d.elements().stream().filter(MechanicalPartReference.class::isInstance).map(MechanicalPartReference.class::cast).mapToInt(MechanicalPartReference::itemNumber).max().orElse(0)+1;}
    private static String defaultPartName(DiagramElement e){if(e instanceof MechanicalSymbol s)return s.kind().name().replace('_',' ');if(e instanceof MechanicalPrimitive p)return p.kind().name().replace('_',' ');return "PART";}

    private static int nextAnnotationOrdinal(DiagramDefinition definition){var n=1;while(containsId(definition.elements(),new DiagramElementId("mechanical-annotation-"+n)))n++;return n;}
    private static DiagramBounds defaultAnnotationBounds(DiagramDefinition d,MechanicalAnnotationKind k){
        double w=switch(k){case PART_LABEL->30;case NOTE->38;case LEADER->42;},h=switch(k){case PART_LABEL->12;case NOTE->14;case LEADER->18;};
        w=Math.min(w,d.canvas().width());h=Math.min(h,d.canvas().height());return placeWithoutOverlap(d,w,h);
    }

    private static int nextSymbolOrdinal(DiagramDefinition definition) {
        var ordinal=1; while(containsId(definition.elements(),new DiagramElementId("mechanical-symbol-"+ordinal))) ordinal++; return ordinal;
    }

    private static DiagramBounds defaultSymbolBounds(DiagramDefinition definition, MechanicalSymbolKind kind) {
        double w=switch(kind){ case SHAFT -> 38; case SPRING, PISTON -> 34; case GEAR, BEARING -> 28; case BOLT -> 30; };
        double h=switch(kind){ case SHAFT -> 10; case SPRING -> 16; case PISTON -> 20; case GEAR, BEARING -> 28; case BOLT -> 14; };
        w = Math.min(w, definition.canvas().width());
        h = Math.min(h, definition.canvas().height());
        return placeWithoutOverlap(definition, w, h);
    }

    private static DiagramBounds placeWithoutOverlap(DiagramDefinition definition, double width, double height) {
        var canvas = definition.canvas();
        var maxX = Math.max(0.0, canvas.width() - width);
        var maxY = Math.max(0.0, canvas.height() - height);
        var centered = new DiagramBounds(maxX / 2.0, maxY / 2.0, width, height);
        if (definition.elements().stream().noneMatch(element -> overlaps(centered, element.bounds()))) {
            return centered;
        }
        var stepX = Math.max(2.0, width + 4.0);
        var stepY = Math.max(2.0, height + 4.0);
        for (double y = 2.0; y <= maxY + 1.0e-9; y += stepY) {
            for (double x = 2.0; x <= maxX + 1.0e-9; x += stepX) {
                var candidate = new DiagramBounds(Math.min(x, maxX), Math.min(y, maxY), width, height);
                if (definition.elements().stream().noneMatch(element -> overlaps(candidate, element.bounds()))) {
                    return candidate;
                }
            }
        }
        return centered;
    }

    private static boolean overlaps(DiagramBounds left, DiagramBounds right) {
        return left.x() < right.right() + 2.0 && left.right() + 2.0 > right.x()
                && left.y() < right.bottom() + 2.0 && left.bottom() + 2.0 > right.y();
    }

    private static int nextOrdinal(DiagramDefinition definition) {
        var ordinal = 1;
        while (containsId(definition.elements(), new DiagramElementId("mechanical-" + ordinal))) {
            ordinal++;
        }
        return ordinal;
    }

    private static int nextDimensionOrdinal(DiagramDefinition definition) {
        var ordinal = 1;
        while (containsId(definition.elements(), new DiagramElementId("dimension-" + ordinal))) {
            ordinal++;
        }
        return ordinal;
    }

    private static int nextConstraintOrdinal(DiagramDefinition definition) {
        var ordinal = 1;
        while (containsId(definition.elements(), new DiagramElementId("constraint-" + ordinal))) {
            ordinal++;
        }
        return ordinal;
    }

    private static boolean containsId(List<DiagramElement> elements, DiagramElementId id) {
        for (var element : elements) {
            if (element.id().equals(id)) return true;
        }
        return false;
    }

    private static int indexOf(List<DiagramElement> elements, DiagramElementId id) {
        if (id == null) return -1;
        for (var index = 0; index < elements.size(); index++) {
            if (elements.get(index).id().equals(id)) return index;
        }
        return -1;
    }

    private static DiagramEditTarget currentTargetAfterDeletion(List<DiagramElement> elements, int deletedIndex) {
        if (elements.isEmpty()) {
            return new dev.rgcb.scholar.editor.DiagramPropertyTarget(dev.rgcb.scholar.editor.DiagramProperty.TITLE);
        }
        var index = Math.min(Math.max(0, deletedIndex), elements.size() - 1);
        return new DiagramElementTarget(index, elements.get(index).id());
    }

    private static DiagramEditResult changed(
            DiagramBlock oldBlock,
            DiagramEditTarget target,
            DiagramDefinition definition
    ) {
        return new DiagramEditResult(
                new DiagramBlock(definition, oldBlock.workspaceAspectRatio()),
                target,
                true);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
