# AGENTS.md — Sling Matrix UI Apps

## Module Context

This is the `sling-matrix.ui.apps` module — the JCR content package for the **Sling Matrix** documentation site.

## What's Here

- **HTL templates** — Page scripts and component templates at `/apps/sling-matrix/`
- **Frontend assets** — CSS and JavaScript bundles (built from `frontend/` sources)
- **Component library** — Editable components with Matrix-themed styling

## Project Theme

Sling Matrix is a documentation site inspired by the 1999 Matrix movie aesthetic:
- **Dark background** with **light-green** (#00ff41) text
- **Digital rain** effect and monospace fonts
- **Circular navigation** (top-left, opens on mouse-over)
- **Code highlighting** component for documentation snippets
- **Editable footer** on homepage that content pages inherit

## Frontend Build

TypeScript and CSS sources live in `frontend/src/`:
- `typescript/editor.ts` — Tiptap inline editing (authenticated users only)
- `typescript/public.ts` — Public-facing interactions (navigation, code highlighting)
- `css/public/` — Matrix-themed design system with OKLCH colour tokens
- `css/editor/` — Inline editing UI

Build outputs are generated into `src/main/content/jcr_root/apps/sling-matrix/css/` and `/js/` during Maven build.

## Development Workflow

```bash
# Watch mode for CSS/JS changes
cd frontend && npm run watch

# Mount JCR content for live reload
mvn sling:fsmount

# Upload changes to running Sling
./content-upload.sh
```

## Component Editing

This project uses **zen-editable** components with Tiptap rich-text editing. See `/home/runner/work/slingslop/slingslop/docs/editing-patterns.md` for the full editing contract.

## Inline Editing Lifecycle

See the zengarden AGENTS.md for the full HTMX + Tiptap editing flow. Sling Matrix follows the same pattern:

1. Authenticated user clicks an editable component
2. HTMX swaps the component view with the edit form
3. Tiptap editor initializes for richtext fields
4. User saves → HTMX POST → component re-renders with new content
