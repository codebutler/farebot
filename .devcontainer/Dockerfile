FROM oven/bun:debian

ARG TZ
ENV TZ="$TZ"

# Install system dependencies + build tools for Android/KMP
RUN apt-get update && apt-get install -y --no-install-recommends \
  ca-certificates curl wget \
  less git procps sudo fzf zsh man-db unzip gnupg2 \
  bash python3 locales make \
  iptables ipset iproute2 dnsutils aggregate jq nano vim tmux \
  build-essential libncurses-dev libz-dev libc6-dev \
  && apt-get clean && rm -rf /var/lib/apt/lists/* \
  && ln -sf /bin/bash /bin/sh \
  && sed -i '/en_US.UTF-8/s/^# //' /etc/locale.gen && locale-gen

ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

# Install JDK 25 (Temurin)
RUN wget -O- https://packages.adoptium.net/artifactory/api/gpg/key/public | tee /usr/share/keyrings/adoptium.asc \
  && echo "deb [signed-by=/usr/share/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list \
  && apt-get update && apt-get install -y temurin-21-jdk temurin-25-jdk \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/temurin-25-jdk
RUN ln -s /usr/lib/jvm/temurin-25-jdk-* ${JAVA_HOME} || true
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install Android SDK Command-Line Tools
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=${ANDROID_HOME}
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
  && wget https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip -O /tmp/cmdline-tools.zip \
  && unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools \
  && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
  && rm /tmp/cmdline-tools.zip

# Accept Android licenses and install SDK components
RUN yes | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --licenses || true \
  && ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0" \
    "ndk;27.2.12479018" \
  && chmod -R 755 ${ANDROID_HOME}

# Install git-delta
ARG GIT_DELTA_VERSION=0.18.2
RUN ARCH=$(dpkg --print-architecture) \
  && wget "https://github.com/dandavison/delta/releases/download/${GIT_DELTA_VERSION}/git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" \
  && dpkg -i "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" \
  && rm "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb"

# Rename bun user to dev
RUN usermod -l dev -d /home/dev -m bun \
  && groupmod -n dev bun

# Set up directories and permissions
RUN mkdir -p /commandhistory /workspace /home/dev/.claude \
  && chown -R dev:dev /commandhistory /workspace /home/dev/.claude ${ANDROID_HOME}

# Give bun user passwordless sudo for firewall setup
RUN echo "dev ALL=(root) NOPASSWD: ALL" > /etc/sudoers.d/dev \
  && chmod 0440 /etc/sudoers.d/dev

# Install Claude Code via native installer
USER dev
ENV HOME=/home/dev
ENV PATH="/home/dev/.local/bin:$PATH"
RUN curl -fsSL https://claude.ai/install.sh | bash

# Install lefthook
USER root
RUN curl -fsSL https://get.lefthook.com | bash -s -- -d /usr/local/bin

USER dev

WORKDIR /workspace

ENV DEVCONTAINER=true
ENV SHELL=/bin/zsh
ENV EDITOR=nano
ENV VISUAL=nano
ENV JAVA_HOME=/usr/lib/jvm/temurin-25-jdk
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk

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
USER dev
