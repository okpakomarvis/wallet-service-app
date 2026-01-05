#!/usr/bin/env sh
set -e

mkdir -p /etc/secrets

echo "$KAFKA_KEYSTORE_B64" | base64 -d > /etc/secrets/client.keystore.p12
echo "$KAFKA_TRUSTSTORE_B64" | base64 -d > /etc/secrets/client.truststore.jks

exec "$@"
