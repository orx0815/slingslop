# TypeScript / Frontend Configuration

This module uses TypeScript (type-checking only), **esbuild** for bundling, ESLint for code quality, and Prettier for formatting.

## File Structure

```
frontend/
├── src/typescript/
│   └── editor-modal.ts        # Editor logic (Tiptap init, toolbar, modal lifecycle)
├── scripts/
│   └── bundle.js              # esbuild-based build script
├── tsconfig.json              # TypeScript config (type-check only, noEmit)
├── .eslintrc.js               # ESLint configuration
├── .prettierrc                # Prettier configuration
└── package.json               # Dependencies and scripts
```

Output files (written to `../src/main/content/jcr_root/apps/slingslop/zengarden/js/`):

| File | Contents | Used in |
|---|---|---|
| `editor-bundle.js` | Tiptap + editor code (unminified) | dev mode (`?minJs=false`) |
| `bundle.min.js` | htmx + Tiptap + editor code (minified) | production |
| `htmx.js` / `htmx.min.js` | htmx (copied from node_modules) | dev mode |

## NPM Scripts

| Script | What it does |
|---|---|
| `npm run build` | Copy htmx (`prebuild`), then run `scripts/bundle.js` via esbuild |
| `npm run typecheck` | Type-check TypeScript without emitting files (`tsc --noEmit`) |
| `npm run copy:htmx` | Copy htmx.js / htmx.min.js from node_modules to JCR content |
| `npm run lint` | Run ESLint |
| `npm run lint:fix` | Run ESLint with auto-fix |
| `npm run format` | Check formatting with Prettier |
| `npm run format:fix` | Auto-format with Prettier |
| `npm run check` | Run all quality gates: `format` + `lint` + `typecheck` |

> **Note**: `tsc` is used for type-checking only. esbuild handles compilation and bundling.

## Build Pipeline

```mermaid
flowchart LR
    A[src/typescript/editor-modal.ts] --> B[esbuild]
    C[node_modules/@tiptap/*] --> B
    D[node_modules/htmx.org/] --> E[copy:htmx]
    E --> F[js/htmx.js]
    B --> G[js/editor-bundle.js\nunminified dev bundle]
    D --> H[esbuild banner prepend]
    A --> H
    C --> H
    H --> I[js/bundle.min.js\nminified prod bundle]
```

## Dependency Management

### Tiptap (Rich Text Editor)
All Tiptap packages are MIT-licensed and bundled via esbuild — no CDN, no API key.

**Core**
- `@tiptap/core` — editor engine
- `@tiptap/starter-kit` — bold, italic, headings H1–H6, ordered/unordered lists, blockquote, inline code, code block, horizontal rule, undo/redo

**Inline formatting**
- `@tiptap/extension-underline` — underline
- `@tiptap/extension-subscript` — subscript
- `@tiptap/extension-superscript` — superscript
- `@tiptap/extension-text-style` + `@tiptap/extension-color` — text colour picker
- `@tiptap/extension-highlight` — multi-colour highlighting

**Block / layout**
- `@tiptap/extension-text-align` — left / center / right / justify for paragraphs and headings
- `@tiptap/extension-link` — insert, edit, and remove hyperlinks
- `@tiptap/extension-image` — insert images by URL

**Tables**
- `@tiptap/extension-table` + `extension-table-row` + `extension-table-cell` + `extension-table-header` — full table editing with column-resize handles and cell merge/split

**UX helpers**
- `@tiptap/extension-placeholder` — placeholder text when editor is empty
- `@tiptap/extension-character-count` — live word & character count bar
- `@tiptap/extension-typography` — smart quotes, em-dashes, ellipsis auto-replacement

### htmx
- **Source**: `node_modules/htmx.org/dist/`
- **Destination**: `js/htmx.js` and `js/htmx.min.js` (copied by `copy:htmx`)
- **Version**: `^1.9.10` (see `package.json`)
- In prod (`bundle.min.js`) htmx is prepended as a banner by esbuild so it is available as `window.htmx` before the editor IIFE runs.

To update a library:
1. Change the version in `package.json`
2. Run `npm install`
3. Run `npm run build`

## JavaScript Loading Modes

### Development Mode
Add `?minJs=false` to any page URL, e.g. `http://localhost:8080/content/page.html?minJs=false`

Loads three separate, unminified files:
- `htmx.js`
- `editor-bundle.js` (Tiptap + editor code)

### Production Mode (default)
Loads a single minified file: `bundle.min.js` (htmx + Tiptap + editor code).

**Benefits**:
- ✅ Single HTTP request
- ✅ Minified and tree-shaken by esbuild
- ✅ No separate TinyMCE asset tree to serve

## Editor Features

The Tiptap toolbar in `edit-form.html` exposes:

- **Undo / Redo**
- **Heading level** select (Paragraph, H1–H6)
- **Inline**: Bold, Italic, Underline, Strikethrough, Inline code, Subscript, Superscript
- **Colour & highlight**: colour picker, multi-colour highlight, clear formatting
- **Alignment**: Left, Center, Right, Justify
- **Lists & blocks**: Bullet list, Ordered list, Blockquote, Code block, Horizontal rule
- **Link**: prompt-based insert / edit / remove
- **Image**: prompt-based insert by URL
- **Tables**: insert, add/delete columns and rows, merge/split cells, delete table
- **HTML source toggle**: switch between WYSIWYG and raw HTML editing
- **Character / word count** bar (live)

Content is saved as HTML into Sling via the existing htmx form POST.

## Maven Integration

The project uses `frontend-maven-plugin`:

1. **generate-resources**: Install Node.js + npm, run `npm install`
2. **process-resources**: Prettier format check + ESLint
3. **compile**: Run `npm run build` → copies htmx, bundles dev + prod outputs via esbuild

`mvn clean install` produces `editor-bundle.js` and `bundle.min.js` in the JCR content directory.

## Configuration Files

### tsconfig.json
- `"noEmit": true` — type-checking only; esbuild does the emit
- `"module": "ESNext"` / `"moduleResolution": "bundler"` — required for esbuild-resolved imports
- `"target": "ES2020"`
- Source root: `src/typescript/`

### .eslintrc.js
- TypeScript-aware linting with `@typescript-eslint`
- Prettier integration (formatting errors reported as lint errors)

### .prettierrc
- Single quotes, 2-space indentation, 100-character line width, ES5 trailing commas

## Development Workflow

1. Edit `src/typescript/editor-modal.ts`
2. `npm run check` — format + lint + type-check
3. `npm run build` — produce the JS bundles
4. Or just `mvn compile` for the full Maven pipeline

## Troubleshooting

| Symptom | Fix |
|---|---|
| Node/npm not found | Run `mvn clean` to clear and reinstall Node |
| Type errors | Check `tsconfig.json`; run `npm run typecheck` for details |
| Lint errors | Run `npm run lint:fix` |
| Formatting errors | Run `npm run format:fix` |
| `editor-bundle.js` / `bundle.min.js` missing | Run `npm run build` |
| htmx missing at runtime (dev mode) | Run `npm run copy:htmx` |
| Tiptap extensions not working | Ensure `npm install` completed and `editor-bundle.js` was rebuilt |
