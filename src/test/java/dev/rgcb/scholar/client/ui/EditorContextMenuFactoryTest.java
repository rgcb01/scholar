package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.editor.BuiltInEditorActionCatalog;
import dev.rgcb.scholar.editor.BuiltInEditorActions;
import dev.rgcb.scholar.editor.ContextMenuEntryKind;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.EditorState;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EditorContextMenuFactoryTest {
    @Test
    void resolvedEntriesRetainUnifiedCatalogDescriptors() {
        var factory = new EditorContextMenuFactory(BuiltInEditorActions.editMenuActions());
        var state = new EditorState(
                new Document(List.of(new Paragraph(new InlineContent(List.of(new Text("abc", Set.of())))))),
                new DocumentPosition(0, 1));

        var entries = factory.resolve(state, false);

        assertFalse(entries.isEmpty());
        entries.stream()
                .filter(entry -> entry.kind() == ContextMenuEntryKind.ACTION)
                .map(entry -> entry.action().orElseThrow())
                .forEach(action -> assertSame(BuiltInEditorActionCatalog.require(action.id()), action.descriptor()));
    }
}
