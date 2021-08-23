
FROM us-docker.pkg.dev/cookies-eng/public/base/codespaces:latest

ENV SHELL zsh
ENV C6S_CODESPACES_VERSION=v1a
ENV HOME /home/dev

USER dev
WORKDIR /home/dev

RUN echo "Installing BuildBuddy credentials..." \
	&& echo "$BUILDBUDDY_CERT" > /home/dev/buildbuddy-cert.pem \
	&& echo "$BUILDBUDDY_KEY" > /home/dev/buildbuddy-key.pem;

COPY defs/container.bazelrc /home/dev/.bazelrc

