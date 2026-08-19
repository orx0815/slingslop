#!/bin/bash
# Prunes each unpacked sample-content package (see unpack-sample-content-it in
# launcher/pom.xml) down to only the JCR paths starter.check.paths actually needs,
# then re-packages each into a zip under OUT_DIR.
#
# Fully generic, derived from starter.check.paths only - nothing app-specific is
# hardcoded here or in the content-package modules: for every "/content/**.html"
# entry, its own JCR node (including its own content, e.g. _jcr_content/**) is
# kept in full; any SIBLING node at any level along that path - other demo pages,
# asset libraries, ... - is dropped. This is what keeps growing/heavier sample
# content (more pages, more images) from ever slowing down the IT.
#
# Usage: prune-sample-content-it.sh <work-dir> <out-dir> <check-paths-csv>
#   work-dir: parent dir containing one unpacked package per subdirectory
#   out-dir:  where <subdirectory-name>.zip is written for each package
set -eu

WORK_DIR="$1"
OUT_DIR="$2"
CHECK_PATHS="$3"

mkdir -p "${OUT_DIR}"

# 1. Collect the JCR paths (relative to jcr_root, no leading slash) referenced by
#    starter.check.paths, e.g. "/content/sling-matrix/home.html" -> "content/sling-matrix/home"
keep_paths=()
oldifs=$IFS; IFS=,
for raw in ${CHECK_PATHS}; do
  p=$(echo "${raw}" | tr -d '[:space:]')
  case "${p}" in
    /content/*.html)
      kp="${p#/}"
      keep_paths+=("${kp%.html}")
      ;;
  esac
done
IFS=$oldifs

is_exact_keep() {   # $1 = path relative to jcr_root, e.g. content/sling-matrix/home
  local rel="$1" kp
  for kp in "${keep_paths[@]}"; do [ "${rel}" = "${kp}" ] && return 0; done
  return 1
}

is_ancestor_of_keep() {   # $1 = path relative to jcr_root
  local rel="$1" kp
  for kp in "${keep_paths[@]}"; do case "${kp}" in "${rel}"/*) return 0 ;; esac; done
  return 1
}

# 2. Recursively prune a jcr_root/content subtree in place.
prune_dir() {
  local dir="$1" rel="$2" entry name childrel
  if is_exact_keep "${rel}"; then
    # This IS a checked page: keep its own content (_jcr_content, files) but drop
    # sibling real child nodes - other pages/subtrees the ITs never look at.
    for entry in "${dir}"/*; do
      [ -e "${entry}" ] || continue
      if [ -d "${entry}" ] && [ "$(basename "${entry}")" != "_jcr_content" ]; then
        rm -rf "${entry}"
      fi
    done
    return 0
  fi
  for entry in "${dir}"/*; do
    [ -d "${entry}" ] || continue
    name=$(basename "${entry}")
    childrel="${rel}/${name}"
    if is_exact_keep "${childrel}" || is_ancestor_of_keep "${childrel}"; then
      prune_dir "${entry}" "${childrel}"
    else
      rm -rf "${entry}"
    fi
  done
}

for app_dir in "${WORK_DIR}"/*/; do
  [ -d "${app_dir}" ] || continue
  name=$(basename "${app_dir}")
  content_dir="${app_dir}jcr_root/content"

  relevant=0
  if [ "${#keep_paths[@]}" -gt 0 ] && [ -d "${content_dir}" ]; then
    for kp in "${keep_paths[@]}"; do
      [ -e "${app_dir}jcr_root/${kp}" ] && relevant=1
    done
  fi

  if [ "${relevant}" = 1 ]; then
    prune_dir "${content_dir}" "content"
    echo "[prune-sample-content-it] ${name}: pruned to $(du -sh "${app_dir}" | cut -f1)"
  else
    echo "[prune-sample-content-it] ${name}: no starter.check.paths entry found under it, packaging as-is"
  fi

  jar cfM "${OUT_DIR}/${name}.zip" -C "${app_dir}" .
done
