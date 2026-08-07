# Concept: CONGA-driven configuration generation for public-facing Slingslop apps

> **Status:** implemented (Phases 1–7). The `devops/conga/` Maven module now
> renders the reverse-proxy, webcache, Sling URL-mapping and launcher config for
> every public-facing app from a single per-app *tenant* block. This document is
> the design rationale; the working module lives at
> [devops/conga/](../devops/conga/README.md). Build it with
> `mvn -pl devops/conga clean package` (or as part of the root reactor).
>
> **CONGA version:** `conga-maven-plugin` 1.20.0 with the `sling` + `ansible`
> plugins 1.6.0 — builds cleanly on the repo's JDK 25 toolchain.

---

## 1. The problem — "one new app, many places to edit"

Today, adding a public-facing app (like `zengarden` or `sling-matrix`) means a
human — or Agent Smith — must touch a scattered set of files, each with the
app's name, content root and sub-domain hard-coded. Miss one and the app is
half-deployed.

### Inventory of touch-points per new public app

| # | Concern | File(s) today | What is app-specific |
|---|---|---|---|
| 1 | **Traefik router** (subdomain, TLS/ACME, optional basicAuth) | [devops/ansible/roles/traefik/templates/compose.edge.yml.j2](../devops/ansible/roles/traefik/templates/compose.edge.yml.j2) (container labels) + [dynamic.yml.j2](../devops/ansible/roles/traefik/templates/dynamic.yml.j2) (file-provider routers/middlewares) | `Host(\`<app>.<domain>\`)`, cert resolver, which middlewares apply |
| 2 | **Apache webcache vhost** (short-URL rewrites, proxy, cache policy) | one `*.conf.j2` per app, e.g. [zengarden.conf.j2](../devops/ansible/roles/webcache/templates/zengarden.conf.j2), [www.conf.j2](../devops/ansible/roles/webcache/templates/www.conf.j2) | `ServerName`, `DocumentRoot`, every `ProxyPass* /content/slingslop/<app>` and `/apps/slingslop/<app>`, home page node |
| 3 | **Webcache bind-mount + render task** | [compose.edge.yml.j2](../devops/ansible/roles/traefik/templates/compose.edge.yml.j2) (volume line) + [roles/webcache/tasks/main.yml](../devops/ansible/roles/webcache/tasks/main.yml) (stat/remove/render loop) | one more `*.conf` file to mount and render |
| 4 | **Sling short-URL mapping** | *currently faked in Apache `RewriteRule`/`ProxyPassMatch`* — no real Sling mapping yet | `/<app>/` ⇄ `/content/slingslop/<app>/` |
| 5 | **Launcher OSGi config / feature aggregate** | [launcher/src/main/features/*.json](../launcher/src/main/features/launcher.json) | per-app OSGi configs (e.g. resolver mapping, rendition service), feature include |
| 6 | **Doc / var comments** | [group_vars/all/main.yml](../devops/ansible/inventory/group_vars/all/main.yml) header lists every published sub-domain | the sub-domain list |
| 7 | **Sling context-aware config** | content packages (`sling:configs`) | *runtime config — see §7, deliberately out of scope for now* |

---

## 2. Why CONGA — and the key insight

CONGA ([general concepts](https://devops.wcm.io/conga/general-concepts.html))
splits configuration into a **developer** half and an **operations** half:

- **Developers** own **roles**, **role variants** and **Handlebars file
  templates**, plus the **configuration-parameter definitions** (with defaults
  and doc-comments). This is the reusable, versioned, merge-stable part.
- **Operations** own **environments** → **nodes** → **roles/variants**, and the
  **tenants** with their concrete values (`domain`, which apps, basicAuth…).

### The insight: *"a public Slingslop app" is a CONGA tenant*

CONGA's [multitenancy section](https://devops.wcm.io/conga/general-concepts.html#Multitenancy)
describes our exact problem, almost verbatim:

> *"…in some occasions the system configuration is affected as well, e.g. **one
> vhost file for each tenant's website** in the webserver configuration; **Short
> URL Mapping** … for each website. Using the **Tenant Multiply** plugin it is
> possible to generate multiple configuration files (one per tenant) based on a
> single file template."*

So each Slingslop app = **one tenant**. Its `contentRoot`, `subdomain` and
cache/gate flags = **tenant config values**. Traefik router, Apache vhost and
Sling mapping = **files multiplied per tenant** from a single template.

Adding an app becomes: **append one tenant block** to an environment file and
re-run `mvn -pl devops/conga conga:generate`. No template edits, no five-file safari.

---

## 3. Responsibility split (developer vs. operations)

```
┌──────────────────────────── DEVELOPER (this repo, versioned) ───────────────────────────┐
│ devops/conga/src/main/                                                                    │
│   roles/                    ← what a node/app *is*: files, variants, param definitions   │
│   templates/                ← Handlebars: traefik router, apache vhost, sling mapping,   │
│                               feature-model cfg.json, group_vars fragment                │
│   validation/               ← optional CONGA validators (e.g. vhost syntax)              │
│   → sensible DEFAULTS + inline doc-comments on every parameter                           │
└──────────────────────────────────────────────────────────────────────────────────────────┘
                                          │ mvn conga:generate
                                          ▼
┌──────────────────────────── OPERATIONS (per node / per fork) ───────────────────────────┐
│ devops/conga/src/main/environments/                                                       │
│   prod-motorbrot.yaml       ← domain: motorbrot.org, node vps1, tenant list, ACME on     │
│   local.yaml                ← domain: slingslop.local, self-signed, basicAuth off        │
│   staging.yaml              ← …                                                          │
│   (secrets & host facts may be pulled from ansible vault/inventory via the ansible plugin)│
└──────────────────────────────────────────────────────────────────────────────────────────┘
                                          │ generated files
                                          ▼
                     devops/ansible  copies/mounts them onto the VPS
```

The golden rule (CONGA's own):
**developers never encode a domain or an app list**; **operations never edit a
template**. A fork lives entirely on the operations side.

---

## 4. Proposed CONGA definition model

### 4.1 Node roles (developer-owned)

| Role | Purpose | Variants |
|---|---|---|
| `slingslop-app-base` | **No files.** Holds the shared per-app parameter *definitions* + defaults + doc (§4.3). Inherited by the roles below via `inherits:` so those params are declared once. | — |
| `slingslop-edge` | Traefik reverse proxy: per-app routers, TLS/ACME, middlewares | `acme` (Let's Encrypt) · `selfsigned` (local VM) |
| `slingslop-webcache` | The cache + short-URL layer in front of Sling | **`apache`** (mod_cache, default) · `varnish` (VCL) · `nginx` (proxy_cache) |
| `slingslop-runtime` | The Sling instance (feature-model launcher, OSGi configs) | `composite-nodestore` · `segment` |

> **Role inheritance** (`Role.inherits`, confirmed present in CONGA ≥ 1.20) lets
> `slingslop-edge` and `slingslop-runtime` both `inherits: [ slingslop-app-base ]`
> and reuse the same parameter menu instead of duplicating `contentRoot`,
> `subdomain`, `homePage`, … in each. Defaults and doc-comments live in exactly
> one place.

> **Webcache variants** — `apache`, `varnish` and `nginx` are *alternative
> implementations of the same job*: cache Sling's output and re-expand short
> URLs for each public app. Each variant ships its **own template** for the
> per-app file (gated by `RoleFile.variants`), yet all three read the **same
> tenant params** (`contentRoot`, `subdomain`, `homePage`, …). Operations picks
> one per node with `variant:`; the app catalogue and everything downstream is
> unchanged. This is precisely CONGA's node-role-variant axis (§4.2): one
> deployment slot, three renderings.

**Files on `slingslop-edge`** (Traefik). In a `RoleFile`, `file`/`dir` names use
shell-style `${…}` substitution (e.g. `${tenant}`); the template *body* uses
Handlebars `{{ }}`:

*Per-app* (`multiply: tenant`, filtered by tenant role via `multiplyOptions.roles`):

- `traefik/dynamic/router-${tenant}.yml` — one router per public app

*Node-level aggregate* (rendered once, iterating **`tenantsByRole`** — **not** multiplied):

- `traefik/traefik.yml` — static Traefik config (entrypoints, ACME resolver)
- `traefik/dynamic/middlewares.yml` — shared middlewares (`sec-headers`, `editor-basicauth`, …)

**Files on `slingslop-webcache`** — one per-app cache config, but the `template:`
is **variant-specific** (same logical file, three templates each gated by `variants:`):

- `${tenant}.conf` · `variants: [apache]` — Apache `mod_cache` + `mod_proxy` vhost
- `${tenant}.vcl`  · `variants: [varnish]` — Varnish VCL (backend, TTLs, filters, short-URL restore)
- `${tenant}.nginx.conf` · `variants: [nginx]`  — nginx `server{}` with `proxy_cache`
- plus a node-level include/mount list built from `{{#each tenantsByRole.public-cached}}…{{/each}}`

> **Distinct output names are required.** CONGA de-duplicates `RoleFile` entries
> by their `dir` + `file` key *before* variant filtering, so two variants cannot
> both emit `webcache/${tenant}.conf` — the second silently loses. The nginx
> file is therefore `${tenant}.nginx.conf` (still matches nginx's `conf.d/*.conf`
> include). A node runs a single variant, so its `webcache/` dir holds only that
> engine's files.

> **One shared parameter set for all three engines.** The security + cache knobs
> — `allowedMethods`, `denySelectors`, `denyPathPrefixes`, `jsonAllowlist`,
> `uncachedPatterns`, `passthroughPaths`, `serverAliases`, `htmlTtlSeconds`,
> `staticTtlSeconds` — live once in `slingslop-app-base` and are rendered by each
variant in its own syntax. See [devops/webcache.md](../devops/webcache.md) for the
  per-app knob table and [devops/conga-handlebars-101.md](../devops/conga-handlebars-101.md)
  for a how-to on promoting a hard-coded value to a tenant parameter.

**Files on `slingslop-runtime`:**

- `slingmappings/jcr_root/etc/map/https/${tenant}/.content.xml` — **multiply: tenant** → one `/etc/map` subtree per app (host-based `sling:Match` / `sling:internalRedirect`). Because `/etc/map` is *JCR content* (not an OSGi config), a **content-package post-processor** wraps all per-app subtrees into a single **FileVault (vlt) zip**, `slingslop.slingmappings` — it ships as a *package*, not inside a feature JSON.
- `features/apps-aggregate.json` — node-level; iterates `tenantsByRole` to include each app's launcher feature, **and lists the generated `slingslop.slingmappings` vlt zip right next to `slingslop.complete`** in `content-packages:ARTIFACTS` (see [launcher/src/main/features/launcher.json](../launcher/src/main/features/launcher.json)).
- per-app OSGi configs that *are* real config (e.g. the FFM rendition service) stay as `.cfg.json` fragments in the feature model.

A `RoleFile` also offers, for free, several things this concept would otherwise
hand-roll: `condition:` (emit ACME-only lines when `acme=true` without a
separate variant), `validators`/`validatorOptions` (real vhost/JSON syntax
checks at generate-time), and `fileHeader` (the "# Managed by … — do not edit"
banner, injected automatically).

### 4.2 Tenant roles (developer-owned) — and the two orthogonal axes

The CONGA object model has **two independent role axes** — keep them distinct:

- **Node-role *variant*** (`NodeRole.variant`, e.g. `acme` / `selfsigned`): a
  *per-box* flavor. It selects *which template files render on that node* (a
  `RoleFile.variants` list gates the file). Analogous to the diagram's
  `stdVariant` vs `secureVariant` apache nodes.
- **Tenant *roles*** (`Tenant.roles: List<String>`, below): *per-app capability
  tags*. They feed `multiplyOptions.roles` to decide *which apps* a
  `multiply: tenant` file is emitted for.

Tenant roles express *how an app is exposed*, independent of node roles:

| Tenant role | Adds | Typical app |
|---|---|---|
| `public-cached` | Apache webcache vhost + Traefik router (no auth) + Sling mapping | `zengarden`, `sling-matrix` |
| `public-uncached` | Traefik router straight to Sling + Sling mapping (no Apache) | API-ish / dynamic apps |
| `gated` | as `public-cached/uncached` **+** `editor-basicauth` middleware + optional `ipAllowList` | authoring/preview hosts |
| `internal` | Sling mapping only, no external host | building-block apps behind others |

So the per-app webcache vhost is declared like this (dev side):

```yaml
# in the slingslop-edge role
files:
  - file: "${tenant}.conf"
    dir: webcache
    template: vhost.conf.hbs
    multiply: tenant
    multiplyOptions:
      roles: [ public-cached ]   # only apps tagged public-cached get a vhost
    fileHeader: "# Generated by CONGA — do not edit. Source: vhost.conf.hbs"
    validators: [ ]              # e.g. an apachectl-configtest post-check
```

### 4.3 Configuration parameters (developer defines defaults + doc)

Every parameter is declared **once**, in the role definition, with a default and
a `doc:` comment. This is the self-documenting "menu" ops fills in per app.
Illustrative `role.yaml` excerpt (final syntax per
[CONGA YAML definitions](https://devops.wcm.io/conga/yaml-definitions.html)):

```yaml
# devops/conga/src/main/roles/slingslop-edge.yaml   (DEVELOPER-OWNED)
role:
  # ---- node-level, one value for the whole box ----
  domain:
    doc: "Base domain. Public host of an app becomes <subdomain>.<domain>."
    # no default — operations MUST set it per environment
  acme:
    doc: "Use Let's Encrypt. false → Traefik self-signed cert (local/private VM)."
    default: true

  # ---- per-tenant (per-app) values ----
  contentRoot:
    doc: "JCR content root, e.g. /content/slingslop/zengarden. Drives proxy + mapping."
    # no default — every app must declare it
  appsRoot:
    doc: "Scripts/clientlib root under /apps, e.g. /apps/slingslop/zengarden."
    default: "/apps/slingslop/${tenant}"
  subdomain:
    doc: "Left-most DNS label. Final host = <subdomain>.<domain>. Use 'www' for the primary app."
    default: "${tenant}"
  homePage:
    doc: "Home page node name mapped from '/', e.g. 'home' → home.html."
    default: "home"
  cached:
    doc: "Serve through Apache mod_cache webcache. false → Traefik proxies Sling directly."
    default: true
  gated:
    doc: "Require basicAuth (editor-basicauth middleware) before reaching the app."
    default: false
  allowlistCidrs:
    doc: "Extra IP allow-list for gated hosts. Empty → basicAuth only."
    default: []
```

Because defaults reference `${tenant}`, a well-behaved app that follows the
`/content/slingslop/<name>` + `/apps/slingslop/<name>` convention needs to
declare **only** `contentRoot` (and sometimes `subdomain`/`homePage`).

These definitions live in the **`slingslop-app-base`** role (§4.1) and are pulled
into `slingslop-edge`/`slingslop-runtime` with `inherits:` — so the parameter
menu, its defaults and its doc-comments exist in a single file.

### 4.4 Config merge precedence (which value wins)

Every `Environment`, `Node`, `NodeRole`, `RoleConfig` and `Tenant` is a
`Configurable` (carries a `config: Map`). CONGA merges them; **later overrides
earlier**. Per the CONGA object model, the order (lowest → highest priority) is:

| # | Layer | Owner | Typical use |
|---|---|---|---|
| 1 | Role default `config` | dev | the documented defaults from §4.3 |
| 2 | Role variant `config` | dev | `acme` vs `selfsigned` deltas |
| 3 | Environment global `config` | ops | `domain`, ACME email |
| 4 | Node `config` | ops | box-specific overrides |
| 5 | Environment `roleConfig` (per role) | ops | "all `public-cached` apps use TTL=10" in one place |
| 6 | Node-role `config` | ops | this role *on this node* |
| 7 | **Tenant `config`** | ops | **per-app values win over everything** |

The practical upshot: **app-specific values (tenant, level 7) always override
role defaults (level 1)**, so an app can special-case one setting without
touching the shared template — exactly what we want for a scaffolding tool.

---

## 5. Environments — what operations edits (incl. the fork story)

An environment file is the **entire** per-deployment surface. Adding an app =
one tenant block; forking = copy the file and change `domain` + the tenant list.

```yaml
# devops/conga/src/main/environments/prod-motorbrot.yaml   (OPERATIONS-OWNED)
# NOTE: environment YAML uses TOP-LEVEL keys (nodes/config/tenants) — there is
# no `environment:` wrapper. `domain` lives in the environment-global `config:`
# so both node- and tenant-scoped values can resolve `${domain}`.
config:
  domain: motorbrot.org            # ← the only place the real domain lives

nodes:
  - node: vps1
    roles:
      - role: slingslop-edge
        variant: acme
      - role: slingslop-webcache
        variant: apache
      - role: slingslop-runtime
        variant: composite-nodestore

tenants:
  - tenant: zengarden
    roles: [ public-cached ]
    config:
      contentRoot: /content/slingslop/zengarden

  - tenant: sling-matrix
    roles: [ public-cached ]
    config:
      contentRoot: /content/sling-matrix
      appsRoot: /apps/sling-matrix   # deviates from the /apps/slingslop/<t> default
      subdomain: www                 # sling-matrix is the primary www.* site
      serverAliases: [ "${domain}" ]
      passthroughPaths: [ /bin/public ]

  - tenant: digitalmedia
    roles: [ gated ]                 # author/preview only, basicAuth-gated
    config:
      contentRoot: /content/digitalmedia
      backendHost: sling             # uncached: Traefik straight to Sling
      backendPort: 8080
      middlewares: [ sec-headers, editor-basicauth ]
```

### Fork example — `example-fork.yaml`

```yaml
# A downstream fork: different domain, different app set, no auth changes.
config:
  domain: someoneelse.org            # ← changed
nodes:
  - node: box
    roles: [ { role: slingslop-edge, variant: acme },
             { role: slingslop-webcache, variant: apache },
             { role: slingslop-runtime, variant: composite-nodestore } ]
tenants:
  - tenant: zengarden                # they keep zengarden
    roles: [ public-cached ]
    config: { contentRoot: /content/slingslop/zengarden }
  - tenant: myshop                   # ← their own new app
    roles: [ public-cached ]
    config: { contentRoot: /content/myshop, subdomain: www, serverAliases: [ "${domain}" ] }
```

Templates, roles and defaults are **untouched** in the fork, so `git pull` from
upstream merges cleanly. This is the core payoff of the dev/ops split.

### Even cleaner fork: consume the roles as a published artifact

A fork that *copies* the `devops/conga/` module still has to merge upstream template
changes by hand. CONGA's **`Environment.dependencies`** (present in ≥ 1.20)
removes even that: publish the Slingslop roles + templates as a Maven artifact,
and let a fork's environment file pull them in — the fork then contains **only**
its environment YAML, no roles/templates at all.

```yaml
# a fork repo that owns nothing but its environment
# roles + Handlebars templates come from the upstream release, by coordinates
dependencies:
  - mvn:org.motorbrot/slingslop.conga-roles/1.4.0
config:
  domain: someoneelse.org
nodes:
  - node: box
    roles: [ { role: slingslop-edge, variant: acme },
             { role: slingslop-webcache, variant: apache },
             { role: slingslop-runtime, variant: composite-nodestore } ]
tenants:
  - tenant: myshop
    roles: [ public-cached ]
    config: { contentRoot: /content/myshop, subdomain: www }
```

Upgrading is now a version bump, not a merge. Use this for external forks; keep
the in-repo `devops/conga/` module (source templates) for Slingslop itself.

### Local-development environments — topology by role selection

The *same* roles compose different **local** stacks purely by choosing which
node-roles/variants an environment assigns — no "dev vs. prod" branches in the
templates. The three local topologies we care about:

| Environment | Roles on the single node | Access | Use |
|---|---|---|---|
| `local-plain` | `slingslop-runtime` only | Sling directly on `:8080` (via `launcher/launch.sh`) | fastest inner loop; hit `/content/…` (or Sling-mapped) URLs |
| `local-webcache` | `slingslop-runtime` + `slingslop-webcache:apache` | webcache Docker `:80 → :8080` | verify caching + inbound short-URL re-expansion locally |
| `local-full` (vagrant) | `slingslop-edge:selfsigned` + `slingslop-webcache` + `slingslop-runtime` + observability | Traefik `:443` on `slingslop.local` | full prod-like stack incl. TLS + Grafana/Loki |

```yaml
# devops/conga/src/main/environments/local-webcache.yaml   (OPS)
config:
  domain: localhost
nodes:
  - node: localhost
    roles:
      - role: slingslop-webcache
        variant: apache
tenants:
  - { tenant: zengarden,    roles: [public-cached], config: { contentRoot: /content/slingslop/zengarden } }
  - { tenant: sling-matrix, roles: [public-cached], config: { contentRoot: /content/sling-matrix, appsRoot: /apps/sling-matrix, subdomain: www } }
```

`local-plain` simply omits the edge + webcache roles, so CONGA emits only the
runtime feature/OSGi files that `launch.sh` consumes — nothing else. `local-full`
adds `slingslop-edge:selfsigned` and the observability roles. The **topology is
the role list**; the templates never learn which stack they're in.

---

## 6. How the generated output meets Ansible

CONGA only *generates files* — it is explicitly **not** a deployment tool. Two
integration patterns; we recommend a hybrid.

> **Resolved — the seam is wired (Pattern A).** `devops/ansible` now **consumes the
> CONGA output** instead of hand-rendering per-app `.j2`:
>
> - The **webcache** role ships `devops/conga/target/configuration/<env>/<node>/webcache/`
>   (one `<app>.conf` per public-cached tenant) and compose bind-mounts the whole
>   dir over the image's `sites-enabled`. The old `www.conf.j2`/`zengarden.conf.j2`
>   render tasks are gone.
> - The **traefik** role copies the generated `traefik/dynamic/router-<app>.yml`
>   into the file-provider dir; the per-app container labels were removed from
>   `compose.edge.yml.j2`. Ansible still owns `traefik.yml` + `dynamic.yml` (the
>   shared middlewares those routers reference, plus the obs/dashboard routers),
>   so CONGA's own `traefik.yml`/`middlewares.yml` are deliberately **not** shipped.
> - `conga_env`/`conga_node` (group_vars; `local-full`/`localhost` for the VM,
>   `prod-motorbrot`/`vps1` for prod) select which rendered tree is shipped. A
>   fork points them at its own environment file.
>
> **What still requires a build, by design:** an app's `/content` is baked into
> the `slingslop` image, so a new app reaches a host only after the image is
> rebuilt (`mvn … -Ddocker.skip=false`). The Vagrant harness does this for you —
> it builds the image locally and `docker load`s it into the VM
> (`slingslop_image_load_tar`), so no registry push is needed for a local test.
> With both wired, **adding a tenant + re-running the playbook publishes the new
> host** end-to-end. Walk-through: root
> [ReadMe](../ReadMe.md#adding-a-new-public-facing-app).

### Pattern A — CONGA renders the final config files, Ansible ships bytes *(recommended for §1 items 1–5)*

`mvn -pl devops/conga conga:generate` writes, per node, e.g.:

```
devops/conga/target/configuration/prod-motorbrot/vps1/
  traefik/traefik.yml
  traefik/dynamic/middlewares.yml
  traefik/dynamic/router-zengarden.yml
  traefik/dynamic/router-sling-matrix.yml
  traefik/dynamic/router-digitalmedia.yml
  webcache/zengarden.conf
  webcache/sling-matrix.conf
  slingmappings/jcr_root/etc/map/https/<host>/.content.xml   ┐ FileVault package
  slingmappings/META-INF/vault/{filter,properties}.xml       ┘ source (zip = follow-up)
  features/apps-aggregate.json   ← lists slingslop.complete + slingslop.slingmappings
  ...
  model.yaml            ← machine-readable list of tenants + generated files
```

> Note: the on-disk layout is `<env>/<node>/<dir>/…` — the role name is **not**
> part of the output path (files from all roles on a node share one tree).

Ansible then simplifies to a **generic sync** — the per-app render tasks in
[roles/webcache/tasks/main.yml](../devops/ansible/roles/webcache/tasks/main.yml)
and the hand-maintained bind-mount / router blocks disappear:

```yaml
# roles/webcache/tasks/main.yml  (AFTER)
- name: Ship generated webcache vhosts
  ansible.builtin.copy:
    src: "{{ conga_out }}/{{ node }}/slingslop-webcache/"
    dest: "{{ slingslop_root }}/data/webcache/"
  notify: restart webcache
```

The Traefik file-provider already watches `data/traefik/dynamic/` — dropping one
more `router-<app>.yml` there needs **zero** Ansible change. The self-healing
"stale bind-mount directory" dance is no longer per-named-file.

CONGA's exported `model.yaml` gives Ansible the authoritative tenant list, so
loops that *must* stay in Ansible (e.g. compose bind-mounts) iterate over data,
not hard-coded names.

### Pattern B — CONGA renders an Ansible `group_vars` fragment, existing Jinja loops over it

CONGA generates a single `tenants.yml` var file; the current `.j2` templates are
rewritten to `{% for t in tenants %}`. Lighter migration, but keeps complex
vhost logic in Jinja. Good **interim** step.

### The CONGA Ansible plugin closes the loop

The [CONGA Ansible plugin](https://devops.wcm.io/conga/plugins/ansible/) lets
CONGA **read our existing `inventory/` and `vault.yml`**. So `domain`, ACME
email and the htpasswd secrets can stay in Ansible (single source of truth) and
be *pulled into* CONGA at generate-time — we don't duplicate secrets into
environment YAML.

---

## 7. Per-concern mapping (and where CONGA stops)

| Concern | CONGA template produces | Notes |
|---|---|---|
| **Traefik** | `router-<app>.yml` file-provider fragment (Host rule, cert resolver, middleware chain from `gated`/`allowlistCidrs`) | dropped into the watched `dynamic/` dir; container-label routers move here too for uniformity |
| **Apache webcache** | `<app>.conf` vhost from a single template parameterised by `contentRoot`/`appsRoot`/`homePage`/`subdomain` | our real `mod_cache`+`mod_proxy` template, **not** the AEM dispatcher one; `varnish`/`nginx` are alternative variants of the same role (§4.1) |
| **Short URLs — both directions** | (a) a **host-based `/etc/map` FileVault package** (`slingslop.slingmappings`), CONGA-generated per env and **installed into the running Sling at deploy time** (Composum package manager), so **Sling writes short links into the markup** via `resourceResolver.map()` (outbound); (b) the webcache vhost/VCL/nginx conf that **re-expands `/…` back to `/content/<app>/…`** before proxying (inbound) | **Both required.** Deploy-time install (not image-baked) is what lets ops add a tenant — a new `host → contentRoot` — **without rebuilding the image** (§11). |
| **Launcher OSGi / feature** | per-app `configurations` fragment + include line in the apps aggregate | feeds [launcher/src/main/features](../launcher/src/main/features) — **Feature Model**, not provisioning |
| **Doc var-comment** | (optional) regenerate the sub-domain list header in a `group_vars` fragment | removes drift between docs and reality |
| **Sling context-aware config** | ❌ **out of scope** | CA config (`sling:configs`) is *runtime/site* config, which CONGA explicitly does **not** target. Keep it in content packages. |

Calling out the CA-config boundary explicitly answers the "Sling context-aware
config?" question: it stays in the app's content package because it is editable
at runtime by authors — CONGA is for *static, deploy-time* system config only.

**Short URLs need both halves, pulling in opposite directions:**

- *Outbound (Sling → browser):* the Sling resource-resolver **mapping** rewrites
  links as the page renders (`resourceResolver.map()`). **Implemented as a
  host-based `/etc/map` package**: `/etc/map/https/<host>` with
  `sling:internalRedirect` = the app's content root. CONGA generates it per env
  (`slingmappings/` vlt source), and **Ansible installs it into the running Sling
  at deploy time** (Composum package manager) — it is *not* baked into the image.
  `MappedLinkBuilder` calls `resolver.map(String)`, so the result is the
  **absolute canonical URL** for that host — e.g. `https://www.slingslop.local/home.html`
  instead of `/content/sling-matrix/home.html`.
  **Why `/etc/map` and not a static `resource.resolver.mapping` OSGi config:** the
  *same app* can serve *different* content roots per deployment (sample
  `/content/sling-matrix` vs prod `/content/realProdContent/anothermatrix`), so the
  `host → root` binding is a **per-deployment** fact ops must be able to add
  **without rebuilding the image**. `/etc/map` is JCR content installed at runtime;
  an image-baked OSGi config cannot vary per deployment. **Verified on the VM.**
- *Inbound (browser → Sling):* the request for `/home.html` carries no
  `/content/…` prefix, so the **webcache** (Apache rewrite / Varnish VCL / nginx)
  prepends it again before proxying to Sling. Emitted as the per-app vhost on
  `slingslop-webcache`.

Neither replaces the other: drop the Sling mapping and the markup emits long
URLs; drop the webcache rewrite and those short URLs 404. Because CONGA derives
**both** from the single `contentRoot`/`subdomain` tenant params, the inbound and
outbound halves stay in lock-step — the main reason to generate them rather than
hand-maintain two mirror-image rule sets (one of which also has to be re-authored
for each webcache variant).

---

## 8. Repository layout (as implemented)

```
devops/conga/                             ← Maven module (packaging `config`; runs io.wcm.devops.conga:conga-maven-plugin 1.20.0)
  README.md                              ← brief, plain-language front door (non-devops first; see below)
  pom.xml                                ← packaging `config` (auto-binds conga:generate); declares sling + ansible plugins
  src/main/
    roles/
      slingslop-app-base.yaml                 # shared param defs + defaults + doc (no files)
      slingslop-edge.yaml                     # Traefik;  inherits: [ slingslop-app-base ]; variants: acme | selfsigned
      slingslop-webcache.yaml                 # inherits app-base; variants: apache | varnish | nginx
      slingslop-runtime.yaml                  # inherits app-base; variants: composite-nodestore | segment
      # NOTE: tenant roles (public-cached / gated / …) are plain STRINGS, not
      # role-definition files — CONGA needs no tenant-*.yaml files.
    templates/
      slingslop-edge/
        traefik.yml.hbs
        middlewares.yml.hbs
        router.yml.hbs                        # file: router-${tenant}.yml, multiply: tenant
      slingslop-webcache/
        apache/vhost.conf.hbs                 # file: ${tenant}.conf, variants:[apache], multiply: tenant
        varnish/app.vcl.hbs                   # file: ${tenant}.vcl,  variants:[varnish], multiply: tenant
        nginx/app.conf.hbs                    # file: ${tenant}.conf, variants:[nginx],  multiply: tenant
      slingslop-runtime/
        mapping.content.xml.hbs               # → slingmappings/jcr_root/etc/map/https/${subdomain}.${domain}/.content.xml (multiply: tenant)
        filter.xml.hbs                        # → slingmappings/META-INF/vault/filter.xml (FileVault package source)
        properties.xml.hbs                    # → slingmappings/META-INF/vault/properties.xml
        apps-aggregate.json.hbs               # lists slingslop.slingmappings next to slingslop.complete
    environments/
      prod-motorbrot.yaml                     # OPS  (no dots — env name becomes a Maven classifier)
      local-plain.yaml                        # OPS — runtime only, launch.sh on :8080
      local-webcache.yaml                     # OPS — apache webcache :80
      local-full.yaml                         # OPS — vagrant: traefik+webcache+runtime
      example-fork.yaml                       # OPS (documented template for forks)
  target/configuration/…                      # generated (git-ignored)

devops/ansible/…                                 ← slimmed: generic "ship generated config" tasks
```

For external forks this whole `src/main/roles` + `templates` tree can instead be
**published as a Maven artifact** (e.g. `org.motorbrot/slingslop.conga-roles`)
and consumed via `Environment.dependencies` (see §5) — the fork keeps only its
`environments/*.yaml`.

### READMEs lead with a brief, plain-language concept (non-devops first)

Both `devops/conga/README.md` and the existing [devops/README.md](../devops/README.md)
target someone who is **not** a DevOps engineer. Each must open with a short,
no-jargon *concept-first* section **before** any CONGA/Ansible internals:

1. **What it does** — one sentence ("turns your app into a live sub-domain").
2. **Add your app** — the single `tenant:` block to copy, with the 2–3 fields to fill.
3. **Generate & ship** — the one command to run.

The deep mechanics (roles, variants, `multiply`, merge order, FileVault packaging)
stay **below the fold** — or here in this concept doc. The README is a quick front
door, not a manual.

The README should also carry a short **“Advanced — running your own fleet”** note.
Slingslop itself is a single mono-repo (roles live in-tree, secrets in the
committed encrypted vault). A **larger org** that runs *several* independent
Slingslop deployments can instead **publish the `devops/conga` roles + templates
as a versioned artifact** (e.g. `your-org.conga-roles`) and have each deployment
repo depend on it via `Environment.dependencies` (§5). Why they'd bother:

- **Version-pinned upgrades** — each deployment bumps the roles version on its own
  schedule instead of cherry-picking template changes across repos.
- **Ownership boundary** — a platform team owns the roles/templates (and, if they
  diverge, the shared Ansible roles); product teams own only their
  `environments/*.yaml` + their vault.
- **One source, many boxes** — the same reviewed templates drive every deployment,
  so config can't drift between them.

For a single mono-repo (the default) none of this is needed — keep the in-tree
module and skip the release step.

---

## 9. Agent Smith integration

The [Agent Smith skill](agent-skills/create-Sling-app-with-Agent_Smith.md) and
the [cloud instructions](../.github/copilot-instructions.md) currently stop at
the build. With CONGA, a new **13th task** for public-facing apps becomes tiny
and mechanical:

> **Task 13 — Register the app for deployment (public-facing only):**
> Append a tenant block to `devops/conga/src/main/environments/*.yaml` mapping the new
> app. Derive values from existing Agent Smith variables:
>
> | Agent Smith variable | Tenant config |
> |---|---|
> | `CONTENT_ROOT` | `contentRoot` |
> | `APPS_ROOT` | `appsRoot` (usually the default) |
> | `PROJECT_NAME` (hyphenated) | `tenant` / `subdomain` |
> | home page node | `homePage` |
> | "is it gated?" | `roles: [ gated ]` vs `[ public-cached ]` |
>
> Then run `mvn -pl devops/conga conga:generate` to prove it renders. **Do not** hand-edit
> Traefik/webcache/launcher files — CONGA owns them now.

This replaces the error-prone five-file edit with a single, validated data
change — exactly the reliability win that matters for non-interactive cloud runs.

**GitOps trigger.** The tenant change deploys itself: a change under
`devops/conga/src/main/**` on the deploy branch fires the **`deploy-edge`** job in
[`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml), which regenerates
the CONGA config and runs
[`devops/ansible/playbooks/deploy-tenant-edge.yml`](../devops/ansible/playbooks/deploy-tenant-edge.yml)
to ship the new tenant's Traefik router + webcache vhost + Sling `/etc/map`
mapping to the running host — **no image rebuild** (the mapping is installed into
the live Sling via the Composum package manager). Adding a tenant is therefore a
pure data change that auto-deploys the reverse-proxy + short-URL layer.

---

## 10. Rollout phases

Phases 1–7 are **implemented** in `devops/conga/`; phase 8 remains an opt-in.

1. ✅ **Bootstrap module.** `devops/conga/` Maven module (`packaging: config`) wired
   to `conga:generate`; plugin chain (core 1.20.0 + sling/ansible 1.6.0) resolves
   and runs on JDK 25.
2. ✅ **Webcache first (highest pain).** `apache/vhost.conf.hbs` authored from the
   current `zengarden.conf`/`www.conf`; tenant params defined; both apps generate
   functionally-equivalent vhosts (`+ varnish` / `+ nginx` variants).
3. ✅ **Traefik routers.** Per-app `dynamic/router-${tenant}.yml` fragments +
   node-level `traefik.yml` and `middlewares.yml` generated once.
4. ⚠️ **Short URLs (both directions).** Outbound half generated as a **FileVault
   package *source*** (`slingmappings/jcr_root/etc/map/…` + `META-INF/vault/…`);
   inbound half is the generated webcache vhost/VCL/nginx conf. Zipping the vlt
   source into an installable artifact + wiring it into the launcher is the one
   remaining follow-up (see §11).
5. ✅ **Launcher features.** `features/apps-aggregate.json` lists `slingslop.complete`
   plus the generated `slingslop.slingmappings` package.
6. ✅ **Agent Smith Task 13.** The skill now has a §2.8 registration task; the
   cloud instructions count 14 tasks.
7. ✅ **Docs & fork template.** `example-fork.yaml` ships; this doc + `devops/conga/README.md`
   updated.
8. ⬜ **Publish roles artifact (optional).** Release `slingslop.conga-roles` so
   external forks can consume roles/templates via `Environment.dependencies`
   instead of copying the `devops/conga/` module. Not needed for the mono-repo.

Each phase is independently shippable and byte-diff-verifiable, so we never do a
big-bang cutover.

---

## 11. Decisions & open questions

### Decided

- **Secrets: committed, encrypted (GitOps).** Because deployment is GitOps, the
  Ansible Vault file stays **committed** (ansible-vault-encrypted) and CONGA
  reads it via the [Ansible plugin](https://devops.wcm.io/conga/plugins/ansible/)
  at generate-time — secrets are never duplicated into environment YAML. The
  `main` branch additionally carries **non-sensitive defaults for the local
  environment** (self-signed TLS, basicAuth off, throw-away credentials) so a
  fresh clone runs locally **without a vault password**; a real vault key is only
  needed to render prod.
- **Generated output: build-time only, never committed.** The rendered config is
  tailored to one specific box, so it lives in a git-ignored `target/` and is
  produced on demand — committing it would only add per-box noise and merge
  conflicts. Ansible ships it straight to the host (§6).
- **Forks: simple mono-repo (in-tree module).** Slingslop keeps roles + templates
  in-tree at `devops/conga/`. Publishing a versioned `slingslop.conga-roles`
  artifact is an **opt-in for larger orgs** running many independent deployments
  — see the README guidance (§8) and §5. For a single mono-repo it's unnecessary.
- **CONGA vs. a plain Ansible `tenants:` loop — CONGA it is.** A Jinja loop only
  helps the Ansible-shipped files (traefik routers, webcache vhosts). But two of
  the required outputs live on the **build/Maven side, not the host side**: the
  Sling **Feature Model** fragments and the **FileVault `/etc/map` vlt package** —
  Ansible can neither render nor package those. One tenant list has to drive all
  **three** artifact classes (reverse-proxy config, feature JSON, vlt content
  package), plus webcache **variants**, the **dev/ops split** and **validators** —
  exactly CONGA's job; a `tenants:` loop reaches barely half of it. We still use
  **Pattern B** (a generated Ansible `tenants` var) as a low-risk *first step* for
  the webcache files, then migrate to Pattern A.

### Still open

- **Keep the CONGA env `domain` and the Ansible `domain` in lock-step.** Pattern A
  is now wired (Ansible ships the CONGA output — see §6 and the *Resolved* note
  below), but each CONGA environment hard-codes its `domain` (`prod-motorbrot` →
  `motorbrot.org`, `local-full` → `slingslop.local`). A deployment must select a
  `conga_env` whose domain matches its Ansible `domain`; a fork adds its own
  environment file. Closing this fully means having CONGA *read* the domain (and
  secrets) from the Ansible inventory/vault via the Ansible plugin (below).
- **Ansible vault wiring.** The Ansible plugin is declared as a dependency, but the
  environments currently carry **self-contained non-sensitive values** (so the
  build needs no vault key). Wiring the vault value-provider to pull `acmeEmail` /
  htpasswd from `devops/ansible` at generate-time is a follow-up once prod rendering
  is exercised end-to-end.
- **Which webcache engine is the default?** `apache` (mod_cache) is the default and
  is byte-checked against the current vhosts; `varnish` and `nginx` variants are
  generated but not yet validated against the cache-busting + query-string-DoS
  behaviour — keep `apache` until a variant is proven.

### Resolved during implementation

- **Outbound short URLs via a deploy-time `/etc/map` package.** CONGA generates the
  host-based `/etc/map` vlt **source** (`slingmappings/`); the `slingslop` Ansible
  role inserts the `properties.xml` DOCTYPE (which CONGA's XML file-header step
  strips), zips it, and **installs it into the running Sling at deploy time** via
  the **Composum package manager** (`POST /bin/cpm/package.upload.json` then
  `package.install.json<path>`), after the container is up. This is the mechanism
  that lets **ops add a tenant — a `host → contentRoot` binding — without rebuilding
  the image**: the same app can serve different content roots per deployment
  (sample `/content/sling-matrix` vs prod `/content/realProdContent/anothermatrix`).
  `MappedLinkBuilder` uses `resolver.map(String)`, so links come out as the
  absolute canonical host URL (`https://<host>/home.html`). A short-lived experiment
  with a static, image-baked `resource.resolver.mapping` OSGi config was dropped:
  it can't vary per deployment, which is the whole requirement. **Verified on the VM.**

### Resolved during implementation

- **Pattern A is wired end-to-end.** The `webcache` role ships CONGA's
  `webcache/*.conf` (whole dir bind-mounted over the image's `sites-enabled`);
  the `traefik` role copies CONGA's `router-*.yml` into the file-provider dir and
  the per-app container labels were dropped from `compose.edge.yml.j2`. Content
  ships via the image: the Vagrant harness builds the image locally and
  `docker load`s it into the VM (`slingslop_image_load_tar`, `slingslop_image_pull:
  false`) so a locally-added app is present without a registry push. Selected by
  `conga_env`/`conga_node` in group_vars (prod) and `vars.local.yml` (VM).
- **JDK 25.** `conga-maven-plugin` 1.20.0 (min JDK 21) builds and generates
  cleanly on the repo's JDK 25 toolchain. Plugin version is pinned.
- **No bundled `yaml` validator.** CONGA ships `xml` and (via the sling plugin)
  `json` validators, but **no `yaml` validator** — the Traefik `.yml` files use
  `validators: []`. XML (`/etc/map`, vault descriptors) and JSON (feature
  aggregate) files are validated at generate-time.
- **Environment name → Maven classifier.** The `config` packaging zips each
  environment using its name as a Maven classifier, which **must not contain
  dots** — hence `prod-motorbrot.yaml`, not `prod.motorbrot.yaml`.
- **`domain` must be environment-global.** A tenant-scoped `${domain}` only
  resolves if `domain` sits in the environment-global `config:` (not node config).
- **`${…}` in template bodies passes through literally** (Handlebars-only),
  so Apache `${RENDERER_URL}` and the Feature Model `${slingslop.launcher.version}`
  survive generation untouched (the latter escaped as `\${…}` where it lives in a
  role config value).

---

## 12. References

- CONGA overview & concepts — <https://devops.wcm.io/conga/general-concepts.html>
- CONGA YAML definitions — <https://devops.wcm.io/conga/yaml-definitions.html>
- CONGA Handlebars quickstart — <https://devops.wcm.io/conga/handlebars-quickstart.html>
- CONGA Sling plugin (cfg.json / feature model) — <https://devops.wcm.io/conga/plugins/sling/index.html>
- CONGA Ansible plugin (vault + inventory) — <https://devops.wcm.io/conga/plugins/ansible/>
- Training decks — DATM-57 (CONGA overview) & DATM-58 (AEM config with CONGA), <https://training.wcm.io/conga/>
- Current deployment architecture — [devops/README.md](../devops/README.md)
