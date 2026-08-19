# Composite NodeStore for Slingslop

> Why and how we split the JCR repository into an **immutable application part**
> (the Docker image) and a **mutable content part** (a Docker volume).

## Mental model in one sentence

Composite is **additive and opt-in**: the project always builds and runs in
classic single-store mode; `-Pcomposite-image` produces a *second* image,
side-by-side, that you can pull when you want the immutable-`/apps` benefits,
and you can always go back by deploying the non-composite image again.

## TL;DR

Slingslop ships as a single Docker image. Without changes, every JCR node -
including all the templates, scripts and configuration that *make* the app -
lives in a writable segment store inside a Docker volume. That has three
undesirable consequences:

1. The container's `/apps` and `/libs` can drift from what we built. Anyone with
   admin rights (or any rogue piece of code) can mutate the application live in
   production.
2. Rolling a new image forward (or back) is destructive of the volume - or, more
   commonly, the new image's `/apps` gets shadowed by stale data already in the
   volume.
3. There is no clean separation between "code" (built artefact, version pinned)
   and "content" (editorial state, lives forever).

The **Composite NodeStore** fixes all three. We mount `/apps` and `/libs` from a
**read-only Segment NodeStore baked into the Docker image** and keep everything
else in a **read-write Segment NodeStore in a Docker volume**. The two stores
are stitched together at runtime by Oak's `CompositeNodeStoreService`.

## Background reading

