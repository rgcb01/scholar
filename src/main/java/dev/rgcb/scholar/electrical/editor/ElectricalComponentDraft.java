package dev.rgcb.scholar.electrical.editor;

import java.util.Objects;

/** Editable authored annotations for one selected electrical component. */
public record ElectricalComponentDraft(String referenceDesignator, String valueLabel) {
    public ElectricalComponentDraft {
        referenceDesignator = Objects.requireNonNull(referenceDesignator, "referenceDesignator");
        valueLabel = Objects.requireNonNull(valueLabel, "valueLabel");
    }
}
