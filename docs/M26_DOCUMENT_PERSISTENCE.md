# M26 - Document Persistence

Status: complete and manually accepted. This report was written at the automated
checkpoint before Minecraft QA and M27. M26 is one milestone; codec/schema/storage/editor
integration are internal components, not M26A/B/C roadmap milestones.

The user accepted M25, including its manual Minecraft QA, before this milestone.
M25 transfer is an input contract, not the native file format.

## Audit And Decisions

- Document is an immutable value record containing ordered blocks and ordered datasets;
  there is no authored document title, file UUID, origin or other root metadata to invent.
- The eight current block families are Paragraph, Heading, EquationBlock, TableBlock,
  PlotBlock, DiagramBlock, FigureBlock and TableOfContentsBlock. Inline nodes are Text
  and CrossReference. Unknown implementations are explicitly unsupported in V1.
- Math has eleven node families, including MathSymbol, MathNamedOperator and MathText,
  beyond the initial Sequence/Number/Identifier/Operator/Fraction/Script/Root/Group set.
- ScientificDataset owns columns and rows; table/plot consumers store dataset/column
  bindings, not independent live datasets. DatasetValue uses BigDecimal, text or missing.
- DiagramDefinition contains owner-local elements and connections. Generic nodes own
  explicit ports. ElectricalComponent and ElectricalJunction ports are catalog-derived;
  endpoints store their stable terminal IDs. Mechanical constraint/part references are
  authored elements; measured dimensions, symbol strokes and electrical nets are derived.
- Figure owns exactly its supported Plot/Diagram content and caption. Diagram workspace
  aspect ratio is stored semantic presentation state; pan/zoom are not.
- Existing Markdown, TSV and diagram summary serializers cannot preserve the full model.
  There was no suitable complete native codec, storage or semantic dirty baseline.
- Gson is already available through the runtime; it is now an explicit implementation
  dependency (2.10.1). No Minecraft Codec, Java serialization or reflection DTO mapping.
- JSON with `.scholar.json` is inspectable and recognizable. A deliberately authored V1
  schema uses JsonObject/JsonArray trees as wire DTOs, with manual bidirectional mapping;
  domain instances are never passed to Gson reflection serialization.

## Architecture And Ownership

```text
Save: live Document -> DocumentValidator -> explicit V1 schema tree -> JSON
      -> bounded schema check -> temporary UTF-8 file -> safe replacement

Open: file -> bounded UTF-8 read -> strict JSON tree -> format/version dispatch
      -> explicit V1 semantic reconstruction -> DocumentValidator
      -> staged valid EditorSession -> workspace replacement -> normal derived relayout
```

`dev.rgcb.scholar.persistence` owns codec, wire mapping, diagnostics and storage.
It depends only on semantic models, validation, Gson and Java filesystem APIs.
It does not import Minecraft, client, editor, layout, clipboard or transfer classes.
`DocumentWorkspace` is editor/application lifecycle state outside history; it delegates
I/O and owns the current live session, optional file name and last saved semantic baseline.
Minecraft screens own file actions/dialogs, unsaved-work choices and game-directory setup.

## V1 Root And Version Dispatch

```json
{
  "format": "scholar-document",
  "version": 1,
  "blocks": [],
  "datasets": []
}
```

This illustrates root shape, not a recommended editable empty document. New creates
one empty Paragraph. A value document without any selectable block cannot replace an
editor session; opening it returns a structured selection failure without mutation.

Root fields are required. Reader verifies format and integral version before interpreting
the body as V1. Future versions fail with UNSUPPORTED_VERSION even when their body uses
different fields. Missing/fractional/string versions and wrong format fail explicitly.
The V1 reader is a separate dispatch boundary; no historical migration exists yet.
Future versions must deliberately supply a decoder/migration rather than masquerade as V1.

V1 rejects unknown fields/types/enums instead of silently dropping authored state.
Optional semantic fields are required wire members with either a value or JSON null.
Ordered arrays remain ordered; marks have stable enum ordering. Fields are emitted in a
deliberate order, with two-space indentation and a final newline. No class-name tags.
Enum wire spellings are lowercase with hyphens (`deg-90`, `part-label`); these spellings,
field names and explicit discriminators are V1 schema, not permission to rename persisted
values when runtime classes/enums change. V1WireLabels fixes enum spellings explicitly,
without deriving them from enum names/ordinals. Wire changes require aliases/migration/versioning.

## Supported Semantic Schema

