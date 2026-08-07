# AGENTS.md — Alf vs Agent Smith UI Apps

> Context for AI coding agents working in this module.

## Module Overview

This is the `ui.apps` content package for the **Alf vs Agent Smith** Sling application. It contains HTL templates, components, CSS, JavaScript, and the frontend build configuration.

## Key Conventions

- **`sling:resourceType` prefix:** `alf-vs-agent`
- **Apps path:** `/apps/alf-vs-agent`
- **Page delegation:** content node → `pages/page/html.html` → delegates to `jcr:content` child
- **Component supertypes:** `editable-component` (richtext) and `editable-component-modal` (modal-only) — zen-editable
- **Frontend build:** esbuild — TypeScript + CSS, two bundles (editor / public)

## Inline Editing

This project uses the HTMX + Tiptap inline editing stack. See `docs/editing-patterns.md` at the repo root for the full component contract and editing lifecycle.

## Frontend Build

```bash
cd frontend
npm install
npm run build      # Compile TypeScript + CSS
npm run watch      # Watch mode
npm run check      # Format + lint + typecheck
```
