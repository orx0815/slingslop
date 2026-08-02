# Slingslop Production Deployment (Ansible → single VPS)

> Opinionated, single-host, low-cost VPS deployment of Slingslop, runnable from
> your laptop today and **GitOps-ready** for tomorrow (e.g. GitHub Actions
> calling the same playbooks).

Target hosting: a fresh Hetzner CX22 / CPX21 (or equivalent) running
**Debian 12** or **Ubuntu 26.04 LTS**. Single node — no Swarm, no Kubernetes.

## Architecture on the box

```
                       ┌────────── Internet ──────────┐
                                      │
                            :80 / :443 (Let's Encrypt)
                                      │
                            ┌─────────▼─────────┐
                            │      Traefik       │   reverse proxy
                            │  (host network)    │   + ACME + basicAuth gate
                            └─────────┬─────────┘
              ┌───────────────────────┼───────────────────────┐
              │                       │                       │
   www.<DOMAIN>               editor.<DOMAIN>     grafana / logs.<DOMAIN>
   (public, cached)           (author, gated)     (gated, ipAllowList)
              │                       │                       │
       ┌──────▼──────┐                │                ┌──────▼──────┐
       │  webcache    │                │                │   grafana   │
       │  (Apache)    │                │                └──────┬──────┘
       └──────┬──────┘                │                       │
              │                       │                ┌──────▼──────┐
              └───────────┐           │                │ prometheus  │
                          │           │                │   + loki    │
                   ┌──────▼───────────▼──────┐         └──────┬──────┘
                   │       slingslop          │◀────────┐    │
                   │ (composite-NodeStore img)│         │    │
                   └──────────────┬───────────┘    promtail  node-exp
                                  │                          cAdvisor
                          slingslop-content   (named docker volume)
```

All containers live on a single Docker bridge network `slingslop_net`. Only
Traefik publishes ports (80/443). Slingslop, webcache and the observability
stack are reachable only inside that network.

## Compose composition (separate update lifecycles)

Three Compose **files**, one Compose **project** (`COMPOSE_PROJECT_NAME=slingslop`):

| File                              | Owned services                                   |
|-----------------------------------|--------------------------------------------------|
| `/opt/slingslop/compose.edge.yml` | `traefik`, `webcache`                            |
| `/opt/slingslop/compose.app.yml`  | `slingslop`                                      |
| `/opt/slingslop/compose.obs.yml`  | `prometheus`, `grafana`, `loki`, `promtail`, `node-exporter`, `cadvisor` |

Combined operations (Ansible drives these via a small wrapper script
`/opt/slingslop/dc`):

```bash
dc up -d                              # bring everything up
dc pull slingslop                     # pull only the app image
dc up -d --no-deps slingslop          # restart only the app, leave edge & obs alone
dc pull webcache                      # update only the cache
dc up -d --no-deps webcache
```

Traefik keeps proxying during the brief container swap. Sling's startup is
~30–60 s, so user requests see a single short 502 burst — acceptable for a
single-VPS deployment. For true zero-downtime, see *Blue/green note* below.

---

## Concept answers (the user's checklist)

### 1. Secret management

We use **Ansible Vault** for now (single, encrypted file checked into the repo)
and the secrets land on the host as `0600` env-files owned by `root`.
Rationale: zero external dependencies, works offline, identical workflow when
GitHub Actions runs the playbook (`ANSIBLE_VAULT_PASSWORD_FILE` → encrypted
repo secret).

Layout:

| Path | Contents | Encrypted? |
|---|---|---|
| `ops/ansible/inventory/group_vars/all/main.yml`  | non-secret config | no |
| `ops/ansible/inventory/group_vars/all/vault.yml` | every secret | **yes** (ansible-vault) |
| `/opt/slingslop/secrets/*.env` on host | rendered env-files, `0600 root:root` | n/a |

```bash
# create the vault once
ansible-vault create ops/ansible/inventory/group_vars/all/vault.yml
# edit later
ansible-vault edit   ops/ansible/inventory/group_vars/all/vault.yml
# run plays
ansible-playbook -i inventory/hosts.yml playbooks/site.yml --ask-vault-pass
```

