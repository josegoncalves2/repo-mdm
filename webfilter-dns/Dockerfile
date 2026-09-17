FROM python:3.12-slim

ARG BLOCKY_VERSION=v0.34.0
ARG BLOCKY_DIGEST=17b03f892346a160e9faf974ce68baae85fa4f2a94d7bf8ea52592a94be5eeb4

RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates bash && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL "https://github.com/0xERR0R/blocky/releases/download/${BLOCKY_VERSION}/blocky_${BLOCKY_VERSION}_Linux_x86_64.tar.gz" -o /tmp/blocky.tar.gz \
    && echo "${BLOCKY_DIGEST}  /tmp/blocky.tar.gz" | sha256sum -c \
    && tar -xzf /tmp/blocky.tar.gz -C /usr/local/bin/ blocky \
    && chmod +x /usr/local/bin/blocky \
    && rm /tmp/blocky.tar.gz

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ /app/
COPY entrypoint.sh /app/
RUN chmod +x /app/entrypoint.sh && mkdir -p /app/dns /app/profiles /app/certs

# Create default config.yml
RUN cat > /app/config.yml <<'EOF'
ports:
  dns: 53

log:
  level: info

upstreams:
  groups:
    default:
      - https://dns.google/dns-query
EOF

WORKDIR /app
ENTRYPOINT ["/app/entrypoint.sh"]
