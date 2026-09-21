package dev.rgcb.scholar.document;

import java.util.Objects;

public record PageDecoration(String headerText, String footerText, boolean pageNumbers) {
    public PageDecoration { headerText = Objects.requireNonNull(headerText); footerText = Objects.requireNonNull(footerText); }
    public static PageDecoration none() { return new PageDecoration("", "", false); }
}
