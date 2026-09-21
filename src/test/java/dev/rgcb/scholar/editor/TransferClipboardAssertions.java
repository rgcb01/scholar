package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.*;
import dev.rgcb.scholar.clipboard.DocumentFragmentClipboardPayload;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.transfer.FragmentContent;

/** Shared assertions for migrated legacy suites: require the real fragment carrier. */
public final class TransferClipboardAssertions {
    private TransferClipboardAssertions() {}
    public static <T> T root(Class<T> type, Object payload) {
        var fragment = assertInstanceOf(DocumentFragmentClipboardPayload.class, payload).fragment();
        var content = assertInstanceOf(FragmentContent.Blocks.class, fragment.content());
        assertEquals(1, content.roots().size());
        return assertInstanceOf(type, content.roots().get(0));
    }
    public static InlineContent inline(Object payload) {
        var fragment = assertInstanceOf(DocumentFragmentClipboardPayload.class, payload).fragment();
        var content = assertInstanceOf(FragmentContent.InlineSegments.class, fragment.content());
        assertEquals(1, content.segments().size());
        return content.segments().get(0);
    }
    public static ScientificDataset dataset(Object payload) {
        var fragment = assertInstanceOf(DocumentFragmentClipboardPayload.class, payload).fragment();
        assertInstanceOf(FragmentContent.ResourcePrimary.class, fragment.content());
        assertEquals(1, fragment.resources().size());
        return fragment.resources().get(0);
    }
}
