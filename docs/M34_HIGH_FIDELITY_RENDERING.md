# M34 High-Fidelity Document Rendering

## Initial audit

- `ScholarEditorScreen` applies document zoom with a GUI pose transform around the viewport. The original Minecraft font providers were 11 px TTFs with 4x raster oversampling. Enlarging their 11 px glyph geometry at 150-200% enlarged an already small sampled image. The renderer also used the vanilla font for page furniture. Nothing in this path changed Minecraft's global texture filtering.
- The current checked-in prose family is **Source Sans 3**, not Atkinson Hyperlegible Next as stated in the M34 request. Noto Sans Math is the math family. `MinecraftTypographyResolver`, font JSON, and layout measurement all agree on Source Sans 3. Replacing it here would change measured widths and caret/hit geometry; that is a separate typography migration requiring approval and visual QA.
- `MinecraftDocumentRenderer` and `MinecraftMathRenderer` consumed the established laid-out document; there was no second layout engine. Plot line segments, electrical wires/circles, mechanical lines/arcs/circles and root diagonals were rasterized with integer Bresenham/midpoint steps. Fit samples were already derived (65 points) but rounded to integer layout coordinates before rendering.
- Table cells each rendered a full outline. Shared borders were therefore issued twice, which could make separator weight inconsistent under scaling. Page color had little contrast with its surroundings and no depth cue.
- Viewport and nested workspace `GuiGraphics` scissors used unscaled GUI coordinates while the pose was zoomed. For zoomed content, that could clip the wrong region. Mouse inversion used equivalent arithmetic in `ScholarEditorScreen`, but not a shared transform. GUI scale is applied by Minecraft after GUI-coordinate drawing; screen/window pixel density is not semantic document state.
- `CaretGeometryResolver`, selection geometry and hit testing already use the laid-out document. The M32 caret correction remains in that layer. M34 must not invent a second placement algorithm.

## Final rendering boundary

```
Document (unchanged) -> DocumentLayoutEngine (unchanged)
  -> LaidOutDocument -> MinecraftDocumentRenderer / MinecraftMathRenderer
  -> document-only typography and ScientificStroke -> GuiGraphics
```

`ScholarEditorScreen` retains the outer screen-space viewport clip and zoom pose. `DocumentViewTransform` is the shared mapping between layout/GUI coordinates and displayed GUI coordinates. The inner renderer receives the ceiling of physical viewport width/height divided by zoom, so zooming below 100% does not clip the right or bottom edge. Nested document and diagram scissors are mapped to screen space before being submitted; caret/selection use the already-physical outer viewport. Mouse inversion uses the same transform. Selection, caret and hit geometry still originate in the existing layout and editor resolvers. Neither zoom nor this transform enters semantic state/history/persistence.

## Text and math

Five document-only font profiles use the same bundled TTF families as the 11 px profiles, rasterized at 22 px / 2x oversampling and drawn at half scale. The effective underlying raster density remains bounded while 200% zoom can show approximately native-size 22 px glyphs instead of enlarging an 11 px glyph. This is selected only by the document surface; Home, ribbon, menus, dialogs and other Minecraft UI retain their established appearance. **The same 22 px profile, converted back to logical units, now measures layout, math, caret, hit testing and selection.** Measuring with the 11 px profile and rendering with the independently rounded 22 px profile caused the first M34 manual QA failure. No font atlas is rebuilt per frame; Minecraft's resource manager owns and reloads font atlases. There is no new GPU render target or manual texture lifecycle.

Math glyphs, diagram labels, plot labels and page furniture share the same document text path. Script scale and relative baseline offsets remain authored typography rules; prose lines now reserve top inset and descent for them. Fraction rules remain geometry from the Math layout; root diagonals use the document-only stroke path. Font fallback references remain available, but glyphs absent from both bundled TTFs may fall back to Minecraft's default at a smaller size; manual QA should flag any such glyphs.

## Strokes, plots, diagrams and tables

`ScientificStroke` draws non-axis-aligned lines with deterministic 2x subpixel coverage and alpha, clipped to the target rectangle where needed. Axis-aligned lines keep a one-call crisp fill to avoid per-pixel cost. Root strokes, solid plot series/fits, electrical lines/circles, and mechanical lines/circles/arcs use the shared path. Authored dashed/dotted plot patterns retain their existing pattern rasterizer and therefore remain a deliberate visual limitation. Plot samples and semantic data are not changed; the stroke renderer smooths the displayed path between existing samples. Existing plot-area clipping is retained.