| Family | V1 fields preserved |
|---|---|
| paragraph | type, content |
| heading | type, optional id, level, content |
| equation | type, optional id, recursive expression |
| table | type, optional id, headerRows, ordered rows/cell inline arrays, optional binding |
| plot | type, definition |
| diagram | type, definition, workspaceAspectRatio |
| figure | type, id, content (Plot/Diagram block only), caption inline array |
| toc | type marker only |
| text inline | type, content, ordered marks (`bold`, `italic`) |
| reference inline | type=`reference`, kind, targetId |

Inline arrays preserve empty content, empty Text nodes, exact boundaries and marks.
Unicode is not flattened. Unpaired UTF-16 surrogates are explicitly JSON-escaped so
UTF-8 storage cannot silently replace them; ordinary Unicode remains human-readable.

Math discriminators are `sequence`, `number`, `identifier`, `named-operator`, `text`,
`operator`, `symbol`, `fraction`, `script`, `root`, `group`. Fields retain authored strings,
symbol/operator kinds/roles, ordering, fraction slots, script base/optional sub/superscript,
root radicand/optional index and explicit group delimiter. Empty sequences and distinct
token segmentation remain intact. No LaTeX intermediate or automatic normalization.

Datasets encode id, optional displayName, ordered columns (id/displayName/type), ordered
rows (optional local id, values). NUMBER is a JSON number parsed as exact BigDecimal,
including scale; TEXT is a string; MISSING is its own tagged value with no content.
No inferred numeric types, formatted-value import or column slicing.

Table binding has datasetId and ordered columnIds (empty means all columns). Stored
canonical rows are preserved, including placeholders in bound tables; resolved displayed
rows are never generated for storage. A bound table stays bound after loading.

Plot definition stores title, x/y axes (label/scale/optional min/max range), series
(name/kind/authored points/optional datasetId+xColumnId+yColumnId binding), legendVisible,
gridVisible and height. Canonical authored points are retained even in bound series;
resolved points, auto-ranges, ticks and rendered coordinates are not stored.

## Identities, References And Resource Sharing

Save/load preserves optional SECTION/EQUATION/TABLE and required FIGURE/DATASET IDs
exactly, plus dataset-local column/row IDs and diagram-local IDs. No ID allocator/remapper
or DocumentTransfer runs. Noncanonical IDs that model constructors would silently trim
are rejected on decode instead of repaired. Duplicate semantic IDs fail via constructors
or DocumentValidator, without choosing a target or allocating replacements.

Resources appear once in root datasets. Table, Plot and Figure<Plot> bindings retain the
same ID and resolve through the reconstructed root resource. They do not own deserialized
dataset copies. Value equality of immutable Documents is the semantic round-trip oracle.

CrossReference kind/targetId remain active authored semantics, even if unresolved.
Existing CrossReferenceResolver derives labels after load; no display snapshot, heuristic
repair, synthetic ID, transfer degradation or second resolver is involved in persistence.
Missing references/datasets/columns currently classified as warnings remain warnings;
warnings may save/load. A missing required binding field is malformed schema and fails.

## Diagram Schema And Ownership

Definition stores title, canvas width/height, ordered elements and connections. Every
element stores local id and authored bounds (x/y/width/height), plus explicit type fields:

| Element discriminator | Additional semantic fields |
|---|---|
| node | label; ports id/label/side/offset |
| electrical-component | kind, orientation, referenceDesignator, valueLabel |
| electrical-junction | netLabel |
| mechanical-primitive | kind, orientation |
| mechanical-symbol | kind, orientation |
| mechanical-dimension | kind |
| mechanical-annotation | kind, text |
| mechanical-constraint | kind, subjectId, optional peerId |
| mechanical-part-reference | targetId, itemNumber, partName, quantity, description |

Connections preserve source/target elementId+portId and label. Definition reconstruction
checks IDs, endpoint existence and authored canvas geometry. Invalid endpoints fail.
Existing mechanical degraded-target warnings remain warnings, not silently repaired links.
Derived electrical ports/nets, dimension display values, callout geometry and hit-test
caches are recomputed. Catalog terminal IDs must remain V1-compatible; renaming them
requires deliberate migration, not undocumented new port names.

Figure content is nested exactly once. There is no arbitrary block containment,
flattening, duplicate root representation or diagram-local ID globalization.

## Codec API And Failure Model

- `DocumentJsonCodec.encode(Document) -> PersistenceResult<String>`.
- `DocumentJsonCodec.decode(String) -> PersistenceResult<Document>`.
- `PersistenceResult.Success<T>` contains complete value and warning diagnostics only.
- `PersistenceResult.Failure<T>` contains ERROR diagnostics and no partial value.
- Diagnostics have severity, code, path/context and message. Codes distinguish malformed
  JSON, wrong format, unsupported version, missing field, invalid value, unknown type,
  validation failure, I/O failure, invalid filename and size limit.
