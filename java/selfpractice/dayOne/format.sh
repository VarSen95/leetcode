#!/usr/bin/env bash
set -euo pipefail

if ! command -v google-java-format >/dev/null 2>&1; then
  echo "google-java-format not found. Install with: brew install google-java-format"
  exit 1
fi

if [ $# -lt 1 ]; then
  echo "Usage: $(basename "$0") <File.java>"
  echo "Example: $(basename "$0") Employee.java"
  exit 1
fi

FILE="$1"

if [ ! -f "$FILE" ]; then
  echo "File not found: $FILE"
  exit 1
fi

google-java-format -i "$FILE"
