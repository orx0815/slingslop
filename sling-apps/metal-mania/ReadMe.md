# Metal Mania — Sling Application

Metal band showcase built on **Apache Sling 14**, rendered with HTL, live-edited via **HTMX + Tiptap**, styled in blood-red and steel-silver OKLCH.

---

## Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Development Workflow](#development-workflow)
- [Built-in Tools (Composum)](#built-in-tools-composum)
- [Project Structure](#project-structure)
- [Architecture Overview](#architecture-overview)
- [OKLCH Colour System](#oklch-colour-system)
- [Project Genesis](#project-genesis)
- [Next Steps](#next-steps)

---

## Prerequisites

- **JDK 25** (preview features enabled in the parent POM)
- **Maven 3.9+**
- **Node.js v24 / npm 11** — managed automatically by `frontend-maven-plugin`; no manual install needed for full builds
- A modern browser (Chrome, Firefox, Safari, Edge)
- Optional: Docker for container deployment

---

## Quick Start

```bash
# Build everything from the repo root
cd /path/to/slingslop
mvn clean install -DskipITs

# Launch locally
cd launcher
./launch.sh

# Open in browser
http://localhost:8080/content/metal-mania/home.html

# Login (to activate inline editing)
http://localhost:8080/system/sling/login.html
# Default credentials: admin / admin

# Style guide
http://localhost:8080/content/metal-mania/styleguide.html
```

---

## Development Workflow

### Frontend (CSS / TypeScript changes)

```bash
cd sling-apps/metal-mania/metal-mania.ui.apps/frontend

# One-time: install dependencies
npm install

# Watch mode: rebuilds bundles on file save
npm run watch

# In a separate terminal: mount JCR content to disk for live sync
cd sling-apps/metal-mania/metal-mania.ui.apps
mvn sling:fsmount
```

Open any page with `?minLibs=no` to load unminified sources so browser devtools map directly to your TypeScript/CSS files:

```
http://localhost:8080/content/metal-mania/home.html?minLibs=no
```

### Content changes

```bash
# Download content from a running Sling instance
cd content-packages/metal-mania.sample-content
./content-download.sh

# Upload the content package to a running Sling instance
./content-upload.sh
```

### Full rebuild

```bash
# From repo root
mvn clean install -DskipITs
```

---

## Built-in Tools (Composum)

The Sling Starter bundles Composum applications:

| Tool | URL | Purpose |
|---|---|---|
| **Package Manager** | http://localhost:8080/bin/packages.html | Install, download, and manage JCR content packages |
| **Node Browser** | http://localhost:8080/bin/browser.html | Browse and edit the JCR repository (like CRX/DE in AEM) |
| **User Admin** | http://localhost:8080/bin/users.html | Manage users and permissions |

---

## Project Structure

```
sling-apps/metal-mania/
  ReadMe.md                        ← you are here
  metal-mania.core/                ← OSGi bundle (Java)
    src/main/java/mtl/mania/metal/
      slingmodels/
        UserIsLoggedIn.java        ← auth gate for editor UI
  metal-mania.ui.apps/             ← HTL templates, CSS, TypeScript, JCR config
    frontend/
      src/
        css/
          public/                  ← public CSS (variables, layout, components …)
          editor/                  ← editor CSS (modal, toolbar, tiptap …)
        typescript/
          public.ts                ← nav toggle, scroll animations
          editor.ts                ← HTMX lifecycle, Tiptap wiring
          editor/                  ← modular editor internals
    src/main/content/jcr_root/apps/metal-mania/
      pages/                       ← HTL page templates
        page/                      ← thin wrapper → delegates to jcr:content
        basepage/                  ← page shell (head, nav, footer)
        homepage/
        contentpage/
        styleguide/
      components/
        editable-component/        ← richtext inline-edit supertype
        editable-component-modal/  ← modal-edit supertype
        hero/
        text-block/
        footer/
        band-card/

content-packages/metal-mania.sample-content/
  src/main/content/jcr_root/content/metal-mania/
    home/                          ← homepage (hero + 3 band-cards + promo)
    about/                         ← content page
    styleguide/                    ← living style guide
```

---

## Architecture Overview

### 1. Resource resolution

Sling maps the URL `/content/metal-mania/home.html` to the JCR node at `/content/metal-mania/home`. That node's `sling:resourceType=metal-mania/pages/page` tells Sling to render it with the script at `/apps/metal-mania/pages/page/html.html`.

### 2. Page delegation pattern

`pages/page/html.html` immediately delegates to the `jcr:content` child:

```html
<sly data-sly-resource="${'jcr:content'}"/>
```

The `jcr:content` node carries the *real* page type, e.g. `sling:resourceType=metal-mania/pages/homepage`. Sling renders that via `pages/homepage/html.html` — or, because `homepage` has `sling:resourceSuperType=metal-mania/pages/basepage`, it falls through to `basepage/html.html` for the shell.

### 3. Component model (zen-editable)

Every editable component has two rendering modes, swapped by HTMX on click:

| State | Script rendered | What the browser shows |
|---|---|---|
| View mode | `{component}.html` | The published component HTML |
| Edit mode | `edit-form.html` | Inline Tiptap editor or a modal form |

The `editable-component` supertype provides the full Tiptap toolbar and form infrastructure. Concrete components (`hero`, `text-block`, etc.) only override `edit-form-fields.html` to add their own input fields.

### 4. Frontend build pipeline

`frontend-maven-plugin` runs during `mvn install`:

1. Installs Node v24.14.0 / npm 11.10.1 locally into `frontend/node/`
2. `npm install` — resolves all dependencies
3. `npm run copy:libs` — copies `htmx.js` into the JCR content tree
4. `npm run format` — Prettier check (run `npm run format:fix` if it fails)
5. `npm run lint` — ESLint check
6. `npm run build` — esbuild produces four output files:
   - `js/editor/editor-bundle.js` + `.min.js`
   - `js/public/public-bundle.js` + `.min.js`
   - `css/editor/editor.css` + `.min.css`
   - `css/public/public.css` + `.min.css`

### 5. Content packages

VLT (FileVault) packages zip up JCR subtrees. `metal-mania.ui.apps` covers `/apps/metal-mania` (application code). `metal-mania.sample-content` covers `/content/metal-mania` (page content). Both are bundled into `content-packages/complete` for a single-step deployment.

---

## OKLCH Colour System

The entire colour scheme is derived from two hue values in `frontend/src/css/public/00-variables.css`:

```css
--hue-primary:   22;   /* blood red */
--hue-secondary: 245;  /* steel silver */
```

Think of the OKLCH colour wheel as a clock. Change `--hue-primary` and every primary colour — buttons, accents, highlights, tinted blacks, tinted whites — shifts together. The complementary colour sits 180° opposite on the wheel.

```
--hue-primary:   22  → blood red
--hue-secondary: 245 → steel silver / cold blue-grey
Complement:      22 + 180 = 202 → teal
```

Only these two numbers need to change for a complete visual rebrand.

---

## Project Genesis

This application was generated by Agent Smith following `docs/new-sling-app.md`.

| Input | Value |
|---|---|
| **Project name** | Metal Mania |
| **groupId** | `mtl.mania` |
| **artifactId prefix** | `metal-mania` |
| **resourceType prefix** | `metal-mania` |
| **Content root** | `/content/metal-mania` |
| **Type** | Metal band showcase website |
| **Colours** | Blood red + steel silver (OKLCH hue 22 + 245) |
| **Navigation** | Fixed top bar with hamburger on mobile |
| **Zen-editable** | Yes (HTMX + Tiptap inline editing) |
| **Visual mood** | Full metal — dark near-black background, serif headings |

---

## Next Steps

This scaffolding does **not** include:

- User authentication UI (login / logout pages)
- Search functionality
- Form handling (contact, newsletter)
- Complex Sling Models beyond `UserIsLoggedIn`
- Asset management / image upload
- SEO meta tags and structured data
- Sitemap generation
- Error pages (404, 500)
- Dark mode toggle (the CSS structure supports it — add `light-dark()` variables)
- Production deployment configuration (reverse proxy, SSL termination)
- Tour dates / band detail pages

Implement these in focused, smaller iterations with specialised agents.
