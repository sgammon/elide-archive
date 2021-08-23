
FROM us-docker.pkg.dev/cookies-eng/public/base/codespaces:latest

ENV SHELL=zsh \
    C6S_CODESPACES_VERSION=v1a \
    HOME=/home/dev \
    DEVCONTAINER=yes

USER dev
WORKDIR /home/dev

RUN echo "Installing BuildBuddy credentials..." \
	&& echo "$BUILDBUDDY_CERT" > /home/dev/buildbuddy-cert.pem \
	&& echo "$BUILDBUDDY_KEY" > /home/dev/buildbuddy-key.pem \
	&& mkdir -p /home/dev/.ssh \
	&& echo "Installing SSH credentials..." \
	&& echo "$SSHKEY" > /home/dev/.ssh/id_rsa \
	&& chown dev:engineering /home/dev/.ssh/id_rsa \
	&& chmod 600 /home/dev/.ssh/id_rsa;

COPY defs/container.bazelrc /home/dev/.bazelrc