Tables now draw each unique row/column boundary once, while preserving header fill, cell text and current non-merged grid semantics. The page surface is a restrained near-white with a small shadow and edge; dimensions, margins, columns, page numbering and page breaks still come from M30 layout and settings. The outer viewport clip prevents a shadow or antialiased stroke from bleeding into Minecraft chrome.

## Precision, performance and fallback

Layout's integer coordinates remain deterministic. Fractional positions are retained in view transforms and in sampled curved strokes until drawing. `DocumentViewTransform` floors clip origins and ceils clip ends, avoiding accidental loss of edge pixels. GUI-scale 1-4 and zoom 75-200% mapping are covered by deterministic tests; only an in-game check can verify each GPU/font implementation visually.

There is no framebuffer allocation, global shader/filter change, or platform-specific native dependency. Glyph atlases are owned by Minecraft's existing font loader and reload naturally with resources. Stroke work is bounded by each laid-out segment; axis-aligned segments use one fill. Very dense diagrams or thousands of fitted segments may still warrant measured profiling before adopting an offscreen pipeline. If custom font resources fail to load, Minecraft font references provide its usual fallback; this is not a guarantee that every missing font glyph will match Scholar metrics.

The current approach does **not** deliver arbitrary vector/SDF quality at every scale. Dashed plot patterns, axis-aligned one-pixel rules at fractional zoom, and fallback glyphs can still alias. Integer layout quantization can leave at most a small edge discrepancy between a glyph's fractional advance and a caret; test placement and clicking at 75%, 125% and 200% in-game. Do not declare M34 accepted solely from Java tests.

## Manual QA geometry correction

The first M34 manual pass found selection rectangles far to the right and script-heavy lines too dense. `LaidOutText.x` is already absolute in paginated layout, while `SelectionGeometryResolver` had added `line.x` again. It now uses the placed run origin and calculates a partial selection's right edge from the difference of measured prefix advances, so kerning and mid-line ranges are handled consistently. Empty-block boundary selection likewise uses the placed line origin once. Rendering, selection and caret share the screen pose supplied by `DocumentViewTransform`; mouse inversion and inner clipping use that transform too.

The original 9 px Minecraft line-height calculation was not a line box for Scholar's 11 px document glyphs and displaced scripts. `ScholarTypography.documentLineHeight` now derives a deterministic box from the document font em, the shared 0.75 script scale, a 3-unit rise/drop, and one unit of leading. `renderText` uses the same typography constants and positions normal text after the reserved top inset. The client math measurer also uses this 11 px nominal em for ascent/descent rather than the generic 9 px GUI line height. H₂O, x², CO₂ and explicit inline script marks therefore fit in consecutive lines without GPU-dependent height measurements. The test-only visual fixture contains repeated adversarial scientific lines. This correction changes derived layout, not semantic content, history, transfer or persistence.

## Automated coverage

- `DocumentViewTransformTest`: fractional round trips and enclosing scissors over zoom 75/100/125/150/200% and simulated GUI scales 1-4.
- `ScientificStrokeTest`: reversible line coverage, antialiased edge falloff and thickness.
- `TypographyResourceTest`: high-resolution profiles use the same font families and exactly double the native glyph size.
- `SelectionGeometryResolverTest`: paginated single/two-column origins, partially selected kerning pairs, wrapped multi-line selection versus caret endpoints, and zoomed rectangles.
- `ScientificLineMetricsTest`: logical normal/subscript/superscript extents, consecutive scientific lines, caret after explicit script runs, and transformed hit positions.
- `DevelopmentVisualQaDocumentTest`: the test-only comprehensive visual fixture remains valid and now has a high-fidelity extension containing a derived M33 quadratic fit. It asserts that authored fit samples remain absent and resolved samples are derived.
- Existing caret, plot layout, diagram, editor, boundary and persistence tests remain authoritative. GPU screenshots are intentionally not golden-tested across vendors.

## Manual visual QA in production

Use `/scholar` only. Create/open a document with several paragraphs, headings, bold/italic, Unicode scientific characters, equations (scripts, roots and nested fractions), a table, a plot with data points and a quadratic fit, electrical/mechanical diagrams, a figure caption and a cross-reference. The corresponding test-only fixture is `DevelopmentDocument.createHighFidelityQa()`; it is not a production command or screen.

