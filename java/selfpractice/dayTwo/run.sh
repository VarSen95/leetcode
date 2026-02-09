#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$SCRIPT_DIR/env.sh"

if [ $# -lt 1 ]; then
  DIR="$SCRIPT_DIR"
  BASE=""
  CLASS=""
else
  FILE="$1"
  DIR="$(cd "$(dirname "$FILE")" && pwd)"
  BASE="$(basename "$FILE")"
  CLASS="${BASE%.java}"
fi
OUT_DIR="$DIR/out"

pushd "$DIR" >/dev/null
mkdir -p "$OUT_DIR"

# Compile all Java files in the same directory so dependencies are included.
javac -d "$OUT_DIR" *.java

if [ -n "${CLASS:-}" ]; then
  PACKAGE="$(grep -E '^[[:space:]]*package[[:space:]]+[^;]+' "$BASE" | head -n1 | sed -E 's/^[[:space:]]*package[[:space:]]+//; s/;.*$//')"
  if [ -n "${PACKAGE:-}" ]; then
    java -cp "$OUT_DIR" "${PACKAGE}.${CLASS}"
  else
    java -cp "$OUT_DIR" "$CLASS"
  fi
fi
popd >/dev/null
