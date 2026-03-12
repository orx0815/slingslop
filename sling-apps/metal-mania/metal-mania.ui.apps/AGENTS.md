# AGENTS.md — Metal Mania UI Apps

## Module Overview

This is the `ui.apps` module for **Metal Mania** — a metal band showcase built on Apache Sling with HTMX and Tiptap inline editing.

## Key Paths

| JCR Path | Purpose |
|---|---|
| `/apps/metal-mania/pages/` | Page rendering scripts (HTL) |
| `/apps/metal-mania/components/` | Component scripts and editing forms |
| `/apps/metal-mania/css/` | Build output: CSS bundles (generated) |
| `/apps/metal-mania/js/` | Build output: JS bundles (generated) |

## resourceType Prefix

All `sling:resourceType` values use the prefix `metal-mania/`, e.g.:
- `metal-mania/pages/page` — thin-wrapper delegation page
- `metal-mania/pages/homepage` — homepage (supertype: basepage)
- `metal-mania/components/hero` — hero component
- `metal-mania/components/text-block` — richtext text block (zen-editable)
- `metal-mania/components/footer` — footer (modal-editable)

## Java Class for Auth Gating

`mtl.mania.metal.slingmodels.UserIsLoggedIn` — use in HTL `data-sly-use.auth` to gate editor markup.

## Editing Pattern

Components that support inline editing use `data-zen-editable="true"` and HTMX attributes to swap in an edit form on click. See `docs/editing-patterns.md` for the full contract.

## Frontend Build

```bash
cd frontend
npm install
npm run watch        # watch mode, pairs with mvn sling:fsmount
npm run build        # full production build
```

Open pages with `?minLibs=no` to load unminified bundles in the browser.

## Colour System

Primary hue: **22** (blood red). Secondary hue: **245** (steel silver).
Change only `--hue-primary` and `--hue-secondary` in `frontend/src/css/public/00-variables.css` to completely retheme the site.