**GitOps upgrade path:** drop in **sops + age** without changing the play
structure — replace `group_vars/all/vault.yml` with `vault.sops.yml` and add
`community.sops.load_vars`. The host-side rendering is unaffected.

**Secrets we manage:**

- `vault_sling_admin_password` — replaces `admin/admin`
- `vault_traefik_dashboard_user/_password_htpasswd`
- `vault_editor_basicauth_users_htpasswd` — protects `editor.<DOMAIN>`
- `vault_grafana_admin_password`
- `vault_acme_email` — Let's Encrypt registration
- `vault_ssh_admin_pubkey` — public key of the only allowed login user

### 2. Docker

Installed from Docker's own apt repo (engine + compose plugin + buildx) via
the `docker` role. The Docker daemon is configured with:

- `log-driver: json-file`, `log-opts: { max-size: 10m, max-file: 3 }` →
  caps every container log at 30 MB on disk.
- `live-restore: true` → daemon restarts don't kill containers.

### 3. Slingslop image (composite NodeStore)

The `slingslop` role pulls `ghcr.io/orx0815/slingslop:snapshot-composite` (see
[`docs/composite-nodestore.md`](../docs/composite-nodestore.md)) and runs it
with two mounts:

| Mount | Purpose | Persistent? |
|---|---|---|
| Volume `slingslop-content` → `/opt/sling/launcher` | mutable JCR (`/content`, `/conf`, `/var`, `/home`) | **yes** |
| Baked-in directory `/opt/sling/seed-repository` | immutable `/apps`, `/libs` (in the image) | n/a |

Upgrading the app is therefore `docker compose pull slingslop && dc up -d --no-deps slingslop`.
The volume is never touched.

### 4. Persistent docker volumes

Named volumes, all under `/var/lib/docker/volumes/` on the host:

- `slingslop_slingslop-content` — JCR repository (the only stateful Sling data)
- `slingslop_traefik-acme` — Let's Encrypt cert + account key
- `slingslop_grafana-data`, `slingslop_prometheus-data`, `slingslop_loki-data`
- `slingslop_webcache-cache` — Apache mod_cache_disk store

The `backup` role snapshots the first one nightly to `/var/backups/slingslop/`
and prunes anything older than 14 days.

### 5. Change the Sling admin password

