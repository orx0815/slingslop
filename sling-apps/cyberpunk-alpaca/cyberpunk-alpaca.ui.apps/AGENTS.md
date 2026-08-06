# Cyberpunk Alpaca — ui.apps Agent Context

## Module Overview

This module contains the HTL templates, CSS, JavaScript, and JCR component definitions for the **Cyberpunk Alpaca** Sling application.

## Key Paths

| JCR Path | Purpose |
|---|---|
| `/apps/cyberpunk-alpaca/pages/` | Page rendering scripts |
| `/apps/cyberpunk-alpaca/components/` | Component scripts |
| `/apps/cyberpunk-alpaca/css/` | Built CSS bundles (public + editor) |
| `/apps/cyberpunk-alpaca/js/` | Built JS bundles (public + editor) |

## Frontend Build

Located in `frontend/`. Uses esbuild via Node.js.

```bash
cd frontend
npm install
npm run build     # full build (dev + minified)
npm run watch     # watch mode (dev builds only)
```

## Inline Editing (Zen-editable)

This project uses the HTMX + Tiptap inline editing stack. See `docs/editing-patterns.md` for the component contract.

## ResourceType Prefix

All `sling:resourceType` values use the prefix `cyberpunk-alpaca` (e.g. `cyberpunk-alpaca/pages/homepage`).
