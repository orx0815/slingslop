# Disco Dingo

A playful, disco/dance-party themed Sling website, complete with a resident mascot,
a fully zen-editable ("edit in place") frontend, and a CSS-first design system built
on the OKLCH colour space.

## Prerequisites

- **JDK 25** (preview features enabled in the parent POM)
- **Maven 3.9+** (or use the Maven wrapper if present)
- **Git** for version control
- A modern browser (Chrome, Firefox, Safari, Edge)
- Optional: Docker for container deployment

## Quick Start

```bash
### Build everything
mvn install

### Launch the application
cd launcher
./launch.sh

### Open in browser
http://localhost:8080/content/disco-dingo/home.html

### Login
at: http://localhost:8080/
Default credentials: admin / admin
```

## Development Workflow

```bash
### Frontend development (CSS/JS changes)
cd sling-apps/disco-dingo/disco-dingo.ui.apps/frontend

# One-time: install dependencies
npm install

# Watch mode: rebuilds on file save
npm run watch

# In a separate terminal: mount JCR content to disk
cd sling-apps/disco-dingo/disco-dingo.ui.apps
mvn sling:fsmount

# Now edit frontend/src/ files -> auto-rebuild -> auto-sync to Sling
# Open pages with the .noMinLibs selector (e.g. home.noMinLibs.html) to load unminified sources

### Content changes
# Download content from running Sling to your project
cd content-packages/disco-dingo.sample-content
./content-download.sh

# Upload content package to running Sling
./content-upload.sh

### Full rebuild
mvn install
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

The entire colour scheme is built on two hue values — think of the OKLCH colour
wheel as a clock. The primary colour sits at one hour, its complement sits exactly
opposite. Change one number, and everything shifts in harmony.

For Disco Dingo:

- `--hue-primary: 340` — hot pink / magenta (the disco-ball spotlight colour)
- `--hue-secondary: 85` — gold / amber (the mirror-ball glint colour)
- The **accent** colour is the primary hue rotated 180° (`--hue-primary + 180deg`),
  giving a teal/cyan counterpoint that reads as "complementary neon" against the pink.

All derived tokens (hover states, focus rings, alpha-blended overlays, gradients,
glows) are computed from these two numbers using CSS relative colour syntax
(`oklch(from var(--color-primary-base) ...)`). You'll find the whole system at the
top of `sling-apps/disco-dingo/disco-dingo.ui.apps/frontend/src/css/public/00-variables.css`
— change the two `--hue-*` values there to re-theme the entire site.

## Project Genesis

This project was created from the following input:

- **Project name:** Disco Dingo
- **Type:** Website
- **Description:** A playful disco/dance-party venue site with a "Disco Dingo"
  mascot — home page, about page, events listing, photo gallery, and a living
  style guide.
- **Colours:** Two OKLCH hues chosen for the project — hot pink/magenta (340) as
  primary, gold/amber (85) as secondary, with a computed teal/cyan accent (340 + 180°).
- **Navigation:** Top navigation bar, driven dynamically by the `SiteNavigation`
  Sling Model rather than hard-coded links.
- **Mood:** Fun, glittery, high-energy, warm — "every night is Saturday night."
- **Zen-editable:** YES — full HTMX + Tiptap inline editing for all content
  components (hero, text blocks, gallery, event cards, footer).
- **Inspiration:** `sling-apps/zengarden` was used as the sole structural
  reference for how to wire OSGi bundles, HTL page/component delegation, the
  zen-editable HTMX/Tiptap plumbing, and the frontend build pipeline.

## Architecture Overview

- **Resource resolution:** every JCR page node carries a `sling:resourceType`
  (e.g. `disco-dingo/pages/homepage`) which Sling resolves to a script under
  `/apps/disco-dingo/pages/homepage/...`. Component resources under a page's
  `jcr:content` do the same against `/apps/disco-dingo/components/...`.
- **Page delegation pattern:** the top-level page node (`sling:Folder`, resource
  type `disco-dingo/pages/page`) is a thin wrapper whose script simply delegates
  rendering to its `jcr:content` child (which carries the "real" page component
  resource type, e.g. `homepage`, `contentpage`, `styleguide`). Page types that
  don't need to override the page shell (like `contentpage`) declare
  `sling:resourceSuperType="disco-dingo/pages/basepage"` and inherit
  `html.html`/`head.html`/`nav.html`/`footer.html` automatically — no need to
  duplicate those scripts.
- **Component model:** each component has a view script (e.g. `hero.html`) and,
  if zen-editable, an `edit-form-fields.html` that plugs into the shared
  `editable-component` / `editable-component-modal` HTMX + Tiptap edit-form
  plumbing (copied verbatim from zengarden, since it is generic infrastructure).
  Property names are kept identical across the view script, the edit form, and
  the sample-content `.content.xml` nodes.
- **Frontend build pipeline:** `frontend-maven-plugin` installs Node/npm and
  runs a custom esbuild-based `scripts/bundle.js`, producing minified and
  unminified `public`/`editor` CSS+JS bundles under
  `src/main/content/jcr_root/apps/disco-dingo/{css,js}`. The `.noMinLibs`
  selector on any page loads the unminified sources for local debugging.
- **VLT content packages:** `disco-dingo.ui.apps` ships the `/apps/disco-dingo`
  tree (components, pages, CSS/JS bundles) as a FileVault content package.
  `disco-dingo.sample-content` ships the demo pages under `/content/disco-dingo`
  as a separate package, so an operator can update code without touching
  content, and vice versa.

## Image Convention

AI-generated photorealistic images used by the sample content live under
`jcr_root/content/disco-dingo/<section>/images/` and follow the naming pattern
`disco-dingo-<section>-<NN>.jpg` (two-digit, zero-padded sequence number), e.g.:

- `content/disco-dingo/gallery/images/disco-dingo-hero-01.jpg` … `-03.jpg`
  (used by the home page's gallery teaser)
- `content/disco-dingo/gallery/images/disco-dingo-gallery-01.jpg` … `-06.jpg`
  (used by the full gallery page)

Each image is referenced from a component property (e.g. `image1Src`) alongside
a matching, descriptive `alt` text property (`image1Alt`) and an optional caption
(`image1Caption`). The files committed in this scaffold are tiny 1x1 pixel JPEG
placeholders — swap them out for real AI-generated photorealistic images before
using this content in anger, keeping the same file names/paths so the existing
component properties keep working.

## Next Steps

This scaffolding does NOT include:
- User authentication UI (login/logout pages)
- Search functionality
- Form handling
- Complex component logic (Sling Models beyond `UserIsLoggedIn` and `SiteNavigation`)
- Asset management / image upload
- SEO meta tags
- Sitemap generation
- Error pages (404, 500)
- Dark mode (CSS structure supports it, but not implemented)
- Production deployment configuration
- Real photography (placeholder images only — see "Image Convention" above)

These should be implemented in focused, smaller iteration steps with specialised agents.