- Internal parser exceptions and filesystem exceptions become results; normal malformed
  files do not propagate stack traces to Minecraft. Semantic warnings/errors retain the
  existing validator code in their diagnostic message; no second validation framework.

Streaming parsing rejects duplicate JSON members and excessive nesting before mapping.
Limits: 16 Mi UTF-16 code units of JSON, 128 JSON nesting levels, numeric literal length
1024 and precision/absolute scale <=1024. File reads are bounded at 64 MiB and strict UTF-8.
The encoder applies the same bounds. Signed-zero double coordinates survive; BigDecimal
value/scale remain exact. These are safety ceilings, not large-document optimization.

## Storage And File Safety

`DocumentStorage` provides save(name, Document), load(name), list(). `FileDocumentStorage`
receives an application-owned Path; only the client chooses `<game directory>/scholar/documents`.
No developer-machine paths, world/server dependency or remote persistence.

Names are 1-64 ASCII letters/digits/underscore/hyphen, starting with a letter/digit;
Windows reserved basenames are rejected case-insensitively. No separators, dots, absolute
paths or traversal. Storage adds `.scholar.json`; picker uses names without extension.
Directory symlink ancestors and symlink document files are refused, and reads/writes use
NOFOLLOW_LINKS. This is local application storage, not an adversarial filesystem sandbox;
concurrent external directory replacement is not a multi-process locking protocol.

Save validates/encodes before touching the destination, creates a same-directory temp,
writes all bytes, forces/closes the channel and attempts atomic replacement. If the
filesystem specifically lacks atomic moves, it uses closed-file replace as a documented
fallback: no in-place truncation, but not crash-atomic and not a power-loss journal.
Other write/move errors are failures; temp cleanup is attempted. Save failure never changes
the session, saved baseline or name; tested failures leave the previous good file intact.
No promise of parent-directory fsync/power-loss durability or cross-process locking.

Load reads/decode/validates completely before creating a new valid EditorSession. Only
then does workspace replace session/name/baseline. Failed files leave document, selection,
history, runtime owner and dirty state unchanged. Resource additions are not progressive.
List returns sorted safe regular `.scholar.json` names; empty/missing directories work.

## Editor Lifecycle, Dirty State And UI

`DocumentWorkspace` tracks the last successfully saved semantic Document snapshot.
Dirty is true without a saved baseline or when current Document differs by value. Save/
Save As capture the saved snapshot, not history depth. Selection/navigation/menus do not
change dirty; Undo to saved value is clean, Redo away is dirty, and vice versa after saving.
Save/Save As create zero semantic history entries and do not clear Undo/Redo.

Open creates a new session and valid deterministic selection at the first Paragraph/
Heading using existing initial-state conventions; an atomic-only document uses existing
BlockSelection. Typing marks, history, clipboard metadata, layout, viewport, widgets and
active selection are not serialized. New creates one empty Paragraph, no datasets, an
unnamed dirty baseline, fresh session/history and caret (0,0).

Every newly constructed session gets a fresh RuntimeDocumentToken. File IDs never prove
same-document transfer, even reopening the same file or equal AST. Process clipboard
can survive opening, but old copied content then follows cross-document M25 rules.

File menu actions are EditorActions with Ctrl+N/Open Ctrl+O/Save Ctrl+S/Save As Ctrl+Shift+S.
They reuse controller/menu dispatch and respect existing popup/drag input ownership.
Save As supports name entry, Enter-to-save, validation messages and confirmation before
overwriting a different saved name. Open lists paginated saved files with native buttons,
keyboard focus/navigation, empty/error states and Cancel. Footer shows name and dirty `*`.
Successful Open/New reconstructs the screen/controller, discarding transient viewport/UI
state. Bad Open stays in the picker with an error; the previous editor is intact.

Open/New/Close while dirty requires Save/Discard/Cancel. Failed Save keeps the prompt/
editor usable; Cancel aborts the pending navigation; Discard is explicit. There is no
autosave or protection against OS termination/game shutdown beyond normal editor actions.
The workspace's internal open/new methods assume the caller obtained that explicit choice;
they are not a public lifecycle API. The UI guard is authoritative for user navigation.

## Automated Acceptance

- Full regression: **1425 tests, zero failures/errors/skips**, 96 new M26 executions.
- `gradlew.bat test`: BUILD SUCCESSFUL. `gradlew.bat build`: BUILD SUCCESSFUL.
- Codec golden is DevelopmentDocument.createPersistenceFixture: full current mixed model,
  all diagram families, hierarchy, marks/Unicode, references, math, authored/bound tables,
  shared plots/figures/resources, captions and TOC. Value equality and zero ERROR hold.
