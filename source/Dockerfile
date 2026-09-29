# Dockerfile UNICO da stack HWMDM. Cada servico construido aqui e' um alvo (target) no
# docker-compose.yaml; o MDM (Tomcat) usa a imagem oficial headwindmdm/hmdm e so' recebe o
# docker-entrypoint.sh, o overlay do console e o WAR por volume.

# ---------------------------------------------------------------- hwmdm-webfilter
# Resolvedor DNS-over-TLS do Web Filter: Blocky fixado por versao e sha256, supervisionado
# por webfilter/resolver.py (so biblioteca padrao do Python).
FROM python:3.12-slim AS webfilter

ARG BLOCKY_VERSION=v0.34.0
ARG BLOCKY_SHA256=17b03f892346a160e9faf974ce68baae85fa4f2a94d7bf8ea52592a94be5eeb4

RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL "https://github.com/0xERR0R/blocky/releases/download/${BLOCKY_VERSION}/blocky_${BLOCKY_VERSION}_Linux_x86_64.tar.gz" -o /tmp/blocky.tar.gz \
    && echo "${BLOCKY_SHA256}  /tmp/blocky.tar.gz" | sha256sum -c - \
    && tar -xzf /tmp/blocky.tar.gz -C /usr/local/bin blocky \
    && rm /tmp/blocky.tar.gz \
    && mkdir -p /app/dns /app/lists /app/state /app/certs /app/queries

COPY webfilter/resolver.py /app/resolver.py
WORKDIR /app
EXPOSE 853/tcp
ENTRYPOINT ["python3", "-u", "/app/resolver.py"]

# ---------------------------------------------------------------- hwmdm-admin
# Gestao do servidor pela web (configuracao, containers, aplicar mudancas). Fica fora do
# Tomcat para sobreviver ao restart/recreate/build dos outros containers que ele comanda.
FROM alpine:3.20 AS admin

RUN apk add --no-cache python3 py3-psycopg2 docker-cli docker-cli-compose tzdata
COPY admin/app.py /app/app.py
WORKDIR /app
EXPOSE 8090/tcp
HEALTHCHECK --interval=30s --timeout=5s CMD wget -qO- http://127.0.0.1:${ADMIN_PORT:-8090}/health || exit 1
ENTRYPOINT ["python3", "-u", "/app/app.py"]
