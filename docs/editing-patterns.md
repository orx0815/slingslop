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
5. Use generic field classes (`edit-field-*`) for consistent layout.
6. Ensure all field controls include `form="editor-form"`.
7. Add sample content node under `content-packages/zengarden.sample-content/...`.
8. Run build from `frontend`:

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
