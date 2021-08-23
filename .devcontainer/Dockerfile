
FROM us-docker.pkg.dev/cookies-eng/public/base/codespaces:v1h

ENV SHELL=zsh \
    HOME=/home/dev \
    DEVCONTAINER=yes

USER dev
WORKDIR /home/dev

COPY defs/container.bazelrc /home/dev/bazelrc.remote

