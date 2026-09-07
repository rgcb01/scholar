# ADR 0022: Editor State Outside Document AST

## Status

Accepted

## Context

Scholar documents are immutable semantic data. Editing requires caret state and later may require selection, preferred movement columns, composition state, and undo history. These are interaction concerns, not document content.

## Decision

Keep editor-only state outside the Document AST. The initial editor state contains the current immutable `Document` and a logical `DocumentPosition` caret.

## Alternatives Considered

- Store caret or selection state inside `Document`.
- Keep a mutable editor mirror as the primary editing model.
- Delay a distinct editor state model until later.

## Consequences

Document values remain clean, serializable, and Minecraft-independent. Editing code must explicitly carry state beside the document, but future selection and undo data can be added without changing semantic AST nodes.
