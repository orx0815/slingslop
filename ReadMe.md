# Slingslop

[![CI](https://img.shields.io/github/actions/workflow/status/orx0815/slingslop/ci-cd.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=CI)](https://github.com/orx0815/slingslop/actions/workflows/ci-cd.yml)
[![Publish](https://img.shields.io/github/actions/workflow/status/orx0815/slingslop/publish-ghcr.yml?style=for-the-badge&logo=githubactions&logoColor=white&label=Publish)](https://github.com/orx0815/slingslop/actions/workflows/publish-ghcr.yml)
[![Java 25](https://img.shields.io/badge/Java-25-007396?style=for-the-badge&logo=openjdk&logoColor=white)](#prerequisites)
[![Maven 3.9.12+](https://img.shields.io/badge/Maven-3.9.12%2B-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](#prerequisites)

## Hypermedia Driven Applications (HDA) based on Apache Sling.  
Using HTMX to GET component **markup** and to POST content via Sling's OOTB endpoints

(The name is a pun about the Apache Sling Slingshot sample application, that didn't get much love either.)

## Try it in your browser

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://github.com/codespaces/new/orx0815/slingslop?hide_repo_select=true)

Branch-specific quickstart URL pattern:
`https://codespaces.new/orx0815/slingslop/tree/<branch>?quickstart=1`

One click → a full VS-Code-in-the-browser dev environment, no local setup.
See [Running the Application](#running-the-application) for details and the
Agent-Smith → review → deploy loop.

### Key Directories

- `sling-apps/`  
	Contains the actual applications.  Each one consists of two types of  modules:  
	-  OSGi bundles (e.g. `zengarden.core`)  for java business-logic in the form of OSGi-Services, -Components and classic Servlets.  Also Sling-Models, referenced by templates. Very powerful ⚠️
	-  UI content-packages (e.g. zengarden.ui.apps) for html templates and CSS. Also as little JavaScript as possible.
	  The `frontent` sub-folder has npm-scripts to deal with that: TypeScript compilation, ESLint checks, dependencies and bundling everything into a minified js.
	
- `content-packages/`  
	Holds sample-content (text and images) in `Jackrabbit FileVault` (VLT)  zips.  
	Usually this is used to transfer content between instances, in this case they also become part of the launcher/docker-image.   
	A complete-package to install everything at once into a running instance.  
	(Note the three different kinds of  [packageType](https://jackrabbit.apache.org/filevault-package-maven-plugin/generate-metadata-mojo.html#packageType) : `application`|`content`|`container` using different validators.) 
	
- `docker/`  
  Contains `docker-compose.yml` with example configurations for the web cache proxy.
  
- `launcher/`  
  The core module responsible for building the runnable Sling application using the `slingfeature-maven-plugin`. The feature definitions are in `launcher/src/main/features`.
  
  Includes a launcher dependency BOM module (`launcher/launcher-dependencies`) for provided API version alignment with Sling. Dependencies listed there can simple be used, because they are already present in the launcher so do not need to be added to own OSGi bundles.

- doc/
  Help coding agents grasp some concepts and conventions

### Prerequisites

1.  **Java 25** and
2.  **Maven 3.9.12+** for building the project.
3.  **Docker** Optional, for running the apps and their web-proxy a in a container

### Building the Project

To build the entire `slingslop` project, run the following command from the root directory:

```bash
mvn clean install -DskipITs
```

To build the Docker image as well, including integration-tests, use:

```bash
mvn clean install -Ddocker.skip=false
```

This produces the standard image where **everything** (code under `/apps`,
`/libs` and content under `/content`, `/home`, `/var`, …) lives in a single
read-write Segment NodeStore inside the Docker volume `/opt/sling/launcher`.
That is the original mode and remains the default.

#### Composite NodeStore image (immutable `/apps` and `/libs`)

To additionally produce a **composite-mode** image - where `/apps` and `/libs`
are baked into the image as a read-only Segment NodeStore and only the
mutable content sits in the Docker volume - add `-Pcomposite-image`:

```bash
mvn clean install -Ddocker.skip=false -Pcomposite-image
```

This runs the base build first, then `launcher/src/build/seed_and_bake.sh` which boots
the base image once to seed `/apps` and `/libs`, freezes that segment store
and bakes it into a derived image tagged
`ghcr.io/orx0815/slingslop:snapshot-composite`.

The why, how and verification steps live in
[`docs/composite-nodestore.md`](docs/composite-nodestore.md).

### Running the Application

There are four primary ways to run the application locally:

1.  **Using the Launcher Script (Recommended for development):**
    The `launcher/launch.sh` script starts the application directly without Docker. This is useful for quick development cycles.

    ```bash
    cd launcher && ./launch.sh
    ```

    The The CSS Zen Garden can be enjoyed at: http://localhost:8080/content/slingslop/zengarden/home.html  
    The admin-ui at http://localhost:8080/  

2.  **Using Docker Compose:**
    This method uses the `docker-compose.yml` file to start the application and the web cache proxy.

    ```bash
    # From the root directory
    docker-compose -f docker/docker-compose.yml up --build
    ```
    You will also need to add entries to your `/etc/hosts` file as described in `docker/README.md` to access the different sites like you would in production.

    http://editor.motorbrot.local/  
    http://www.motorbrot.local/  

3.  **Start the official Sling Docker image** backed by 

    - Oak SegmentStore with  

       ```bash
       docker volume create sling-launcher
       docker run --rm -p 8080:8080 -v sling-launcher:/opt/sling/launcher apache/sling:13
       ```  

    - Oak MongoDB DocumentStore with  
    
      ```bash
      docker volume create sling-launcher
      docker run --rm -p 27017:27017 mongo:4.4.6
      docker run --rm -p 8081:8080 -v sling-launcher:/opt/sling/launcher apache/sling:13 oak_mongo
      ```
     and install the ./content-packages/complete/target/slingslop.complete-x.y.z-SNAPSHOT.zip 

4.  **Run the prebuilt Slingslop image from GHCR:**

    ```bash
    docker pull ghcr.io/orx0815/slingslop:latest
    docker run --rm -p 8080:8080 ghcr.io/orx0815/slingslop:latest
    ```

    Then open http://localhost:8080/content/slingslop/zengarden/home.html

5.  **Run the composite-NodeStore variant** (immutable `/apps` and `/libs`,
    only content sits in the volume - see
    [`docs/composite-nodestore.md`](docs/composite-nodestore.md)):

    ```bash
    docker run --rm -p 8080:8080 \
      -v slingslop-content:/opt/sling/launcher \
      ghcr.io/orx0815/slingslop:snapshot-composite
    ```

    The same image, started with the default aggregate, also runs in
    classic single-store mode - so you can roll back without rebuilding:

    ```bash
    docker run --rm -p 8080:8080 \
      -v slingslop-volume:/opt/sling/launcher \
      ghcr.io/orx0815/slingslop:snapshot \
      slingslop_aggregate
    ```

6.  **In the browser (GitHub Codespaces / DevPod / Ona):**
    Use the [Open in Codespaces](#try-it-in-your-browser) badge. On the GitHub
    free tier personal accounts get 120 core-hours + 15 GB/month — on the 4-core
    box this dev container asks for that's ~30 h, comparable to the old Gitpod
    allowance. The [`.devcontainer/`](.devcontainer/devcontainer.json) provisions
    Java 25 + Maven, pre-builds the project, and forwards Sling on port **8080**
    with an in-browser preview. Once the codespace is up:

    ```bash
    cd launcher && ./launch.sh
    # → the forwarded 8080 preview serves e.g.
    #   /content/slingslop/zengarden/home.html
    ```

    The same `devcontainer.json` is portable: open it in
    **[DevPod](https://devpod.sh/)** (open-source, self-host for unlimited hours)
    or in **[Ona](https://ona.com/)** (the rebranded Gitpod) without changes.

    For a specific branch, open Codespaces with:

    ```text
    https://codespaces.new/orx0815/slingslop/tree/<branch>?quickstart=1
    ```

    **The Agent-Smith → review → deploy loop** — fully GitOps, closing the loop
    the old Gitpod button only hinted at:

    1. **Author** — assign a GitHub issue (using the *"Agent Smith — new Sling
       app"* template) to the Copilot coding agent. It runs the
       [Agent Smith skill](docs/agent-skills/create-Sling-app-with-Agent_Smith.md)
       in the cloud and opens a pull request with the new app.
    2. **Review live** — open that PR branch in a Codespace (one click from the
       PR's *Code* menu), `./launch.sh`, and eyeball the running app in the
       forwarded preview — no local checkout, "works on my machine" confirmed.
    3. **Ship it** — merge to `deploy/motorbrot_prod`. The
       [CI/CD workflow](.github/workflows/ci-cd.yml) builds the immutable
       `sha-<commit>` image and rolls it onto the VPS (Traefik + Ansible),
       typically within ~10 minutes.

### Developing the Application

With a running instance, each bundle or package can be deployed separately. This allows fast code->compile->run  development cycles.

**Inside an OSGi bundle** project, run :

```bash
mvn install sling:install
```  

**Inside an content-package** project , run :

```bash
mvn install wcmio-content-package:install
```  

**This one works in both**:

```bash
mvn -Pfast cq:install
```  

### File System Resource Provider
When working on an **application content project** ( html/css/js/sightly/jsp/freemarker/thymeleaf ) the fastest is to mount the local filesystem directly into JCR:

```bash
mvn sling:fsmount
```  

That way you can save a file inside your IDE and immediately reload your browser.  
When done, you should unmount again:  

```bash
mvn sling:fsmount
```
followed by `cq:install` to have a "normal" state in JCR.

---

## Adding a new public-facing app

A new app lives **next to `zengarden`** under `sling-apps/` and follows the same
shape — `zengarden` is the smallest "Learn Sling" example to copy from. An app is:

- an OSGi bundle (`*.core`) for Java business logic — **optional**; omit it for a
  pure template/content app,
- a `ui.apps` content-package with the HTL scripts under `/apps/slingslop/<app>`
  and its page(s) under `/content/slingslop/<app>`.

The render chain mirrors `zengarden`: a `home` node is a *page*
(`sling:resourceType = slingslop/<app>/pages/page`) whose `html.html` delegates to
its `jcr:content` child, which carries the *homepage* resourceType that emits the
markup. For a full-featured app let **Agent Smith** scaffold it (see
[`docs/agent-skills/create-Sling-app-with-Agent_Smith.md`](docs/agent-skills/create-Sling-app-with-Agent_Smith.md)).

### Register it in three places

```jsonc
// a) pom.xml (root)                          — add the module(s)
// b) content-packages/complete/pom.xml       — add a <dependency> + <subPackage>
//                                               (bakes it into the all-in-one package)
// c) devops/conga/src/main/environments/*.yaml — add the CONGA tenant
```

If the app follows the `/content/slingslop/<name>` + `/apps/slingslop/<name>`
convention, the CONGA tenant is **zero-config** — CONGA derives `contentRoot`,
`appsRoot`, `subdomain` and `homePage` from the name, so the whole deployment
registration is three lines. Put it next to the `zengarden` tenant:

```yaml
# devops/conga/src/main/environments/prod-motorbrot.yaml
tenants:
  - tenant: zengarden
    roles: [ public-cached ]
    config: { contentRoot: /content/slingslop/zengarden }
  - tenant: my-app
    roles: [ public-cached ]
```

From those three lines, `mvn -pl devops/conga clean package` generates — per
environment — the Apache cache/short-URL vhost (`webcache/my-app.conf`), the
Traefik router (`traefik/dynamic/router-my-app.yml`) and the outbound Sling
`/etc/map` short-URL mapping. No proxy/router/mapping file is hand-edited; see
[`devops/conga/README.md`](devops/conga/README.md) and
[`docs/conga-config-generation-concept.md`](docs/conga-config-generation-concept.md).

### Build & run it locally

```bash
mvn clean install -DskipITs
cd launcher && ./launch.sh
curl http://localhost:8080/content/slingslop/my-app/home.html
```

> **Gotcha — stale local repository.** `launch.sh` keeps a **persisted**
> repository under `launcher/launcher/`. If you rebuilt but the page still 404s,
> the launcher reused the old repo. Wipe it and relaunch:
>
> ```bash
> pkill -f 'org.apache.sling.feature.launcher'   # stop the running instance
> rm -rf launcher/launcher                        # drop the persisted repo
> cd launcher && ./launch.sh                       # fresh install
> ```

To see it live on the real (Traefik + webcache + Sling) stack — the same Ansible
playbooks prod runs, on a throwaway Vagrant VM — follow
**[Local testing (Vagrant VM)](devops/README.md#local-testing-vagrant-vm)** in the
ops guide. Adding the tenant above is all a new public sub-domain needs: the
GitOps `deploy-tenant-edge` job ships that generated config (router + vhost +
`/etc/map`) to a running host **without an image rebuild**.

> **Demo credentials** — local Vagrant VM, all throwaway, never reuse anywhere real:
>
> | Surface | Login |
> |---|---|
> | Sling admin — author UI at `editor.slingslop.local`, console at `/` | `admin` / `admin` |
> | Edge basicAuth gate — `editor.` / `grafana.` / `logs.` / `traefik.slingslop.local` | `localtest` / `localtest` |
> | Grafana app login (inside Grafana) | `admin` / `admin` |
>
> These come from `devops/ansible/local/vars.local.yml` (+ generated
> `secrets.local.yml`). A production deploy uses the private vault on
> `deploy/motorbrot_prod` instead — see [devops/README.md](devops/README.md#1-secret-management).

### Where the content lives (important)

The sample content under `content-packages/` is **baked into the image** so the
demo apps run out of the box. **Real / production content should *not* live in
the image** — it is authored, grows and changes independently of code, and
belongs in the writable content volume (and, at scale, is replicated to publish
instances via **Sling Content Distribution** — see
[devops/README.md](devops/README.md#scaling-out-a-multi-node-topology-concept)).

A **tenant supports this directly**: its `contentRoot` can point anywhere, so the
*same* app can serve different content trees per deployment — e.g. a `sling-matrix`
app rooted at `/content/realProdContent/anothermatrix` on one host and at
`/content/sling-matrix` on another. Ops adds such a tenant at deploy time (router
+ vhost + `/etc/map`) **without a new app or a code change** — the mapping is
installed into the running repository, not compiled into the image.
