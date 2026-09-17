#!/bin/sh
set -e

mkdir -p /app/dns /app/profiles /app/certs

# If blocky.yml exists in the writable dns directory, use it
if [ -f /app/dns/blocky.yml ]; then
    cp /app/dns/blocky.yml /app/config.yml
    echo "Using blocky.yml from /app/dns"
else
    echo "No blocky.yml found in /app/dns, using default config.yml"
fi

cd /app
exec /usr/local/bin/blocky --config /app/config.yml
