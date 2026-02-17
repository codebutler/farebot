#!/bin/bash
set -euo pipefail

# Fix volume permissions (Docker volumes are created as root)
sudo chown -R dev:dev /commandhistory /home/dev/.config/gh 2>/dev/null || true

# Set up firewall
sudo /usr/local/bin/init-firewall.sh

# Configure git to use gh for authentication (if logged in)
gh auth setup-git 2>/dev/null || true

# Install lefthook git hooks
lefthook install