* Oak: [Composite NodeStore](https://jackrabbit.apache.org/oak/docs/nodestore/compositens.html)
* Sling: [How to Create a Composite NodeStore with the Feature Model](https://sling.apache.org/documentation/feature-model/howtos/create-sling-composite.html)
* Dan Klco: [Exporting Sling Feature Model, part 2 - Composite NodeStore](https://danklco.com/posts/2020-08-exporting-sling-feature-model-part-2-composite-nodestore/)

## What a Composite NodeStore is

A Composite NodeStore is a single logical `NodeStore` assembled from two or more
real `NodeStore` instances. Each backing store is called a **mount** and owns a
fixed set of JCR paths, like Unix mount points.

There is exactly **one default mount** (always named `composite-global`) which
is **read-write** and owns everything that isn't explicitly claimed by another
mount. Every additional mount is **read-only** - this is a design constraint of
Oak that will not change (atomic commits across stores are not feasible).

For Slingslop the layout is:

```
                ┌────────────────────────────────────────────────┐
                │                CompositeNodeStore               │
                └────────────────────────────────────────────────┘
                          │                            │
                          ▼                            ▼
         ┌────────────────────────┐     ┌──────────────────────────┐
         │  composite-mount-libs  │     │      composite-global     │
         │  (read-only)           │     │      (read-write)         │
         │  /apps, /libs          │     │      everything else       │
         │                        │     │      (/content, /home,    │
         │  SegmentNodeStore at   │     │       /var, /conf, …)     │
         │  /opt/sling/           │     │                           │
         │   seed-repository/     │     │  SegmentNodeStore at      │
         │   segmentstore         │     │  /opt/sling/launcher/     │
         │                        │     │   repository/segmentstore │
         │  baked into the image  │     │                           │
         │                        │     │  Docker VOLUME            │
         └────────────────────────┘     └──────────────────────────┘
```

## Why we want it (concretely, for Slingslop)

| Concern | Without composite | With composite |
|---|---|---|
| Where does `/apps/slingslop/zengarden/...` live? | In the volume, written by `jcr.contentloader` on first start. Persists across image upgrades - **stale code wins**. | In the image, read-only. Upgrading the image atomically swaps the code. |
| Can an admin mutate a script at runtime? | Yes. Any POST to `/apps/...` succeeds. | No. Writes return `500` (read-only mount). |
| Bundle redeploys (`mvn install sling:install`) for production? | Allowed and destructive. | The OSGi bundles are inside the image - `sling:install` to a production node is no longer the upgrade path; rolling a new image is. |
| Blue/green / canary upgrade? | Requires content copy. | Same volume, new image. Roll forward or back instantly. |
| Disk usage in the volume? | Grows with every build (old code stays). | Volume only carries content. |
| `mvn install sling:install` during development? | Works. | Works too - but only against a dev launcher that is **not** in composite mode. Production keeps using composite. |

The cost is two:

1. A **seeding step** in the build pipeline. The read-only mount can only be
   populated when it is the *only* store. So we run Sling once with a normal
   single-store config, let it install everything, stop it, and capture the
   resulting `segmentstore` directory. That capture is what we bake into the
   image.
2. **No referenceable nodes in `/apps` or `/libs`.** This is an Oak constraint:
   cross-mount references can dangle. In practice this means no `mix:referenceable`
   and no `nt:resource` (use `oak:Resource`) in the read-only paths. Our current
   content packages comply with this.

## How it is wired

### Three OSGi configs and one factory config

All the OSGi configurations live in
[launcher/src/main/features/composite-nodestore.json](../launcher/src/main/features/composite-nodestore.json).

1. **Override the existing default `SegmentNodeStoreService`** so it stops being
   the global `NodeStore` and instead exposes itself with the role
   `composite-global`. The data location is unchanged
   (`${sling.home}/repository/segmentstore` = inside the volume).

   ```json
   "org.apache.jackrabbit.oak.segment.SegmentNodeStoreService": {
     "name": "Global NodeStore",
     "role": "composite-global"
   }
   ```

2. **Add a second `SegmentNodeStoreService` via factory config** that points at
   the baked-in seed directory and exposes itself with role
   `composite-mount-libs`.

   ```json
   "org.apache.jackrabbit.oak.segment.SegmentNodeStoreService~libs": {
     "name": "Libs NodeStore (read-only)",
     "role": "composite-mount-libs",
     "repository.home": "/opt/sling/seed-repository"
   }
   ```

3. **Tell Oak which paths belong to the non-default mount** via the
   `MountInfoProviderService`. The mount's name is `libs`; Oak looks for a
   `NodeStore` registered with role `composite-mount-<mountName>`.

   ```json
   "org.apache.jackrabbit.oak.composite.MountInfoProviderService": {
     "mountedPaths": ["/libs", "/apps"],
     "mountName": "libs",
     "readOnlyMount:Boolean": true
   }
   ```

4. **Activate the assembly** via `CompositeNodeStoreService`. It waits until
   every mount declared by the `MountInfoProviderService` is present, then
   registers itself as the global `NodeStore` (which is what
   `org.apache.sling.jcr.oak.server` consumes).

   ```json
   "org.apache.jackrabbit.oak.composite.CompositeNodeStoreService": {
     "ignoreMissingMount:Boolean": false,
     "partialReadOnly:Boolean": true
   }
   ```

The bundle `oak-store-composite` is already part of `oak_base` in Sling Starter
14, so no additional bundles are required.

### A second feature-model aggregate

`launcher/pom.xml` defines two aggregates:

| Classifier | Persistence | Intended for |
|---|---|---|
| `slingslop_aggregate` | Single `SegmentNodeStore` (writable `/apps`, `/libs`) | Development, integration tests, seeding |
| `slingslop_composite_aggregate` | Composite NodeStore | Production Docker image |

The composite aggregate is `slingslop_aggregate` plus the
`composite-nodestore` feature pulled in via a second `<filesInclude>`. The
`SegmentNodeStoreService` PID exists in both `oak_persistence_sns` (with `name`
only) and in `composite-nodestore` (with `role=composite-global`), so the
aggregator would normally fail with "Configuration override rule required …".
We declare:

```xml
<configurationOverrides>
  <configurationOverride>org.apache.jackrabbit.oak.segment.SegmentNodeStoreService=MERGE_LATEST</configurationOverride>
</configurationOverrides>
```

`MERGE_LATEST` merges the two configs (so `role` from composite-nodestore plus
`name` from oak_persistence_sns end up on the same PID); for any key present in
both, the later contribution wins. The other three composite-related PIDs are
unique to `composite-nodestore` and need no override.

### The Dockerfile

The Dockerfile now creates `/opt/sling/seed-repository` (a **plain directory**,
**not** a `VOLUME`) for the seed segment store. The launcher volume
`/opt/sling/launcher` is unchanged.

`CMD ["slingslop_aggregate"]` still selects the single-store aggregate, so
nothing breaks for existing users. To run the composite variant, override the
command:

```bash
docker run ... ghcr.io/orx0815/slingslop:composite slingslop_composite_aggregate
```

(Or build a derived image whose `CMD` is the composite aggregate - see
[`seed_and_bake.sh`](../launcher/src/build/seed_and_bake.sh).)

## The seeding workflow

This is the non-obvious bit. You cannot start Sling directly in composite mode
against an empty `seed-repository/segmentstore` - there would be no `/apps` or
`/libs` for anyone to read. The read-only mount has to be **populated first**
by a regular non-composite run, then frozen.

The script [`launcher/src/build/seed_and_bake.sh`](../launcher/src/build/seed_and_bake.sh)
orchestrates this. It does, in order:

1. **Build the base image** (`mvn -Pdocker -DskipITs ...`).
2. **Run the base image once** with the `slingslop_aggregate` feature against a
   throw-away local directory mounted as `/opt/sling/launcher`.
3. **Wait for content installation to finish** - poll a known URL such as
   `/content/slingslop/zengarden/home.html` until it returns 200.
4. **Stop the container gracefully** (`docker stop`, not `kill`) so the segment
   store is left in a clean checkpoint state.
5. **Copy the seeded `repository/segmentstore`** from the throw-away volume
   into a local `target/seed/` directory.
6. **Build a derived image** (`Dockerfile.composite`) that
   * `COPY`s the seeded segmentstore to `/opt/sling/seed-repository/segmentstore`,
   * sets `CMD ["slingslop_composite_aggregate"]`.

Once that image exists, every container started from it begins life with a
fully-populated, read-only `/apps` and `/libs`. The volume only ever needs to
contain `/content`, `/home`, `/var`, `/conf` and friends.

### Re-seeding when the app changes

Any change to `/apps` or `/libs` (a new component, a fixed bug in HTL, a new
content package) requires a **new image** because the read-only store is
immutable at runtime. The pipeline is:

```
build code → build base image → seed → bake composite image → push
```

No content migration is needed: the volume - owned by the running site - is not
touched. Upgrading is `docker compose pull && docker compose up -d`.

### What about content that *should* be shipped (sample pages)?

The `complete` content package contains both code (`/apps`) and sample content
(`/content/...`). When the seed step runs, both end up in the seed segment
store. We then need either to:

* let `/content` paths *leak* into the read-only mount on first deploy (they
  don't - `MountInfoProviderService` only routes `/apps` and `/libs` to the
  read-only mount; anything else goes to the global default mount, which during
  seeding is still the single SNS, so `/content` does end up in the seeded
  store, **but** the composite container's default mount is empty on first run
  and it will not see it), **or**
* re-install the sample content into the global mount on first start.

For Slingslop we accept the second model: the seed image's job is *only* to
populate `/apps` and `/libs`. The container's first start in composite mode
installs the sample content into the global mount via the existing
`jcr.contentloader` + content-package install hooks, because those subscribe to
the global (writable) `NodeStore`.

If you want a *fully pre-populated* setup, ship the same content packages also
through the seed phase by including only the path prefixes you want frozen
(extend `mountedPaths`, e.g. `/apps`, `/libs`, `/conf/slingslop`).

## Verifying it works

Once a composite container is up:

```bash
# read works
curl -u admin:admin http://localhost:8080/apps/slingslop/zengarden/pages/homepage.json

# write to /apps fails (read-only mount)
curl -s -o /dev/null -w '%{http_code}\n' \
     -u admin:admin -FtestProperty='nope' \
     http://localhost:8080/apps/slingslop/zengarden
# expect: 500

# write to /content succeeds (default mount in the volume)
curl -s -o /dev/null -w '%{http_code}\n' \
     -u admin:admin -FtestProperty='yes' \
     http://localhost:8080/content/slingslop/zengarden/home
# expect: 200
```

In the Felix Web Console (`/system/console/components`) you should see:

* Two `SegmentNodeStoreService` components active (one with role
  `composite-global`, one with role `composite-mount-libs`).
* One `CompositeNodeStoreService` component active, providing the
  `NodeStore` service.
* One `MountInfoProviderService` component active.

## Caveats and rough edges

* **Referenceable nodes are forbidden in the read-only mount.** Anything under
  `/apps` or `/libs` with `mix:referenceable` (including `nt:resource`) will
  cause the composite to refuse to assemble. Use `oak:Resource` for binary
  carriers if you ever need them under `/apps`.
* **The seed step depends on a clean container shutdown.** A killed container
  leaves an un-checkpointed segment store. `seed_and_bake.sh` uses `docker stop`
  with a generous grace period.
* **Bundle redeploys against production are no longer a thing.** This is a
  feature, not a bug, but it is a workflow change. Development still uses the
  `slingslop_aggregate` (single store) and `mvn install sling:install`.
* **Oak version pinning.** Composite requires `oak-store-composite` matching the
  rest of Oak. Slingslop inherits Oak 1.90.0 from Sling Starter 14, which is
  fine.
* **Repository upgrades.** A future Oak upgrade may rewrite the seed
  segmentstore on first start. Because that store is on the image (read-only
  bind by `/opt/sling/seed-repository`), the upgrade either no-ops (compatible
  version) or fails (incompatible). Always re-seed against the target Oak
  version.

## File map

| Path | Role |
|---|---|
| `launcher/src/main/features/composite-nodestore.json` | OSGi configs that flip persistence into composite mode |
| `launcher/pom.xml` (`slingslop_composite_aggregate`) | Aggregate that combines the standard features with the composite feature |
| `launcher/Dockerfile` | Adds `/opt/sling/seed-repository` |
| `launcher/Dockerfile.composite` | Derived image: copies seed in, sets composite `CMD` |
| `launcher/src/build/seed_and_bake.sh` | Orchestrates seed → bake → push |
| `docs/composite-nodestore.md` | This document |

## Glossary

* **NodeStore** - Oak's persistence abstraction. SegmentNodeStore and
  DocumentNodeStore are the two production-grade implementations.
* **Segment NodeStore (SNS)** - Tar-based on-disk store. What Slingslop uses.
* **Mount** - In a composite store, a `NodeStore` plus the set of JCR paths it
  owns.
* **Default mount** (`composite-global`) - The read-write mount that owns
  everything not claimed by another mount.
* **Read-only mount** - Any non-default mount. Always read-only by Oak design.
* **Seeding** - Populating a `NodeStore` with content *before* it is wrapped by
  the composite as a read-only mount. Once frozen, it cannot be modified.