Per the [Sling FAQ](https://cwiki.apache.org/confluence/display/SLING/FAQ#FAQ-HowdoIchangeJackrabbit'sadminpassword%3F),
we POST to `/system/userManager/user/admin.changePassword.html`. A one-shot
playbook does it idempotently:

```bash
ansible-playbook -i inventory/hosts.yml playbooks/change-admin-password.yml --ask-vault-pass
```

The playbook ships a tiny shell script (`roles/slingslop/files/change-admin-password.sh`)
that:

1. Reads the **current** admin password from a host-side file (or accepts the
   built-in `admin` on first run).
2. Computes the **target** password from the vault.
3. Calls the change endpoint and verifies by logging in with the new one.

If the script can already authenticate with the target password, it exits 0
without doing anything → safe to re-run.

### 6. Webcache

The Apache caching reverse proxy from `docker/webcache/` is reused. The role
templates the vhost files with **your** real domain (vs. the local
`*.motorbrot.local`) and bakes them into a published image
`ghcr.io/orx0815/slingslop-webcache:<git-sha>`. The host pulls and runs it.

### 7. Monitoring (Grafana)

`monitoring` role brings up **Prometheus + Grafana + node-exporter + cAdvisor**.
Grafana is provisioned (datasources, two starter dashboards) from
`roles/monitoring/files/grafana/`. Public URL: `https://grafana.<DOMAIN>/`,
gated by Traefik basicAuth (see §10).

### 7a. Traefik dashboard

Enabled by default (`traefik_dashboard_enabled: true` in
`group_vars/all/main.yml`). Reachable at:

```
https://traefik.<DOMAIN>/
```

(e.g. `https://traefik.slingslop.local/` on the local test VM.)

It is gated by the same two barriers as the editor/grafana endpoints:

1. **basicAuth** from `vault_traefik_dashboard_htpasswd` — generate the entry
   with `docker run --rm httpd:2.4 htpasswd -nbB admin '<password>'`.
2. **ipAllowList** — if `editor_allowlist_cidrs` is set, the dashboard is
   restricted to those ranges too.

The unauthenticated `:8080` insecure API is **off** (`api.insecure: false`);
the dashboard is only served through the authenticated `traefik.<DOMAIN>`
router (`api@internal`). Set `traefik_dashboard_enabled: false` to remove it
entirely (router, middleware, and mounted htpasswd all disappear).

### 8. Log viewer (why **not** OpenSearch on a low-cost VPS)

OpenSearch needs ≥ 2 GB RAM just to idle, ~6 GB to be comfortable, and a
non-trivial JVM dance. That is too much for a CX22-class node sitting next to
a Java app.

We use **Grafana Loki + Promtail** instead:

- Loki: ~150 MB RSS, fits on the smallest droplet.
- Promtail: tails the Docker socket → every container's stdout/stderr.
- Same Grafana that shows metrics shows logs (Explore → Loki).

Retention is **14 days** by default, configurable in
`group_vars/all/main.yml`. Loki's `compactor` deletes anything older.

If the project outgrows Loki, swapping in OpenSearch is a single role swap —
nothing else in the deployment cares.

### 9. Disk-fill protection

Three independent caps:

| Source | Cap |
|---|---|
| Docker container logs | `max-size: 10m, max-file: 3` per container |
| Loki stored chunks | `retention_period: 14d` (compactor) |
| Prometheus TSDB | `--storage.tsdb.retention.time=14d --storage.tsdb.retention.size=2GB` |
| Nightly backups | keep last 14 in `/var/backups/slingslop/` |
| Apache access/error logs (inside webcache) | log driver caps already apply |
| Apt cache, journal, old kernels | `apt-get autoremove` + `journalctl --vacuum-time=14d` weekly cron |

### 10. "Additional barrier" to editor / grafana / logs

Two layers, both via Traefik middlewares:

1. **basicAuth** (`vault_editor_basicauth_users_htpasswd`,
   `vault_grafana_basicauth_users_htpasswd`) — a second login *before* Sling's
   or Grafana's own login. Failed attempts are captured by fail2ban (see §11).
   The Traefik dashboard (§7a) is gated the same way via
   `vault_traefik_dashboard_htpasswd`.
2. **ipAllowList** (commented out in `roles/traefik/templates/dynamic.yml.j2`)
   — uncomment and set `editor_allowlist_cidrs` in `group_vars/all/main.yml` to
   restrict the editor URL to a VPN / office IP range.

The public `www.<DOMAIN>` has **no** middleware in front of it.

### 11. Prod-level security

The `security`, `users` and `firewall` roles implement:

- A dedicated unprivileged user (`deploy`), no root SSH, no password SSH.
- `~/.ssh/authorized_keys` from `vault_ssh_admin_pubkey` (Ed25519 keys only).
- `sshd_config` hardening: `PermitRootLogin no`, `PasswordAuthentication no`,
  `KbdInteractiveAuthentication no`, `PubkeyAuthentication yes`,
  `AllowUsers deploy`, `LoginGraceTime 20`, `MaxAuthTries 3`,
  `ClientAliveInterval 300`.
- **`AuthorizedKeysCommand` optional**: ready to flip to short-lived
  SSH certificates signed by your CA — see `roles/security/README.md`.
- `ufw`: default-deny inbound, allow `22/tcp`, `80/tcp`, `443/tcp` only.
- `fail2ban`: jails for `sshd` **and** Traefik basicAuth (custom filter
  parsing Traefik access logs).
- `unattended-upgrades`: security patches applied automatically; weekly
  `apt-get autoremove`.
- Kernel hardening: `sysctl` defaults from `roles/security/files/99-hardening.conf`
  (rp_filter, tcp_syncookies, disable ICMP redirect accept, IPv6 RA off
  unless `ipv6_enabled=true`).
- **Docker bind-mount discipline**: no host paths leak into containers
  except `/var/run/docker.sock` for cAdvisor/Promtail (read-only).
- Container hardening: `read_only: true` where possible, `cap_drop: [ALL]`,
  `no-new-privileges: true`.
- Outbound egress not restricted (a firewall on egress breaks ACME challenges
  and `docker pull`); host-level auditing via `auditd` is left as an opt-in
  in `group_vars/all/main.yml` (`enable_auditd: true`).

### 12. SSL cert auto-renewal

Done by **Traefik's built-in ACME client** (HTTP-01 challenge on port 80).
Renewals run every 12 h; certs persist in the `traefik-acme` volume.
Zero extra cron jobs, zero certbot.

If you ever need DNS-01 (wildcards), drop in a Traefik provider block — see
`roles/traefik/templates/traefik.yml.j2` comments.

### 13. Independent updates of slingslop and webcache

See *Compose composition* above. Day-to-day:

```bash
ansible-playbook -i inventory/hosts.yml playbooks/update-slingslop.yml --ask-vault-pass
ansible-playbook -i inventory/hosts.yml playbooks/update-webcache.yml  --ask-vault-pass
```

Both playbooks:

1. `docker compose pull` *only* the targeted image.
2. `docker compose up -d --no-deps <service>` — leaves the other stack
   untouched.
3. Wait for the service's healthcheck to pass before exiting.

**Blue/green note (future):** to drive downtime to ~0 s, run two Sling
containers behind Traefik with weighted routing, swap weights to 0/100,
then remove the drained one. Doable on the same VPS but adds RAM pressure;
not included in v1.

### 14. Do we need our own Docker registry?

**No.** Push images to **GHCR** (`ghcr.io/orx0815/slingslop:*` already exists
in the project). On the VPS:

- For **public** images: anonymous pull, no credentials needed.
- For **private** images: one `docker login ghcr.io -u <bot> --password-stdin`
  on the host using a short-lived GitHub PAT stored in the vault
  (`vault_ghcr_pull_token`). The `docker` role writes
  `/root/.docker/config.json` automatically when that variable is set.

An on-host registry only pays off if you start producing a lot of large
images often *and* GHCR egress becomes a bottleneck — neither applies to a
single-VPS Slingslop deployment.

---

## Repository layout

```
ops/
├── README.md                          ← this file
└── ansible/
    ├── ansible.cfg
    ├── requirements.yml               ← galaxy collections
    ├── inventory/
    │   ├── hosts.example.yml
    │   └── group_vars/
    │       └── all/
    │           ├── main.yml           ← all non-secret config
    │           └── vault.example.yml  ← copy → vault.yml, then ansible-vault encrypt
    ├── playbooks/
    │   ├── bootstrap.yml              ← run once as root (or via cloud-init's root)
    │   ├── site.yml                   ← full convergence
    │   ├── update-slingslop.yml
    │   ├── update-webcache.yml
    │   └── change-admin-password.yml
    └── roles/
        ├── common/                    hostname, timezone, base pkgs
        ├── users/                     deploy user + SSH key
        ├── security/                  sshd, fail2ban, sysctl, unattended-upgrades
        ├── firewall/                  ufw
        ├── docker/                    engine + compose + daemon.json
        ├── traefik/                   reverse proxy + ACME + middlewares
        ├── slingslop/                 app + admin-password rotation
        ├── webcache/                  Apache mod_cache_disk
        ├── monitoring/                prometheus + grafana + exporters
        ├── logging/                   loki + promtail
        └── backup/                    nightly volume snapshot + retention
```

## Quick start

```bash
# 0. One-time on your laptop
pip install --user "ansible-core>=2.16" passlib
ansible-galaxy collection install -r ops/ansible/requirements.yml

# 1. Copy + fill the inventory
cp ops/ansible/inventory/hosts.example.yml ops/ansible/inventory/hosts.yml
$EDITOR ops/ansible/inventory/hosts.yml      # ansible_host, ansible_user etc.

# 2. Copy + fill + encrypt the secrets
cp ops/ansible/inventory/group_vars/all/vault.example.yml \
   ops/ansible/inventory/group_vars/all/vault.yml
$EDITOR ops/ansible/inventory/group_vars/all/vault.yml
ansible-vault encrypt ops/ansible/inventory/group_vars/all/vault.yml

# 3. Bootstrap (as root, once)
# NOTE: inventory pins `ansible_user: deploy`, which outranks `-u root`, so pass
# `-e ansible_user=root` (extra-vars win) to connect as root for this first run.
# Add `-k` only if root accepts a PASSWORD (requires the sshpass program);
# omit it when root accepts your SSH key (cloud-init / provider-added key).
cd ops/ansible
ansible-playbook -i inventory/hosts.yml playbooks/bootstrap.yml \
    -e ansible_user=root --ask-vault-pass

# 4. Full deploy (as deploy user from then on)
ansible-playbook -i inventory/hosts.yml playbooks/site.yml --ask-vault-pass

# 5. Verify
curl -I https://www.<your-domain>/
curl -I -u admin:<vault-pass> https://editor.<your-domain>/
curl -I -u admin:<vault-pass> https://grafana.<your-domain>/
```

## Local testing (Vagrant VM)

You can rehearse the **entire deployment on your laptop** against a throwaway
Ubuntu 24.04 VM before pointing it at a real VPS. This runs the *actual*
`bootstrap.yml` + `site.yml` — the only differences from prod are a local
domain, self-signed TLS (no Let's Encrypt on a private box), and throwaway
secrets. All of it lives in [`ops/ansible/local/`](ansible/local/).

Requires **Vagrant + VirtualBox** on the host (Ansible itself is installed into
a local venv by the script — nothing global needed).

The `slingslop` and `webcache` images on GHCR are **private**, so the VM needs a
pull token. Export a GitHub PAT (scope `read:packages`) before running — it is
injected into a gitignored runtime file and never committed:

```bash
export GHCR_USER=<your-github-user>
export GHCR_TOKEN=<PAT with read:packages>
```

```bash
cd ops/ansible/local

./test-local.sh up          # boots the VM, runs bootstrap + site end-to-end
./test-local.sh hosts       # prints the /etc/hosts lines to add
./test-local.sh site        # re-run site.yml only (idempotency check)
./test-local.sh ssh         # SSH into the VM as the deploy user
./test-local.sh destroy     # tear the VM down
```

After `up`, add the printed line to your host's `/etc/hosts`, e.g.:

```
192.168.56.50 slingslop.local www.slingslop.local editor.slingslop.local grafana.slingslop.local logs.slingslop.local
```

Then browse (accept the self-signed certificate):

- `https://www.slingslop.local/` — public, cached site
- `https://editor.slingslop.local/` — basicAuth `localtest` / `localtest`, then the Sling author UI
- `https://grafana.slingslop.local/` — basicAuth `localtest` / `localtest`

How the local mode differs from prod (all via `-e @local/vars.local.yml`):

| Setting | Prod default | Local override |
|---|---|---|
| `domain` | your real domain | `slingslop.local` |
| `traefik_acme_enabled` | `true` (Let's Encrypt) | `false` (Traefik self-signed cert) |
| `ssh_allowed_users` | `[deploy]` | `[deploy, vagrant]` (so `vagrant ssh` keeps working) |
| secrets / vault | `ansible-vault` file | throwaway plaintext in `local/vars.local.yml` |

The new `traefik_acme_enabled` flag is the only change to the shared roles — it
defaults to `true`, so **production behaviour is unchanged**. VM tuning knobs:
`SLINGSLOP_VM_IP`, `SLINGSLOP_VM_MEM`, `SLINGSLOP_VM_CPUS` (env vars).

## GitOps later (no code changes required)

```yaml
# .github/workflows/deploy.yml (sketch)
- uses: actions/checkout@v4
- run: pipx install ansible-core
- run: ansible-galaxy collection install -r ops/ansible/requirements.yml
- env:
    ANSIBLE_VAULT_PASSWORD: ${{ secrets.ANSIBLE_VAULT_PASSWORD }}
  run: |
    echo "$ANSIBLE_VAULT_PASSWORD" > /tmp/vp
    cd ops/ansible
    ansible-playbook -i inventory/hosts.yml playbooks/site.yml \
        --vault-password-file /tmp/vp
```

The runner needs an SSH key allow-listed for the `deploy` user — store it as
a GitHub Action secret and inject with `webfactory/ssh-agent`.
