FROM oven/bun:debian

ARG TZ
ENV TZ="$TZ"

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
  ca-certificates curl wget \
  less git procps sudo fzf zsh man-db unzip gnupg2 \
  bash python3 locales \
  iptables ipset iproute2 dnsutils aggregate jq nano vim tmux \
  && apt-get clean && rm -rf /var/lib/apt/lists/* \
  && ln -sf /bin/bash /bin/sh \
  && sed -i '/en_US.UTF-8/s/^# //' /etc/locale.gen && locale-gen

ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

# Install gh CLI
RUN curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
  | dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" \
  > /etc/apt/sources.list.d/github-cli.list \
  && apt-get update && apt-get install -y gh \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install git-delta
ARG GIT_DELTA_VERSION=0.18.2
RUN ARCH=$(dpkg --print-architecture) \
  && wget "https://github.com/dandavison/delta/releases/download/${GIT_DELTA_VERSION}/git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" \
  && dpkg -i "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" \
  && rm "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb"

# Set up directories
RUN mkdir -p /commandhistory /workspace /home/bun/.claude \
  && chown -R bun:bun /commandhistory /workspace /home/bun/.claude

# Give bun user passwordless sudo for firewall setup
RUN echo "bun ALL=(root) NOPASSWD: ALL" > /etc/sudoers.d/bun \
  && chmod 0440 /etc/sudoers.d/bun

# Install Claude Code via native installer
USER bun
ENV PATH="/home/bun/.claude/local/bin:$PATH"
RUN curl -fsSL https://claude.ai/install.sh | bash

# Install lefthook
USER root
RUN curl -fsSL https://get.lefthook.com | bash -s -- -d /usr/local/bin

USER bun

WORKDIR /workspace

ENV DEVCONTAINER=true
ENV SHELL=/bin/zsh
ENV EDITOR=nano
ENV VISUAL=nano

# Set up zsh
ARG ZSH_IN_DOCKER_VERSION=1.2.0
RUN sh -c "$(wget -O- https://github.com/deluan/zsh-in-docker/releases/download/v${ZSH_IN_DOCKER_VERSION}/zsh-in-docker.sh)" -- \
  -p git -p fzf \
  -a 'source /usr/share/doc/fzf/examples/key-bindings.zsh' \
  -a 'source /usr/share/doc/fzf/examples/completion.zsh' \
  -a 'export HISTFILE=/commandhistory/.zsh_history' \
  -x

# Set up firewall script
COPY init-firewall.sh /usr/local/bin/
USER root
RUN chmod +x /usr/local/bin/init-firewall.sh
USER bun
