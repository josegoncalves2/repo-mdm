# Resolvedor DNS-over-TLS do Web Filter (design D3/D8): Blocky fixado por versao e sha256,
# supervisionado por app/resolver.py (so biblioteca padrao do Python).
FROM python:3.12-slim

ARG BLOCKY_VERSION=v0.34.0
ARG BLOCKY_SHA256=17b03f892346a160e9faf974ce68baae85fa4f2a94d7bf8ea52592a94be5eeb4

RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL "https://github.com/0xERR0R/blocky/releases/download/${BLOCKY_VERSION}/blocky_${BLOCKY_VERSION}_Linux_x86_64.tar.gz" -o /tmp/blocky.tar.gz \
    && echo "${BLOCKY_SHA256}  /tmp/blocky.tar.gz" | sha256sum -c - \
    && tar -xzf /tmp/blocky.tar.gz -C /usr/local/bin blocky \
    && rm /tmp/blocky.tar.gz \
    && mkdir -p /app/dns /app/lists /app/state /app/certs /app/queries

COPY app/resolver.py /app/resolver.py
WORKDIR /app
EXPOSE 853/tcp
ENTRYPOINT ["python3", "-u", "/app/resolver.py"]
