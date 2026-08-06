#!/usr/bin/env bash
# Pre-flight guard for the GitOps deploy.
#
# The committed prod vault MUST decrypt with the real ANSIBLE_VAULT_PASSWORD and
# carry known keys. The vault is byte-identical on every branch, so this is not
# about branch divergence — it catches a commit that replaced it with a
# different / empty / garbled file, failing the deploy BEFORE the VPS is touched
# instead of shipping wrong or broken secrets.
#
# See ops/README.md.
set -euo pipefail

VP="${1:?usage: verify-vault.sh <vault-password-file>}"

VAULT="ops/ansible/inventory/group_vars/all/vault.yml"
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
  echo "::error::This looks like the wrong vault was committed. Refusing to deploy."
  exit 1
fi

echo "vault OK: decrypted and all ${#REQUIRED_KEYS[@]} sentinel keys present."
