package dev.rgcb.scholar.mechanical;
import static org.junit.jupiter.api.Assertions.*;
import dev.rgcb.scholar.diagram.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.editor.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class MechanicalPartReferenceTest {
 @Test void balloonReferencesSelectedPartByStableId(){
  var shaft=new MechanicalSymbol(new DiagramElementId("shaft"),new DiagramBounds(20,20,40,10),MechanicalSymbolKind.SHAFT);
  var block=new DiagramBlock(new DiagramDefinition("a",new DiagramCanvas(120,80),List.of(shaft),List.of()));
  var result=new dev.rgcb.scholar.mechanical.editor.MechanicalDiagramEditor().addPartReference(block,new DiagramElementTarget(0,shaft.id()));
  assertTrue(result.changed());var ref=(MechanicalPartReference)result.diagram().definition().elements().get(1);assertEquals(shaft.id(),ref.targetId());assertEquals(1,ref.itemNumber());assertEquals("SHAFT",ref.partName());
 }
 @Test void generatedBomUsesSemanticReferencesAndIsOneUndoableEdit(){
  var shaft=new MechanicalSymbol(new DiagramElementId("shaft"),new DiagramBounds(20,20,40,10),MechanicalSymbolKind.SHAFT);
  var ref=new MechanicalPartReference(new DiagramElementId("ref"),new DiagramBounds(70,10,12,12),shaft.id(),1,"SHAFT",2,"Drive shaft");
  var diagram=new DiagramBlock(new DiagramDefinition("a",new DiagramCanvas(120,80),List.of(shaft,ref),List.of()));
  var doc=new Document(List.of(paragraph("before"),diagram));var session=new EditorSession(doc,0);session.enterDiagramEditing(1,new DiagramElementTarget(1,ref.id()));
  assertTrue(session.generateMechanicalBom());assertTrue(session.current().document().blocks().get(2) instanceof TableBlock);var table=(TableBlock)session.current().document().blocks().get(2);assertEquals(2,table.rows().size());assertEquals(4,table.columnCount());assertTrue(session.undo());assertEquals(doc,session.current().document());
 }
 private static Paragraph paragraph(String t){return new Paragraph(new InlineContent(List.of((InlineNode)new Text(t,Set.of()))));}
}
