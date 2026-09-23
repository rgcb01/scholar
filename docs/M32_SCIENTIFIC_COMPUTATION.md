# M32 Scientific Variables and Computed Content

Status: complete and manually accepted. The production QA procedure below is retained for regression checks.

## Semantic boundary

`VariableDefinition` is a document block with a stable variable ID, authored name, and M31 scientific value. `ComputedResult` is a distinct document block whose authored expression AST stores `VariableDependencyReference` IDs. Ordinary `EquationBlock` and `MathIdentifier` remain publication content and are never evaluated.

Name resolution occurs when an expression is authored or edited, across the entire document regardless of block order. An unresolved name also binds when a uniquely named variable is subsequently inserted or renamed, as part of that same editor transaction. Once bound, dependency identity is the variable ID, not its name. Renaming a variable or later creating a duplicate name therefore leaves existing computations bound. Unknown or ambiguous names remain recoverable authored expressions with diagnostics; ambiguous names are never guessed. Editing an expression retries name resolution.

## Evaluation and history

Evaluation uses BigDecimal, M31 quantities/units, and `QuantityArithmeticPolicy`. `ComputationEngine` caches derived results by immutable computed block and dependent variable values. Changing an unrelated variable does not reevaluate a result. The derived cache is not part of `Document`, persistence, or editor history. Undo/redo restores authored document snapshots; the cache is refreshed from the restored document.

The current dependency shape is variables to computed results only. A `ComputedResult` does not define a variable, so computed-to-computed dependency chains and cycles are not representable in M32. Cycle detection is intentionally absent. General uncertainty propagation is also absent: measured values can be displayed and referenced directly, but arithmetic requiring propagation reports `UNSUPPORTED_OPERATION`.

## Interchange

Persistence V2 stores authored variable values, expression trees, stable references, and display preferences, not evaluated results. M25 transfer remaps IDs when a variable travels with its dependent result, preserves a result-only external dependency only with same-document proof, and rejects unsafe cross-document transfer atomically. Plain text and Markdown are lossy readable exports without stable IDs.

## Production QA

Use only `/scholar`. Create a document, use Insert > Computation to add `m = 2.5 kg`, `g = 9.81 m/s²`, `h = 1.2 m`, then a Computed Result `m*g*h`. Check `29.43 J`; edit `h` to `2 m`, check `49.05 J`, undo/redo, and rename `m` to `mass`. Add `room = 20 °C` and a temperature-difference `increase = 5 °C` through the value-kind control; `room+increase` should show `25 °C`. Check `30 °C - 20 °C` shows `Δ10 °C` and `5 m + 3 s` shows a diagnostic. Save, return Home, reopen, and verify results recompute. Insert an ordinary equation and confirm it remains non-computational. Capture screenshots from the real production editor before manual acceptance.
