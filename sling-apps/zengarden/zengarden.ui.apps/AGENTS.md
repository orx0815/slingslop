# Agents.md – In-place Editing Flow

> Describes the end-to-end inline editing pipeline:
> **HTMX GET → Tiptap → Modal → HTMX POST → Sling POST Servlet**

---

## Key files

| Role | Path |
|---|---|
| Page shell (modal mount point) | `sling-apps/zengarden/zengarden.ui.apps/src/main/content/jcr_root/apps/slingslop/zengarden/pages/basepage/html.html` |
| View-mode component (example) | `.../components/main/explanation/explanation.html` |
| Edit form (toolbar + Tiptap mount) | `.../components/main/rich-text-block/edit-form.html` |
| JS/CSS build config | `frontend/scripts/bundle.js` |
| TypeScript entry point | `frontend/src/typescript/editor.ts` |
| TypeScript modules | `frontend/src/typescript/editor/` (state, tiptap, toolbar, component-modal, save) |
| CSS entry (editor) | `frontend/src/css/editor/editor.css` (imports 6 partials) |

---

## Flow

### 1 – View mode (click to edit)

Each editable component renders with HTMX attributes:

```html
<div data-zen-editable="true"
     hx-get="${resource.path}.edit-form.html"
     hx-trigger="click"
     hx-swap="outerHTML">
  …rendered content…
</div>
```

A click fires an **HTMX GET** to `<resource>.edit-form.html` (Sling selector).  
The response replaces the component's `outerHTML` with the edit form.

---

### 2 – Edit form injected

`edit-form.html` is rendered by Sling/HTL and contains:

- `<form id="editor-form" hx-post="${resource.path}" hx-target="…" hx-swap="outerHTML">`
- `<textarea id="content-editor" style="display:none;">` — seeded with current JCR `text` property via HTL `${properties.text @ context='html'}`.
- `<div id="tiptap-editor">` — Tiptap mount point.
- `<input type="hidden" id="content-hidden" name="text">` — populated on save.
- `#editor-component-modal` — overlay for structured fields (e.g. `headline` → `name="headline"`).
- `input[name=":redirect"]` — tells Sling POST Servlet where to redirect after write.

Because the form is injected dynamically, `htmx.process(form)` is called after swap to register its HTMX attributes.

---

### 3 – Tiptap initialisation (`htmx:afterSwap`)

```
htmx:afterSwap  (registered in editor.ts)
  └─ #tiptap-editor present?
       ├─ htmx.process(#editor-form)   ← register dynamic HTMX attrs
       ├─ initializeTiptap()           ← editor/tiptap.ts
       │    └─ new Editor({ element: #tiptap-editor,
       │                    content: #content-editor.value,
       │                    extensions: [StarterKit, Underline, Link,
       │                                 Image, TextAlign, Color, Highlight,
       │                                 Subscript, Superscript, Table*,
       │                                 Placeholder, CharacterCount, Typography] })
       ├─ setupToolbar()               ← editor/toolbar.ts: bind [data-action] buttons + heading select + colour picker
       └─ wireComponentModal()         ← editor/component-modal.ts: attach open/close handlers; move modal to #editor-modal-container
```

`document.body` receives `data-zen-editing` so CSS can style the editing state globally.

---

### 4 – Editing

| Feature | Mechanism |
|---|---|
| Rich-text marks/nodes | Toolbar buttons → `handleToolbarAction(action)` → Tiptap chain commands |
| Heading level | `<select id="heading-select">` → `setHeading` / `setParagraph` |
| Text colour | `<input type="color">` → `setColor` |
| HTML source | Toggle button ↔ `<textarea id="html-source">`; toolbar disabled in source mode |
| Structured fields | **Edit Component** button → modal (`#editor-component-modal`) with `input[name="headline"]` etc. |
| Keyboard shortcut | `Escape` closes the modal |

---

### 5 – Save (`saveEditorContent()`)

```
saveEditorContent()
  ├─ isSourceView?
  │    ├─ yes → content = #html-source.value
  │    └─ no  → content = editor.getHTML()
  ├─ #content-hidden.value = content
  └─ htmx.trigger(#editor-form, 'submit')
        └─ HTMX POST  →  resource.path
                          fields: text, headline, :redirect
```

HTMX serialises the form and **POSTs to the Sling POST Servlet** at the component's JCR path.  
Sling stores `text` and `headline` as JCR properties and then returns the updated component HTML.

---

### 6 – Teardown (`htmx:beforeSwap`)

When the POST response is about to swap in the view-mode HTML:

```
htmx:beforeSwap  (target has data-zen-editable-editing)
  ├─ hideComponentModal()
  ├─ unmountComponentModal()    ← remove modal DOM node
  ├─ destroyEditor()            ← editor.destroy(); editor = null
  └─ document.body.removeAttribute('data-zen-editing')
```

The view-mode component is swapped back via `outerHTML`, completing the round-trip.

---

## Cancel path

Both the footer **Cancel** button and the modal **Cancel** button fire an HTMX GET back to `resource.path.html`.  
`htmx:beforeSwap` still fires and cleans up identically — no manual teardown needed.

---

## Design notes

- **No full-page reload** — all transitions are `outerHTML` swaps within the live DOM.
- **Sling selector pattern** — `.edit-form.html` selector routes to the edit-form script; `.html` routes to the view script.
- **Modal portalling** — `#editor-component-modal` is moved into the global `#editor-modal-container` (in `<body>`) so it escapes any clipping `overflow` context.
- **htmx global** — in development (`?minJs=false`) htmx is loaded as a separate `htmx.js` script tag; in production it is prepended as an esbuild banner inside `editor-bundle.min.js`.
- **Frontend build** — JS and CSS are compiled by `frontend/scripts/bundle.js` using esbuild (JS) and a custom CSS `@import` resolver (CSS). Two bundles are produced: `editor` (edit mode) and `public` (placeholder). Each has a plain and a minified output. See `frontend/FRONTEND_README.md` for full details.
