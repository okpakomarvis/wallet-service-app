#!/usr/bin/env bash

ENV_FILE=".env"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ $ENV_FILE file not found"
  return 1  # IMPORTANT: use return, not exit
fi

echo "🔄 Loading environment variables from $ENV_FILE..."

# Automatically export all variables
set -a
source "$ENV_FILE"
set +a

echo "✅ Environment variables loaded"