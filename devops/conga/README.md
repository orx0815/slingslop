# devops/conga — deploy config generator

> **Status: implemented.** This module generates the reverse-proxy, webcache,
> Sling URL-mapping and launcher config for every public-facing Slingslop app
> from one per-app *tenant* block. Build it with `mvn -pl devops/conga clean package`.
> The full design rationale lives in
> [docs/conga-config-generation-concept.md](../../docs/conga-config-generation-concept.md).

Turns a Slingslop app into a live, cached sub-domain — it generates the reverse-proxy
config, the Sling URL mappings and the launcher wiring for you, so you don't edit
five files by hand.

---

## Add your app (the one thing you edit)

Copy one `tenant:` block into the environment file for your box
(`src/main/environments/<env>.yaml`) and fill in 2–3 fields:

```yaml
tenants:
  - tenant: myapp                                   # your app's short name
    roles: [ public-cached ]                        # public-cached | gated | internal
    config:
      contentRoot: /content/slingslop/myapp         # where your app's pages live
      # subdomain: www                              # optional; defaults to the tenant name
      # homePage: home                              # optional; defaults to "home"
```

That's it. Everything downstream (Traefik router, webcache vhost, short URLs) is
derived from those fields.

### A tenant is not always an app — *content-only* tenants

A **tenant** is a `host → content-root` mapping, **not** necessarily its own
application. Most tenants *do* point at their own app, but several tenants can
reuse **one** app at different content roots. We call a tenant that ships no code
of its own a **content-only tenant**.

Example: the `zen` tenant re-serves the existing **zengarden** app at a second
content root:

```yaml
  - tenant: zen
    roles: [ public-cached ]
    config:
      contentRoot: /content/slingslop/zen        # a *copy* of the zengarden content
      appsRoot:    /apps/slingslop/zengarden      # clientlibs come from the zengarden app!
```

Two things make this work:

- **`appsRoot` must point at the app that actually serves the clientlibs.** The
  webcache passes `appsRoot` through unrewritten (the app's css/js); the default
  is `/apps/slingslop/<tenant>`, which for a content-only tenant would be **wrong**
  and the URL-shortener would rewrite its css/js to a 404. Point it at the reused
  app's root instead (here, the zengarden app).
- **The content itself is not in git.** `/content/slingslop/zen` is created *live*
  in production (e.g. copy zengarden → zen in the author UI) and lives in the
  Sling **content volume**, which survives redeploys. Only the tenant block above
  is version-controlled. This is the intended "clone a sample → add a tenant →
  edit the prod content in place" workflow for spinning up a new site from an
  existing app without writing any code.

### Fine-grained per-app cache & security rules

The vhost is the front-line protection. Each tenant can additionally tune the
cache tier with **shared** knobs read identically by all three webcache engines
(apache / nginx / varnish): `serverAliases` (add domains), `passthroughPaths`
(allow-list paths to Sling), `jsonAllowlist`, `denySelectors`,
`denyPathPrefixes`, `uncachedPatterns`, `allowedMethods`, `htmlTtlSeconds` /
`staticTtlSeconds`. They are declared with defaults + docs in
[`roles/slingslop-app-base.yaml`](src/main/roles/slingslop-app-base.yaml). See
[devops/webcache.md](../webcache.md) for the knob table + worked example,
and [devops/conga-handlebars-101.md](../conga-handlebars-101.md) for a
**how-to on promoting any hard-coded template value to a tenant parameter**.

## Generate & ship

```bash
mvn -pl devops/conga conga:generate     # render config for the target box
# then let Ansible ship it:
cd devops/ansible && ansible-playbook -i inventory/hosts.yml playbooks/site.yml
```

You do **not** hand-edit the Traefik, webcache or launcher files — they are generated.

---

<details>
<summary><b>Why CONGA — and not just a plain Ansible <code>tenants:</code> loop?</b></summary>

Adding one public app produces output in **three different worlds**, and a
templating loop inside Ansible can only reach one of them:

1. **Reverse-proxy config** — Traefik routers + webcache vhosts. *Host-side files;
   Ansible could template these itself.*
2. **Sling Feature Model fragments** — launcher `.json` / `.cfg.json`. *Maven /
   build-side; Ansible can't meaningfully render these.*
3. **A FileVault `/etc/map` content package** (the outbound short URLs, installed
   next to the `complete`/all package). *A packaged JCR artifact; Ansible can't
   build a vlt zip at all.*

A `tenants:` loop is stuck at #1. CONGA drives **all three** artifact classes from
a **single tenant list**, and on top of that gives us:

- **webcache variants** — apache / varnish / nginx as swappable engines,
- a **developer/operations split** — devs own roles + templates, ops own the
  environment + the encrypted vault — which keeps **forks** merge-clean,
- **validators / file headers / post-processors** (incl. the FileVault packaging).

So it isn't a toss-up: the moment the short-URL outbound half became a vlt package
and the launcher features entered the picture, an Ansible-only approach was off the
table. (We still *start* by having CONGA emit an Ansible `tenants` var and letting
existing Jinja loop over it — a low-risk first step — then migrate to CONGA
rendering the final files.)

</details>

<details>
<summary><b>Advanced — running your own fleet</b></summary>

Slingslop is a single mono-repo: roles live in-tree here, secrets in the committed
**encrypted** Ansible vault, and the `main` branch carries **non-sensitive local
defaults** so a fresh clone runs locally with no vault key.

A larger org running **several** independent Slingslop deployments can instead
publish this `devops/conga` roles + templates tree as a **versioned artifact**
(e.g. `your-org.conga-roles`) and have each deployment repo depend on it via
CONGA's `Environment.dependencies`. Why bother:

- **version-pinned upgrades** — each deployment bumps the roles version on its own
  schedule instead of cherry-picking template changes;
- **ownership boundary** — a platform team owns the roles/templates, product teams
  own only their `environments/*.yaml` + their vault;
- **one source, many boxes** — the same reviewed templates drive every deployment,
  so config can't drift.

For a single mono-repo none of this is needed — keep the in-tree module.

</details>

---

**Learn more:** [docs/conga-config-generation-concept.md](../../docs/conga-config-generation-concept.md)
· CONGA docs: <https://devops.wcm.io/conga/>
