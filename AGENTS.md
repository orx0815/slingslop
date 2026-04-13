# AGENTS.md — Slingslop

> Context for AI coding agents working in this repository.

## What Is This?

Slingslop is a mono-repo for **Hypermedia-Driven Applications** on [Apache Sling](https://sling.apache.org/), using HTMX for component-level GET/POST and Sling's out-of-the-box content endpoints. The name is a pun on Apache Sling's neglected Slingshot sample app.

## Repository Layout

```
sling-apps/           ← Sling applications (each = ui.apps + core bundle)
  zengarden/          ← CSS Zen Garden demo app (reference implementation)
content-packages/     ← Sample content (VLT zips) + 'complete' container package
launcher/             ← Feature-model launcher, Docker image, integration tests
docker/               ← docker-compose + web cache proxy configs
docs/
  agent-skills/       ← Executable agent skill files (invoke via VS Code Copilot)
  editing-patterns.md ← Component editing supertypes and contracts
```

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Apache Sling 14 on Felix OSGi + Oak JCR |
| Templates | HTL (Sightly) — server-side HTML rendering |
| Hypermedia | HTMX — swaps component HTML via GET/POST |
| Inline editing | Tiptap rich-text editor (optional per app) |
| Frontend build | esbuild — TypeScript + CSS, two bundles (editor / public) |
| Build | Maven multi-module, JDK 25, `io.wcm.maven.aem-global-parent` |
| Content packages | Jackrabbit FileVault (application / content / container) |
| Launcher | Sling Feature Model aggregate |
| Tools included | Composum Node Browser, Package Manager, User Admin |

## Key Conventions

- **`sling:resourceType`** drives rendering — scripts live under `/apps/{prefix}/`
- **Page delegation:** content node → `pages/page/html.html` → delegates to `jcr:content` child (which carries the actual page resourceType)
- **Component supertypes:** `editable-component` (richtext) and `editable-component-modal` (modal-only) — see `docs/editing-patterns.md`
- **Frontend builds** are per–ui.apps module under a `frontend/` subfolder, driven by `frontend-maven-plugin`
- **Content packages** use `wcmio-content-package-maven-plugin` with upload/download shell scripts
- **The `complete` package** in `content-packages/complete/` aggregates all apps — adding new artifacts there makes them part of the launcher and integration tests automatically

## Build & Run

```bash
# Build everything
mvn clean install -DskipITs

# Launch locally (after build)
cd launcher && ./launch.sh
# → http://localhost:8080/content/slingslop/zengarden/home.html

# Fast deploy a single module
mvn install sling:install        # OSGi bundle
mvn install wcmio-content-package:install  # content package
mvn sling:fsmount                # filesystem mount (live reload)
```

## Agent Skills

Skills are detailed instruction sets for specific tasks. In VS Code Copilot, reference the file path to invoke the skill.

| Skill | File | Purpose |
|---|---|---|
| **New Sling App with Agent Smith** | `docs/agent-skills/create-Sling-app-with-Agent_Smith.md` | Scaffold a complete new Sling application from scratch via guided conversation |

## Per-Module Agent Context

Some modules have their own `AGENTS.md` with module-specific context:

- `sling-apps/zengarden/zengarden.ui.apps/AGENTS.md` — Inline editing flow (HTMX + Tiptap lifecycle)

## Documentation

| Document | Description |
|---|---|
| `docs/editing-patterns.md` | Editing supertypes, component contract, new-component checklist |
| `ReadMe.md` | Project overview, prerequisites, build & run instructions |
