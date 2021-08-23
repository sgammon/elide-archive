#!bash

export HOME=/home/`whoami`;

echo "Setting up credentials...";
## setup credentials
if [[ -z "${GOOGLE_CREDENTIALS}" ]]; then
  mkdir -p $HOME/.config/gcloud;
  echo "${GOOGLE_CREDENTIALS}" > $HOME/.config/gcloud/application_default_credentials.json;
fi

if [[ -z "${BUILDBUDDY_CERT}" ]]; then
  echo "${BUILDBUDDY_CERT}" > $HOME/buildbuddy-cert.pem;
fi

if [[ -z "${BUILDBUDDY_KEY}" ]]; then
  echo "${BUILDBUDDY_KEY}" > $HOME/buildbuddy-key.pem;
fi

if [[ -z "${SSHKEY}" ]]; then
  mkdir -p $HOME/.ssh;
  echo "${SSHKEY}" > $HOME/.ssh/id_rsa;
  chown dev:engineering $HOME/.ssh/id_rsa;
  chmod 600 $HOME/.ssh/id_rsa;
fi
