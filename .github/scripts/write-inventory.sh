#!/usr/bin/env bash
# Writes the (gitignored) prod inventory + host_vars from CI env vars so the
# deploy jobs have something to target. Run from devops/ansible as the CWD.
#
#   DEPLOY_HOST    SSH target / public IP of the VPS   (e.g. motorbrot.org)
#   DEPLOY_DOMAIN  public domain published by Traefik   (e.g. motorbrot.org)
set -euo pipefail

: "${DEPLOY_HOST:?DEPLOY_HOST not set}"
: "${DEPLOY_DOMAIN:?DEPLOY_DOMAIN not set}"

mkdir -p inventory/host_vars

cat > inventory/hosts.yml <<YAML
---
all:
  hosts:
    slingslop-prod:
      ansible_host: ${DEPLOY_HOST}
      ansible_user: deploy
      ansible_port: 22
      ansible_python_interpreter: /usr/bin/python3
  children:
    slingslop:
      hosts:
        slingslop-prod: {}
YAML

cat > inventory/host_vars/slingslop-prod.yml <<YAML
---
domain: ${DEPLOY_DOMAIN}
editor_allowlist_cidrs: []
YAML

echo "Wrote inventory for host=${DEPLOY_HOST} domain=${DEPLOY_DOMAIN}"
