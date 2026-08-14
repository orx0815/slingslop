# Sling Bench

A dashboard that turns [`vt-bench.sh`](vt-bench.sh) — the
virtual-thread-vs-platform-thread load benchmark for Apache Sling — from a
terminal-only script into a shareable, persisted benchmark log with charts.

This module also ships `VtBenchProbeFilter` (`vt-bench-probe.core`), the
app-independent Servlet Filter that `vt-bench.sh` actually drives — see
[VT Bench Probe](#vt-bench-probe-the-filter) below.

## What it does

1. **Generate** — the dashboard's script-generator panel builds the exact
   `./vt-bench.sh <baseUrl> <delayMs> <duration> <concurrencies>` command line
   client-side (no server round-trip), with a copy-to-clipboard button.
2. **Run** — paste that command into a terminal that has `vt-bench.sh` on its
   path, pointed at whichever Sling instance you want to benchmark.
3. **Upload** — a logged-in user uploads the resulting `vt-bench-*.txt` (and
   optionally the `cpu-*.csv` sampler file) through a plain multipart form.
4. **Persist** — a custom servlet parses the text and creates a page node
   under `/content/sling-bench/runs/{id}`.
5. **Render** — every run gets its own page with server-rendered inline SVG
   charts (requests/sec, average and p99 latency, and CPU/RSS if the sampler
   CSV was included) — **no client-side charting library**. The homepage shows
   a run-history table plus a virtual-thread-vs-platform-thread comparison
   chart aggregated across every run uploaded so far.

## Prerequisites

- **JDK 25** (preview features enabled in the parent POM)
- **Maven 3.9+**
- **Git**
- A modern browser
- Optional: Docker for container deployment

## Quick Start

```bash
# Build everything
mvn install

# Launch the application
cd launcher
./launch.sh

# Open in browser
http://localhost:8080/content/sling-bench/home.html

# Log in (required to upload a run)
http://localhost:8080/
# Default credentials: admin / admin
```

## Development Workflow

```bash
# Frontend development (CSS/JS changes)
cd sling-apps/sling-bench/sling-bench.ui.apps/frontend
npm install
npm run watch

# In a separate terminal: mount JCR content to disk
cd sling-apps/sling-bench/sling-bench.ui.apps
mvn sling:fsmount

# Open pages with .noMinLibs for unminified sources, e.g.
# http://localhost:8080/content/sling-bench/home.noMinLibs.html

# Content changes
cd content-packages/sling-bench.sample-content
./content-download.sh   # pull content from running Sling
./content-upload.sh     # push content to running Sling

# Full rebuild
mvn install
```

## Architecture

### Page rendering chain

Standard Sling resourceType resolution: content folder (`sling:resourceType`)
→ `pages/page/html.html` delegates to `jcr:content` → its own, more specific
`sling:resourceType` (`homepage` / `rundetail` / `styleguide`, all extending
`basepage` via `sling:resourceSuperType`) → `basepage/html.html` shell (head,
nav, `content.html`, footer).

| Page type | Overrides `content.html`? | Why |
|---|---|---|
| `homepage` | No | Renders `./main`'s children via the generic `parsys` component (intro text-block + run-form + upload-form + run-history, in JCR child order) |
| `rundetail` | **Yes** | Reads `RunDetailModel` directly off the `jcr:content` resource — the upload servlet writes all parsed metrics as properties on that single node, so there's nothing to iterate |
| `styleguide` | Yes | Static showcase, no content nodes needed |
| `docpage` | No | Static doc pages (e.g. the VT Bench Probe docs below) — `./main`'s children are `doc-section`/`code-block` components via the same generic `parsys` |

### Upload → parse → persist pipeline (sling-bench.core)

- **`BenchmarkRunUploadServlet`** (`/bin/slingbench/upload`, POST only, requires
  a logged-in user) — accepts `label`, `resultFile` (required), `cpuFile`
  (optional), each capped at 5 MB.
- **`BenchResultParser`** — regex-based parser that handles *both* shapes
  `vt-bench.sh` can produce: a single `wrk` summary block per concurrency
  level, or many sequential `hey` per-page blocks per level. Every
  `Requests/sec:` occurrence in a level's section is summed, every `Average:`
  is averaged, and the worst (max) `99% in` is kept — this normalizes both
  shapes into one `ConcurrencyMetric` per level.
- Persisted as a page node: `/content/sling-bench/runs/{id}` (`sling:Folder`,
  `sling-bench/pages/page`) → `jcr:content` (`sling-bench/pages/rundetail`,
  all metrics as properties: `concurrencyLevels`, `totalRps`, `avgLatencyMs`,
  `p99Ms`, `rawResultText`, and the optional CPU arrays).
- **`SvgChartBuilder`** — pure-Java bar/line/grouped-bar SVG string builder.
  `RunDetailModel` and `RunHistoryModel` call it directly; charts are embedded
  in HTL via `${model.someChartSvg @ context='unsafe'}` — **must** be
  `'unsafe'`, not `'html'`: HTL's `context='html'` sanitizer strips `<svg>`
  and its children (not in its safe-HTML allowlist) but keeps their text
  content, silently leaving floating numbers with no visible chart at all.

### Why uploaded runs live at a nested-but-excluded content path

`/content/sling-bench/runs` is **inside** the app's content root (so the CONGA
tenant's `contentRoot` covers it for public routing) but **excluded** from
`sling-bench.sample-content`'s own filter via `mode="merge"` plus an explicit
`<exclude>` — so redeploying sample content (home/styleguide) can never wipe
uploaded runs. The folder itself is pre-created by launcher repoinit
(`launcher/src/main/features/launcher-repoinit.txt`), independent of any
content package install.

### Zen-editable scope

Only the homepage's "About this dashboard" intro is zen-editable (Tiptap
richtext, `text-block` component). Everything else — the script generator, the
upload form, the run history, the run-detail charts — is a functional tool
surface, not CMS content, so it isn't editable and doesn't need to be.

