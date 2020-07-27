
FROM gcr.io/cloud-marketplace/google/ubuntu1804@sha256:48c02d82c9c5a8e63b12fc064fd97a9cd75c5714cc1ff721d50dce200933b2bb
RUN apt-get update \
    && apt-get install --no-install-recommends -y \
        python python-pip python3 python3-pip build-essential default-jdk git golang unzip \
    && apt-get autoremove \
    && apt-get clean \
    && curl -sSL https://sdk.cloud.google.com > /tmp/gcl && bash /tmp/gcl --install-dir=/gcloud --disable-prompts \
    && mkdir -p /bazelisk/bin \
    && curl -sSL https://github.com/bazelbuild/bazelisk/releases/download/v1.3.0/bazelisk-linux-amd64 > /bazelisk/bin/bazelisk \
    && chmod +x /bazelisk/bin/bazelisk \
    && /gcloud/google-cloud-sdk/bin/gcloud components install \
        appctl kpt kubectl bq gsutil docker-credential-gcr alpha beta \
    && rm -rf /var/lib/apt/lists/* \
    && python2 -m pip install --upgrade \
        pip setuptools wheel \
    && python3 -m pip install --upgrade \
        pip setuptools wheel \
    && python2 -m pip install --upgrade \
        pypandoc \
    && python3 -m pip install --upgrade \
        pypandoc \
    && mkdir -p /builder/google-cloud-sdk \
    && ln -s /gcloud/google-cloud-sdk/bin /builder/google-cloud-sdk/bin \
    && echo "Build image ready."

ENV PATH $PATH:/gcloud/google-cloud-sdk/bin:/bazelisk/bin

ENTRYPOINT ["/bin/bash"]