1. At 100%, 125%, 150% and 200% zoom, compare prose edges, small text, math symbols, scripts, fraction bars, root strokes, page edge/shadow, table separators, plot axes and curved fit, electrical wires, mechanical dimensions and labels. Also inspect 75/80% for small-text legibility.
2. Repeat at Minecraft GUI scales 1, 2, 3 and 4 where supported. Use a normal 16:9 window and narrower/wider windows. Check both one- and two-column sections and page transitions.
3. Click and drag text, a caption, a table cell, a plot and a diagram. Verify visual selection, caret and mouse targets agree, including after scroll and zoom. Check context-menu placement and that content never bleeds into ribbon or outside plot/diagram workspace.
4. Zoom/scroll/resize, then undo/redo an actual text edit. Visual operations must not add a history step or dirty the document. Check resource reload (`F3+T`) and a GUI-scale change do not corrupt fonts or clips.
5. Inspect frame rate in a document with several plots/diagrams. Report any GPU-specific blur, missing glyph, bad baseline or slow frame with zoom, GUI scale and screenshot.

Acceptance criterion: the scientific document surface is visibly cleaner and easier to read than before M34, while surrounding Minecraft controls remain unchanged. Manual acceptance remains pending.

## Second manual-QA correction (pending in-game recheck)

The text caret was positioned at a laid-out line's top and drawn through its entire height. M34 line height now reserves extra space for raised and lowered scripts, so this made the caret noticeably taller than the normal glyph em at high zoom. The M34 paginated coordinate path also had a second horizontal addition: both `LaidOutLine.x` and `LaidOutText.x` are already placed document coordinates. The resolver now uses the target line/run origin once. The caret takes its logical top inset and visual em from the same `ScholarTypography` rules as document text; the extra script clearance remains line spacing, not caret ink. `DocumentViewTransform` applies zoom once to the resulting logical endpoints. Table-cell carets use the same visual-metric contract. No semantic selection or history value stores screen pixels.

The **M34 Readability Sample** is opt-in from `/scholar` → New document → M34 Readability Sample. This creates a normal persisted document named `M34` (or `M34 2`, etc. when the name is already occupied). It includes Lorem Ipsum paragraphs, mixed marks and Unicode scientific text, real Math AST fractions/root/scripts, a dataset-backed table, quadratic fit analysis, a dataset plot inside a Figure, caption/reference, and a two-column section. It is not a development command or an auto-seeded file. Deleting it does not recreate it; choosing the sample again creates a new document deliberately.

Home document cards now have a right-click **Delete** action. It opens a Minecraft-styled confirmation with Cancel/Delete. The application refuses deletion while any workspace for the document is open (dirty workspaces require save/discard and close first). Storage deletion validates the application ID/path and removes only its own document file. The workspace index reserves retired IDs before file removal, then removes the card's metadata; if storage removal fails, Home still shows the document and reports the failure. A metadata cleanup failure after successful removal is reported as a warning, while Home refreshes from actual storage. Delete never calls editor mutation or creates undo history.

### Manual recheck

1. Open `/scholar`, choose New document → M34 Readability Sample, then open the resulting `M34` document from Home.
2. At 100%, 125%, 150%, and 200%, place the caret after normal prose, H₂O, x², and on wrapped lines. Scroll and repeat. Check its height, vertical alignment, and X against the insertion point.
3. Read several Lorem Ipsum paragraphs at those zooms. Inspect glyph sharpness, line/paragraph spacing, margins, page breaks, columns, equations, table, fit plot, Figure and caption.
4. Drag a half-line, full-line, and wrapped multi-line selection; verify highlight follows the text.
5. Close the editor to Home. Right-click a disposable card: clicking outside the menu must not open it. Choose Delete → Cancel and verify it remains. Repeat → Delete and verify only that card disappears. Delete `M34` if desired; it must stay deleted until explicitly created again.

M34 remains open until this Minecraft visual QA is accepted.

## Final caret-space and document status-bar QA fix (pending in-game acceptance)

The previous correction fixed logical caret metrics, but the final `GuiGraphics.fill` still ran inside the document zoom pose. Its final screen rectangle was implicit and could not be asserted by the logical tests. The final rendering path now resolves a logical caret, transforms its top and bottom once via `DocumentViewTransform.caretRect`, removes the zoom pose, and draws that exact one-pixel-wide GUI-space rectangle under the document viewport scissor. Endpoint rounding can change height by at most one GUI pixel; script clearance is not added to caret height. The code did not contain an explicit second multiplication of caret height, so the previous in-game oversized appearance needs another visual check rather than a claim that a confirmed zoom-squared formula was removed.

The persistent status bar is application chrome below the document workspace; it is not part of the paper. `ScholarShellLayout` reserves 24 GUI pixels and the document viewport stops above it. It uses existing raised panel colors, small icons, hover treatment, and tooltips. Page count comes from `LaidOutDocument.pages`; the current page follows the text caret or selected block when available, otherwise the center of the visible viewport. It is never serialized.

