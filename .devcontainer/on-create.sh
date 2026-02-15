#!/bin/bash
set -euo pipefail

# Seed Claude config from repo defaults (no-clobber: won't overwrite existing)
mkdir -p /home/bun/.claude
cp -n /workspace/.devcontainer/claude-config/settings.json /home/bun/.claude/settings.json
