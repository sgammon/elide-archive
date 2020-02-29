
FROM gcr.io/cloud-marketplace/google/ubuntu1804@sha256:ca6f2441610b5eca910cb7e5d8494df11e539743d57e87abfc71f85f3d634d94
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y python3 build-essential default-jdk git golang unzip \
    && apt-get autoremove \
    && curl -sSL https://sdk.cloud.google.com > /tmp/gcl && bash /tmp/gcl --install-dir=/gcloud --disable-prompts \
    && mkdir -p /bazelisk/bin \
    && curl -sSL https://github.com/bazelbuild/bazelisk/releases/download/v1.3.0/bazelisk-linux-amd64 > /bazelisk/bin/bazelisk \
    && chmod +x /bazelisk/bin/bazelisk \
    && /gcloud/google-cloud-sdk/bin/gcloud components install \
        appctl kpt kubectl bq gsutil docker-credential-gcr alpha beta \
    && rm -rf /var/lib/apt/lists/* \
    && echo "Installation done."

ENV PATH $PATH:/gcloud/google-cloud-sdk/bin:/bazelisk/bin

ENTRYPOINT ["/bazelisk/bin/bazelisk"]

