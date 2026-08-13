# Agents.md – In-place Editing Flow (Disco Dingo)

> Describes the end-to-end inline editing pipeline:
> **HTMX GET → Tiptap → Modal → HTMX POST → Sling POST Servlet**

This app is zen-editable — the full HTMX + Tiptap inline editing stack (copied
verbatim from `sling-apps/zengarden/zengarden.ui.apps`, see
`docs/editing-patterns.md`) powers in-place editing for every component that
declares `sling:resourceSuperType="disco-dingo/components/editable-component"`
or `.../editable-component-modal`.

---

## Key files

| Role | Path |
|---|---|
| Page shell (modal mount point) | `src/main/content/jcr_root/apps/disco-dingo/pages/basepage/html.html` |
| View-mode component (example) | `.../components/text-block/text-block.html` |
| Edit form (toolbar + Tiptap mount) | `.../components/editable-component/edit-form-inner.html` |
| JS/CSS build config | `frontend/scripts/bundle.js` |
| TypeScript entry point | `frontend/src/typescript/editor.ts` |
| TypeScript modules | `frontend/src/typescript/editor/` (state, tiptap, toolbar, component-modal, save) |
| CSS entry (editor) | `frontend/src/css/editor/editor.css` (imports partials) |
| CSS entry (public) | `frontend/src/css/public/public.css` (imports partials, the disco visual identity) |

---

## Flow

### 1 – View mode (click to edit)

Each editable component renders with HTMX attributes, guarded by
`UserIsLoggedIn` so anonymous visitors never receive editor markup:

```html
<div data-zen-editable="true"
     hx-get="${resource.path}.edit-form.html"
     hx-trigger="click"
     hx-swap="outerMorph">
  …rendered content…
</div>
```

A click fires an **HTMX GET** to `<resource>.edit-form.html` (Sling selector).
The response **morphs** the component in place with the edit form — htmx 4's
`outerMorph` diffs the old and new DOM and patches only what changed.

### 2 – Edit form injected

`edit-form.html` (from the supertype, or overridden per-component to preserve
layout classes) renders the Tiptap mount point, hidden textarea/input pair,
and the component-properties modal. See `docs/editing-patterns.md` for the
full contract.

### 3 – Save

`saveEditorContent()` copies the Tiptap HTML into `#content-hidden`, then
triggers the HTMX form submit, which POSTs to the Sling POST Servlet at the
component's JCR path. Sling persists the properties and re-renders the
view-mode HTML, which is morphed back in.

---

## Rules for this project

- Do **not** patch generated bundles (`css/editor/*`, `css/public/*`,
  `js/editor/*`, `js/public/*`) manually — they are build output. Edit the
  sources under `frontend/src/` and run `npm run build` (or `mvn install`).
- Colours for both the public site and the editor chrome live **only** in
  `frontend/src/css/public/00-variables.css` and
  `frontend/src/css/editor/00-variables.css` — change the two `--hue-*`
  values there to re-theme the whole app.
- New editable components: follow the checklist in
  `docs/editing-patterns.md` ("New Component Checklist"), using
  `disco-dingo/components/editable-component` (richtext) or
  `disco-dingo/components/editable-component-modal` (modal-only) as the
  `sling:resourceSuperType`.
- Images referenced from HTL/sample-content live under
  `jcr_root/content/disco-dingo/<page>/images/` in the sample-content
  package — see the sample-content ReadMe section on the AI-generated photo
  convention.
