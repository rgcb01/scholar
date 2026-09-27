# Template And Creation Architecture

This document describes internal product policy. It does not add a public
template-registration API or change the persisted document format.

## Template Catalog

`DocumentTemplateCatalog` is the ordered source of truth for templates exposed
by production Home. Each immutable `DocumentTemplateDescriptor` contains:

- a lowercase stable `DocumentTemplateKey`, independent of localized text;
- title and description translation keys;
- the existing single/two-column Home preview identity;
- the initial stored document-name stem;
- a deterministic `Document` factory.

The catalog contains the three existing production choices: `blank`,
`ieee-style`, and `readability-sample`. `ScholarHomeScreen` iterates the catalog
for layout, rendering, and selection. `ScholarApplication` resolves the same
descriptor to choose the unique initial name and invoke its factory.

`DocumentTemplateKey` is creation-time product identity. It is intentionally
separate from `DocumentTemplateId`, which remains persisted semantic
typesetting identity (`BLANK` or `IEEE_STYLE`). The readability sample uses
IEEE-style document settings without introducing a new persistence value.

## Factory Ownership

Blank and IEEE factories remain in `DocumentTemplates`, where page settings,
semantic styles, and initial authored structure already belong. The readability
factory remains in the application layer because it assembles an opt-in
production sample across several scientific domains. Factories have no client
screen or rendering dependencies and are directly testable.

## Creation Policies

`ScientificContentCreationPolicy` owns deterministic initial tables, plots,
diagrams, datasets, and empty-caption figures. It centralizes initial table
dimensions, diagram canvas dimensions, dataset columns, axes, series, and
generated semantic labels.

`MechanicalContentCreationPolicy` owns the authored bill-of-materials table:
column vocabulary, row construction, header count, and item ordering.

These classes are immutable static policy definitions. They do not hold global
mutable configuration. Session and editor commands still own history,
selection, generated stable IDs, and insertion position.

## Intentional Constants

Scientific vocabulary, persistence field names, schema versions, enum
identities, validation rules, and analysis algorithms are not configuration.
Visual colors, spacing, and control sizes remain in the Scientific Instrument
UI system. Diagram element sizes and non-overlap placement remain in diagram
editors because they are algorithms derived from the current canvas and
existing elements, not document-creation defaults.

Initial authored labels such as `Untitled Plot`, axis names, IEEE prompt text,
and BOM headings remain deterministic semantic document content. They are not
used as machine identity. Preserving them keeps existing template and document
output semantically equivalent across persistence, transfer, and automation.

## Adding A Built-In Template

Add a deterministic factory, translation keys in both supported locales, and
one descriptor in `DocumentTemplateCatalog`. Home and application creation do
not require template-specific branches. Dynamic registration, JSON loading,
resource-pack templates, and addon registration are intentionally out of
scope.
