# Cyberpunk Alpaca

A hypermedia-driven Sling application with a neon cyberpunk aesthetic. Built on the Slingslop mono-repo stack using HTMX for dynamic component swaps and Tiptap for inline rich-text editing.

## Overview

Cyberpunk Alpaca delivers a dark, neon-lit UI powered by an OKLCH colour system built on two hues:

- **Magenta** `oklch(0.72 0.25 330)` — primary accent, headings, CTAs
- **Cyan** `oklch(0.75 0.15 195)` — secondary accent, links, hover states

The entire palette derives from these two hue values (`--hue-primary` and `--hue-secondary`), making the theme trivially re-skinnable.

## Module Structure

```
cyberpunk-alpaca.core/         OSGi bundle — Sling Models (UserIsLoggedIn)
cyberpunk-alpaca.ui.apps/      Content package — /apps/cyberpunk-alpaca
  frontend/                    esbuild TypeScript + CSS frontend build
    src/typescript/
      public.ts                Nav toggle, scroll animations, glitch effects
      editor/                  Tiptap inline editing stack (zen-editable)
    src/css/
      public/                  Dark cyberpunk theme (OKLCH variables, components)
      editor/                  Editor UI styles
```

## Sample Content

Sample content lives in `content-packages/cyberpunk-alpaca.sample-content`:

| Path | Description |
|---|---|
| `/content/cyberpunk-alpaca/home.html` | Homepage with hero + text blocks |
| `/content/cyberpunk-alpaca/about.html` | About page |
| `/content/cyberpunk-alpaca/styleguide.html` | Design token reference |

## Pages

| Resource Type | Purpose |
|---|---|
| `cyberpunk-alpaca/pages/basepage` | Shell: `<html>`, nav, footer includes |
| `cyberpunk-alpaca/pages/homepage` | Hero component + parsys |
| `cyberpunk-alpaca/pages/contentpage` | Parsys-only content page |
| `cyberpunk-alpaca/pages/styleguide` | Static design token showcase |

## Components

| Resource Type | Supertype | Notes |
|---|---|---|
| `cyberpunk-alpaca/components/hero` | `editable-component-modal` | Modal edit: eyebrow, title, subtitle, CTAs |
| `cyberpunk-alpaca/components/text-block` | `editable-component` | Inline richtext body via Tiptap |
| `cyberpunk-alpaca/components/footer` | `editable-component-modal` | Modal edit: tagline |
| `cyberpunk-alpaca/components/parsys` | — | Container: renders child resources by resourceType |

## Build

From the repo root:

```bash
mvn clean install -DskipITs
```

To deploy a single module to a running Sling instance:

```bash
# OSGi bundle
cd cyberpunk-alpaca.core && mvn install sling:install

# UI apps package
cd cyberpunk-alpaca.ui.apps && mvn install wcmio-content-package:install

# Sample content
cd ../../content-packages/cyberpunk-alpaca.sample-content && mvn install wcmio-content-package:install
```

Or use the helper scripts in each module directory:

```bash
./content-upload.sh    # package + install to Sling
./content-download.sh  # download from Sling to local filesystem
```

## Frontend Development

```bash
cd cyberpunk-alpaca.ui.apps/frontend
npm install
npm run build            # production build (minified)
npm run build:dev        # development build (source maps)
npm run lint
npm run format
```

The build produces two bundles under `src/main/content/jcr_root/apps/cyberpunk-alpaca/`:

- `js/cyberpunk-alpaca.public.min.js` — public site bundle
- `js/cyberpunk-alpaca.public.js` — development bundle
- `js/cyberpunk-alpaca.editor.min.js` — Tiptap editor bundle (logged-in users only)
- `css/cyberpunk-alpaca.public.min.css` / `cyberpunk-alpaca.public.css`
- `css/cyberpunk-alpaca.editor.min.css` / `cyberpunk-alpaca.editor.css`

## Zen-Editable (Inline Editing)

This app includes the full HTMX + Tiptap inline editing stack:

- `editable-component` supertype — click-to-edit richtext, saves via HTMX POST
- `editable-component-modal` supertype — modal form for structured fields

Editing requires an authenticated session. The editor assets are conditionally loaded via the `UserIsLoggedIn` Sling Model.
