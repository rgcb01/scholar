package dev.rgcb.scholar.document;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DocumentStructure(List<SectionEntry> sections) {
    public DocumentStructure {
        sections = List.copyOf(Objects.requireNonNull(sections, "sections"));
    }

    public Optional<SectionEntry> sectionById(String id) {
        Objects.requireNonNull(id, "id");
        return sections.stream()
                .filter(section -> section.id().isPresent())
                .filter(section -> section.id().orElseThrow().equals(id))
                .findFirst();
    }

    public Optional<SectionEntry> sectionAtBlock(int blockIndex) {
        return sections.stream()
                .filter(section -> section.blockIndex() == blockIndex)
                .findFirst();
    }
}
