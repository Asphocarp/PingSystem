#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MATRIX_FILE="${ROOT_DIR}/ci/version-matrix.yml"
GRADLE_PROPERTIES="${ROOT_DIR}/gradle.properties"
OVERRIDES_DIR="${ROOT_DIR}/ci/version-overrides"
OPTIONAL_MATRIX_PROPERTIES=(
  "sable_version"
)

strip_quotes() {
  local value="$1"
  value="${value#\"}"
  value="${value%\"}"
  printf '%s\n' "${value}"
}

baseline() {
  awk -F': *' '/^baseline:/ { gsub(/"/, "", $2); print $2; exit }' "${MATRIX_FILE}"
}

versions() {
  awk '
    /^[[:space:]]*-[[:space:]]+minecraft_version:/ {
      value=$0
      sub(/^.*minecraft_version:[[:space:]]*/, "", value)
      gsub(/"/, "", value)
      print value
    }
  ' "${MATRIX_FILE}"
}

entry() {
  local target="$1"
  awk -v target="${target}" '
    function clean(value) {
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      gsub(/^"|"$/, "", value)
      return value
    }

    /^[[:space:]]*-[[:space:]]+minecraft_version:/ {
      value=$0
      sub(/^.*minecraft_version:[[:space:]]*/, "", value)
      value=clean(value)
      in_entry=(value == target)
      if (in_entry) {
        found=1
        print "minecraft_version=" value
      }
      next
    }

    in_entry && /^[[:space:]]+[A-Za-z0-9_]+:/ {
      key=$0
      sub(/^[[:space:]]*/, "", key)
      sub(/:.*/, "", key)

      value=$0
      sub(/^[[:space:]]*[A-Za-z0-9_]+:[[:space:]]*/, "", value)
      value=clean(value)
      print key "=" value
    }

    END {
      if (!found) {
        exit 2
      }
    }
  ' "${MATRIX_FILE}"
}

loaders() {
  local target="$1"
  entry "${target}" | awk -F= '$1 == "loaders" { gsub(/,/, " ", $2); print $2; exit }'
}

set_property() {
  local key="$1"
  local value="$2"
  local tmp
  tmp="$(mktemp)"
  awk -v key="${key}" -v value="${value}" '
    BEGIN { replaced=0 }
    $0 ~ "^" key "=" {
      print key "=" value
      replaced=1
      next
    }
    { print }
    END {
      if (!replaced) {
        print key "=" value
      }
    }
  ' "${GRADLE_PROPERTIES}" > "${tmp}"
  mv "${tmp}" "${GRADLE_PROPERTIES}"
}

remove_property() {
  local key="$1"
  local tmp
  tmp="$(mktemp)"
  awk -v key="${key}" '
    $0 !~ "^" key "=" {
      print
    }
  ' "${GRADLE_PROPERTIES}" > "${tmp}"
  mv "${tmp}" "${GRADLE_PROPERTIES}"
}

migrate() {
  local target="$1"
  local applied=0

  for key in "${OPTIONAL_MATRIX_PROPERTIES[@]}"; do
    remove_property "${key}"
  done

  while IFS='=' read -r key value; do
    case "${key}" in
      ""|"loaders")
        continue
        ;;
      *)
        set_property "${key}" "${value}"
        applied=1
        ;;
    esac
  done < <(entry "${target}")

  if [ "${applied}" -eq 0 ]; then
    echo "No version matrix entry found for ${target}." >&2
    exit 1
  fi

  if [ -d "${OVERRIDES_DIR}/${target}" ]; then
    cp -R "${OVERRIDES_DIR}/${target}/." "${ROOT_DIR}/"
    if [ -f "${OVERRIDES_DIR}/${target}/.delete" ]; then
      while IFS= read -r path_to_delete; do
        case "${path_to_delete}" in
          ""|\#*)
            continue
            ;;
          /*|*..*)
            echo "Unsafe delete path in ${OVERRIDES_DIR}/${target}/.delete: ${path_to_delete}" >&2
            exit 1
            ;;
          *)
            rm -rf "${ROOT_DIR}/${path_to_delete}"
            ;;
        esac
      done < "${OVERRIDES_DIR}/${target}/.delete"
      rm -f "${ROOT_DIR}/.delete"
    fi
  fi
}

case "${1:-}" in
  --baseline)
    baseline
    ;;
  --versions)
    versions
    ;;
  --loaders)
    if [ -z "${2:-}" ]; then
      echo "Usage: $0 --loaders <minecraft-version>" >&2
      exit 64
    fi
    loaders "$2"
    ;;
  "")
    echo "Usage: $0 <minecraft-version>|--baseline|--versions|--loaders <minecraft-version>" >&2
    exit 64
    ;;
  *)
    migrate "$1"
    ;;
esac
