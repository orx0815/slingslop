# roles/security — Production SSH and host hardening

## Default behaviour

- SSH: pubkey-only, Ed25519 + RSA-SHA2 host keys, modern crypto only.
- Root login is **disabled** on convergence runs.
  During `bootstrap.yml` (where `bootstrap_phase=true`) we keep
  `prohibit-password` so the very first run can still authorise the
  root-via-key initial deploy.
- `AllowUsers` is restricted to `{{ deploy_user }}` (default `deploy`).
- fail2ban jails: `sshd` (3 tries / 10 min → 1 h ban) and a custom
  `traefik-basicauth` jail that tails Traefik's access log and bans IPs that
  fail basicAuth.
- `unattended-upgrades` enabled for security patches.
- `sysctl` hardening: rp_filter, syncookies, no source routes, no ICMP
  redirect accept, hidden kernel pointers, restricted dmesg.
- Optional `auditd` — flip `enable_auditd: true` in `group_vars/all/main.yml`.

## Going further — short-lived SSH certificates (recommended)

Pubkey auth is good. SSH **certificates** are better: they let you issue
time-bound (e.g. 8 h) credentials from a CA you control, and revoke instantly
by rotating the CA. To enable:

1. Generate a CA keypair on a secure machine:
   ```bash
   ssh-keygen -t ed25519 -f ssh-ca -C 'slingslop SSH CA'
   ```
2. Put `ssh-ca.pub` on the host at `/etc/ssh/trusted-user-ca.pub` (Ansible
   `copy:` task — extend this role).
3. In `sshd_config.j2`, add:
   ```
   TrustedUserCAKeys /etc/ssh/trusted-user-ca.pub
   ```
4. Issue short certs to engineers:
   ```bash
   ssh-keygen -s ssh-ca -I 'alice' -n deploy -V +8h alice_ed25519.pub
   ```

This role is left without certificate enforcement by default because it
requires you to operate a (very small) CA. Pubkeys remain the fallback in the
default config; once you switch on certificates you can drop `authorized_keys`
entirely.