## Colour system

Two OKLCH hues drive the whole theme (`frontend/src/css/public/00-variables.css`):

- `--hue-primary: 195` (cyan) — "virtual threads" everywhere: badges, charts, links
- `--hue-secondary: 38` (amber) — "platform threads"

Everything else (lightness/chroma variants, neutrals, borders) derives from
those two numbers.

## Built-in Tools (Composum)

- **Package Manager:** http://localhost:8080/bin/packages.html
- **Node Browser:** http://localhost:8080/bin/browser.html
- **User Admin:** http://localhost:8080/bin/users.html

## VT Bench Probe (the filter)

`vt-bench-probe.core` (`VtBenchProbeFilter`) is what `vt-bench.sh` actually
drives. It has nothing to do with the dashboard's JCR/HTL — it's a single
OSGi bundle, no `ui.apps`, no content.

The doc page at `/content/sling-bench/vt-bench-probe.html` (linked from the
nav bar) covers this in full: what it does, why it's a Servlet Filter
registered on the Sling filter whiteboard (`sling.filter.scope=REQUEST`, runs
before resource resolution — works on any URL in any app, zero per-app
wiring) rather than a per-app Sling Model + selector script, how to enable it
at `/system/console/configMgr` (disabled by default), the CONGA knob for
turning on virtual threads, and how to set custom JVM parameters.

## Next Steps

Not included in this scaffold:
- Deleting/archiving old runs (currently accumulate forever under `/runs`)
- Filtering/searching the run history table by tool, date range, or delay
- Multi-user attribution UI beyond the raw `uploadedBy` string
- Exporting a run's chart data (CSV/JSON download)
- Any automated way to *trigger* `vt-bench.sh` remotely — this app only
  ingests results a human ran and uploaded by hand
