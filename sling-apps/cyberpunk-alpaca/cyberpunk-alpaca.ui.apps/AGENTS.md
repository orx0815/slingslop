# AGENTS.md — cyberpunk-alpaca.ui.apps

Module-specific context for the Cyberpunk Alpaca UI package.

## What lives here

- `src/main/content/jcr_root/apps/cyberpunk-alpaca/` — HTL templates, components, and
  the compiled `css/` + `js/` bundles (the compiled bundles are git-ignored; the
  frontend build regenerates them).
- `frontend/` — TypeScript + CSS sources built by esbuild via `frontend-maven-plugin`.
  Two bundles are produced: `public` (every visitor) and `editor` (authenticated
  authors only).

## Inline editing flow (HTMX + Tiptap)

1. A component view (e.g. `components/main/text-block/text-block.html`) exposes
   `data-zen-editable="true"` plus `hx-get="${resource.path}.edit-form.html"` — but
   **only when the user is logged in** (`UserIsLoggedIn` Sling model).
2. Clicking the component swaps its markup (HTMX `outerMorph`) with the edit form
   from the editing supertype (`components/editable-component` for richtext,
   `components/editable-component-modal` for modal-only).
3. `editor-bundle.js` initialises Tiptap on `#tiptap-editor`, seeding it from the
   hidden `#content-editor` textarea, and mirrors the HTML into `#content-hidden`
   before the HTMX POST back to `${resource.path}`.

## Editing element-ID contract (do not rename)

Richtext components' `edit-form-fields.html` MUST contain:

```html
<textarea id="content-editor" style="display:none;">${properties.text @ context='html'}</textarea>
<input type="hidden" id="content-hidden" name="text" form="editor-form" />
```

The editor JS looks for these exact IDs. See `docs/editing-patterns.md` at the repo root.

## Build

```bash
mvn -pl sling-apps/cyberpunk-alpaca/cyberpunk-alpaca.ui.apps install
# or from this dir, live-reload frontend:
cd frontend && npm run watch
```
