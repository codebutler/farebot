# Development Container

A devcontainer is provided for sandboxed development with [Claude Code](https://claude.com/claude-code). It runs Claude with `--dangerously-skip-permissions` inside a network-restricted Docker container so agents can work unattended without risk of arbitrary network access.

## What's included

* Bun runtime + Claude Code
* Java 21 + Gradle (via devcontainer feature)
* tmux, zsh, git-delta, fzf, gh CLI
* iptables firewall allowing only: Anthropic API, GitHub, Maven Central, Google Maven, Gradle Plugin Portal, JetBrains repos, npm/bun registries
* All other outbound traffic is blocked

## Quick start

```bash
bun install -g @devcontainers/cli   # one-time
.devcontainer/dc up                 # build and start
.devcontainer/dc auth               # one-time: authenticate with GitHub
.devcontainer/dc claude             # run Claude (--dangerously-skip-permissions, in tmux)
```

The `dc claude` command runs Claude inside a tmux session. Re-running it reattaches to the existing session instead of starting a new one. Other commands:

```
.devcontainer/dc shell              # zsh shell in the container
.devcontainer/dc run <cmd>          # run any command (e.g. ./gradlew allTests)
.devcontainer/dc down               # stop the container
```

Git push uses HTTPS via `gh auth` — no SSH keys are mounted. Credentials persist in a Docker volume across container restarts.

Compatible with [Zed](https://zed.dev/docs/dev-containers), VS Code (Remote - Containers extension), and the `devcontainer` CLI.
