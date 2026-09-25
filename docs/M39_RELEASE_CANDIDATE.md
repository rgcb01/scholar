# M39 - Scholar V1 Release Candidate

Status: **manually accepted RC; final 1.0.0 not released**. This report records the original qualification checkpoint on `codex/m39-v1-release-candidate`. M38 was accepted and merged to `main` as `8f9659a34b9ceb37a8b7cf96b066263aff491e25`; `main` matched `origin/main` and was clean before the RC branch. Absolute feature freeze remains in effect.

## RC environment and artifact

| Item | RC value |
| --- | --- |
| Mod version | `1.0.0-rc.1`; do not tag or publish final `1.0.0` |
| Minecraft | 1.21.1 exactly |
| NeoForge | built against 21.1.249; declared required range `[21.1.249,21.2)` |
| Java | bytecode target 21 (class major 65); Gradle toolchain Eclipse Temurin 21.0.12.1; local Gradle launcher JVM Oracle 25.0.2 |
| Gradle | wrapper 9.2.1 |
| Release command | `./gradlew clean build` or `./gradlew.bat clean build` |
| User artifact | `build/libs/scholar-1.0.0-rc.1.jar` (never the separate reference addon) |
| Artifact size / SHA-256 | 6,217,849 bytes / `5E3FC7C58702B93C58471AE2F443D31B11EF60D46CC2C605C1E09337453A4377` |

## Build and package audit

`clean build --rerun-tasks` passed, including 1706 JUnit tests (0 failures), all five architecture boundaries and separate reference-addon compilation. The final RC JAR is 6,217,849 bytes with SHA-256 `5E3FC7C58702B93C58471AE2F443D31B11EF60D46CC2C605C1E09337453A4377` and 771 entries. ZIP inspection confirmed the generated NeoForge TOML, production API/classes, font resources, two packaged OFL notices, and PDFBox/FontBox/pdfbox-io as NeoForge nested JARs. It did not contain test classes, `DevelopmentStressDocument`, `ReferenceAddon`, source `.java`, local logs, IDE files or report artifacts. No duplicate Scholar nested JAR was found. `ReleaseResourceTest` guards the font notices and product description in future builds.

The first clean worktree build exposed CRLF checkout differences in the bundled font JSON and OFL notices. `.gitattributes` now fixes LF for exactly those distributed text resources. A newly recreated detached worktree at commit `59ec803` ran `clean build --rerun-tasks` successfully; its artifact hash and size exactly match the main RC workspace. This is a packaging reproducibility fix, not a semantic/runtime change.

Generated metadata reports `modId="scholar"`, display name Scholar, RC version, product description, `All Rights Reserved`, exact Minecraft 1.21.1 and NeoForge `[21.1.249,21.2)`. Java 21 is the compiled/runtime baseline. The `authors` field remains unset pending owner confirmation; do not infer legal authorship from the repository account.

The public API contract remains V1 (`ScholarApi.majorVersion() == 1`, `minorVersion() == 0`). The supported API namespace is `dev.rgcb.scholar.api` and documented subpackages; broad `public` internal classes are not promised integration surfaces. The build compiles the separate reference addon against current Scholar source/API; this does not prove that it loads in a standalone installed instance.

## Licenses and third-party notices

There is no project `LICENSE` file. Repository README and mod TOML both say `All Rights Reserved`; **the owner must decide the project license/public-distribution terms before final 1.0.0 publication**. M39 has not chosen one. Source Sans 3 and Noto Sans Math are bundled under their own SIL OFL 1.1 notices. Those texts remain in `docs/licenses/fonts/` and are now included in the RC JAR under `META-INF/licenses/fonts/`. Each nested PDFBox component retains `META-INF/LICENSE`, `NOTICE` and `DEPENDENCIES`. No font is distributed as a standalone report attachment.

## Compatibility and qualification gates