`DocumentStatus.wordCount` counts authored `Text` in paragraphs, headings, Figure captions, and manual table cells using root-locale word boundaries. It excludes derived TOC/numbering, math, datasets, plot/diagram internals, IDs, and computed outputs. The screen caches this derived count by immutable `Document` identity and invalidates it on a semantic edit. It is not part of history or dirty state.

Fit Page, Fit Width, Zoom Out, and Zoom In dispatch the same `viewportActions` that populate View. The slider uses the same `zoom` field and `setZoom` path, ranges from 40% to 200%, and updates live while dragged. Fit actions update its thumb and percentage automatically. Narrow layouts keep zoom controls first, then page information, then word count, then optional fit buttons; hidden slots cannot intercept clicks. Zoom, page, and word count remain transient UI state.

Automated tests assert final caret screen-space height at 75/100/125/150/200% across prose, empty content, scripts, wrapping, headings, and scrolling. Pure status tests cover authored word count, page selection, wide/narrow geometry, and slider mapping. Final visual acceptance still requires the Minecraft recheck at those zoom levels and several window sizes.

### Scissor-stack correction after in-game flicker report

The first final-caret implementation left the outer viewport scissor active after drawing the screen-space caret. Internal document, selection, and caret clips were individually balanced, but the enclosing screen clip was not popped. This could clip subsequent application chrome and later frames without producing a Java exception or a GL invalid-operation message; the reported capture showed the prior Home frame behind the paper. The screen now draws the caret after popping the zoom pose but while the original viewport scissor remains active, then closes that outer scissor exactly once in `finally`. The `latest.log` from the reported session contains repeated Windows clipboard-access errors but no render exception; those clipboard messages are separate from this scissor-state defect. Reopen `/scholar` and the M34 sample for the final visual recheck.

### Optical caret metrics after the scissor fix

The next in-game capture confirmed that the blink/clip issue was fixed but the prose caret was still too low and too long. Pixel inspection of that capture found a 33-pixel caret at y=378–410 while surrounding glyph ink began about 13 pixels earlier and ended above the caret. At the approximately 3x combined zoom/GUI scale, this maps to roughly four logical units of top displacement. The old `MinecraftTextMeasurer.caretMetrics` used the **layout em** (11 units, plus heading line-height adjustment) and `textTopInset` (+3), although Minecraft's TrueType sheet positions the Source Sans glyph ink above that text origin (`SheetGlyphInfo.getTop()` is based on the 7-unit baseline). Layout line height also reserves script clearance and is not a glyph-ink bound.

`ScholarTypography.documentCaretMetrics` now holds the source typeface's optical caret envelope: four logical units above the text origin and ten units tall at the standard authored size, scaling with the authored font size. Heading leading and script reservation no longer inflate the visible caret. A negative top inset is valid because glyph ascenders can protrude above a line's nominal top. This changes neither text layout nor semantic content; the final screen-space caret rectangle still receives zoom once. Focused tests cover body, headings, scripts, empty and wrapped text, authored size changes and zoom/scroll. The calibrated envelope is based on this typeface and the reported pixels; Minecraft visual acceptance remains necessary.

The following manual capture showed that the text caret remained continuously visible. Unlike equation and table editing, `renderCaret` had no blink condition, making the insertion point look like a literal bar. A transient `CaretBlink` now follows Minecraft `EditBox`'s 300 ms visible/hidden cadence. It starts visible and restarts after a selection or document change, or when the editor regains focus. Text selections and modal/menu focus do not show a document caret. Equation and table carets use the same phase. Blink timing is screen-only and never enters document history or persistence. `CaretBlinkTest` covers cadence, movement, edit, loss of focus and refocus. This still requires manual review of appearance in the M34 sample.

## Crash on opening the M34 sample (23 September)

The M34 sample was saved successfully, but opening it crashed in `DocumentLayoutEngine.placeTextBlock`: a long paragraph started near the bottom of one column and continued at the top of the next. The old block height calculation subtracted the first line's Y from the last line's bottom; Y can decrease at a same-page column transition, producing a negative height rejected by `LaidOutBlock`. Text-block bounds now enclose all placed lines rather than assuming their Y values are monotonic. `DocumentHitTester` also uses actual line proximity in both coordinates so the broad bounds of a cross-column paragraph do not steal clicks from a later paragraph. Focused tests cover both the crossing case and pagination of the production M34 sample. The already-created `M34` document remains in Home; no user file was removed or recreated. Reopen that card for manual recheck.

## Final acceptance

M34 was subsequently manually accepted in Minecraft. Earlier pending-QA notes above describe
the individual implementation checkpoints, not the current milestone status. The current
production flow is `/scholar` with the opt-in M34 Readability Sample; no M35 addon API work
was included.
