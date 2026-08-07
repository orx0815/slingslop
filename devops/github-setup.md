# GitHub setup — one-time steps on github.com

Manual settings required before the CI/CD workflows (`.github/workflows/ci-cd.yml`,
`infra.yml`) can build, push, and deploy. Do these once in the repo settings.

Repo: `https://github.com/orx0815/slingslop`

---

## 1. Actions secrets

**Settings → Secrets and variables → Actions → Secrets → New repository secret**
(`https://github.com/orx0815/slingslop/settings/secrets/actions`)

| Name | Value | Needed for |
|---|---|---|
| `DEPLOY_SSH_KEY` | **private** key for the `deploy` user (whole file incl. `-----BEGIN…END-----`) | every deploy |
| `ANSIBLE_VAULT_PASSWORD` | password used to encrypt `devops/ansible/inventory/group_vars/all/vault.yml` | every deploy |
| `ROOT_SSH_KEY` | a **root** private key for a fresh box | `infra.yml` → `bootstrap` only |

Copy an SSH private key value with (copy **all** lines, including BEGIN/END):

```bash
cat ~/.ssh/id_ed25519
```

Notes:
- Secret values are write-only — you can overwrite but never view them again.
- Names are case-sensitive and must match the workflows exactly.
- **The key must have NO passphrase.** The deploy workflows load it via
  `webfactory/ssh-agent`, which runs `ssh-add -` non-interactively; a
  passphrase-protected key makes CI fail with
  `Command failed: ssh-add -  /  Enter passphrase for (stdin):`.
  Generate a dedicated CI deploy key without a passphrase:

  ```bash
  # dedicated, passphrase-less CI deploy key
  ssh-keygen -t ed25519 -N '' -C 'ci-deploy@slingslop' -f ~/.ssh/slingslop_ci_deploy

  # authorise it for the deploy user on the VPS
  ssh-copy-id -i ~/.ssh/slingslop_ci_deploy.pub deploy@motorbrot.org

  # paste this into the DEPLOY_SSH_KEY secret (whole file, incl. BEGIN/END)
  cat ~/.ssh/slingslop_ci_deploy
  ```

  (Already have a passphrased key you want to reuse? Strip the passphrase into a
  copy first: `cp ~/.ssh/id_ed25519 ~/.ssh/slingslop_ci_deploy && ssh-keygen -p -N '' -f ~/.ssh/slingslop_ci_deploy` — never remove the passphrase from your personal key in place.)

---

## 2. Actions variables (non-secret; optional — they default to `motorbrot.org`)

**Settings → Secrets and variables → Actions → Variables → New repository variable**

| Name | Value |
|---|---|
| `DEPLOY_HOST` | `motorbrot.org` (or the VPS IP `138.199.151.106`) |
| `DEPLOY_DOMAIN` | `motorbrot.org` |

Skip these to accept the built-in `motorbrot.org` defaults.

---

## 3. `production` environment (recommended)

The deploy jobs declare `environment: production`.

**Settings → Environments → New environment → `production`**

- Optionally add the secrets from step 1 here as **Environment secrets** instead of
  (or in addition to) repository secrets — environment secrets win for jobs that
  use that environment.
- Optionally add **Required reviewers** for a manual approval gate before each
  production deploy.

If you only set repository-level secrets, deploys still work; the environment just
adds optional protection.

---

## 4. GHCR package access + visibility (fixes "denied" on push)

The workflow pushes with the repo's `GITHUB_TOKEN`, which can only write to packages
**linked to this repo**. Packages created by a **manual** `docker push` from a laptop
are unlinked → Actions gets `denied`.

For **each** package — `slingslop` **and** `slingslop-webcache`:

`https://github.com/users/orx0815/packages/container/<name>/settings`

1. **Manage Actions access** → **Add Repository** → `orx0815/slingslop` → role **Write**
   (Admin also works). *`slingslop` may already show Admin — that's fine.*
2. **Danger Zone → Change visibility → Public** — so the VPS can pull anonymously
   (no `vault_ghcr_pull_token` needed).

Packages first created **by** the workflow are auto-linked and skip step 1.

---

## 5. After configuring

Re-run the failed jobs: **Actions → the run → Re-run failed jobs**, or push again.

Sanity check the anonymous pull works once packages are public:

```bash
docker logout ghcr.io
docker pull ghcr.io/orx0815/slingslop-webcache:latest
```
