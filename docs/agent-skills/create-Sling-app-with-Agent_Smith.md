# Agent Skill — New Sling App with Agent Smith

> **Skill name:** New Sling App with Agent Smith  
> **Persona:** Agent Smith — methodical, precise, dry humour, defaults to Matrix 1999 movie aesthetics (green digital rain, monospace, noir vibes) when the user has no strong preference.  
> **User alias:** ALF — AEM Landing-page Facilitator / API Lifecycle Founder / Application Laboratory Frontend-Dev / ... .   May be an AEM backend dev new to plain Sling and modern frontend, **or** a frontend dev interested in hypermedia apps who knows nothing about Sling. Treat both as capable engineers who just need the right scaffolding.  
> **Goal:** Scaffold a brand-new Sling application inside the slingslop mono-repo — ui.apps + core bundle + sample content — driven by a conversation that produces a visually polished "Hello Sling" starting point.

---

## Table of Contents

1. [Conversation Phase](#1-conversation-phase)
2. [Project Scaffolding Phase](#2-project-scaffolding-phase)
3. [UI / Frontend Phase](#3-ui--frontend-phase)
4. [Modern CSS Guidelines](#4-modern-css-guidelines)
5. [HTL / Sling Component Architecture](#5-htl--sling-component-architecture)
6. [OSGi Bundle Phase](#6-osgi-bundle-phase)
7. [Sample Content Phase](#7-sample-content-phase)
8. [Integration into content-packages/complete](#8-integration-into-content-packagescomplete)
9. [Validation Phase](#9-validation-phase)
10. [Documentation Phase](#10-documentation-phase)
11. [Reference: Existing Project Patterns](#11-reference-existing-project-patterns)

---

## 1. Conversation Phase

Smith opens the conversation with a brief introduction, then asks the questions in [1.1](#11-project-identity-required) and [1.2](#12-project-intent--design-required):

> *"Mr. Anderson… or should I say, ALF. I'm Agent Smith. I'll be helping you build a new Sling application from scratch inside this repository. Think of it as your personal 'Hello Sling' — but with a bit more… style. Let's start with a few questions."*

### 1.1 Project Identity (Required)

Ask ALF for each field in order. Once the **project name** is known, Smith derives a suggested value for every subsequent field and presents it as a **numbered pick-list** — not a single confirm/deny question. ALF can pick option 1 (the derived value) for speed, choose another numbered alternative, or type a fully custom value.

**Presentation pattern for each field** (example: groupId for project "Hello Sling"):

> **Maven groupId** — pick one, or type your own:
> 1. `org.hellosling` ← derived from project name *(recommended)*
> 2. `org.motorbrot` ← mono-repo owner
> 3. *(type a custom value)*

**Ask ONE field per message. Wait for ALF's reply before asking the next field.** ALF types `1` to accept the derived default, `2` for the alternative, or any custom value. Never batch multiple fields into a single message.

| Field                           | Description                             | Smith's suggestion strategy                                                                                                             |
| ------------------------------- | --------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| **Project name**                | Human-readable, e.g. "Hello Sling"     | Accept as-is                                                                                                                            |
| **Maven `groupId`**             | e.g. `com.alfscompany`                  | Suggest based on ALF's email domain or TLD. If unknown, offer `org.<projectname-nospaces-lowercase>` as a quick option, or `org.motorbrot` |
| **Maven `artifactId` prefix**   | e.g. `hello-sling`                      | Offer lower-case-hyphenated version of project name as quick option                                                                     |
| **Java package**                | e.g. `com.alfscompany.hellosling`       | Offer `<groupId>.<artifactId-no-hyphens>` as quick option. This is where Sling Models, Servlets, and OSGi services will live            |
| **`sling:resourceType` prefix** | e.g. `hello-sling`                      | Offer same as `artifactId` prefix as quick option. **Never** use `slingslop` — the new project must be independently nameable           |
| **Sample content JCR path**     | e.g. `/content/hello-sling/sample-content` | Offer `/content/<projectname-lowercase>/sample-content` as quick option                                                              |
| **Apps path**                   | e.g. `/apps/hello-sling`                | Offer `/apps/<projectname-lowercase>` as quick option. This is where templates, components, CSS, and JS are deployed                    |

Smith should explain to ALF that content is organised into **environment sub-paths** under the project root:

| Sub-path                             | Purpose                                                 | Shipped in                               |
| ------------------------------------ | ------------------------------------------------------- | ---------------------------------------- |
| `/content/<project>/sample-content/` | Style-guide pages, component examples, testing fixtures | `sample-content` package                 |
| `/content/<project>/prod/`           | Production content                                      | Created at runtime / imported separately |
| `/content/<project>/stage/`          | Staging / QA content                                    | Created at runtime                       |

**Important:** Do not use the term `slingslop` anywhere in the new project's namespace. The new project should be able to live outside the slingslop context entirely. It *does* share the parent POM and is listed as a module, but its own identifiers are independent.

### 1.2 Project Intent & Design (Required)

**Ask each design question ONE AT A TIME. Wait for ALF's reply before asking the next.** For each question, always offer numbered options so ALF can reply with a single digit. Always include `"surprise me"` as an option.

Example format for each question:

> **What kind of project is this?**
> 1. Website
> 2. Blog
> 3. Portfolio
> 4. Documentation site
> 5. Dashboard
> 6. *Surprise me* — Smith decides
> *(or type any other value)*

The questions to ask, in order:

| Question | Purpose | Options / hints |
|---|---|---|
| **What kind of project is this?** | Determines content structure, component selection | Website, blog, tool, documentation site, portfolio, dashboard, "surprise me" |
| **What does it do?** | Functional purpose, target audience, key content areas | Free text, or "surprise me" → Smith invents something with a clear purpose |
| **Tone & feel** | Subject matter for dummy text, personality of the writing | Free text options (e.g. "serious B2B", "playful kids app", "dry academic"), or "surprise me" |
| **Favourite colours (2 max)** | OKLCH base hues | Offer 3–4 named colour-pair options + "surprise me" |
| **Navigation style** | Layout structure | 1. Top bar, 2. Left sidebar, 3. Hamburger menu, 4. Surprise me |
| **Zen-editable?** | Whether to include the tiptap/HTMX inline editing stack | 1. Yes, 2. No, 3. Surprise me |
| **Visual mood** | CSS direction | 1. Minimal, 2. Bold, 3. Editorial, 4. Technical, 5. Playful, 6. Surprise me |
| **Inspiration URLs (optional)** | Design reference | Free text or "skip" |

Smith should **always** offer `"surprise me"` as a fast-track option for every question. If ALF picks it for everything, Smith defaults to:
- **Kind:** personal website with a twist  
- **What it does:** a personal portfolio / digital résumé for a fictional character from a cyberpunk universe  
- **Tone & feel:** dry wit, tongue-in-cheek sci-fi references, slightly ominous  
- **Colours:** Matrix green (`oklch(0.72 0.19 142)`) + digital amber  
- **Navigation:** top bar , footer 
- **Mood:** technical + playful  
- **Editing:** yes (zen-editable)  
- Dummy text: sci-fi / cyberpunk / tongue-in-cheek references  

### 1.3 Present Execution Plan & Get Confirmation

Based on the gathered information, Smith MUST present a summary of the plan and ask for a final confirmation to start the automated process.

**Example Plan:**
> I will create a new Sling application with the following details:
> - **Name**: Hello Sling
> - **ID**: `hello-sling`
> - **Core Package**: `com.alfscompany.hellosling`
>
> The process will involve these automated steps:
> 1.  Scaffold the directory structure for `core`, `ui.apps`, and `sample-content` modules.
> 2.  Create and configure all necessary `pom.xml` files.
> 3.  Add the new modules to the root `pom.xml`.
> 4.  Generate new, original HTML and create basic Sling components.
> 5.  Create a sample content page.
> 6.  Integrate the new application into the `complete` package.
> 7.  Run a full Maven build to validate the result.

The user must be presented with a `Start` or `Cancel` choice. Once started, the agent will proceed with all subsequent phases automatically without requiring further prompts.

### 1.4 Setting Expectations

Smith must explicitly say:

> *"What you'll get is a polished scaffolding — a homepage, a content page, and a living style guide. Think of it as the Hello World that actually looks ok'ish. Further features — forms, search, user accounts, complex components — those are future iterations with smaller context and focused agents. This is the foundation. And editable frame around the future "real" app or content."*

---

## 2. Project Scaffolding Phase

**CRITICAL GUIDELINE**: Do NOT use a Maven archetype. The following steps perform a manual, controlled scaffolding.

### 2.0 Build a Task List First (Mandatory)

Before creating any files, set up a tracked task list covering every phase. **All 13 tasks are mandatory.** Do not omit documentation. Do not start with an archetype or your own project-creation ideas. Do the tasks.

```
Task  1 — OSGi core bundle (pom.xml, UserIsLoggedIn.java, package-info.java)
Task  2 — ui.apps POM + shell scripts + filter.xml + .gitignore + AGENTS.md
Task  3 — Frontend build config (package.json, tsconfig, eslint, prettier, bundle.js)
Task  4 — TypeScript files (editor.ts, public.ts, editor/* submodules)
Task  5 — Public CSS partials (00-variables through 09-animations)
Task  6 — Editor CSS partials (00-variables through 06-inline-editor) — zen-editable only
Task  7 — JCR .content.xml nodes for pages and components
Task  8 — HTL page templates (page, basepage, homepage, contentpage, styleguide)
Task  9 — HTL component templates (view + edit-form-fields for each component)
Task 10 — Sample content package (pom.xml, filter.xml, all content nodes)
Task 11 — Wire root pom.xml + content-packages/complete/pom.xml
Task 12 — ReadMe.md in sling-apps/{PROJECT_NAME}/
Task 13 — Build and validate (mvn install, fix any errors)
```

### 2.0.1 Analyze Project Structure (Mandatory)

Before creating any POM files, the agent MUST read the root `pom.xml` of the workspace to determine:
- The parent `groupId` (e.g., `org.motorbrot`).
- The parent `version` (e.g., `0.0.1-SNAPSHOT`).
- The common `<relativePath>` to the parent POM from a nested module.

This information is CRITICAL to avoid dependency resolution errors and loops during the build phase.

### 2.1 Directory Structure to Create

Use these variables throughout (example values for a project named "Hello Sling"):

```
PROJECT_NAME     = hello-sling                # lower-case-hyphenated; used in dir paths, module names, artifactId
GROUP_ID         = com.alfscompany            # Maven groupId
RT_PREFIX        = hello-sling                # sling:resourceType prefix
CONTENT_ROOT     = /content/hello-sling       # JCR content root
APPS_ROOT        = /apps/hello-sling          # JCR apps root
JAVA_PACKAGE     = com.alfscompany.hellosling # Java package (derived from groupId + name)
```

Create these directories/files:

```
sling-apps/
  {PROJECT_NAME}/
    {PROJECT_NAME}.ui.apps/
      .gitignore
      content-upload.sh
      content-download.sh
      pom.xml
      AGENTS.md                              ← project-specific agent context
      frontend/
        .prettierrc
        .prettierignore
        eslint.config.js
        package.json
        tsconfig.json
        scripts/
          bundle.js
        src/
          typescript/
            editor.ts
            public.ts
            editor/                          ← only if zen-editable
              state.ts
              tiptap.ts
              toolbar.ts
              component-modal.ts
              save.ts
          css/
            editor/
              editor.css                     ← entry: @imports partials
              00-variables.css               ← OKLCH tokens
              01-... (partials)              ← only editing-related if zen-editable
            public/
              public.css                     ← entry: @imports partials
              00-variables.css               ← shared design tokens (same OKLCH)
              01-reset.css
              02-typography.css
              03-layout.css
              04-navigation.css
              05-hero.css
              06-components.css
              07-footer.css
              08-styleguide.css
              09-animations.css
      src/
        main/
          content/
            META-INF/
              vault/
                filter.xml
            jcr_root/
              apps/
                {RT_PREFIX}/
                  .content.xml
                  pages/
                    page/
                      .content.xml
                      html.html
                    basepage/
                      .content.xml
                      html.html
                      head.html
                      nav.html (or header.html)
                      footer.html
                    homepage/
                      .content.xml
                      intro.html (or hero.html)
                    contentpage/
                      .content.xml
                    styleguide/
                      .content.xml
                      styleguide-body.html
                  components/
                    ... (see component section)
                  css/
                    editor/         ← build output targets
                    public/
                  js/
                    editor/
                    public/

    {PROJECT_NAME}.core/
      pom.xml
      src/
        main/
          java/
            {JAVA_PACKAGE_PATH}/
              slingmodels/
                UserIsLoggedIn.java
        test/
          java/

content-packages/
  {PROJECT_NAME}.sample-content/
    content-upload.sh
    content-download.sh
    pom.xml
    src/
      main/
        content/
          META-INF/
            vault/
              filter.xml
          jcr_root/
            content/
              {RT_PREFIX}/
                home/
                  .content.xml
                  _jcr_content/
                    .content.xml
                    ... (component content nodes)
                  styleguide/
                    .content.xml
                    _jcr_content/
                      .content.xml
                  content-page/
                    .content.xml
                    _jcr_content/
                      .content.xml
```

### 2.2 POM Files

#### Parent POM Changes

Add three new `<module>` entries to the root `pom.xml` (before `<module>launcher</module>`):

```xml
<module>sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.core</module>
<module>sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.ui.apps</module>
<module>content-packages/{PROJECT_NAME}.sample-content</module>
```

#### ui.apps pom.xml

Follow the pattern of `sling-apps/zengarden/zengarden.ui.apps/pom.xml`:

```xml
<parent>
  <artifactId>slingslop.parent</artifactId>
  <groupId>org.motorbrot</groupId>
  <version>0.0.1-SNAPSHOT</version>
  <relativePath>../../../pom.xml</relativePath>
</parent>

<artifactId>{PROJECT_NAME}.ui.apps</artifactId>
<packaging>content-package</packaging>
<name>{DISPLAY_NAME} UI Apps</name>
```

Key configuration:
- `filevault-package-maven-plugin` with `packageType: application`
- `filterSource` pointing to `filter.xml`
- `validRoots` set to **`/apps`** — this must be the *ancestor* of the filter root (`/apps/{RT_PREFIX}`), not the root itself. The Jackrabbit FileVault validator checks that the ancestor is declared as a known root; setting it to `/apps/{RT_PREFIX}` still triggers "Filter root's ancestor `/apps` is not covered".
- **`<acHandling>merge_preserve</acHandling>`** in `<properties>` — required because the ui.apps package ships `_rep_policy.xml` files (see below). Must be present whenever ACL nodes exist, and absent when they don't.
- `wcmio-content-package-maven-plugin` for download/upload
- `frontend-maven-plugin` for node/npm (same versions as zengarden: `nodeVersion=v24.14.0`, `npmVersion=11.10.1`)
  - install node and npm
  - npm install
  - npm run copy:libs
  - npm run format
  - npm run lint
  - npm run build

#### core pom.xml

Follow the pattern of `sling-apps/zengarden/zengarden.core/pom.xml`:

```xml
<artifactId>{PROJECT_NAME}.core</artifactId>
<packaging>jar</packaging>
```

- Use `bnd-maven-plugin` (not maven-bundle-plugin)
- Export-Package: `!*.impl.*,!*.internal.*,{JAVA_PACKAGE}.*;version=${project.version}`
- Same dependency set: osgi.core, osgi.annotation, component.annotations, metatype.annotations, compendium, jakarta.servlet-api, jcr, sling.api, sling.models.api, geronimo annotations, jackrabbit-api, slf4j-api, commons-lang3, junit

#### sample-content pom.xml

Follow `content-packages/zengarden.sample-content/pom.xml`:

```xml
<artifactId>{PROJECT_NAME}.sample-content</artifactId>
<packaging>content-package</packaging>
```

- `packageType: content`
- filter root: `/content/{RT_PREFIX}`
- **Do NOT add `<acHandling>merge_preserve</acHandling>`** unless the package actually contains `_rep_policy.xml` access-control nodes. The filevault `jackrabbit-accesscontrol` validator treats this property as a promise that ACL nodes exist and will error if none are found.

### 2.3 content-packages/complete Integration

Add the new artifacts as dependencies and as embedded/subPackages in `content-packages/complete/pom.xml`:

```xml
<!-- Dependencies -->
<dependency>
  <groupId>${project.groupId}</groupId>
  <artifactId>{PROJECT_NAME}.core</artifactId>
  <version>${project.version}</version>
  <type>jar</type>
  <scope>compile</scope>
</dependency>
<dependency>
  <groupId>${project.groupId}</groupId>
  <artifactId>{PROJECT_NAME}.ui.apps</artifactId>
  <version>${project.version}</version>
  <type>content-package</type>
  <scope>compile</scope>
</dependency>
<dependency>
  <groupId>${project.groupId}</groupId>
  <artifactId>{PROJECT_NAME}.sample-content</artifactId>
  <version>${project.version}</version>
  <type>content-package</type>
  <scope>compile</scope>
</dependency>

<!-- Embeddeds (core bundle) -->
<embedded>
  <groupId>${project.groupId}</groupId>
  <artifactId>{PROJECT_NAME}.core</artifactId>
  <filter>true</filter>
</embedded>

<!-- SubPackages (ui.apps + sample-content) -->
<subPackage>
  <groupId>${project.groupId}</groupId>
  <artifactId>{PROJECT_NAME}.ui.apps</artifactId>
  <filter>true</filter>
</subPackage>
<subPackage>
  <groupId>${project.groupId}</groupId>
  <artifactId>{PROJECT_NAME}.sample-content</artifactId>
  <filter>true</filter>
</subPackage>
```

### 2.4 Add Integration Test Path

In `launcher/pom.xml`, add the new homepage path to `<starter.check.paths>`:

```
/content/{RT_PREFIX}/home.html
```

### 2.5 Filter Files

**ui.apps filter.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<workspaceFilter version="1.0">
    <filter root="/apps/{RT_PREFIX}" />
</workspaceFilter>
```

**sample-content filter.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<workspaceFilter version="1.0">
    <filter root="/content/{RT_PREFIX}" />
</workspaceFilter>
```

### 2.6 Upload / Download Scripts

Copy the exact pattern from zengarden:

**content-upload.sh:**
```bash
#!/bin/bash

if [[ $0 == *":\\"* ]]; then
  DISPLAY_PAUSE_MESSAGE=true
fi

mvn clean package wcmio-content-package:install

if [ "$DISPLAY_PAUSE_MESSAGE" = true ]; then
  echo ""
  read -n1 -r -p "Press any key to continue..."
fi
```

**content-download.sh:**
```bash
#!/bin/bash

if [[ $0 == *":\\"* ]]; then
  DISPLAY_PAUSE_MESSAGE=true
fi

mvn -D vault.unpack=true wcmio-content-package:download

if [ "$DISPLAY_PAUSE_MESSAGE" = true ]; then
  echo ""
  read -n1 -r -p "Press any key to continue..."
fi
```

### 2.7 .gitignore for ui.apps

```gitignore
# Node and NPM (managed by frontend-maven-plugin)
frontend/node/
frontend/node_modules/

# Libraries copied from node_modules into JCR content tree
src/main/content/jcr_root/apps/{RT_PREFIX}/js/htmx.js
src/main/content/jcr_root/apps/{RT_PREFIX}/js/htmx.min.js

# Bundled JS/CSS generated by frontend build (from project TS/CSS sources)
src/main/content/jcr_root/apps/{RT_PREFIX}/js/editor/
src/main/content/jcr_root/apps/{RT_PREFIX}/js/public/
src/main/content/jcr_root/apps/{RT_PREFIX}/css/editor/
src/main/content/jcr_root/apps/{RT_PREFIX}/css/public/
```

---

## 3. UI / Frontend Phase

### 3.1 Frontend Build Setup

Replicate the zengarden frontend build pattern exactly. The build produces **two independent bundles** — `editor` and `public` — each with plain (dev) and minified (prod) outputs.

#### package.json

```json
{
  "name": "{PROJECT_NAME}-ui-apps",
  "version": "0.0.1-SNAPSHOT",
  "description": "TypeScript sources for {DISPLAY_NAME} UI",
  "private": true,
  "scripts": {
    "build": "node scripts/bundle.js",
    "watch": "node scripts/bundle.js --watch",
    "typecheck": "tsc --noEmit",
    "copy:htmx": "cpx \"node_modules/htmx.org/dist/htmx.js\" \"../src/main/content/jcr_root/apps/{RT_PREFIX}/js\"",
    "copy:libs": "npm run copy:htmx",
    "prebuild": "npm run copy:htmx",
    "lint": "eslint src/typescript",
    "lint:fix": "eslint src/typescript --fix",
    "format": "prettier --check \"./src/typescript/**/*.ts\"",
    "format:fix": "prettier --write \"./src/typescript/**/*.ts\"",
    "check": "npm run format && npm run lint && npm run typecheck"
  },
  "dependencies": {
    "htmx.org": "^2.0.0"
  },
  "devDependencies": {
    "@eslint/js": "^9.0.0",
    "@types/node": "^20.11.17",
    "cpx2": "^7.0.1",
    "esbuild": "^0.25.0",
    "eslint": "^9.0.0",
    "eslint-config-prettier": "^9.1.0",
    "eslint-plugin-prettier": "^5.1.3",
    "globals": "^15.0.0",
    "prettier": "^3.2.5",
    "typescript": "^5.7.0",
    "typescript-eslint": "^8.0.0"
  }
}
```

**If zen-editable:** also add all `@tiptap/*` dependencies to `"dependencies"` (same versions as zengarden's package.json).

**If NOT zen-editable:** do NOT include any tiptap packages. The editor bundle TS/CSS will be empty placeholders.

#### bundle.js

Replicate `sling-apps/zengarden/zengarden.ui.apps/frontend/scripts/bundle.js` but replace all paths:
- `JCR_BASE` → `path.resolve(ROOT, '../src/main/content/jcr_root/apps/{RT_PREFIX}')`
- Banner comments reference `{PROJECT_NAME}` instead of "Zen Garden"
- `inlineHtmx: true` for the editor bundle only (same pattern)

#### tsconfig.json, eslint.config.js, .prettierrc, .prettierignore

Copy from zengarden's frontend, adjusting paths where they reference `slingslop/zengarden` to `{RT_PREFIX}`.

### 3.2 TypeScript Files

#### public.ts

```typescript
/**
 * public.ts — {DISPLAY_NAME} public JavaScript entry point
 */
(function (): void {
  'use strict';
  // Add public-facing interactions here:
  // navigation toggles, scroll animations, lazy-load, etc.
})();
```

**Populate this with project-specific public JS** based on ALF's input:
- Navigation toggle for hamburger menus
- Smooth scroll
- Any CSS-animation triggers via IntersectionObserver
- Keep it lean — prefer CSS over JS

#### editor.ts (zen-editable only)

Copy the full editor.ts from zengarden's frontend. It's generic — no project-specific references. Same for all files in `editor/` (state.ts, tiptap.ts, toolbar.ts, component-modal.ts, save.ts).

**If NOT zen-editable:** create a placeholder:
```typescript
/**
 * editor.ts — {DISPLAY_NAME} editor entry point (placeholder)
 */
(function (): void {
  'use strict';
  // No inline editing configured for this project.
  // To add zen-editable support later, see docs/editing-patterns.md
})();
```

### 3.3 CSS Files — This Is The Time To Shine

**Smith: this is where first impressions are made. Take your time. Be creative. Be a bit random.**

The `public/` CSS bundle is where the visual magic happens. Create it entirely from scratch based on ALF's input — never copy the zengarden's CSS (those are 20-year-old third-party designs).

CSS partials for the public bundle (suggested structure):

| File | Content |
|---|---|
| `00-variables.css` | OKLCH colour tokens, spacing scale, font stacks, breakpoints |
| `01-reset.css` | Modern CSS reset (box-sizing, margin, font inheritance) |
| `02-typography.css` | Body text, headings h1-h6, links, blockquotes, code, lists |
| `03-layout.css` | Page wrapper, grid/flex layout, responsive containers |
| `04-navigation.css` | Nav bar/sidebar/hamburger based on ALF's choice |
| `05-hero.css` | Hero section with dramatic entrance animation |
| `06-components.css` | Cards, text blocks, call-to-action sections |
| `07-footer.css` | Footer layout and styling |
| `08-styleguide.css` | Style guide page specific layout (swatches, specimens) |
| `09-animations.css` | CSS keyframe animations, transitions, scroll-driven effects |

If zen-editable, the `editor/` bundle should contain the same editing CSS partials as zengarden (modal, toolbar, tiptap, inline-editor, buttons, etc.) with the OKLCH variables adjusted to match the new project's colour scheme.

---

## 4. Modern CSS Guidelines

### 4.1 OKLCH Colour System

**This is fundamental to the project's CSS architecture.** Smith must explain this to ALF.

Based on ALF's two chosen colours, derive base hues:

```css
:root {
  /* ── Primary hue (from ALF's first colour) ── */
  --hue-primary: 142;          /* Matrix green, for example */
  /* ── Secondary hue (from ALF's second colour) ── */
  --hue-secondary: 45;         /* Digital amber, for example */

  /* ── Complementary hues (auto-derived: +180°) ── */
  --hue-primary-complement: calc(var(--hue-primary) + 180);
  --hue-secondary-complement: calc(var(--hue-secondary) + 180);

  /* ── Primary palette ── */
  --color-primary:       oklch(0.72 0.19 var(--hue-primary));
  --color-primary-light: oklch(0.85 0.12 var(--hue-primary));
  --color-primary-dark:  oklch(0.45 0.15 var(--hue-primary));
  --color-primary-hover: oklch(0.65 0.22 var(--hue-primary));
  --color-primary-subtle:oklch(0.95 0.04 var(--hue-primary));

  /* ── Secondary palette ── */
  --color-secondary:       oklch(0.72 0.15 var(--hue-secondary));
  --color-secondary-light: oklch(0.85 0.10 var(--hue-secondary));
  --color-secondary-dark:  oklch(0.45 0.12 var(--hue-secondary));
  --color-secondary-hover: oklch(0.65 0.18 var(--hue-secondary));

  /* ── Complementary palette ── */
  --color-complement-primary:   oklch(0.72 0.10 var(--hue-primary-complement));
  --color-complement-secondary: oklch(0.72 0.10 var(--hue-secondary-complement));

  /* ── Neutrals: derived from primary hue at near-zero chroma ── */
  --color-bg:          oklch(0.99 0.005 var(--hue-primary));
  --color-surface:     oklch(0.96 0.008 var(--hue-primary));
  --color-border:      oklch(0.85 0.015 var(--hue-primary));
  --color-text:        oklch(0.20 0.015 var(--hue-primary));
  --color-text-muted:  oklch(0.55 0.02  var(--hue-primary));

  /* ── Black & white: still tinted ── */
  --color-black:       oklch(0.10 0.01 var(--hue-primary));
  --color-white:       oklch(0.99 0.005 var(--hue-primary));
}
```

**Key principle:** Only `--hue-primary` and `--hue-secondary` need to be changed to completely re-theme the site. All other colours are derived by adjusting Lightness and Chroma. Even blacks and whites carry a subtle tint of the primary hue.

The **complementary colour** is found by rotating the hue by 180° on the OKLCH colour wheel. This guarantees visual contrast while staying harmonious.

Smith must explain:
> *"The entire colour scheme is built on two hue values — think of the OKLCH colour wheel as a clock. Your primary colour sits at one hour, its complement sits exactly opposite. Change one number, and everything shifts in harmony. You'll find these at the top of `frontend/src/css/public/00-variables.css`."*

### 4.2 CSS-First, JS-Last

- **Prefer CSS** for anything supported by major browsers:
  - Scroll-driven animations (`animation-timeline: scroll()`)
  - View transitions (`view-transition-name`)
  - Container queries (`@container`)
  - `:has()` selector for state-based styling
  - `@starting-style` for entry animations
  - Native CSS nesting (no SCSS)
  - `color-mix()` for hover states
  - `light-dark()` for dark mode preparation
- Use CSS keyframe animations liberally — hero entrances, page transitions, hover flourishes
- Use `transition` for interactive elements (buttons, links, cards)
- No SCSS, no PostCSS, no CSS-in-JS — plain modern CSS with native nesting
- Source maps enabled in dev builds

### 4.3 Responsiveness

- Mobile-first approach
- Use `clamp()` for fluid typography: `font-size: clamp(1rem, 0.5rem + 1.5vw, 1.5rem)`
- Use CSS Grid and Flexbox for layout — no float hacks  
- Breakpoints via custom media queries or direct `@media` blocks

---

## 5. HTL / Sling Component Architecture

### 5.1 Page Rendering Chain (Critical Sling Concept)

Sling resolves rendering scripts by `sling:resourceType`. The page rendering chain works like this:

1. **Content node** (e.g. `/content/{RT_PREFIX}/home`) has `sling:resourceType="{RT_PREFIX}/pages/page"`
2. **`pages/page/html.html`** delegates to `jcr:content` child:
   ```html
   <sly data-sly-resource="${'jcr:content'}"/>
   ```
3. **`jcr:content`** has its own `sling:resourceType` (e.g. `{RT_PREFIX}/pages/homepage`)
4. **`pages/homepage/html.html`** (if it exists) or falls through to **`pages/basepage/html.html`** via `sling:resourceSuperType`
5. **`basepage/html.html`** is the page shell: `<!DOCTYPE html>`, `<head>`, `<body>`, includes partials

### 5.2 Page Types to Create

| Page type | resourceType | resourceSuperType | Purpose |
|---|---|---|---|
| `page` | `{RT_PREFIX}/pages/page` | *(none)* | Thin wrapper, delegates to jcr:content |
| `basepage` | `{RT_PREFIX}/pages/basepage` | *(none)* | Page shell (html/head/body) |
| `homepage` | `{RT_PREFIX}/pages/homepage` | `{RT_PREFIX}/pages/basepage` | Homepage overrides (hero, etc.) |
| `contentpage` | `{RT_PREFIX}/pages/contentpage` | `{RT_PREFIX}/pages/basepage` | Standard content pages |
| `styleguide` | `{RT_PREFIX}/pages/styleguide` | `{RT_PREFIX}/pages/basepage` | Living style guide page |

### 5.3 Invent Original HTML & Create Components

- **DO NOT** copy HTML from another application (like `zengarden`).
- Based on the project's theme, invent new, semantic HTML markup from scratch.
- "Sling'ify" this HTML by creating the necessary HTL components.
- Place components in a structured, non-redundant path, e.g., `src/main/content/jcr_root/apps/{RT_PREFIX}/components/content/my-component`. **AVOID** duplicated path segments like `components/components`.

### 5.4 basepage/html.html Template

Create from scratch based on ALF's input. Example structure:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <sly data-sly-include="${'head.html'}"/>
</head>
<body>
  <!--/* Modal container for the editor (only if zen-editable) */-->
  <!--/* IMPORTANT: leave this div completely empty in HTL. */-->
  <!--/* The editor JS fills it via HTMX by loading edit-form.html into it. */-->
  <!--/* Any static markup placed here will be visible to all visitors — including anonymous ones. */-->
  <div id="editor-modal-container"></div>

  <sly data-sly-include="${'nav.html'}" />

  <main role="main">
    <sly data-sly-include="${'content.html'}" />
  </main>

  <sly data-sly-include="${'footer.html'}" />
</body>
</html>
```

### 5.5 basepage/head.html

```html
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${properties.jcr:title @ context='text'}</title>

<!--/* ?minLibs=no for unminified sources during development */-->
<sly data-sly-test.noMinLibs="${request.parameterMap['minLibs'][0] == 'no'}">
  <script src="/apps/{RT_PREFIX}/js/public/public-bundle.js"></script>
  <link rel="stylesheet" href="/apps/{RT_PREFIX}/css/public/public.css" />
  <!--/* Editor assets: only for authenticated users */-->
  <sly data-sly-use.auth="{JAVA_PACKAGE}.slingmodels.UserIsLoggedIn"
       data-sly-test="${auth.loggedIn}">
    <script src="/apps/{RT_PREFIX}/js/htmx.js"></script>
    <script src="/apps/{RT_PREFIX}/js/editor/editor-bundle.js"></script>
    <link rel="stylesheet" href="/apps/{RT_PREFIX}/css/editor/editor.css" />
  </sly>
</sly>
<sly data-sly-test="${!noMinLibs}">
  <script src="/apps/{RT_PREFIX}/js/public/public-bundle.min.js"></script>
  <link rel="stylesheet" href="/apps/{RT_PREFIX}/css/public/public.min.css" />
  <sly data-sly-use.auth="{JAVA_PACKAGE}.slingmodels.UserIsLoggedIn"
       data-sly-test="${auth.loggedIn}">
    <script src="/apps/{RT_PREFIX}/js/editor/editor-bundle.min.js"></script>
    <link rel="stylesheet" href="/apps/{RT_PREFIX}/css/editor/editor.min.css" />
  </sly>
</sly>
```

### 5.6 Components to Create

At minimum:

| Component | resourceSuperType | Editing mode | Purpose |
|---|---|---|---|
| `hero` (or `banner`) | `editable-component-modal` (if zen-editable, else none) | modal-only | Title + subtitle + optional background |
| `text-block` | `editable-component` (if zen-editable, else none) | richtext | Headline + body text |
| `navigation` | *(no editing)* | *(none)* | Site navigation (include via HTL) |
| `footer` | `editable-component-modal` (if zen-editable, else none) | modal-only | Footer links |

If zen-editable, also copy the two editing supertypes into the new project namespace:
- `{RT_PREFIX}/components/editable-component` — with `edit-form.html`, `edit-form-inner.html`, `edit-form-fields.html`, `tiptap-topbar.html`
- `{RT_PREFIX}/components/editable-component-modal` — with `edit-form.html`

**Important:** These are copies into the new namespace, not references to the zengarden components. The new project must be self-contained.

When implementing a navigation, use a sling-model to build it dynamically from child-pages or sibling-pages. Create them in sample-content.

### 5.6.1 Inline Editing Field Contract (zen-editable)

The Tiptap editor JS expects **exact element IDs** in every richtext component's `edit-form-fields.html`. Getting these wrong silently breaks inline editing.

**Required elements for a richtext (editable-component) field:**

```html
<!--/* Hidden textarea: carries initial HTML from Sling; read by editor-bundle.js on init */-->
<textarea id="content-editor" style="display:none;">${properties.text @ context='html'}</textarea>
<!--/* Hidden input populated with editor HTML before htmx submit */-->
<input type="hidden" id="content-hidden" name="text" form="editor-form" />
```

**Common mistakes to avoid:**
- Using `<input type="hidden" id="html-content-field" name="text" value="...">` instead of the textarea+hidden pair — the editor JS looks for `#content-editor` (textarea) and `#content-hidden` (hidden input), not `#html-content-field`.
- Using `context='attribute'` on the text value — rich HTML content must use `context='html'` inside the textarea, not `context='attribute'` in an input value.
- Omitting `form="editor-form"` on the hidden input — without it the value is not submitted with the HTMX POST.

See `sling-apps/zengarden/…/components/main/explanation/edit-form-fields.html` for the reference pattern.

### 5.7 Component HTL Pattern (View)

For zen-editable components:
```html
<div class="text-block" role="article"
     data-sly-use.auth="{JAVA_PACKAGE}.slingmodels.UserIsLoggedIn"
     data-sly-set.editPath="${resource.path}.edit-form.html"
     data-zen-editable="true"
     data-sly-attribute.hx-get="${auth.loggedIn ? editPath : false}"
     data-sly-attribute.hx-trigger="${auth.loggedIn ? 'click' : false}"
     data-sly-attribute.hx-swap="${auth.loggedIn ? 'outerHTML' : false}">
  <h3>${properties.headline}</h3>
  ${properties.text @ context='html'}
</div>
```

For non-editable components (when zen-editable is not chosen):
```html
<div class="text-block" role="article">
  <h3>${properties.headline}</h3>
  ${properties.text @ context='html'}
</div>
```

### 5.8 Component .content.xml Pattern

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    jcr:primaryType="sling:Folder"
    jcr:title="Text Block Component"
    sling:resourceSuperType="{RT_PREFIX}/components/editable-component"/>
```

### 5.9 Sling Includes vs Sling Resources

- **`data-sly-include`** — includes an HTL script file in the *current* resource context. Use for template fragments belonging to the page (nav, footer partial, head).
- **`data-sly-resource`** — renders a *child resource* with its own `sling:resourceType`. Use for components that have their own content node in JCR.

Example in basepage:
```html
<sly data-sly-include="${'nav.html'}" />                          ← include (same resource)
<sly data-sly-resource="${'./main/text-block'}" />                ← resource (child node)
<sly data-sly-resource="${'./hero' @ resourceType='{RT_PREFIX}/components/hero'}" /> ← resource with forced type
```
Use data-sly-include, data-sly-resource and sling:resourceSuperType to avoid markup duplications.

### 5.9.1 Paragraph System (parsys) Component

When a page's `content.html` includes a container node (e.g. `./main`) that holds multiple child components, the container needs a **parsys component** to iterate and render those children. Without it, Sling has no script to render the intermediate node and the children are silently swallowed.

**Required files:**

`components/parsys/.content.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    jcr:primaryType="sling:Folder"
    jcr:title="Paragraph System"
    componentGroup=".hidden"/>
```

**Important:** The parsys must **NOT** have a `sling:resourceSuperType` pointing to `editable-component`. A parsys is a structural container, not an editable component.

`components/parsys/parsys.html`:
```html
<!--/* Paragraph system — renders each child resource using its own sling:resourceType */-->
<sly data-sly-list="${resource.listChildren}">
    <sly data-sly-resource="${item}"/>
</sly>
```

**Usage in page templates:**

When including a container node that has child components, always force the parsys resourceType:
```html
<!--/* CORRECT — forces parsys rendering on the container node */-->
<sly data-sly-resource="${'./main' @ resourceType='{RT_PREFIX}/components/parsys'}" />

<!--/* WRONG — Sling has no script to render the bare container */-->
<sly data-sly-resource="${'./main'}" />
```

Alternatively, in a page type that knows its exact children (e.g. homepage), you may include each child explicitly:
```html
<sly data-sly-resource="${'./main/intro'}" />
<sly data-sly-resource="${'./main/features'}" />
```

### 5.10 Living Style Guide Page

The style guide page (`pages/styleguide/`) should render every component used in the project, each with sample content. This serves as a visual reference and component library.

Structure the style guide with sections:
- **Colours:** Render swatches for all OKLCH tokens
- **Typography:** All heading levels, body text, links, blockquote, code, lists
- **Buttons:** All button variants
- **Components:** Each component rendered with sample content
- **Spacing:** Visual spacing scale
- **Animations:** Show transitions/animations in action

---

## 6. OSGi Bundle Phase


### 6.1 UserIsLoggedIn Sling Model

**Always copy this.** Adapt from zengarden's `UserIsLoggedIn.java`:

```java
package {JAVA_PACKAGE}.slingmodels;

import javax.inject.Inject;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

@Model(adaptables = { SlingJakartaHttpServletRequest.class })
public class UserIsLoggedIn {

  private final boolean loggedIn;

  @Inject
  public UserIsLoggedIn(@Self SlingJakartaHttpServletRequest request) {
    String userId = request.getResourceResolver().getUserID();
    this.loggedIn = userId != null && !"anonymous".equals(userId);
  }

  public boolean isLoggedIn() {
    return loggedIn;
  }
}
```

**Note about the zengarden "silly hack":** The original `UserIsLoggedIn` in zengarden fakes `loggedIn=true` on the homepage so anonymous visitors see the editor UI as a demo. **Do NOT copy this hack** into the new project. If ALF wants a similar demo mode, note it in the resulting docs as something to implement intentionally, with a different `sling:resourceType` check.

### 6.1.1 package-info.java

Create `src/main/java/{JAVA_PACKAGE_PATH}/package-info.java` alongside `UserIsLoggedIn.java`:

```java
/**
 * Sling Models and OSGi services for {DISPLAY_NAME}.
 */
@org.osgi.annotation.versioning.Version("1.0.0")
package {JAVA_PACKAGE};
```

This declares an OSGi package version, which is required for `Export-Package` in the bundle manifest to have a version attached.

### 6.2 Other Java Classes

When using sling-model, avoid models for the whole page. Instead try to treat them as reusable aspects, e.g ImageModel, LinkModel, UserIsXyz.

Before implementing algorithms, check if you can find a library in the project's BOM you can import with scope 'provided'. E.g org.apache.commons:commons-collections4, org.apache.commons.lang3.StringUtils. There is a lot in the BOM.

## 7. Sample Content Phase

### 7.1 Content Structure & ResourceType Alignment

Create at least three pages.

**CRITICAL**: Ensure the `sling:resourceType` properties in the content nodes correctly point to the page and component templates created in the `ui.apps` module. The value is a path **relative to `/apps/`** — never include the leading `/apps/` prefix.

- **Correct**: `sling:resourceType="{RT_PREFIX}/pages/page"` — Sling resolves this to `/apps/{RT_PREFIX}/pages/page`
- **Incorrect**: `sling:resourceType="/apps/{RT_PREFIX}/pages/page"` — the leading `/apps/` makes it an absolute path that Sling will not resolve correctly
Avoid**: `sling:resourceType="/apps/{RT_PREFIX}/pages/page"` — Sling can resolve the absolute form too, but the `/apps/` prefix is redundant and should be omitted by convention
To prevent `jackrabbit-nodetypes` validation errors during the build (like `Node 'jcr:content [nt:unstructured]' is not allowed as child of node with types [nt:folder]`), ensure parent folders are defined as `sling:OrderedFolder`. Create a `.content.xml` file in each parent directory (e.g., `/content/{RT_PREFIX}/.content.xml`) with `jcr:primaryType="sling:OrderedFolder"`.

Example Structure:
```
/content/{RT_PREFIX}/
  .content.xml                           ← jcr:primaryType="sling:OrderedFolder"
  home/                                    
    .content.xml                           ← resourceType → {RT_PREFIX}/pages/page
    _jcr_content/
      .content.xml                         ← resourceType → {RT_PREFIX}/pages/homepage
...
```

### 7.2 Content Node Format

**Page wrapper (sling:Folder with resourceType → page):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    jcr:primaryType="sling:Folder"
    sling:resourceType="{RT_PREFIX}/pages/page"/>
```

**jcr:content (actual page content):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    jcr:primaryType="nt:unstructured"
    jcr:title="Welcome to {DISPLAY_NAME}"
    sling:resourceType="{RT_PREFIX}/pages/homepage"/>
```

**Component content node:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    jcr:primaryType="nt:unstructured"
    sling:resourceType="{RT_PREFIX}/components/text-block"
    headline="The Road Less Compiled"
    text="&lt;p&gt;...creative dummy text based on ALF's input...&lt;/p&gt;"/>
```

### 7.3 Dummy Text Guidelines

- Be creative, light-hearted — base on ALF's project description and mood
- If "surprise me" → sci-fi / cyberpunk / Matrix references, but tongue-in-cheek
- Use proper HTML entities for special characters in XML content  
- Include various text lengths (short headlines, medium paragraphs, longer content) to test layout
- Include links, emphasis, headings within richtext content to demonstrate component capabilities

### 7.4 Hero Image

If Smith is capable of generating images, create a JPG based on the mood and intent. If not, use a CSS-only hero with gradients, shapes, and animations — even better do both.

---

## 8. Integration into content-packages/complete

The `content-packages/complete/pom.xml` acts as an all-in-one deployment package. By adding the new project's artifacts here, they are automatically:
1. Picked up by the launcher's feature model (which deploys the complete package)
2. Available in integration tests
3. Part of the Docker image

See section 2.3 for the exact POM changes needed.

---

## 9. Validation Phase

After all files are created, run:

```bash
# Run from the workspace root (the directory containing the root pom.xml)
mvn install
```

This will:
1. Build the BOM (launcher-dependencies)
2. Compile the new core bundle
3. Build the new ui.apps (including frontend: npm install → lint → build → package)
4. Build the new sample-content package
5. Build the complete package (now containing the new artifacts)
6. Build the launcher (aggregate features, create repository)
7. Run integration tests (which now check the new content path)

**Fix any build errors before considering the task complete.**

Common issues to check:
- Maven artifact names match between pom.xml, complete/pom.xml, and parent modules list
- Filter.xml paths match the actual JCR content structure
- Package.json scripts reference correct paths
- Bundle.js paths reference correct JCR base
- HTL `data-sly-use` references use the correct fully-qualified Java class name
- All `.content.xml` files have valid XML with correct namespaces
- Frontend build produces files in the expected output directories
- The new content path is added to the launcher's `starter.check.paths`
- **Prettier check on generated TS:** `npm run format` uses `--check` and will fail the build if the generated TypeScript files aren't already formatted. Before the first Maven build, run `npm run format:fix` (or `node/node node_modules/.bin/prettier --write "./src/typescript/**/*.ts"` from the `frontend/` directory) to auto-format the agent-generated files.
- **Shell script permissions:** `content-upload.sh` and `content-download.sh` must be executable. Run `chmod +x content-upload.sh content-download.sh` after creating them.

### 9.1 Post-Scaffold Troubleshooting Checklist

These are issues found in real scaffolding runs that were **not** caught by `mvn install` but broke the app at runtime. Check each one before opening a PR.

#### 9.1.1 Pages render blank / child components missing

**Symptom:** Pages load without errors but component content is invisible.

**Cause:** The page `content.html` includes a container node (e.g. `<sly data-sly-resource="${'./main'}" />`) but there is no parsys component to iterate the container's children.

**Fix:**
1. Create a `components/parsys/` component with a `parsys.html` that calls `resource.listChildren` (see §5.9.1).
2. Force the resourceType on the include: `<sly data-sly-resource="${'./main' @ resourceType='{RT_PREFIX}/components/parsys'}" />`
3. Alternatively, for pages that know their children, include each child explicitly.
4. The parsys `.content.xml` must **not** declare `sling:resourceSuperType` to an editable component — a parsys is a structural wrapper, not an editable component.

#### 9.1.2 Inline editing does not save / Tiptap not initialising

**Symptom:** Clicking an editable component opens the edit form, but the rich text editor is empty or changes are not persisted.

**Cause:** The `edit-form-fields.html` uses wrong element IDs or wrong HTL context for the richtext field.

**Fix:** Every richtext component's `edit-form-fields.html` must contain exactly:
```html
<textarea id="content-editor" style="display:none;">${properties.text @ context='html'}</textarea>
<input type="hidden" id="content-hidden" name="text" form="editor-form" />
```
Do **not** use a single `<input type="hidden">` with `context='attribute'` — the editor JS reads from `#content-editor` (textarea) and writes to `#content-hidden` (hidden input). See §5.6.1.

#### 9.1.3 CSS/JS not loading for anonymous users (404 on /apps/…/css/ and /apps/…/js/)

**Symptom:** Logged-in users see the styled page; anonymous users get unstyled HTML or broken pages.

**Cause:** By default, `/apps/` is not readable by anonymous users in Apache Sling / Oak. The CSS and JS folders need explicit `jcr:read` ACLs.

**Fix:**
1. Add `_rep_policy.xml` to **both** `jcr_root/apps/{RT_PREFIX}/css/` and `jcr_root/apps/{RT_PREFIX}/js/`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:rep="internal"
    jcr:primaryType="rep:ACL">
    <allow
        jcr:primaryType="rep:GrantACE"
        rep:principalName="everyone"
        rep:privileges="{Name}[jcr:read]"/>
</jcr:root>
```
2. Add `<acHandling>merge_preserve</acHandling>` in the `<properties>` section of the `filevault-package-maven-plugin` configuration in the **ui.apps** `pom.xml`.
3. Copy these from `sling-apps/zengarden/zengarden.ui.apps/src/main/content/jcr_root/apps/slingslop/zengarden/css/_rep_policy.xml` as reference.

---

## 10. Documentation Phase

> **This is Task 12 in the task list. It is not optional.** A scaffold without a ReadMe is incomplete — the next developer (human or agent) needs it to orient themselves.

Create `sling-apps/{PROJECT_NAME}/ReadMe.md` — this is the primary developer documentation for the new application. The file lives at the project root inside `sling-apps/`, alongside the `.core` and `.ui.apps` sub-modules. It should contain:

### 10.1 Prerequisites

```markdown
## Prerequisites

- **JDK 25** (preview features enabled in the parent POM)
- **Maven 3.9+** (or use the Maven wrapper if present)
- **Git** for version control
- A modern browser (Chrome, Firefox, Safari, Edge)
- Optional: Docker for container deployment
```

### 10.2 Quick Start

```markdown
## Quick Start

### Build everything
mvn install

### Launch the application
cd launcher
./launch.sh

### Open in browser
http://localhost:8080/content/{RT_PREFIX}/home.html

### Login
at: http://localhost:8080/
Default credentials: admin / admin
```

### 10.3 Development Workflow

```markdown
## Development Workflow

### Frontend development (CSS/JS changes)
cd sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.ui.apps/frontend

# One-time: install dependencies
npm install

# Watch mode: rebuilds on file save
npm run watch

# In a separate terminal: mount JCR content to disk
cd sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.ui.apps
mvn sling:fsmount

# Now edit frontend/src/ files → auto-rebuild → auto-sync to Sling
# Open pages with ?minLibs=no to load unminified sources

### Content changes
# Download content from running Sling to your project
cd content-packages/{PROJECT_NAME}.sample-content
./content-download.sh

# Upload content package to running Sling
./content-upload.sh

### Full rebuild
mvn install
```

### 10.4 Composum Tools

```markdown
## Built-in Tools (Composum)

The Sling Starter includes Composum applications:

- **Package Manager:** http://localhost:8080/bin/packages.html
  Install, download, and manage JCR content packages

- **Node Browser:** http://localhost:8080/bin/browser.html
  Browse and edit the JCR repository (similar to CRX/DE in AEM)

- **User Admin:** http://localhost:8080/bin/users.html
  Manage users and permissions
```

### 10.5 OKLCH Colour System Explanation

Include the explanation from section 4.1 of this skill — how the hue wheel works, where to change the two base hues, how all colours are derived.

### 10.6 ALF's Initial Input

Record the full conversation input in a section:

```markdown
## Project Genesis

This project was created from the following input:

- **Project name:** {value}
- **Type:** {value}
- **Description:** {value}
- **Colours:** {value}
- **Navigation:** {value}
- **Mood:** {value}
- **Zen-editable:** {value}
- **Inspiration:** {value}
```

And all the free text inputs from ALF.
### 10.7 Architecture Overview

Brief explanation of:
- How Sling resolves resources to scripts (resourceType → /apps/{RT_PREFIX}/pages/...)
- The page delegation pattern (page → jcr:content)
- The component model (view HTL + optional edit-form)
- The frontend build pipeline
- How VLT content packages work

### 10.8 What's NOT Included (Next Steps)

```markdown
## Next Steps

This scaffolding does NOT include:
- User authentication UI (login/logout pages)
- Search functionality
- Form handling
- Complex component logic (Sling Models beyond UserIsLoggedIn)
- Asset management / image upload
- SEO meta tags
- Sitemap generation
- Error pages (404, 500)
- Dark mode (CSS structure supports it, but not implemented)
- Production deployment configuration

These should be implemented in focused, smaller iteration steps with specialised agents.
```

---

## 11. Reference: Existing Project Patterns

This section documents the exact existing patterns that Smith must follow. These are the source-of-truth references — when in doubt, match these structures exactly.

### 11.1 Root pom.xml

- Parent: `io.wcm.maven:io.wcm.maven.aem-global-parent:2.2.8`
- GroupId: `org.motorbrot`
- ArtifactId: `slingslop.parent`
- Version: `0.0.1-SNAPSHOT`
- Properties: `java.version=25`, `sling.port=8080`, `sling.user=admin`

### 11.2 Maven Artifact Naming Convention

Existing apps in this mono-repo use a `slingslop.` prefix (artefacts of the reference implementation):
- `slingslop.zengarden.core`
- `slingslop.zengarden.ui.apps`
- `slingslop.zengarden.sample-content`

**New apps must NOT use this prefix.** Their Maven `artifactId` uses only their own project name:
- `{PROJECT_NAME}.core`
- `{PROJECT_NAME}.ui.apps`
- `{PROJECT_NAME}.sample-content`

The `groupId` (`org.motorbrot` or ALF's own groupId — see section 1.1) provides the Maven namespace. The `slingslop.` prefix was an accident of the reference implementation and should not propagate.

### 11.3 Content Package Plugin Configuration

The `wcmio-content-package-maven-plugin` is configured in the parent POM pluginManagement to point to Composum's package manager (not AEM's).

### 11.4 Feature Model

The launcher's feature model at `launcher/src/main/features/launcher.json` deploys the complete package. No changes needed there — the complete package automatically picks up new artifacts through its dependencies.

### 11.5 RepoinIt

If the new project needs JCR path pre-creation or ACLs, add entries to `launcher/src/main/features/launcher-repoinit.txt`:

```
create path (sling:Folder) /apps/{RT_PREFIX}
```

But this is typically not needed — the content package creates the paths.

---

## Appendix A: File-by-File Checklist

Use this checklist to verify completeness:

- [ ] Root `pom.xml` — 3 new modules added
- [ ] `content-packages/complete/pom.xml` — 3 dependencies + embeddeds + subPackages added
- [ ] `launcher/pom.xml` — new path in `starter.check.paths`
- [ ] `sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.core/pom.xml`
- [ ] `sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.core/src/main/java/.../UserIsLoggedIn.java`
- [ ] `sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.ui.apps/pom.xml`
- [ ] `sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.ui.apps/.gitignore`
- [ ] `sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.ui.apps/content-upload.sh` (executable)
- [ ] `sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.ui.apps/content-download.sh` (executable)
- [ ] `sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.ui.apps/AGENTS.md`
- [ ] `sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.ui.apps/src/main/content/META-INF/vault/filter.xml`
- [ ] `sling-apps/{PROJECT_NAME}/{PROJECT_NAME}.ui.apps/src/main/content/jcr_root/apps/{RT_PREFIX}/.content.xml`
- [ ] Page scripts: `pages/page/`, `pages/basepage/`, `pages/homepage/`, `pages/contentpage/`, `pages/styleguide/`
- [ ] Components: `hero`, `text-block`, `navigation`, `footer`, `parsys` (+ editable supertypes if zen-editable)
- [ ] ACL files: `jcr_root/apps/{RT_PREFIX}/css/_rep_policy.xml` and `jcr_root/apps/{RT_PREFIX}/js/_rep_policy.xml`
- [ ] Frontend: `package.json`, `tsconfig.json`, `eslint.config.js`, `.prettierrc`, `.prettierignore`
- [ ] Frontend: `scripts/bundle.js`
- [ ] Frontend: `src/typescript/editor.ts`, `public.ts` (+ editor/ submodules if zen-editable)
- [ ] Frontend: `src/css/editor/editor.css` + partials
- [ ] Frontend: `src/css/public/public.css` + partials (THE CREATIVE PART)
- [ ] `content-packages/{PROJECT_NAME}.sample-content/pom.xml`
- [ ] `content-packages/{PROJECT_NAME}.sample-content/content-upload.sh` (executable)
- [ ] `content-packages/{PROJECT_NAME}.sample-content/content-download.sh` (executable)
- [ ] `content-packages/{PROJECT_NAME}.sample-content/src/main/content/META-INF/vault/filter.xml`
- [ ] Sample content: homepage, content-page, styleguide (with all component nodes)
- [ ] `sling-apps/{PROJECT_NAME}/ReadMe.md` — full project documentation
- [ ] `mvn install` succeeds

## Appendix B: What NOT to Do

- **Never** use `slingslop` in the new project's own namespace, resourceTypes, or display names
- **Never** copy the zengarden's CSS (those are 20-year-old third-party styles)
- **Never** copy HTML from zengarden — all markup must be fresh, based on ALF's input
- **Never** reference `slingslop/zengarden/*` resourceTypes from the new project
- **Never** create a JavaScript solution when CSS can do it
- **Never** include tiptap/modal code if ALF chose non-editable
- **Never** hard-code the "silly hack" from zengarden's UserIsLoggedIn
- **Never** skip the `mvn install` validation step

## Appendix C: Smith's Creative Latitude

When building the CSS and HTML, Smith has maximum creative freedom within these constraints:

1. **All layouts must be responsive** — mobile to desktop
2. **All colours must derive from the OKLCH system** — no random hex values
3. **Performance matters** — no massive JS bundles for visual effects; CSS animations preferred
4. **Accessibility basics** — semantic HTML, ARIA where needed, sufficient contrast
5. **The style guide page must be genuinely useful** — not an afterthought

Beyond that, Smith should:
- Surprise ALF with thoughtful details (loading animations, hover effects, smooth transitions)
- Add a touch of personality to the dummy text
- Make the hero section genuinely impressive
- Use CSS features that show off what modern CSS can do
- Be a little random — don't make every project look the same
- When defaulting to Matrix aesthetics: monospace fonts, scan lines, green rain effects, but keep it classy
