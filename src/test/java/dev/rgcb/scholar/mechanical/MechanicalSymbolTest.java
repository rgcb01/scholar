package dev.rgcb.scholar.mechanical;
import static org.junit.jupiter.api.Assertions.*;
import dev.rgcb.scholar.diagram.*;
import dev.rgcb.scholar.diagram.layout.*;
import dev.rgcb.scholar.diagram.clipboard.DiagramPlainTextSerializer;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.editor.*;
import dev.rgcb.scholar.layout.*;
import dev.rgcb.scholar.mechanical.editor.MechanicalDiagramEditor;
import java.util.*;
import org.junit.jupiter.api.Test;
class MechanicalSymbolTest {
 private final MechanicalDiagramEditor editor=new MechanicalDiagramEditor();
 @Test void allSixSymbolsCanBeAuthoredSemantically(){ var b=block(); for(var k:MechanicalSymbolKind.values()){ var r=editor.addSymbol(b,new DiagramElementTarget(0,new DiagramElementId("anchor")),k); assertTrue(r.changed()); assertInstanceOf(MechanicalSymbol.class,r.diagram().definition().elements().get(1)); } }
 @Test void symbolLayoutIsDerivedAndHitSized(){ var r=editor.addSymbol(block(),new DiagramElementTarget(0,new DiagramElementId("anchor")),MechanicalSymbolKind.GEAR); var l=new DiagramLayoutEngine().layout(r.diagram(),0,0,0,240,new M()); assertEquals(1,l.mechanicalSymbols().size()); assertEquals(MechanicalSymbolKind.GEAR,l.mechanicalSymbols().get(0).kind()); }
 @Test void symbolDeletesAsOneElement(){ var r=editor.addSymbol(block(),new DiagramElementTarget(0,new DiagramElementId("anchor")),MechanicalSymbolKind.BEARING); assertTrue(editor.deleteSymbol(r.diagram(),r.target()).changed()); }
 @Test void plainTextNamesSemanticSymbol(){ var r=editor.addSymbol(block(),new DiagramElementTarget(0,new DiagramElementId("anchor")),MechanicalSymbolKind.SPRING); assertTrue(new DiagramPlainTextSerializer().serialize(r.diagram()).contains("Mechanical Symbol: SPRING")); }
 private static DiagramBlock block(){ return new DiagramBlock(new DiagramDefinition("s",new DiagramCanvas(120,80),List.of(new MechanicalPrimitive(new DiagramElementId("anchor"),new DiagramBounds(2,2,20,8),MechanicalPrimitiveKind.LINE)),List.of())); }
 private static final class M implements TextMeasurer { public int measureWidth(String t,TextStyle s){return t.length()*6;} public int lineHeight(TextStyle s){return 9;} }
}
