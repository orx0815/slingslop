# CONGA + Handlebars 101

How to turn a hard-coded value in a CONGA template into a per-app (tenant)
parameter, and the Handlebars idioms used across the Slingslop templates. The
worked example lives in the webcache templates, but the workflow is the same for
any role (edge, runtime, …). Background:
[docs/conga-config-generation-concept.md](../docs/conga-config-generation-concept.md).

---

## Make a static value configurable

When a template still hard-codes something a tenant should control, promote it to
a parameter. The round trip is five small steps — here we turn a hard-coded
`Header set X-Robots-Tag` into a per-app `robotsTag` knob:

1. **Declare the parameter + default + doc** in the shared menu
   [`roles/slingslop-app-base.yaml`](../devop/conga/src/main/roles/slingslop-app-base.yaml)
   (inherited by every webcache/edge/runtime role, so it exists in one place):

   ```yaml
   config:
     # Value for the X-Robots-Tag response header. "" = don't emit it.
     robotsTag: ""
   ```

2. **Reference it in the template(s)** with a Handlebars expression `{{ }}`
   (the file/dir *names* use `${…}`; only the template *body* uses `{{ }}`).
   Guard optional output with `{{#if}}`:

   ```handlebars
   {{#if robotsTag}}
      Header set X-Robots-Tag "{{robotsTag}}"
   {{/if}}
   ```

3. **Set it per tenant** (only where it differs from the default) in the
   environment file — nothing else changes:

   ```yaml
   - tenant: staging
     roles: [ public-cached ]
     config:
       contentRoot: /content/staging
       robotsTag: "noindex, nofollow"
   ```

4. **Regenerate & cross-check**: `mvn -pl devop/conga clean package`, then read
   `devop/conga/target/configuration/<env>/<node>/webcache/<app>.conf`. Because a
   webcache change must work in **all three** engines, spin the
   [webcache bench](../docker/webcache-bench/docker-compose.yml) up and diff the
   apache / nginx / varnish behaviour side-by-side.

5. **Ship** it with the normal playbook run.

---

## Handlebars idioms used in these templates

(jknack Handlebars via CONGA.)

- **Lists** → iterate with `{{#each}}`; `{{this}}` is the current item:

  ```handlebars
  {{#each serverAliases}}
     ServerAlias {{this}}
  {{/each}}
  ```

- **Regex alternation from a list** without needing `@last` — seed the group
  with a never-matching `$^`, then append `|item` for each entry. An **empty**
  list collapses to `(?:$^)`, which matches nothing (a safe "block none"):

  ```handlebars
  RewriteRule "\.(?:$^{{#each denySelectors}}|{{this}}{{/each}})\." - [F,L,NC]
  ```
  renders (default list) as `\.(?:$^|infinity|tidy|sysview|docview|children|query)\.`

- **Conditionals** → `{{#if flag}}…{{else}}…{{/if}}` (see the ACME branch in
  `slingslop-edge/traefik.yml.hbs`).

- **Derived values** → compute in the role default with CONGA string helpers
  instead of asking ops to repeat themselves, e.g. `shortContentPath` in
  `slingslop-app-base.yaml`:

  ```yaml
  shortContentPath: "${stringUtils:removeStart(contentRoot, '/content')}"
  ```

- **Escaping a literal `${…}`** the Feature Model must keep (so CONGA doesn't
  resolve it) → backslash it: `\\${slingslop.launcher.version}`.

---

## Gotcha: distinct output names per variant

CONGA de-duplicates `RoleFile` entries by their `dir` + `file` key **before**
variant filtering, so two variants cannot both emit `webcache/${tenant}.conf` —
the second silently loses. That's why the nginx webcache file is
`${tenant}.nginx.conf` (still matches nginx's `conf.d/*.conf` include) while
apache keeps `${tenant}.conf`. A node runs a single variant, so its `webcache/`
dir holds only that engine's files.

---

**Golden rule:** developers add the parameter (+ default + doc) and use it in the
template; **operations only ever edit the environment file**. Because every knob
has a default, a fork that doesn't care about your new parameter keeps working
untouched, and `git pull` from upstream stays merge-clean.
