# Digital Media Library (DML) — UI Apps

> Agent context for working on the Digital Media Library application

## Overview

The Digital Media Library (DML) is a backoffice-style application for managing media assets in Apache Sling. Unlike zen-editable web pages, this is a dashboard application focused on asset upload, organization, and rendition management.

## Key Features

- **Asset Upload**: Users can upload media files (images, PDFs, videos, audio, etc.)
- **Folder Organization**: Users can create subfolders to organize assets
- **Metadata Extraction**: Uses Apache Tika to extract file metadata (MIME type, size, dimensions for images)
- **Rendition Management**: System for defining MediaFormats and generating renditions (scaled-down versions)
- **Asset Preview**: Preview panel showing asset renditions and metadata
- **Download Links**: Access to original files and generated renditions

## Architecture

**Module**: `digitalmedia.ui.apps`

This module contains:
- HTL templates for the dashboard interface
- TypeScript for HTMX-driven interactions
- CSS for the dashboard UI (serious, editorial style with Stiffkey Blue and India Yellow color scheme)
- Components for folder browsing, asset upload, metadata display

**No Zen-Editing**: This application does NOT use the Tiptap inline editing stack. All interactions are dashboard-style HTMX operations.

## Content Structure

- **Apps root**: `/apps/motorbrot/dma`
- **Content root**: `/content/motorbrot/dml`
- **Asset storage**: Assets are stored as JCR nodes with:
  - Original binary in `jcr:data`
  - Metadata properties extracted by Tika
  - `renditions` subnode containing generated renditions

## Frontend Build

The frontend uses:
- **TypeScript**: Modern TS with esbuild bundling
- **HTMX**: For server-driven UI updates
- **CSS**: Modern CSS with OKLCH color system, no preprocessing
- **No Tiptap**: No inline rich-text editing components

Build output:
- `/apps/motorbrot/dma/js/public/public-bundle.js`
- `/apps/motorbrot/dma/css/public/public.css`

## Development Workflow

```bash
# Frontend development with watch mode
cd frontend
npm run watch

# In another terminal, mount for live updates
mvn sling:fsmount

# Upload changes to running Sling
./content-upload.sh
```

## Dependencies

The application leverages:
- **Apache Tika**: Already available in Sling launcher for metadata extraction
- **HTMX**: For hypermedia-driven interactions
- Standard Sling APIs for JCR access and resource handling
