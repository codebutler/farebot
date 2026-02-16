#!/bin/bash
set -euo pipefail

# Seed Claude config from repo defaults (no-clobber: won't overwrite existing)
mkdir -p /home/dev/.claude
cp -n /workspace/.devcontainer/claude-config/settings.json /home/dev/.claude/settings.json
