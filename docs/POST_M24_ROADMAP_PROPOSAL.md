# Post-M24 Roadmap Proposal

Status: proposal only. Do not replace `ROADMAP.md` with this until the architecture decisions are accepted.

## Critique Of Tentative M25-M30

Tentative sequence:

- M25: Document Fragment & Clipboard Foundation
- M26: Native Document Format & Persistence
- M27: Layout & Rendering Hardening
- M28: Diagnostics & Recovery
- M29: Performance & Large Documents
- M30: Editor UX Stabilization

Assessment:

- M25 is directionally correct, but it should be reframed. The core problem is not clipboard alone; it is document transfer closure: selected content, resources, stable IDs, references, and paste/import identity remapping.
- M26 should not begin until M25 establishes transfer/resource closure and a persistence architecture milestone explicitly decides schema/versioning/degraded loading.
- M27 is useful, but layout hardening should follow persistence architecture because media/resource decisions affect layout assumptions.
- M28 should probably move after native load/import creates real degraded-document states. Validation UI before persistence would likely be abstract and less useful.
- M29 should remain measurement-led. Do not optimize before transfer/persistence/layout contracts are stable enough to measure realistic documents.
- M30 is valuable after the foundation stops shifting. UX polish before transfer/persistence can polish behavior that later changes.

## Revised Roadmap

### M25A - Document Transfer And Identity Closure Architecture

- Why now: clipboard, paste, future import, future persistence, and future media all need the same answer to "what content and resources move together?"
- Problem it solves: prevents more block-specific clipboard/remap paths.
- Architectural output: accepted design for `DocumentFragment` or `DocumentTransfer`, resource closure, ID remapping, reference policy, dataset-backed view policy, and figure/media transfer.
- What it unlocks: safe clipboard unification and persistence schema design.
- Dependencies: current M24 editor foundation.
- Out of scope: native persistence, new media types, third-party extension API, UI redesign.
- Major risks: overgeneralizing into a universal graph system too early.
- Definition of done: design names exact payloads, remap rules, copy/paste semantics, and non-goals.
- Manual QA needed: no, design only.
- Freezes public contract: no.

### M25B - Document Transfer And Clipboard Unification Implementation

- Why now: implementation should retire the current specialized clipboard patchwork before more features use it.
- Problem it solves: centralizes structured transfer, sidecar payloads, resource closure, and ID remapping.
- Architectural output: pure-Java transfer service used by copy/cut/paste paths.
- What it unlocks: native persistence and safer future scientific objects.
- Dependencies: M25A acceptance.
- Out of scope: native file format, cross-process rich clipboard, new block types.
- Major risks: regressions in existing table/plot/diagram/figure/dataset clipboard behavior.
- Definition of done: existing clipboard behavior passes, resource closure behavior is tested, ID remapping is centralized, no Minecraft imports enter core.
- Manual QA needed: yes.
- Freezes public contract: internal transfer semantics only, not public API.

### M26A - Native Persistence Architecture

- Why now: Scholar cannot safely save/load full documents until schema decisions are explicit.
- Problem it solves: prevents accidental permanent serialization of current implementation details.
- Architectural output: schema model, versioning/migration policy, metadata policy, unknown-node/degraded-load policy, resource/embed/link policy, deterministic serialization rules.
- What it unlocks: native persistence implementation.
- Dependencies: M25 transfer/resource closure.
- Out of scope: implementing save/load, cloud sync, export formats.
- Major risks: choosing too much extensibility too early.
- Definition of done: accepted persistence contract and ADR candidates.
- Manual QA needed: no, design only.
- Freezes public contract: prepares first file-format contract but does not freeze it yet.

### M26B - Native Persistence Minimal Implementation

- Why now: once schema is accepted, full document round-trip becomes the next foundational need.
- Problem it solves: Scholar documents become durable without relying on Markdown.
- Architectural output: deterministic save/load for current built-in document model and resources.
- What it unlocks: recovery UI, import/export strategy, real-world document testing.
- Dependencies: M26A.
- Out of scope: third-party node persistence, rich export, collaboration, binary media pipeline unless explicitly included in schema.
- Major risks: malformed/degraded documents and migration edge cases.
- Definition of done: save/load round-trip tests for built-in documents, schema version written, validation on load, degraded-load policy honored.
- Manual QA needed: yes.
- Freezes public contract: first native file format draft; should still be marked pre-1.0.

