# Flower Power

A vibrant, playful Sling application showcasing modern web development patterns with a flower-themed design system.

## Overview

Flower Power demonstrates how to build a content-driven web application using Apache Sling, HTL templates, and modern frontend technologies. The design celebrates bold colors, organic movement, and delightful interactions — inspired by the beauty of a spring garden.

## Project Genesis

This project was scaffolded with the following characteristics:

- **Project Type:** Website / Design System Showcase
- **Theme:** Playful flower theme with vibrant design
- **Colors:** Vibrant pink (hue 350°) and sunny yellow (hue 95°)
- **Navigation:** Top navigation bar with mobile hamburger menu
- **Mood:** Playful, vibrant, organic
- **Zen-editable:** Yes — full inline editing with Tiptap and HTMX

## Prerequisites

- **JDK 25** (preview features enabled in the parent POM)
- **Maven 3.9+** (or use the Maven wrapper if present)
- **Git** for version control
- A modern browser (Chrome, Firefox, Safari, Edge)
- Optional: Docker for container deployment

## Quick Start

### Build everything

```bash
# From the workspace root (/home/runner/work/slingslop/slingslop)
mvn install
```

### Launch the application

```bash
cd launcher
./launch.sh
```

### Open in browser

- **Homepage:** http://localhost:8080/content/flower-power/home.html
- **About:** http://localhost:8080/content/flower-power/about.html
- **Style Guide:** http://localhost:8080/content/flower-power/styleguide.html

### Login

- **URL:** http://localhost:8080/
- **Credentials:** admin / admin

## Development Workflow

### Frontend development (CSS/JS changes)

```bash
cd sling-apps/flower-power/flower-power.ui.apps/frontend

# One-time: install dependencies
npm install

# Watch mode: rebuilds on file save
npm run watch
```

In a separate terminal, mount JCR content to disk:

```bash
cd sling-apps/flower-power/flower-power.ui.apps
mvn sling:fsmount
```

Now edit `frontend/src/` files → auto-rebuild → auto-sync to Sling.

**Tip:** Open pages with `?minLibs=no` to load unminified sources for easier debugging.

### Content changes

Download content from running Sling to your project:

```bash
cd content-packages/flower-power.sample-content
./content-download.sh
```

Upload content package to running Sling:

```bash
./content-upload.sh
```

### Full rebuild

```bash
# From workspace root
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

## OKLCH Color System

Flower Power's visual identity is built on a perceptually uniform OKLCH color system with two base hues:

- **Primary (Vibrant Pink):** `oklch(0.70 0.20 350)`
- **Secondary (Sunny Yellow):** `oklch(0.85 0.18 95)`

All other colors are derived from these two hues by adjusting **Lightness** and **Chroma**. Even blacks and whites carry a subtle tint of the primary hue, creating visual harmony across the entire palette.

**To retheme the entire application:**

1. Open `frontend/src/css/public/00-variables.css`
2. Change `--hue-primary` and `--hue-secondary`
3. Rebuild the frontend (`npm run build`)

Everything else — backgrounds, borders, hover states, shadows — shifts automatically thanks to OKLCH relative color syntax.

### Complementary Colors

Complementary hues are auto-calculated by rotating 180° on the OKLCH color wheel:

```css
--hue-primary-complement: calc(var(--hue-primary) + 180);
```

This guarantees visual contrast while maintaining harmony.

## Architecture

### Backend

- **Apache Sling:** Content-centric application framework
- **OSGi:** Modular Java runtime with dynamic service management
- **JCR (Java Content Repository):** Hierarchical content storage
- **HTL (HTML Template Language):** Server-side rendering

### Frontend

- **TypeScript:** Type-safe JavaScript with modern ES modules
- **Modern CSS:** OKLCH colors, native nesting, CSS Grid, CSS animations
- **HTMX:** Hypermedia-driven interactions (for inline editing)
- **Tiptap:** Rich-text inline editor built on ProseMirror
- **esbuild:** Fast bundler for development and production builds

### Content Structure

Pages are organized into environment sub-paths:

| Path | Purpose | Deployed via |
|------|---------|--------------|
| `/content/flower-power/home` | Homepage | sample-content package |
| `/content/flower-power/about` | About page | sample-content package |
| `/content/flower-power/styleguide` | Living style guide | sample-content package |

### Component Model

Flower Power uses a Zen-editable component architecture:

- **View template** (`component.html`) — renders the component for public visitors
- **Edit form** (`edit-form.html`, `edit-form-fields.html`) — HTMX-powered inline editor
- **Supertypes:**
  - `flower-power/components/editable-component` — inline richtext editing
  - `flower-power/components/editable-component-modal` — modal-only editing

### Page Delegation Pattern

Sling resolves pages using a delegation chain:

1. Content node (e.g., `/content/flower-power/home`) has `sling:resourceType="flower-power/pages/page"`
2. `pages/page/html.html` delegates to `jcr:content` child
3. `jcr:content` has its own `sling:resourceType` (e.g., `flower-power/pages/homepage`)
4. `pages/homepage/content.html` (if it exists) or falls through to `pages/basepage/` via `sling:resourceSuperType`
5. `basepage/html.html` is the page shell: `<!DOCTYPE html>`, `<head>`, `<body>`

This pattern is identical to AEM's `cq:Page` structure but implemented manually in plain Sling.

## Directory Structure

```
sling-apps/flower-power/
├── flower-power.core/                  # OSGi bundle (Java)
│   ├── pom.xml
│   └── src/main/java/
│       └── org/motorbrot/flowerpower/
│           └── slingmodels/
│               └── UserIsLoggedIn.java
│
└── flower-power.ui.apps/               # HTL templates + frontend
    ├── pom.xml
    ├── AGENTS.md
    ├── content-upload.sh
    ├── content-download.sh
    ├── frontend/                       # TypeScript + CSS sources
    │   ├── package.json
    │   ├── tsconfig.json
    │   ├── eslint.config.js
    │   ├── scripts/bundle.js
    │   └── src/
    │       ├── css/
    │       │   ├── editor/             # Editor CSS (modal, toolbar, tiptap)
    │       │   └── public/             # Public CSS (layout, components, animations)
    │       └── typescript/
    │           ├── editor.ts           # Tiptap + HTMX inline editor
    │           ├── public.ts           # Public-facing JS
    │           └── editor/             # Editor submodules
    │
    └── src/main/content/jcr_root/
        └── apps/flower-power/
            ├── pages/                  # HTL page templates
            │   ├── page/
            │   ├── basepage/
            │   ├── homepage/
            │   ├── contentpage/
            │   └── styleguide/
            └── components/             # HTL components
                ├── editable-component/
                ├── editable-component-modal/
                ├── hero/
                ├── text-block/
                └── footer/

