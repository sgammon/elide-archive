#!/bin/bash

## 1: copy dotfiles sources
echo ""; echo "Copying dotfiles...";
mkdir -p ~/dotfiles;
cp -frv ./* ./.* ~/dotfiles/;

## 2: run `rcup`
echo ""; echo "--- Running 'rcup'...";
rcup;

## 3: copy in any secrets
echo ""; echo "--- Setting up credentials...";

if [[ -z "${GOOGLE_CREDENTIALS}" ]]; then
  echo "No Google credentials detected.";
else
  echo "- Installing Google credentials...";
  mkdir -p "$HOME/.config/gcloud";
  echo "${GOOGLE_CREDENTIALS}" > "$HOME/.config/gcloud/application_default_credentials.json";
fi

if [[ -z "${BUILDBUDDY_CERT}" ]]; then
  echo "No Buildbuddy certificate detected.";
else
  echo "- Installing Buildbuddy certificate...";
  echo "${BUILDBUDDY_CERT}" > "$HOME/buildbuddy-cert.pem";
fi

if [[ -z "${BUILDBUDDY_KEY}" ]]; then
  echo "No Buildbuddy key detected.";
else
  echo "- Installing Buildbuddy key...";
  echo "${BUILDBUDDY_KEY}" > "$HOME/buildbuddy-key.pem";
fi

if [[ -z "${SSHKEY}" ]]; then
  echo "No SSH key detected.";
else
  echo "- Installing SSH key...";
  mkdir -p $HOME/.ssh;
  echo "${SSHKEY}" > "$HOME/.ssh/id_rsa";
  chmod 600 $HOME/.ssh/id_rsa;
  chown dev:engineering $HOME/.ssh/id_rsa;
fi

