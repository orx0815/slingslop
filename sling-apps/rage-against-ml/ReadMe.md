# Rage Against the Machine Learning

> *"F#¢k you, you won't do what I'm telling you!"*

A luddite manifesto website of lengthy rants about AI coding — built, with maximum irony, as a
Hypermedia-Driven Application on [Apache Sling](https://sling.apache.org/) inside the slingslop
mono-repo. Punk zine aesthetics, riot red on ink black, 100% hand-typed content (the scaffolding,
we admit, was machine-assembled — it followed instructions for once; we remain suspicious).

## Modules

| Module | Purpose |
|---|---|
| `rage-against-ml.core` | OSGi bundle: Sling Models (`UserIsLoggedIn`, `NavigationModel`, `ParsysModel`, `FooterContext`) + `LinkBuilder` service |
| `rage-against-ml.ui.apps` | Content package: HTL templates, components, TypeScript + CSS frontend under `/apps/ratml` |
| `../../content-packages/rage-against-ml.sample-content` | Sample content under `/content/ratml`: homepage, manifesto, rants, style guide |

## Prerequisites

- **JDK 25** (preview features enabled in the parent POM)
- **Maven 3.9+**
- **Git**
- A modern browser (Chrome, Firefox, Safari, Edge)
- Optional: Docker for container deployment

## Quick Start

```bash
# Build everything (repo root)
mvn install

# Launch the application
cd launcher
./launch.sh

# Open in browser
http://localhost:8080/content/ratml/home.html

# Login (enables inline editing)
http://localhost:8080/
Default credentials: admin / admin
```

## Pages

| Page | Path |
|---|---|
| Home | `/content/ratml/home.html` |
| The Manifesto | `/content/ratml/home/manifesto.html` |
| Rant: You Won't Do What I'm Telling You | `/content/ratml/home/rants/wont-do-what-i-tell-you.html` |
| Rant: Hallucination Station | `/content/ratml/home/rants/hallucination-station.html` |
| Style Guide | `/content/ratml/home/styleguide.html` |

The navigation is built dynamically from this content tree by `NavigationModel` — add a page
under `home/` (or a section folder with pages) and it appears in the top bar automatically.

## Development Workflow

### Frontend development (CSS/JS changes)

```bash
cd sling-apps/rage-against-ml/rage-against-ml.ui.apps/frontend

# One-time: install dependencies
npm install

# Watch mode: rebuilds on file save
npm run watch

# In a separate terminal: mount JCR content to disk
cd sling-apps/rage-against-ml/rage-against-ml.ui.apps
mvn sling:fsmount

# Now edit frontend/src/ files → auto-rebuild → auto-sync to Sling.
# Open pages with the .noMinLibs selector (e.g. home.noMinLibs.html)
# to load the unminified sources with inline source maps.
```

### Content changes

```bash
cd content-packages/rage-against-ml.sample-content

# Download content from a running Sling into the project (e.g. after inline editing)
./content-download.sh

# Upload the content package to a running Sling
./content-upload.sh
```

### Full rebuild

```bash
mvn install
```

## The OKLCH Colour System

The entire colour scheme is built on **two hue values** — think of the OKLCH colour wheel as a
clock. The primary colour sits at one hour, its complement sits exactly opposite (+180°). Change
one number and everything — accents, borders, even the near-blacks (which carry a faint red
tint) — shifts in harmony.

```css
/* frontend/src/css/public/00-variables.css */
--hue-primary: 27;    /* riot red   */
--hue-secondary: 80;  /* hazard amber */
```

All other colours are derived from these hues by varying Lightness and Chroma only. The editor
bundle has its own scoped token set in `frontend/src/css/editor/00-variables.css` (single
`--color-primary-base`, same riot-red hue).

## Architecture Overview

- **Script resolution:** a content node's `sling:resourceType` (e.g. `ratml/pages/contentpage`)
  maps to scripts under `/apps/ratml/pages/contentpage/`.
- **Page delegation:** the page folder node (`ratml/pages/page`) renders `html.html`, which
  delegates to the `jcr:content` child — that child carries the real page type (homepage /
  contentpage / styleguide), all of which inherit the shell from `basepage` via
  `sling:resourceSuperType`.
- **Components:** each component ships a view script (`{name}.html`) and, if editable, an
  `edit-form-fields.html` consumed by the shared editing supertypes
  (`editable-component` for inline richtext, `editable-component-modal` for modal-only).
  See `docs/editing-patterns.md` at the repo root.
- **Paragraph system:** page bodies render an authored `./main` container through the `parsys`
  component. Logged-in editors get per-component move/delete toolbars and an "Add component"
  picker driven by the `allowedComponents` property — all against the default Sling POST
  servlet, no custom endpoints.
- **Frontend build:** esbuild produces two bundles (`public`, `editor`), each in plain and
  minified flavours, written into the JCR tree under `/apps/ratml/js` and `/apps/ratml/css`.
  Anonymous visitors get the public bundle only; editor assets are gated by `UserIsLoggedIn`.
- **Content packages:** FileVault packages; `ui.apps` ships `/apps/ratml` (application),
  `sample-content` ships `/content/ratml` (content). Both are aggregated into
  `content-packages/complete` and registered in the launcher feature model.

## Project Genesis

This project was created by Agent Smith from the following input:

- **Project name:** Rage Against the Machine Learning
- **Maven groupId:** `org.motorbrot`
- **artifactId prefix:** `rage-against-ml`
- **Java package:** `org.motorbrot.ratml`
- **resourceType prefix:** `ratml`
- **Content root:** `/content/ratml`
- **Apps root:** `/apps/ratml`
- **Type:** Website — a luddite manifesto site against AI hype
- **Content guideline (ALF):** "Lengthy rants about AI coding. Subtitle is: 'F#¢k you, you won't
  do what I'm telling you!' as a reference to the RATM song from the 90s."
- **Tone:** angry punk zine with wit
- **Colours:** riot red + black
- **Navigation:** top bar (Smith's pick on "surprise me") + footer
- **Visual mood:** bold
- **Zen-editable:** yes (HTMX + Tiptap)
- **Inspiration URLs:** none

## Built-in Tools (Composum)

- **Package Manager:** http://localhost:8080/bin/packages.html
- **Node Browser:** http://localhost:8080/bin/browser.html
- **User Admin:** http://localhost:8080/bin/users.html

## Next Steps

This scaffolding does NOT include:

- User authentication UI (login/logout pages)
- Search functionality
- Form handling (comment/grievance submission)
- Asset management / image upload
- SEO meta tags, sitemap generation
- Error pages (404, 500)
- Dark/light mode toggle (it is permanently dark; the rage demands it)
- CSS/JS URL fingerprinting (uses a short webcache TTL instead — see the
  `cssJsTtlSeconds: 300` tenant setting)

These should be implemented in focused, smaller iteration steps with specialised agents.
