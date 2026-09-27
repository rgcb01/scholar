package dev.rgcb.scholar.application;

import java.util.Objects;

/** Stable product identity for a creation template; never derived from visible text. */
public record DocumentTemplateKey(String value) {
    public DocumentTemplateKey {
        Objects.requireNonNull(value, "value");
        if (!value.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("Template key must be lowercase kebab-case.");
        }
    }
}
