#!/usr/bin/env bash
# Pulls the newest staging snapshot of Cygnus from Reposilite and rolls it out to the
# CloudNet template of the staging server. Cron runs it every two minutes as user cloud.
#
# Channel: the first open PR labelled "staging" wins (pr-<n>), otherwise main.
# A PR without a snapshot falls back to main.
#
# Usage: deploy-staging.sh [--dry-run]
set -euo pipefail

# Config file with REPO_USER and REPO_TOKEN (and optionally GITHUB_TOKEN). Values set there win.
CONFIG="${CONFIG:-$HOME/.config/cygnus-staging/env}"
if [ -f "$CONFIG" ]; then
  # shellcheck source=/dev/null
  . "$CONFIG"
fi

CLOUDNET_DIR="${CLOUDNET_DIR:-/home/cloud/cloudnet}"
TASK="${TASK:-Slender-Staging}"
TEMPLATE_DIR="${TEMPLATE_DIR:-$CLOUDNET_DIR/local/templates/Slender-Staging/current}"
STATE_FILE="${STATE_FILE:-$HOME/.local/state/cygnus-staging/deployed}"
STATE_DIR="$(dirname -- "$STATE_FILE")"
LOCK_FILE="$STATE_DIR/deploy.lock"
REPO_URL="${REPO_URL:-https://repo.onelitefeather.dev/onelitefeather-snapshots}"
GH_REPO="${GH_REPO:-OneLiteFeatherNET/Cygnus}"
SCREEN_SESSION="${SCREEN_SESSION:-CloudNet}"
TMP_DIR=""

log() {
  printf '%s %s\n' "$(date -Is)" "$*"
}

die() {
  log "ERROR: $*" >&2
  exit 1
}

cleanup() {
  if [ -n "$TMP_DIR" ]; then
    rm -rf -- "$TMP_DIR"
  fi
}

# Authenticated GET of a Reposilite URL. The token never leaves this call.
fetch() {
  curl -fsS -u "$REPO_USER:$REPO_TOKEN" "$1"
}

download() {
  curl -fsS -u "$REPO_USER:$REPO_TOKEN" -o "$2" "$1" || die "download failed: $1"
}

# Prints the number of the first open PR labelled staging, or nothing.
fetch_open_staging_pr() {
  local -a auth=()
  if [ -n "${GITHUB_TOKEN:-}" ]; then
    auth=(-H "Authorization: Bearer $GITHUB_TOKEN")
  fi
  curl -fsS ${auth[@]+"${auth[@]}"} -H 'Accept: application/vnd.github+json' \
    "https://api.github.com/repos/$GH_REPO/issues?labels=staging&state=open&sort=updated&direction=desc&per_page=20" |
    jq -r 'map(select(.pull_request)) | .[0].number // empty'
}

# Reads maven-metadata.xml on stdin and prints the text of every <version> element.
maven_versions() {
  tr -d '\r\n' | sed 's|<version>|\n|g' | sed -n 's|^\([^<]*\)</version>.*|\1|p' || true
}

# Reads a version list on stdin and prints the highest one for channel $1, or nothing.
latest_for_channel() {
  local suffix="-$1-SNAPSHOT" v
  while IFS= read -r v || [ -n "$v" ]; do
    if [[ "$v" == *"$suffix" ]]; then
      printf '%s\n' "$v"
    fi
  done | sort -V | tail -n 1
}

# Reads a snapshot-level maven-metadata.xml on stdin and prints the <value> of the plain
# jar snapshotVersion (no classifier). Prints nothing when there is none.
jar_value_xmllint() {
  xmllint --xpath "string(//snapshotVersion[extension='jar' and not(classifier)][1]/value)" - 2>/dev/null || true
}

jar_value_sed() {
  tr -d '\r\n' | sed 's|<snapshotVersion>|\n<snapshotVersion>|g' |
    grep '^<snapshotVersion>' | grep '<extension>jar</extension>' | grep -v '<classifier>' |
    sed -n 's|.*<value>\([^<]*\)</value>.*|\1|p' | tail -n 1 || true
}

jar_snapshot_value() {
  if command -v xmllint >/dev/null 2>&1; then
    jar_value_xmllint
  else
    jar_value_sed
  fi
}

# Prints the Implementation-Version of the jar $1, or nothing.
manifest_version() {
  unzip -p "$1" META-INF/MANIFEST.MF 2>/dev/null | tr -d '\r' |
    sed -n 's/^Implementation-Version: *//p' | sed 's/[[:space:]]*$//' | tail -n 1 || true
}

# Prints the timestamped jar snapshot value of artifact $1 at version $2.
artifact_snapshot_value() {
  local meta value
  if ! meta=$(fetch "$REPO_URL/net/onelitefeather/$1/$2/maven-metadata.xml"); then
    die "could not fetch snapshot metadata for $1 $2"
  fi
  value=$(printf '%s' "$meta" | jar_snapshot_value)
  if [ -z "$value" ]; then
    die "no jar snapshot in metadata of $1 $2"
  fi
  printf '%s\n' "$value"
}

