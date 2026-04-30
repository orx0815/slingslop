# Sling Matrix

> Enter the Sling. A Matrix-themed documentation site for Apache Sling, OSGi, and JCR — built with hypermedia-driven components and inline editing.

## What Is This?

Sling Matrix is a documentation and demonstration site showcasing Apache Sling's capabilities with a 1999 Matrix movie aesthetic. It features:

- **Dark theme** with Matrix green (#00ff41) on black backgrounds
- **Digital rain** effect on hero sections
- **Circular navigation** (top-left, opens on mouse-over)
- **Inline editing** with Tiptap rich-text editor (zen-editable)
- **Code syntax highlighting** for documentation snippets
- **Editable footer** on homepage, inherited by all content pages

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Apache Sling 14 on Felix OSGi + Oak JCR |
| Templates | HTL (Sightly) — server-side HTML rendering |
| Hypermedia | HTMX — component-level GET/POST |
| Inline editing | Tiptap rich-text editor |
| Frontend | TypeScript + CSS (esbuild bundler) |
| Code highlighting | highlight.js |
| Build | Maven multi-module (JDK 25) |

## Prerequisites

- **JDK 25** (preview features enabled in parent POM)
- **Maven 3.9+** (or use the Maven wrapper from the root)
- **Git** for version control
- A modern browser (Chrome, Firefox, Safari, Edge)
- Optional: Docker for container deployment

## Quick Start

### Build everything (from repository root)

```bash
mvn clean install -DskipITs
```

### Launch the application

```bash
cd launcher
./launch.sh
```

### Open in browser

- **Homepage:** http://localhost:8080/content/sling-matrix/home.html
- **OSGi Intro:** http://localhost:8080/content/sling-matrix/osgi/osgi-intro.html
- **JCR Intro:** http://localhost:8080/content/sling-matrix/jcr/jcr-intro.html
- **Sling Intro:** http://localhost:8080/content/sling-matrix/sling/sling-intro.html

### Login for inline editing

Navigate to http://localhost:8080/ and log in with:
- **Username:** admin
- **Password:** admin

Once logged in, click any editable component to modify it inline.

## Documentation Structure

The site includes three main documentation sections:

### OSGi
- Introduction to OSGi
- OSGi Declarative Services
- Unit Testing with OSGi

### JCR
- Introduction to JCR
- JCR Queries

### Sling
- Apache Sling Introduction
- Sling Components and Script Resolution
- Sling Resource API
- Sling Models
- Unit Testing with Sling
- HTL (Sightly) Templating
- Sling i18n
- Sling Context-Aware Configuration

## Development Workflow

### Frontend development (CSS/JS changes)

```bash
cd sling-apps/sling-matrix/sling-matrix.ui.apps/frontend

# One-time: install dependencies
npm install

# Watch mode: rebuilds on file save
npm run watch
```

In a separate terminal, mount JCR content for live reload:

```bash
cd sling-apps/sling-matrix/sling-matrix.ui.apps
mvn sling:fsmount
```

Now edit `frontend/src/` files → auto-rebuild → auto-sync to Sling.
Open pages with `?minLibs=no` to load unminified sources.

### Content changes

Download content from running Sling:

```bash
cd content-packages/sling-matrix.sample-content
./content-download.sh
```

Upload content package to running Sling:

```bash
./content-upload.sh
```

### Full rebuild

```bash
mvn clean install -DskipITs
```

## Built-in Tools (Composum)

The Sling Starter includes Composum applications:

- **Package Manager:** http://localhost:8080/bin/packages.html
  Install, download, and manage JCR content packages

- **Node Browser:** http://localhost:8080/bin/browser.html
  Browse and edit the JCR repository (similar to CRX/DE in AEM)

- **User Admin:** http://localhost:8080/bin/users.html
  Manage users and permissions

## OKLCH Colour System

The entire design is built on two base hues in OKLCH colour space:

- **Primary hue:** 142° (Matrix green)
- **Secondary hue:** 281° (Stiffkey Blue)

All other colours — including blacks, whites, and neutrals — are derived by adjusting Lightness and Chroma while preserving the hue relationship. This creates a cohesive colour scheme where every element carries a subtle tint of the primary colour.

To retheme the site, change just two CSS variables in `frontend/src/css/public/00-variables.css`:

```css
:root {
  --hue-primary: 142;    /* Change this for a different primary colour */
  --hue-secondary: 281;  /* Change this for a different accent */
}
```

All derived colours (hover states, borders, shadows, text) will shift automatically.

## Project Genesis

This project was scaffolded by **Agent Smith** with the following requirements:

- **Project name:** Sling Matrix
- **Type:** Documentation site
- **Theme:** Matrix 1999 movie (dark background, green letters, digital rain)
- **Navigation:** Circular (top-left, opens on mouse-over)
- **Editable:** Yes (zen-editable with Tiptap + HTMX)
- **Mood:** Playful
- **Colours:** Matrix green + Stiffkey Blue (281)

### Specific Features Requested

1. **Editable footer on homepage** — Created as a modal-editable component that content pages can inherit
2. **Code highlighting component** — Integrated highlight.js for syntax highlighting of HTML, XML, Java, JS/TS, JSON
3. **Two-level documentation structure** — OSGi, JCR, and Sling topics with multiple sub-pages
4. **Circular navigation** — JavaScript-powered navigation that opens first-level items in a circle, second-level items on hover

## Architecture Overview

### Sling Resource Resolution

Sling resolves resources to scripts based on `sling:resourceType`:

1. Content node (e.g., `/content/sling-matrix/home`) has `sling:resourceType="sling-matrix/pages/page"`
2. **`pages/page/html.html`** delegates to the `jcr:content` child
3. **`jcr:content`** has its own `sling:resourceType` (e.g., `sling-matrix/pages/homepage`)
4. **`pages/homepage/html.html`** (or falls through to `basepage` via `sling:resourceSuperType`)
5. **`pages/basepage/html.html`** is the page shell: `<!DOCTYPE html>`, `<head>`, `<body>`, partials

### Component Model

Each editable component has:
- **View HTL** — Displays the component with HTMX attributes for editing (when logged in)
- **Edit form HTL** — The edit form (modal or inline) loaded via HTMX
- **Content node** — JCR node storing component data

Non-editable components (like navigation) skip the edit form.

### Frontend Build Pipeline

TypeScript and CSS sources in `frontend/src/` are bundled by esbuild into two independent bundles:

- **`editor-bundle.js` + `editor.css`** — Loaded only for authenticated users (Tiptap + HTMX modal)
- **`public-bundle.js` + `public.css`** — Loaded for all visitors (navigation, code highlighting, Matrix rain)

Both bundles have minified production versions and unminified dev versions (use `?minLibs=no` for dev).

### VLT Content Packages

- **`sling-matrix.ui.apps`** — HTL templates, CSS, JS (application code)
- **`sling-matrix.sample-content`** — Documentation pages and sample content
- **`sling-matrix.core`** — OSGi bundle with Sling Models (UserIsLoggedIn)

## What's NOT Included (Next Steps)

This scaffolding does NOT include:

- User registration / authentication UI (login/logout pages)
- Search functionality
- Complex Sling Models beyond `UserIsLoggedIn`
- Asset management / image upload
- SEO meta tags / sitemap generation
- Error pages (404, 500)
- Dark mode toggle (CSS supports it, but not implemented)
- Production deployment configuration

These should be implemented in focused, smaller iteration steps with specialized agents.

## Components

### Editable Components

- **hero** — Hero banner with title and subtitle (modal-only editing)
- **text-block** — Richtext component with headline and body (inline Tiptap editing)
- **footer** — Footer with copyright and 3 links (modal-only editing)
- **code-block** — Code snippet with language and syntax highlighting (modal-only editing)

### Structural Components

- **parsys** — Paragraph system (iterates and renders child components)
- **navigation** — Circular navigation menu (not editable)

### Editing Supertypes

- **editable-component** — Base for richtext-editable components (Tiptap inline editor)
- **editable-component-modal** — Base for modal-only editable components

## Circular Navigation Implementation

The circular navigation is implemented purely in JavaScript (no external libraries):

1. **Toggle button** in top-left corner
2. **On mouse-over** → First-level items appear in a circular layout
3. **Hover over first-level items** → Second-level items appear as a linear list
4. **Dynamic** → Automatically picks up new pages added to the content structure

The navigation CSS uses absolute positioning and CSS transforms to arrange items in a circle. The second-level menus are positioned relative to their parent items.

## Code Highlighting

All code blocks use **highlight.js** for syntax highlighting. Supported languages include:

- HTML, XML
- Java, JavaScript, TypeScript
- JSON, YAML
- CSS, SCSS
- Bash, Shell

The code highlighting is automatically applied on page load and after HTMX content swaps.

## Matrix Rain Effect

The digital rain effect on the hero section is implemented as a `<canvas>` animation:

- Characters from the Matrix character set (Latin + Japanese katakana)
- Falls at variable speeds
- Fades to black with a trailing effect
- Runs at 30fps for smooth animation

Can be disabled by removing the `.matrix-rain-container` element from the hero section.

## License

Apache License 2.0 (same as Apache Sling)

## Contributing

Contributions welcome! This is a demonstration project, so feel free to:

- Add new documentation pages
- Improve the Matrix theme
- Add new components
- Fix bugs
- Improve accessibility

---

**Remember:** There is no spoon. Only components.
