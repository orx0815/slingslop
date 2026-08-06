#!/usr/bin/env bash
# Pre-flight guard for the GitOps deploy.
#
# The committed prod vault MUST decrypt with the real ANSIBLE_VAULT_PASSWORD and
# carry known keys. A PR based on `main` carries the PUBLIC demo vault, which is
# encrypted with a DIFFERENT passphrase. If such a PR is merged into the deploy
# branch and clobbers the real vault (e.g. a fast-forward of the vault file that
# no merge driver caught, because GitHub's server-side merge ignores
# `.gitattributes merge=ours`), the box would otherwise be deployed with demo
# credentials. This guard fails the deploy BEFORE the VPS is touched.
#
# See ops/README.md and the two-tier vault note.
set -euo pipefail

VP="${1:?usage: verify-vault.sh <vault-password-file>}"

# The real prod vault lives at the deploy-only group path (group_vars/slingslop/),
# which main/feature branches never carry — so a merge from main can't clobber it.
VAULT="ops/ansible/inventory/group_vars/slingslop/vault.yml"
if [ ! -f "$VAULT" ]; then
  echo "::error::no committed vault found at $VAULT — refusing to deploy."
  exit 1
fi

# Sentinel keys every real prod vault carries (see ops docs). If the file was
# swapped for a different/empty vault these will be missing.
REQUIRED_KEYS=(
  vault_sling_admin_password
  vault_acme_email
  vault_ghcr_pull_token
)

if ! plain="$(ansible-vault view "$VAULT" --vault-password-file "$VP" 2>/dev/null)"; then
  echo "::error::$VAULT did not decrypt with ANSIBLE_VAULT_PASSWORD."
  echo "::error::Wrong password, or the file was replaced/corrupted (e.g. a stray commit or a merge that swapped in a different vault). Refusing to deploy."
  exit 1
fi

missing=()
for k in "${REQUIRED_KEYS[@]}"; do
  printf '%s\n' "$plain" | grep -q "^${k}:" || missing+=("$k")
done

if [ ${#missing[@]} -gt 0 ]; then
  echo "::error::prod vault decrypted but is missing expected keys: ${missing[*]}"
  echo "::error::This looks like the wrong vault was committed to the deploy branch. Refusing to deploy."
  exit 1
fi

echo "vault OK: decrypted and all ${#REQUIRED_KEYS[@]} sentinel keys present."
