package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.client.editor.ScholarEditorController;
import dev.rgcb.scholar.editor.EditorAction;
import dev.rgcb.scholar.editor.EditorActionId;
import dev.rgcb.scholar.editor.EditorContextActionResolver;
import dev.rgcb.scholar.editor.EditorState;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves context actions from the unified catalog-backed action set. */
public final class EditorContextMenuFactory {
    private final EditorContextActionResolver resolver;
    private final Map<EditorActionId, EditorAction> actions;

    public EditorContextMenuFactory(List<EditorAction> actions) {
        this(new EditorContextActionResolver(), actions);
    }

    EditorContextMenuFactory(EditorContextActionResolver resolver, List<EditorAction> actions) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.actions = List.copyOf(Objects.requireNonNull(actions, "actions")).stream()
                .collect(Collectors.toUnmodifiableMap(
                        EditorAction::id, Function.identity(), (first, duplicate) -> first));
    }

    public List<dev.rgcb.scholar.editor.ContextMenuEntry> resolve(EditorState state, boolean emptyArea) {
        Objects.requireNonNull(state, "state");
        return emptyArea ? resolver.resolveEmptyArea(state, actions) : resolver.resolve(state, actions);
    }

    public ContextMenuWidget create(
            ScholarEditorController controller, EditorState state, boolean emptyArea, int x, int y) {
        var entries = resolve(state, emptyArea);
        return entries.isEmpty() ? null : new ContextMenuWidget(controller, entries, x, y);
    }
}
