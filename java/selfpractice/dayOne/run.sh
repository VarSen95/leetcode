#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$SCRIPT_DIR/env.sh"

if [ $# -lt 1 ]; then
  echo "Usage: $(basename "$0") <File.java>"
  echo "Example: $(basename "$0") BankAccount.java"
  exit 1
fi

FILE="$1"
DIR="$(cd "$(dirname "$FILE")" && pwd)"
BASE="$(basename "$FILE")"
CLASS="${BASE%.java}"
OUT_DIR="$SCRIPT_DIR/out"

pushd "$DIR" >/dev/null
mkdir -p "$OUT_DIR"

PACKAGE="$(grep -E '^[[:space:]]*package[[:space:]]+[^;]+' "$BASE" | head -n1 | sed -E 's/^[[:space:]]*package[[:space:]]+//; s/;.*$//')"

# Compile all Java files in the same directory so dependencies are included.
javac -d "$OUT_DIR" *.java

if [ -n "${PACKAGE:-}" ]; then
  java -cp "$OUT_DIR" "${PACKAGE}.${CLASS}"
else
  java -cp "$OUT_DIR" "$CLASS"
fi
popd >/dev/null
