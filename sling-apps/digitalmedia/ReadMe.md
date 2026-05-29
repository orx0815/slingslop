# Digital Media Library (DML)

A modern media asset management application built on Apache Sling, featuring hypermedia-driven interactions via HTMX.

## Overview

The Digital Media Library (DML) is a backoffice-style application for managing media assets. Unlike traditional web pages with inline editing, DML provides a dashboard interface for:

- **Asset Upload**: Upload images, PDFs, videos, audio files, and more
- **Folder Organization**: Create and manage folder hierarchies
- **Metadata Extraction**: Automatic extraction of file metadata using Apache Tika
- **Rendition Management**: Define MediaFormats and generate scaled renditions
- **Asset Preview**: Visual preview with metadata display
- **Download Management**: Access original files and generated renditions

## Project Structure

This project follows the standard Sling multi-module structure:

```
digitalmedia/
├── digitalmedia.core/          # OSGi bundle (Java backend)
│   └── src/main/java/          # Sling Models and services
├── digitalmedia.ui.apps/       # UI application package
│   ├── frontend/               # TypeScript and CSS sources
│   │   ├── src/typescript/     # Dashboard interactions
│   │   └── src/css/public/     # OKLCH-based styling
│   └── src/main/content/       # HTL templates and components
│       └── jcr_root/apps/motorbrot/dma/
│           ├── pages/          # Page templates
│           ├── components/     # Reusable components
│           ├── css/            # Built CSS (generated)
│           └── js/             # Built JavaScript (generated)
└── digitalmedia.sample-content/ # Sample content package
    └── src/main/content/
        └── jcr_root/content/motorbrot/dma/
```

## Prerequisites

- **JDK 25** (preview features enabled in parent POM)
- **Maven 3.9+**
- **Git** for version control
- A modern browser (Chrome, Firefox, Safari, Edge)
- Optional: Docker for container deployment

## Quick Start

### Build everything

```bash
# From the repository root
mvn clean install
```

### Launch the application

```bash
cd launcher
./launch.sh
```

### Open in browser

- **Dashboard**: http://localhost:8080/content/motorbrot/dma/home.html
- **Login page**: http://localhost:8080/
- **Default credentials**: admin / admin

### Built-in Tools (Composum)

The Sling Starter includes Composum applications:

- **Package Manager**: http://localhost:8080/bin/packages.html
  Install, download, and manage JCR content packages

- **Node Browser**: http://localhost:8080/bin/browser.html
  Browse and edit the JCR repository (similar to CRX/DE in AEM)

- **User Admin**: http://localhost:8080/bin/users.html
  Manage users and permissions

## Development Workflow

### Frontend development (CSS/JS changes)

```bash
cd sling-apps/digitalmedia/digitalmedia.ui.apps/frontend

# One-time: install dependencies
npm install

# Watch mode: rebuilds on file save
npm run watch

# In a separate terminal: mount JCR content to disk
cd ..
mvn sling:fsmount

# Now edit frontend/src/ files → auto-rebuild → auto-sync to Sling
# Open pages with ?minLibs=no to load unminified files
```

### Content changes

```bash
# Download content from running Sling to your project
cd sling-apps/digitalmedia/digitalmedia.ui.apps
./content-download.sh

# Upload content package to running Sling
./content-upload.sh
```

### Full rebuild

```bash
# From repository root
mvn clean install
```

## Technology Stack

| Layer | Technology |
|---|---|
| **Runtime** | Apache Sling 14 on Felix OSGi + Oak JCR |
| **Templates** | HTL (Sightly) — server-side HTML rendering |
| **Hypermedia** | HTMX — swaps component HTML via GET/POST |
| **Frontend build** | esbuild — TypeScript + CSS bundling |
| **Build** | Maven multi-module, JDK 25 |
| **Content packages** | Jackrabbit FileVault |
| **Metadata extraction** | Apache Tika (bundled in Sling) |

## Design System

### OKLCH Color System

The entire color scheme is built on two base hues, using the OKLCH color space for perceptually uniform colors:

- **Primary**: Stiffkey Blue (hue ~240°) — a sophisticated dark blue-grey
- **Secondary**: India Yellow No.66 (hue ~80°) — a warm golden yellow

