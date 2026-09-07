# M19F Runtime Fix

The M19F Diagram menu referenced `DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE`,
but `ScholarEditorScreen` did not register that action in its local action list.
Opening the editor therefore reached `action(...).orElseThrow()` and raised
`NoSuchElementException`.

The fix registers the missing action. M19F semantics are unchanged.

Validation:
- Java 21 core compilation: pass
- Client compilation against narrow Minecraft/NeoForge stubs: pass
- Screen sanity check confirms Add Part Reference, Generate BOM, and Delete Part Reference
  are each both registered and referenced by the menu.
