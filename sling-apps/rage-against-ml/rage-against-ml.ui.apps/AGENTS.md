# AGENTS.md — rage-against-ml.ui.apps

> Module-specific context for AI coding agents. Repo-wide context: root `AGENTS.md`.

## What This Module Is

The UI content package for **Rage Against the Machine Learning** (`ratml`), a punk-zine
manifesto site. Ships HTL templates, components, and the built frontend bundles to
`/apps/ratml`.

## Key Facts

- **resourceType prefix:** `ratml` (NOT `rage-against-ml` — the Maven artifactId differs from
  the JCR namespace on purpose).
- **Java package:** `org.motorbrot.ratml` (in sibling module `rage-against-ml.core`).
- **Pages:** `page` (thin delegator) → `basepage` (shell) ← `homepage` / `contentpage` /
  `styleguide` via `sling:resourceSuperType`. Page body scripts MUST be named `content.html`.
- **Components:** `hero` (modal edit), `text-block` (inline richtext), `pull-quote` (modal),
  `navigation` (model-backed, not editable), `footer` (modal, shared via `FooterContext` —
  editable only on the homepage), `parsys` (container with add/move/delete authoring).
- **Editing supertypes** `editable-component` / `editable-component-modal` are verbatim copies
  from sling-matrix with the namespace swapped. Do NOT hand-edit or "simplify" them — the fixed
  element IDs (`tiptap-toolbar`, `inline-editor-footer`, `editor-component-modal`,
  `editor-save-error`, `content-editor`, `content-hidden`) are a contract with
  `frontend/src/typescript/editor*`.

## Frontend

- Build: `frontend/scripts/bundle.js` (esbuild) → two bundles (`public`, `editor`), each plain
  + minified, output into `src/main/content/jcr_root/apps/ratml/{js,css}` (gitignored).
- `npm run watch` + `mvn sling:fsmount` = live reload; request pages with the `.noMinLibs`
  selector for unminified sources.
- Colours: OKLCH only. Public tokens in `src/css/public/00-variables.css`
  (`--hue-primary: 27` riot red, `--hue-secondary: 80` hazard amber). Editor tokens are scoped
  (never `:root`) in `src/css/editor/00-variables.css`.
- `npm run build` (esbuild) does NOT type-check; run `npm run typecheck` when touching TS.

## Property-Name Contracts (3-way: view HTL = edit form = sample content)

| Component | Properties |
|---|---|
| hero | `eyebrow`, `title`, `subtitle`, `description`, `ctaLabel`, `ctaUrl` |
| text-block | `headline`, `text` (richtext, `context='html'`) |
| pull-quote | `quote`, `attribution` |
| footer | `copyrightText`, `link1Text`, `link1Url`, `link2Text`, `link2Url`, `link3Text`, `link3Url` |
| parsys (content node) | `allowedComponents` (multi-value, bare names or full resourceTypes) |

If you rename one, rename it in all three places — the build will NOT catch drift.

## Gotchas

- `/apps/ratml/css` and `/apps/ratml/js` carry `_rep_policy.xml` (everyone → `jcr:read`) so
  anonymous visitors can load the bundles; `pom.xml` therefore needs
  `<acHandling>merge_preserve</acHandling>`. Do not remove either half.
- Navigation is generated from the content tree by `NavigationModel` (level 1 = children of
  `home`, level 2 = dropdowns). Titles come from `jcr:content/jcr:title` or folder `jcr:title`.
- Sample content lives in `content-packages/rage-against-ml.sample-content`; after inline
  editing on a running instance, pull changes back with its `content-download.sh`.
