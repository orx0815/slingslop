# AGENTS.md — sling-bench.ui.apps

Context for AI coding agents working on the Sling Bench frontend/HTL.

## What this app does

A dashboard that turns `sling-apps/sling-matrix/vt-bench.sh` from a terminal
script into a shareable, persisted benchmark log:

1. **Generate** — the homepage's script-generator form builds the exact
   `./vt-bench.sh <baseUrl> <delayMs> <duration> <concurrencies>` command line
   client-side (no server round-trip) with a copy-to-clipboard button.
2. **Run** — the operator pastes that into a terminal against their Sling
   instance.
3. **Upload** — a logged-in user uploads the resulting `vt-bench-*.txt` (and
   optionally the `cpu-*.csv` sampler file) via a plain multipart POST to
   `BenchmarkRunUploadServlet` (`/bin/slingbench/upload`, sling-bench.core).
4. **Persist** — the servlet parses the text with `BenchResultParser` (handles
   both wrk's single-summary-per-level and hey's per-page-sequential shapes)
   and creates a page node under `/content/sling-bench/runs/{id}`.
5. **Render** — `RunDetailModel` and `RunHistoryModel` read those JCR
   properties and build inline **SVG charts server-side** (`SvgChartBuilder`)
   — no client-side charting library.

## Key architectural decision: separate content root for uploads

Uploaded runs live at `/content/sling-bench/runs`, which is **excluded** from
this package's own `filter.xml` (merge-mode + explicit `<exclude>`). This
means redeploying `sling-bench.sample-content` (home/styleguide) can never
wipe uploaded runs. The `/runs` folder itself is pre-created by launcher
repoinit (`launcher/src/main/features/launcher-repoinit.txt`), not by any
content package.

## Page types

- `pages/page`, `pages/basepage` — standard delegation shell (see
  `docs/editing-patterns.md` at the repo root for the generic pattern)
- `pages/homepage` — no override; relies on basepage's default `content.html`
  (renders `./main`'s children via the generic `components/parsys`)
- `pages/rundetail` — **overrides** `content.html` directly to use
  `RunDetailModel` (adapted straight from the `jcr:content` resource — no
  child components needed, since all metrics are properties on that node)
- `pages/styleguide` — overrides `content.html` with a static showcase

## Components

- `run-form` — client-side only script generator (no Sling Model)
- `upload-form` — gated by `UserIsLoggedIn`; plain (non-HTMX) multipart form,
  most robust for file inputs
- `run-history` — backed by `RunHistoryModel`, used only on the homepage
- `text-block`, `footer` — zen-editable (Tiptap), same supertype pattern as
  every other app in this mono-repo (see `docs/editing-patterns.md`)

## Property-name contract

Before changing any component, re-check the three-way contract from the
Agent Smith skill (§5.8.1): the view HTL, the `edit-form-fields.html`, and the
sample content `.content.xml` must all use the *exact same* property name.