- A hand-authored V1 resource separately protects discriminator/field meaning. Codec
  tests cover all math variants, segmentation, decimal scale, signed zero and Unicode.
- Shared Table/Plot/Figure dataset bindings still target exactly one `projectile-test`
  resource. Existing resolvers derive views, sections and reference labels after decode.
- Fixed seed 26026: 75 bounded mixed-document encode/decode/validate/canonical replays.
- Encode-decode-encode is byte-identical for golden/canonical V1 values.
- Malformed corpus: 31 parameterized cases plus focused bounds/version/identity tests;
  includes invalid JSON/shape/version/types/enums, duplicate IDs, missing binding fields,
  ragged tables, wrong Figure content and invalid diagram endpoint/reference shapes.
- Temp-directory storage tests cover overwrite, recreation, full golden file round trip,
  listing, invalid names/traversal, missing/unreadable/malformed files, invalid UTF-8,
  Unicode, injected write/move failure and injected unsupported-atomic-move fallback.
- Workspace tests cover saved-baseline dirty transitions, failures without mutation,
  fresh history/selection/provenance, minimal New, atomic-only selection, nested editing
  and exact Undo/Redo. Loaded Figure paste proves internal remap, external Text degradation,
  independent resource import and one exact M25 history transaction.
- PersistenceBoundaryTest checks platform/editor/transfer/clipboard/layout isolation and
  no Java object serialization. Codec tests run on JVM without starting Minecraft.

Automated tests are not manual visual/input acceptance. No client-startup or Minecraft
QA result is claimed. Existing M25 uncommitted documentation/code remains preserved.

## Manual Minecraft QA (12 Steps)

1. Run `gradlew.bat runClient`, enter a development world and `/scholar_dev_editor`.
   Verify M26 heading/math and scroll through tables, plots, both Figure families and diagrams.
2. File > Save As `m26-qa`. Check the name appears without dirty `*`. Inspect
   `<game directory>/scholar/documents/m26-qa.scholar.json`: format/version, root datasets,
   no transient token/history/layout. Save As the same name must not create undo history.
3. Edit the M26 paragraph; check `*`. Ctrl+S: clean. Undo should be dirty (previous value),
   Redo clean (saved value). Make another edit: Undo to the baseline clean, Redo dirty.
4. Save, close the editor, then restart the client if practical. Re-enter the world,
   `/scholar_dev_editor`, File > Open. Discard the freshly opened unsaved dev fixture
   explicitly and select `m26-qa`. Verify the saved edit and complex content survive.
5. Check section/figure references and TOC/Outline navigation; the intentionally broken
   reference remains missing. No identity suffixes should appear merely from loading.
6. Edit a loaded table cell, equation and Figure caption; Undo/Redo each once. Verify
   editable content and focus remain valid; loaded history did not inherit prior edits.
7. Edit Projectile Test through existing Data actions. Bound Table, Plot and Figure<Plot>
   should still share the single resource. Save, reopen and check the change survives.
8. Inspect electrical connections/junctions and mechanical constraints/annotations/part
   references. Geometry, local topology and workspace aspect ratio should remain intact;
   viewport zoom/pan/selection are not restored from the file.
9. Copy the M25 Self/external Figure, save, reopen `m26-qa` and paste at prose. Self should
   remap, external label become Text, dataset import independently. One Undo/Redo restores
   exact paste. Also copy/paste a whole Table or Plot after loading.
10. Copy `m26-qa.scholar.json` externally to `m26-bad.scholar.json` in the same directory
    and change version to 999 (do not alter the good file). File > Open after choosing
    Discard/Cancel as appropriate: bad file must report error; Cancel returns the intact
    current document. Correct files remain openable afterward.
11. With an unsaved edit test File > New, Open and editor Close: Cancel retains work;
    Discard permits navigation; Save writes first. In Save As try `../bad` and `CON`:
    reject without losing work. Saving over another name requires confirmation.
12. Check picker pagination/keyboard/Enter/Escape, file shortcuts and dialogs at two GUI
    scales/window sizes. Inspect logs for exceptions. Report manual acceptance separately.

## Deferred Work And Next Gate

No remaining automated M26 blocker. Manual QA is ready but pending. Large datasets,
performance, existing crowded chrome at very narrow GUI widths, diagnostic/repair UI,
tolerant recovery, file locking and crash journals are not silently solved here.
No cloud/network/collaboration, autosave, thumbnails, folders, templates, native plugin
schema, compression/encryption, new export formats or scientific feature additions.

V1 freezes deliberate wire meanings for current built-in nodes, not Java implementation
classes or a public mod API. Future format changes need version dispatch/migration tests.
M27 may begin only after manual acceptance; it has not begun. No commit or push performed.
