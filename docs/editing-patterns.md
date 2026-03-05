# Editing Patterns

This project uses reusable HTMX-based inline editing patterns for Sling components.

## Editing Supertypes

Richtext supertype:
- `slingslop/zengarden/components/editable-component`

Modal-only supertype:
- `slingslop/zengarden/components/editable-component-modal`

Reusable contract:
- Reuse supertype `edit-form.html`
- Override only `edit-form-fields.html` in concrete components
- Every form field control must set `form="editor-form"` (`input`, `textarea`, `select`, etc.)

For richtext-enabled components, include in `edit-form-fields.html`:
- `<textarea id="content-editor">...</textarea>` with initial HTML
- `<input type="hidden" id="content-hidden" name="text" />`

## Modal-Only Components

If a component wants the same modal/save flow **without** Tiptap, use:
- `sling:resourceSuperType="slingslop/zengarden/components/editable-component-modal"`

Editor behavior (implemented in frontend TS):
- Detects modal-only mode by missing `#tiptap-editor` in the swapped form
- Opens component modal directly
- Saves via normal HTMX submit (no richtext serialization required)

Reference implementation:
- `components/footer/edit-form-fields.html`

## Preserving Layout During Inline Editing (Richtext Only)

When a component's view element carries CSS classes or semantic attributes (e.g. `class="preamble"`, `id="zen-preamble"`, `role="article"`), replacing it wholesale with the supertype's plain `<div data-zen-editable-editing="true">` breaks the layout during editing.

For richtext supertype components, `edit-form.html` is intentionally split:

| File | Location | Purpose |
|---|---|---|
| `edit-form.html` | supertype or **override in component** | wrapper element only |
| `edit-form-inner.html` | supertype only | full form/Tiptap/modal body |

To preserve layout, add `edit-form.html` to the concrete component with the correct wrapper element and delegate the body via `data-sly-include`:

```html
<div class="preamble" id="zen-preamble" role="article" data-zen-editable-editing="true">
  <sly data-sly-include="${'edit-form-inner.html'}" />
</div>
```

Sling resolves `edit-form-inner.html` up the supertype chain automatically — no copy needed in the component.

This override is **only needed for the richtext supertype**. Modal-only components (`editable-component-modal`) render their modal on top of the page, so the wrapper element's classes are irrelevant.

Reference implementations:
- `components/intro/summary/edit-form.html`
- `components/intro/preamble/edit-form.html`

## Generic Field Row Markup

Use these reusable classes for edit forms:
- `edit-fields-list`
- `edit-fields-group`
- `edit-fields-grid`
- `edit-field-row`
- `edit-field-meta`
- `edit-field-label`
- `edit-field-description`
- `edit-field-input`

Layout intent:
- label + description on the left
- input on the right
- stack on small screens

## Build Rule

When changing frontend TS/CSS, do **not** patch generated bundles manually.

Always rebuild from:

```bash
cd sling-apps/zengarden/zengarden.ui.apps/frontend
npm run build
```

This regenerates:
- `src/main/content/.../css/editor/editor.css` + `.min.css`
- `src/main/content/.../js/editor/editor-bundle.js` + `.min.js`

## New Component Checklist

1. Create component folder with `.content.xml` and set `sling:resourceSuperType`.
2. Add view HTL (for clickable editing):
	- `data-zen-editable="true"`
	- `hx-get="${resource.path}.edit-form.html"`
	- `hx-swap="outerHTML"`
3. Add `edit-form-fields.html` only (reuse supertype `edit-form.html`).
4. Choose mode:
	- Richtext: add `#content-editor` and `#content-hidden`.
	- Modal-only: use `editable-component-modal` as `sling:resourceSuperType`.
5. **(Richtext only)** If the view element carries CSS classes/id/role that must be preserved during editing, add `edit-form.html` with the correct wrapper element and `<sly data-sly-include="${'edit-form-inner.html'}" />`. Skip this for modal-only components.
6. Use generic field classes (`edit-field-*`) for consistent layout.
7. Ensure all field controls include `form="editor-form"`.
8. Add sample content node under `content-packages/zengarden.sample-content/...`.
9. Run build from `frontend`:

```bash
cd sling-apps/zengarden/zengarden.ui.apps/frontend
npm run build
```

## Starter Snippets

### `edit-form-fields.html` (Richtext)

```html
<div class="edit-fields-list">
	<div class="edit-fields-group">
		<div class="edit-fields-grid">
			<label class="edit-field-row">
				<span class="edit-field-meta">
					<span class="edit-field-label">Headline</span>
					<small class="edit-field-description">Main heading shown above body content.</small>
				</span>
				<input type="text" name="headline" value="${properties.headline}" class="edit-field-input" form="editor-form" />
			</label>
		</div>
	</div>
</div>

<textarea id="content-editor" style="display:none;" form="editor-form">${properties.text @ context='html'}</textarea>
<input type="hidden" id="content-hidden" name="text" form="editor-form" />
```

### `edit-form-fields.html` (Modal-Only)

```html
<div class="edit-fields-list">
	<div class="edit-fields-group">
		<div class="edit-fields-grid">
			<label class="edit-field-row">
				<span class="edit-field-meta">
					<span class="edit-field-label">Label</span>
					<small class="edit-field-description">Description for authors.</small>
				</span>
				<input type="text" name="myProperty" value="${properties.myProperty}" class="edit-field-input" form="editor-form" />
			</label>
		</div>
	</div>
</div>
```

### View HTL (Clickable to Edit)

```html
<div data-zen-editable="true"
		 hx-get="${resource.path}.edit-form.html"
		 hx-trigger="click"
		 hx-swap="outerHTML"
		 style="cursor: pointer;"
		 title="Click to edit">
	...rendered output...
</div>
```

### `edit-form.html` (Layout-Preserving Wrapper — Richtext Only)

Only needed when the view element has classes/id/role that must survive the swap into editing mode:

```html
<div class="my-section" id="zen-my-section" role="article" data-zen-editable-editing="true">
  <sly data-sly-include="${'edit-form-inner.html'}" />
</div>
```
