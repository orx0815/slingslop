# Cyberpunk Alpaca

A Hypermedia-Driven Application on Apache Sling, scaffolded with **Agent Smith**.
Cyberpunk Alpaca is a playful neon-on-black demo site — a herd of alpacas loose on
the Grid — showcasing server-side HTL rendering, HTMX component swaps and optional
Tiptap inline editing.

- **Maven groupId:** `org.motorbrot`
- **Java package:** `org.motorbrot.cyberpunkalpaca`
- **`sling:resourceType` prefix:** `cyberpunk-alpaca`
- **Content root:** `/content/cyberpunk-alpaca`
- **Apps root:** `/apps/cyberpunk-alpaca`

## Modules

| Module | Description |
|---|---|
| `cyberpunk-alpaca.core` | OSGi bundle — Sling Models (`Navigation`, `UserIsLoggedIn`) |
| `cyberpunk-alpaca.ui.apps` | Content package — HTL scripts, components, pages, frontend bundle |
| `cyberpunk-alpaca.sample-content` (in `content-packages/`) | Sample JCR content: home, about, style guide |

## Prerequisites

- **JDK 25** (preview features enabled in the parent POM)
- **Maven 3.9+** (or use the Maven wrapper if present)
- **Git** for version control
- A modern browser (Chrome, Firefox, Safari, Edge)
- Optional: Docker for container deployment

## Quick Start

```bash
# Build everything
mvn install

# Launch the application
cd launcher
./launch.sh

# Open in browser
# http://localhost:8080/content/cyberpunk-alpaca/home.html

# Login at http://localhost:8080/  (default credentials: admin / admin)
```

## Development Workflow

```bash
# Frontend development (CSS/JS changes)
cd sling-apps/cyberpunk-alpaca/cyberpunk-alpaca.ui.apps/frontend
npm install          # one-time
npm run watch        # rebuilds on file save

# In a separate terminal: mount JCR content to disk for live reload
cd sling-apps/cyberpunk-alpaca/cyberpunk-alpaca.ui.apps
mvn sling:fsmount

# Open pages with the .noMinLibs selector (home.noMinLibs.html) to load unminified sources

# Content changes
cd content-packages/cyberpunk-alpaca.sample-content
./content-download.sh   # pull content from running Sling
./content-upload.sh     # push content package to running Sling

# Full rebuild
mvn install
```

## Built-in Tools (Composum)

- **Package Manager:** http://localhost:8080/bin/packages.html
- **Node Browser:** http://localhost:8080/bin/browser.html
- **User Admin:** http://localhost:8080/bin/users.html

## Pages & Components

- **Pages:** `page` (delegator) → `basepage` → `homepage` / `contentpage` / `styleguide`
- **Components:**
  - `navigation` — top nav driven by the `Navigation` Sling model
  - `hero` — modal-edited banner (supertype `editable-component-modal`)
  - `parsys` — container that renders its child components
  - `text-block` — inline richtext (supertype `editable-component`, Tiptap)
  - `footer` — modal-edited multi-column footer

Inline editing (HTMX + Tiptap) is gated by `UserIsLoggedIn` — anonymous visitors see
a static page; logged-in authors get click-to-edit affordances.

## Colour System (OKLCH)

All colours derive from **two base hues** defined as CSS custom properties in
`frontend/src/css/public/00-variables.css`:

- `--hue-primary: 152` — Matrix green
- `--hue-secondary: 75` — digital amber

Every surface, text, border and accent colour is expressed in `oklch(L C H)` where the
hue channel references these two variables (or derived complements). Because OKLCH keeps
lightness perceptually uniform, you can re-theme the entire site by changing just the two
hue numbers — swap in different values and the whole palette rotates around the colour
wheel while contrast relationships stay intact.

## Project Genesis

This project was created from the following input (GitHub issue #14, "Agent Smith:
another cyberpunk alpaca"):

- **Project name:** Cyberpunk Alpaca
- **Type:** Surprise me — Smith decides
- **Description:** Surprise me — Smith decides
- **Colours:** Matrix green + digital amber (OKLCH hues 152 / 75)
- **Navigation:** Surprise me — Smith decides
- **Mood:** Playful
- **Zen-editable:** Yes (HTMX + Tiptap inline editing)
- **Inspiration:** —
