#!/usr/bin/env bash
set -euo pipefail

# Use JDK 8 (Amazon Corretto 8 is already installed on this machine)
export JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"
export PATH="$JAVA_HOME/bin:$PATH"