Current native writes remain `.scholar.json` schema V2; V1 is supported for loading. M39 does not bump the document schema or add release metadata to semantic files. Existing V1/V2 decode, M30-M34 content, corrupt-file rejection, atomic-save fault injection and M38 repeated save/load tests run in the full suite. This is automated compatibility evidence, not an installed-artifact upgrade test.

At the time of this initial report, clean installation and process-exit/restart had not yet been performed. The owner subsequently accepted the exact RC artifact after independent-installation QA, including clean boot, restart/reopen, interchange, diagrams, GUI scale, reference-addon compatibility and upgrade compatibility. This acceptance does not replace final 1.0.0 artifact qualification. A Scholar-caused clean-boot failure, data loss, unsupported document load, common workflow crash, fundamental PDF/CSV failure, broken supported addon, or license/notice violation remains a release blocker.

## Clean-install and upgrade procedure

Use the **final** JAR and SHA-256 recorded in this report; any post-QA code/package edit invalidates that checksum and requires affected qualification to be repeated.

1. Create a clean Minecraft 1.21.1 client instance with NeoForge 21.1.249 (or a tested later 21.1.x) and Java 21. Put only the exact Scholar RC JAR in `mods/`; omit reference addon and unrelated gameplay mods.
2. Boot to the main menu, confirm Scholar appears in the mod list, enter a world, run `/scholar`, and verify an empty Home. Inspect `latest.log` for Scholar ERROR/WARN, exceptions, missing resources and classloading failures.
3. Create a blank document, rename it, type prose and a heading, save, return Home and reopen. Add a compact real scientific sample: equation, quantity, dataset/table/plot/analysis, Figure/caption/reference and, if convenient, variable/result and TOC.
4. Copy/paste prose and one scientific object; Undo/Redo. Check electrical and mechanical diagram display/editing. Test normal/small/large window and two GUI scales, including ribbon, dialogs, page and status bar.
5. Save, **exit Minecraft completely**, restart the same clean installation, re-enter the world, reopen the document and verify content, resource bindings, references and layout. A same-JVM close/reopen does not satisfy this gate.
6. Import and export a representative CSV; export PDF and Markdown from the installed artifact; inspect the files outside Minecraft. If practical, place one invalid `.scholar.json` beside valid saved files and verify it cannot destroy them or crash Home.
7. Separately install the M35 reference addon built against this RC; confirm it loads and `/scholar_reference_demo` creates/persists Free Fall Measurements. Do not include it in the core-only boot proof.
8. Back up a pre-RC Scholar data directory, install the RC over the accepted prior version without deleting that directory, verify Home listing, open representative older V1/V2 documents, save and reopen. Retain the backup.
9. Reinspect the installed client's `latest.log` and any crash report. Classify each Scholar-related ERROR/WARN; attach reproducible blockers to M39 before acceptance.

## Known V1 limitations / post-1.0

CSV import is bounded at 16 MiB; Markdown is lossy interchange, not native backup. Very large document layout/PDF preparation is synchronous. The addon API authors existing content only; it does not register arbitrary document blocks or unit/plugin systems. Documents are local client files, not a multiplayer server notebook. These are scope limits, not new M39 features. Streaming import and larger-document scheduling are post-1.0 candidates only after profiling.

## Current gate state

- Clean build, package inspection, metadata/notice audit: passed; final artifact identity is recorded above. Editing only this report does not change the JAR.
- Fresh detached worktree build from committed RC source: passed with a byte-identical JAR. Worktree path is a temporary qualification checkout, not the user artifact path.
- Full suite: 1706 tests, 0 failures. Transfer (3), Persistence (1), Application (1), Production Surface (6), Scholar API (3) boundary tests all passed in the clean build.
- Clean installed-artifact boot, restart, interchange, addon and upgrade: **accepted by the owner for the exact RC artifact above**.
- Project license and authors display: **human decisions pending**.

M39 was merged into `main` after manual acceptance. Do not tag or publish 1.0.0 before the separate final-release gates pass.