### M27 - Diagnostics And Recovery Foundation

- Why now: diagnostics become concrete after documents can be loaded from disk/import.
- Problem it solves: users need actionable visibility into invalid references, missing datasets, malformed content, and degraded loads.
- Architectural output: separation between validation, diagnostics, repair suggestions, and user-facing recovery actions.
- What it unlocks: safe import/export and better editor trust.
- Dependencies: M26B.
- Out of scope: automatic repair of every issue, schema migration UI, lint-style authoring suggestions.
- Major risks: turning validation into a vague catch-all.
- Definition of done: diagnostics have severity, source location where possible, and limited repair actions only where semantics are certain.
- Manual QA needed: yes.
- Freezes public contract: diagnostic categories may become semi-stable.

### M28 - Layout And Rendering Hardening

- Why now: after persistence and diagnostics, layout can be hardened against realistic documents.
- Problem it solves: prepares for larger documents, figures/media, long tables, and future export.
- Architectural output: clearer layout contracts, clipping/scrolling behavior, long-table policy, media sizing policy, and renderer boundary checks.
- What it unlocks: scientific media growth and later export.
- Dependencies: M26/M27 and known media/resource strategy.
- Out of scope: full pagination/export engine, new rendering backend.
- Major risks: accidental rewrite of working rendering.
- Definition of done: current blocks render consistently under resizing, scrolling, long content, and mixed scientific content.
- Manual QA needed: yes.
- Freezes public contract: no, mostly internal layout contracts.

### M29 - Performance Instrumentation And Large Document Budgets

- Why now: optimization should follow realistic persistence/layout usage.
- Problem it solves: identifies actual scaling limits before large-document promises are made.
- Architectural output: repeatable performance fixtures, measurement thresholds, memory/layout/history budgets.
- What it unlocks: targeted optimization and large document roadmap.
- Dependencies: M26B and M28.
- Out of scope: speculative rewrites, incremental layout unless measurements require it.
- Major risks: measuring unrealistic fixtures.
- Definition of done: documented budgets and measured hot spots for validation, layout, history, dataset propagation, and ID scans.
- Manual QA needed: yes, for perceived editor responsiveness.
- Freezes public contract: no.

### M30 - Editor UX Stabilization

- Why now: after foundations stop moving, polish can be stable.
- Problem it solves: makes the editor coherent for regular use across text, math, tables, plots, diagrams, figures, datasets, and references.
- Architectural output: consistent command availability, menus, toolbars, focus affordances, and manual QA scripts.
- What it unlocks: broader playtesting.
- Dependencies: M25-M29.
- Out of scope: new scientific feature families.
- Major risks: slipping into feature work.
- Definition of done: common workflows are smooth, action states are predictable, and manual QA is repeatable.
- Manual QA needed: yes.
- Freezes public contract: no.

### M31 - Public API Boundary Design

- Why now: only after persistence and transfer semantics stabilize can other mods rely on Scholar safely.
- Problem it solves: separates supported mod API from internal implementation.
- Architectural output: API packages, stability annotations/docs, creation/open/render service boundaries, non-goals for third-party block types.
- What it unlocks: safe other-mod integration.
- Dependencies: M25-M30.
- Out of scope: arbitrary extension node support unless explicitly accepted.
- Major risks: promising compatibility too early.
- Definition of done: documented stable/experimental/internal boundary and sample external usage.
- Manual QA needed: no.
- Freezes public contract: yes, if accepted.

### M32 - Scientific Media Expansion

- Why now: after transfer, persistence, layout, and API boundaries are clearer, images/media can fit coherently.
- Problem it solves: expands figures beyond plots/diagrams without corrupting the model.
- Architectural output: media resource model, image/asset references, figure integration, persistence behavior.
- What it unlocks: richer scientific documents.
- Dependencies: M25-M28.
- Out of scope: video/simulation unless explicitly scoped.
- Major risks: media storage and missing-resource behavior.
- Definition of done: image/media block or figure content has validated resources, layout, rendering, persistence, and diagnostics.
- Manual QA needed: yes.
- Freezes public contract: possibly media resource shape.

## Immediate Recommendation

The next milestone should be:

M25A - Document Transfer And Identity Closure Architecture

It should be design-first. Do not implement a generic fragment system until the resource closure, ID remapping, and reference behavior are accepted.

