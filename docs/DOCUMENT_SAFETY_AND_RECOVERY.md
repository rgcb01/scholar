# Document Safety and Recovery

Scholar treats Save, recovery, and backup as separate operations:

- **Save confirms work.** It writes the current semantic `Document` to canonical storage and advances the workspace clean baseline.
- **Recovery protects work.** It records an unconfirmed immutable snapshot outside canonical storage and never changes dirty state or history.
- **Backups protect confirmed work.** Before a changed canonical document is replaced, the previous validated canonical version is preserved.

## Lifecycle map

`ScholarApiRuntime` owns one `ScholarApplication` for the active Minecraft game directory. `ScholarApplication` owns the repository facade and weakly tracks open `ApplicationDocumentWorkspace` instances. A workspace owns its `EditorSession`, current library descriptor, clean semantic baseline, recovery identity, and semantic revision counter. The screen only renders and dispatches commands.

`EditorSession` emits a document-change callback after committed semantic edits, Undo, and Redo. The workspace increments its recovery revision from that callback. Caret, hover, menus, dialogs, animation, layout caches, and other presentation state are not recovery changes.

Dirty means that the current semantic document differs from the last successful canonical Save, or that a recovered snapshot is explicitly marked unconfirmed. Recovery never changes this definition. Successful Save advances the baseline and clears the unconfirmed flag. Failed Save leaves the baseline, dirty state, history, and recovery intact.

New documents are created through the accepted template catalog and receive a stable library ID and initial canonical factory document. Until their first explicit Save, their recovery metadata identifies them as originally untitled. The recovery service also supports a snapshot with no canonical ID, so orphaned and genuinely transient documents remain recoverable.

Open creates a fresh `EditorSession`. Save keeps the application document ID. Save As creates a distinct canonical ID. Rename changes library metadata only. Close requests an immediate recovery capture for dirty work before releasing application ownership. Home and editor transitions do not own persistence scheduling.

## Storage layout

Under the existing `<game>/scholar` root:

```text
scholar/
  workspace.json
  documents/
    <document-id>.scholar.json
  recovery/
    <recovery-id>.recovery.json
  backups/
    <document-id>/
      <saved-epoch-millis>-<sequence>.scholar.json
```

Display names never form paths. `ScholarDocumentId` and `RecoveryId` use the existing restricted filesystem-safe identity rules. Recovery and backup operations are scoped to one exact identity; no broad directory deletion is used.

## Recovery envelope

Recovery uses format `scholar-recovery`, version `1`. Its envelope contains the recovery ID, optional canonical document ID, originally-untitled flag, display name, capture time, optional last-confirmed modification time, semantic revision, and a normal canonical document JSON payload. `DocumentJsonCodec` remains the sole serializer and validator for the document itself; the canonical `scholar-document` format remains version 2 and is unchanged.

Recovery writes create and flush a same-directory temporary file, decode it again for validation, and then atomically replace the prior candidate where supported. A failed write leaves the prior candidate and canonical document untouched. Discovery removes only Scholar-owned orphan temporary files. Malformed and unsupported future envelopes are retained, diagnosed, and skipped so one bad file cannot block Home.

Each open workspace has one recovery stream. The application captures only dirty work whose semantic revision changed since the last successful capture. The default interval is 30 seconds. Client ticks invoke the application policy; screen code does not schedule recovery. Dirty close and game shutdown request an immediate capture as a best effort, but correctness does not rely on graceful shutdown.

Capture takes an immutable `Document` snapshot and performs serialization and local atomic I/O synchronously on the Minecraft client thread. This avoids concurrent access to editor state and out-of-order recovery writes. The interval and revision gate bound the work; moving encoding/I/O to a serial worker remains an extension point if measured real-world documents show a noticeable pause.

## Startup and recovery

Home asks `ScholarApplication` for `RecoveryCandidate` values and never parses files. Candidates are sorted newest first and report existing canonical source, missing source, or untitled origin. Home presents one restrained recovery strip with Recover, Discard, and navigation for multiple candidates. Nothing restores silently.

Recover validates the envelope and canonical payload, then opens a fresh-history workspace containing the recovered document. If the canonical source still exists, it remains the clean comparison baseline. If it changed after capture, both versions remain safe: recovery opens dirty and a later explicit Save uses normal Save behavior, whose backup preserves the then-current canonical version. Missing-source and untitled recovery open as unconfirmed work and use Save As semantics when confirmed.

Discard deletes only the selected recovery file. It does not touch canonical documents, backups, or other candidates. Successful Save discards the workspace's own recovery stream after canonical commit. Recovery cleanup failure is a warning and cannot turn a successful canonical Save into a failed one. Failed Save never discards recovery.

Undo/Redo stacks are intentionally not persisted. A recovered document starts with a fresh history, then behaves like any editable document.

## Backup ordering and retention

For a changed existing document, the repository:

1. loads and validates the current canonical document;
2. writes that previous version to its document-specific backup directory;
3. atomically saves the new canonical document through `FileDocumentStorage`;
4. rolls back the newly prepared backup if canonical Save fails;
5. advances workspace metadata and clean state only after canonical success;
6. prunes old backups after success.

Unchanged Saves do not create redundant backups. Five backups are retained per document. A millisecond timestamp plus a deterministic sequence prevents collisions during rapid Saves. A backup creation failure aborts Save before canonical replacement. A pruning failure becomes a warning and does not invalidate the already successful canonical Save. `DocumentBackupService.list/load` provides the programmatic extension point for a future backup browser; no backup UI is included now.

## Manual QA

Use disposable documents only for abrupt-termination checks.

1. Existing document: save state A, edit, wait at least 30 seconds, terminate the client without closing Scholar, restart, Recover from Home, verify edits and Unsaved status, explicitly Save, restart, and verify the candidate is gone.
2. New document: create from Blank, edit, wait at least 30 seconds, terminate, restart, Recover, verify content, then Save normally.
3. Discard: create disposable recoverable edits, restart, choose Discard, and verify the canonical document is unchanged.
4. Backups: Save state A, edit, Save state B, and inspect `scholar/backups/<document-id>`; the backup decodes to A while the canonical document decodes to B.
5. Localization: repeat the Home recovery surface in `en_us` and `es_mx`; verify the accepted Scientific Instrument styling and that long labels do not overlap.
6. Corrupt-file isolation: copy a disposable recovery file, rename the copy to a different valid recovery filename, replace its contents with `{broken`, restart, and verify Home still opens and other valid candidates remain available. Delete only that disposable corrupt copy after the check.

Known limitation: backups are programmatically inspectable but have no version-history browser. Recovery diagnostics are summarized in Home while detailed persistence diagnostics remain available to application tests/logging boundaries.
