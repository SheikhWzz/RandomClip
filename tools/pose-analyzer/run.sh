#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

run_python() {
  if [ -d ".venv-nix" ]; then
    # shellcheck disable=SC1091
    source .venv-nix/bin/activate
  elif [ -d "venv" ]; then
    # shellcheck disable=SC1091
    source venv/bin/activate
  else
    echo "No virtualenv found. On NixOS run: nix-shell shell.nix" >&2
    exit 1
  fi
  python analyze_video.py "$@"
}

if command -v nix-shell >/dev/null 2>&1 && [ -f shell.nix ] && [ ! -d ".venv-nix" ]; then
  exec nix-shell shell.nix --run "python analyze_video.py $(printf '%q ' "$@")"
fi

run_python "$@"
