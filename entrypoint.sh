#!/usr/bin/env sh
set -e

echo "🔐 Reconstructing Kafka SSL files..."

# Writable location on Render
mkdir -p /tmp/secrets

echo "$KAFKA_KEYSTORE_B64" | base64 -d > /tmp/secrets/client.keystore.p12
echo "$KAFKA_TRUSTSTORE_B64" | base64 -d > /tmp/secrets/client.truststore.jks

echo "✅ Kafka SSL files created:"
ls -l /tmp/secrets

exec "$@"