# Stops every CloudNet service of the task. CloudNet starts fresh ones from the template.
restart_services() {
  local path base service digits count=0
  for path in "$CLOUDNET_DIR/temp/services/$TASK"-*; do
    [ -d "$path" ] || continue
    base=$(basename -- "$path")
    case "$base" in
      *_*) ;;
      *) continue ;;
    esac
    service=${base%%_*}
    digits=${service#"$TASK"-}
    case "$digits" in
      '' | *[!0-9]*) continue ;;
    esac
    if ! screen -S "$SCREEN_SESSION" -p 0 -X stuff "service $service stop"$'\r'; then
      die "could not send stop to $service"
    fi
    log "stopped $service"
    count=$((count + 1))
  done
  log "restarted $count service(s)"
}

main() {
  local dry_run=0 pr channel ver meta marker game_value bridge_value game_url bridge_url impl

  case "${1:-}" in
    --dry-run) dry_run=1 ;;
    '') ;;
    *) echo "usage: $0 [--dry-run]" >&2; exit 2 ;;
  esac

  : "${REPO_USER:?REPO_USER is not set, see $CONFIG}"
  : "${REPO_TOKEN:?REPO_TOKEN is not set, see $CONFIG}"

  for cmd in curl jq unzip; do
    command -v "$cmd" >/dev/null || die "$cmd not found"
  done

  # Single instance. Dry runs change nothing, so they skip the lock.
  if [ "$dry_run" -eq 0 ]; then
    for cmd in flock screen; do
      command -v "$cmd" >/dev/null || die "$cmd not found"
    done
    mkdir -p -- "$STATE_DIR"
    exec 9>>"$LOCK_FILE"
    flock -n 9 || exit 0
  fi

  if ! pr=$(fetch_open_staging_pr); then
    die "GitHub request for staging PRs failed"
  fi
  if [ -n "$pr" ]; then
    channel="pr-$pr"
  else
    channel="main"
  fi

  if ! meta=$(fetch "$REPO_URL/net/onelitefeather/cygnus-game/maven-metadata.xml"); then
    die "could not fetch maven metadata of cygnus-game"
  fi
  ver=$(printf '%s' "$meta" | maven_versions | latest_for_channel "$channel")
  if [ -z "$ver" ] && [ "$channel" != "main" ]; then
    log "no snapshot for $channel yet, falling back to main"
    channel="main"
    ver=$(printf '%s' "$meta" | maven_versions | latest_for_channel "$channel")
  fi
  if [ -z "$ver" ]; then
    log "no snapshot for $channel yet"
    exit 0
  fi

  game_value=$(artifact_snapshot_value cygnus-game "$ver")
  bridge_value=$(artifact_snapshot_value cygnus-bridge "$ver")
  marker="$channel $game_value"

  if [ -f "$STATE_FILE" ] && [ "$(cat -- "$STATE_FILE")" = "$marker" ]; then
    if [ "$dry_run" -eq 1 ]; then
      log "dry-run: $marker is already deployed, nothing to do"
    fi
    exit 0
  fi

  game_url="$REPO_URL/net/onelitefeather/cygnus-game/$ver/cygnus-game-$game_value.jar"
  bridge_url="$REPO_URL/net/onelitefeather/cygnus-bridge/$ver/cygnus-bridge-$bridge_value.jar"

  if [ "$dry_run" -eq 1 ]; then
    log "dry-run: would deploy $channel $ver ($game_value)"
    log "dry-run: game jar $game_url"
    log "dry-run: bridge jar $bridge_url"
    log "dry-run: would install into $TEMPLATE_DIR and stop the $TASK services"
    exit 0
  fi

  TMP_DIR=$(mktemp -d)
  trap cleanup EXIT

  download "$game_url" "$TMP_DIR/cygnus-game.jar"
  download "$bridge_url" "$TMP_DIR/cygnus-bridge.jar"
  unzip -tq "$TMP_DIR/cygnus-game.jar" >/dev/null || die "game jar is not a valid zip file"
  unzip -tq "$TMP_DIR/cygnus-bridge.jar" >/dev/null || die "bridge jar is not a valid zip file"
  impl=$(manifest_version "$TMP_DIR/cygnus-game.jar")
  if [ "$impl" != "$ver" ]; then
    die "game jar Implementation-Version is '$impl', expected '$ver'"
  fi

  if [ ! -d "$TEMPLATE_DIR" ]; then
    die "template directory $TEMPLATE_DIR does not exist"
  fi
  cp -- "$TMP_DIR/cygnus-game.jar" "$TEMPLATE_DIR/application.jar.new"
  mv -- "$TEMPLATE_DIR/application.jar.new" "$TEMPLATE_DIR/application.jar"
  mkdir -p -- "$TEMPLATE_DIR/extensions"
  rm -f -- "$TEMPLATE_DIR"/extensions/cygnus-bridge-*.jar
  cp -- "$TMP_DIR/cygnus-bridge.jar" "$TEMPLATE_DIR/extensions/cygnus-bridge-$ver.jar"

  restart_services

  mkdir -p -- "$STATE_DIR"
  printf '%s\n' "$marker" > "$STATE_FILE"
  log "deployed $channel $ver ($game_value)"
}

# Sourced by tests: define the functions without running them.
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
  main "$@"
fi
