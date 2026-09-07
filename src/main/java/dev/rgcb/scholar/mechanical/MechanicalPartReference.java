package dev.rgcb.scholar.mechanical;
import dev.rgcb.scholar.diagram.*;
import java.util.*;
/** M19F assembly item reference. The target ID is semantic; balloon/leader geometry is derived. */
public record MechanicalPartReference(DiagramElementId id, DiagramBounds bounds, DiagramElementId targetId, int itemNumber, String partName, int quantity, String description) implements DiagramElement {
 public MechanicalPartReference { Objects.requireNonNull(id);Objects.requireNonNull(bounds);Objects.requireNonNull(targetId);Objects.requireNonNull(partName);Objects.requireNonNull(description);if(itemNumber<1)throw new IllegalArgumentException("itemNumber");if(quantity<1)throw new IllegalArgumentException("quantity"); }
 @Override public MechanicalPartReference withBounds(DiagramBounds b){return new MechanicalPartReference(id,b,targetId,itemNumber,partName,quantity,description);}
 public MechanicalPartReference withPartName(String n){return new MechanicalPartReference(id,bounds,targetId,itemNumber,n,quantity,description);}
 @Override public List<DiagramPort> ports(){return List.of();}
}