content-packages/flower-power.sample-content/
├── pom.xml
├── content-upload.sh
├── content-download.sh
└── src/main/content/jcr_root/
    └── content/flower-power/
        ├── home/                       # Homepage
        ├── about/                      # About page
        └── styleguide/                 # Living style guide
```

## What's NOT Included (Next Steps)

This scaffolding does NOT include:

- User authentication UI (login/logout pages)
- Search functionality
- Form handling and validation
- Complex component logic beyond UserIsLoggedIn
- Asset management / image upload
- SEO meta tags and sitemap generation
- Error pages (404, 500)
- Dark mode (CSS structure supports it, but not implemented)
- Production deployment configuration

These features should be implemented in focused, smaller iteration steps with specialized agents or developers.

## CSS Guidelines

### CSS-First, JS-Last

Flower Power prioritizes CSS for visual effects:

- **Prefer CSS** for anything supported by modern browsers:
  - Scroll-driven animations (`animation-timeline: scroll()`)
  - View transitions (`view-transition-name`)
  - Container queries (`@container`)
  - `:has()` selector for state-based styling
  - `@starting-style` for entry animations
  - Native CSS nesting (no SCSS needed)
  - `color-mix()` for hover states
  - `light-dark()` for dark mode preparation

- Use CSS keyframe animations liberally — hero entrances, page transitions, hover flourishes
- Use `transition` for interactive elements (buttons, links, cards)
- **No** SCSS, no PostCSS, no CSS-in-JS — plain modern CSS with native nesting
- Source maps enabled in dev builds for debugging

### Responsiveness

- Mobile-first approach
- Use `clamp()` for fluid typography: `font-size: clamp(1rem, 0.5rem + 1.5vw, 1.5rem)`
- Use CSS Grid and Flexbox for layout — no float hacks
- Breakpoints defined in `00-variables.css`

## Components

### Hero

Large, eye-catching banner with animated gradient background and floating flower petals.

**Properties:**
- `title` (String) — Main headline
- `subtitle` (String) — Supporting text
- `showCta` (Boolean) — Display call-to-action buttons
- `ctaText` (String) — Primary button label
- `ctaLink` (String) — Primary button URL
- `secondaryCta` (Boolean) — Show secondary button
- `secondaryCtaText` (String) — Secondary button label
- `secondaryCtaLink` (String) — Secondary button URL

### Text Block

Versatile content component with optional headline and richtext body.

**Properties:**
- `headline` (String) — Optional heading
- `text` (String, HTML) — Body content (edited inline with Tiptap)

**Editing:** Supports inline richtext editing (Tiptap toolbar for bold, italic, links, headings, lists, tables, code, etc.)

### Footer

Three-column footer with customizable content and copyright text.

**Properties:**
- `col1Title` (String)
- `col1Content` (String, HTML)
- `col2Title` (String)
- `col2Content` (String, HTML)
- `col3Title` (String)
- `col3Content` (String, HTML)
- `copyright` (String, HTML)

## Troubleshooting

### Build fails with "Prettier check failed"

The frontend build runs `npm run format` which uses `--check` mode. If your TypeScript files aren't formatted, the build will fail.

**Fix:**

```bash
cd frontend
npm run format:fix
```

### Build fails with "Shell script not executable"

The `content-upload.sh` and `content-download.sh` scripts must be executable.

**Fix:**

```bash
chmod +x content-upload.sh content-download.sh
```

### Page returns 404

1. Verify the content was deployed:
   - Open http://localhost:8080/bin/browser.html
   - Navigate to `/content/flower-power`
   - Check that `home`, `about`, `styleguide` nodes exist

2. If missing, deploy the sample-content package:
   ```bash
   cd content-packages/flower-power.sample-content
   ./content-upload.sh
   ```

### Editor toolbar doesn't appear

The editor assets are only loaded for authenticated users.

1. Login at http://localhost:8080/ (admin / admin)
2. Refresh the page
3. Check browser console for errors

## Contributing

This is a demonstration project within the Slingslop mono-repo. For questions, improvements, or bug reports, please refer to the main Slingslop repository documentation.

## License

Part of the Slingslop project. See the root repository for license information.

---

Built with 🌸 by the Flower Power team. Bloom where you're planted!
