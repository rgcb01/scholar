package dev.rgcb.scholar.transfer;

/**
 * Runtime-only capability for one live semantic document owner.
 * Equality is object identity. Never persist it or share it between unrelated owners.
 */
public final class RuntimeDocumentToken {
    private RuntimeDocumentToken() {
    }

    public static RuntimeDocumentToken create() {
        return new RuntimeDocumentToken();
    }
}