All other colors are derived by adjusting Lightness and Chroma. To completely re-theme the application, change two variables in `frontend/src/css/public/00-variables.css`:

```css
:root {
  --hue-primary: 240;    /* Change this for primary theme */
  --hue-secondary: 80;   /* Change this for accent color */
}
```

The complementary color is automatically calculated by rotating the hue by 180° on the OKLCH color wheel. Even blacks and whites carry a subtle tint of the primary hue for visual cohesion.

### CSS-First, JS-Last Philosophy

- **Prefer CSS** for interactions:
  - Scroll-driven animations (`animation-timeline: scroll()`)
  - Container queries (`@container`)
  - `:has()` selector for state-based styling
  - `@starting-style` for entry animations
  - Native CSS nesting (no SCSS)
- **Use TypeScript** only when necessary for:
  - HTMX event listeners
  - File upload preview
  - Dynamic asset selection

## Architecture

### Page Rendering Chain

Sling resolves rendering scripts by `sling:resourceType`:

1. Content node (e.g., `/content/motorbrot/dma/home`) has `sling:resourceType="motorbrot/dma/pages/page"`
2. `pages/page/html.html` delegates to `jcr:content` child
3. `jcr:content` has its own `sling:resourceType` (e.g., `motorbrot/dma/pages/dashboard`)
4. `pages/dashboard/content.html` renders the dashboard UI
5. Inherits from `pages/basepage/html.html` via `sling:resourceSuperType`

### Content Paths

- **Apps root**: `/apps/motorbrot/dma`
- **Content root**: `/content/motorbrot/dma`
- **Asset storage**: Assets stored as JCR nodes with:
  - Original binary in `jcr:data`
  - Metadata properties extracted by Tika
  - `renditions/` subnode for generated renditions

## Project Genesis

This project was scaffolded with the following specifications:

- **Project name**: Digital Media Library (DML)
- **Maven groupId**: org.motorbrot
- **Java package**: org.motorbrot.sling.dma
- **Type**: Dashboard application
- **Purpose**: Asset management for logged-in users with upload, folder organization, and rendition generation
- **Style**: Serious, editorial tone
- **Colors**: Stiffkey Blue + India Yellow No.66
- **Zen-editable**: No (dashboard-style interactions, not inline editing)

## Features Implemented

✅ Dashboard layout with sidebar, main area, and metadata panel
✅ Folder tree navigation (placeholder structure)
✅ Asset upload form with HTMX integration
✅ Asset grid display with preview cards
✅ Metadata panel for selected assets
✅ User authentication check (login required for uploads)
✅ Responsive layout (mobile to desktop)
✅ Modern CSS with OKLCH color system
✅ TypeScript with esbuild bundling
✅ ACL configuration for public CSS/JS access

## What's NOT Included (Next Steps)

This scaffolding provides the foundation. The following features should be implemented in focused iterations:

- **Backend Services**:
  - Asset upload servlet (Sling POST servlet customization)
  - Tika metadata extraction service
  - MediaFormat configuration and rendition generation service
  - Folder creation servlet
  - Asset search functionality

- **Frontend Enhancements**:
  - Drag-and-drop file upload
  - Multi-file upload
  - Upload progress indicators
  - Asset selection and bulk operations
  - Rendition generation UI
  - Folder navigation via HTMX
  - Search with live results

- **Content Management**:
  - Asset deletion
  - Asset move/copy between folders
  - Asset rename
  - Metadata editing
  - Tag management

- **Media Processing**:
  - Image rendition generation (resize, crop, format conversion)
  - Video thumbnail extraction
  - PDF preview generation
  - Audio waveform visualization

- **Production Readiness**:
  - Error handling and validation
  - Rate limiting for uploads
  - Storage quota management
  - Background job processing for renditions
  - Audit logging
  - Security hardening

## Contributing

When extending this application:

1. Follow the established patterns from `zengarden` for Sling conventions
2. Keep CSS in the OKLCH color system
3. Use HTMX for server-driven UI updates
4. Minimize JavaScript — prefer CSS solutions
5. Test thoroughly with `mvn clean install`
6. Document new features in this README

## License

This project is part of the Slingslop demonstration repository.

---

For more information about Apache Sling, visit: https://sling.apache.org/
