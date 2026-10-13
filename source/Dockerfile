# Dockerfile UNICO da stack HWMDM. Cada servico construido aqui e' um alvo (target) no
# docker-compose.yaml; o MDM (Tomcat) usa a imagem oficial headwindmdm/hmdm e so' recebe o
# docker-entrypoint.sh, o overlay do console e o WAR por volume.

# ---------------------------------------------------------------- hwmdm-webfilter
# Resolvedor DNS-over-TLS do Web Filter: Blocky fixado por versao e sha256, supervisionado
# por webfilter/resolver.py (so biblioteca padrao do Python).
FROM python:3.12-slim AS webfilter

# Blocky versionado em vendor/ (conferido por vendor/SHA256SUMS): o build nao baixa nada.
COPY vendor/blocky-v0.34.0-linux-x86_64 /usr/local/bin/blocky
RUN mkdir -p /app/dns /app/lists /app/state /app/certs /app/queries

COPY webfilter/resolver.py /app/resolver.py
WORKDIR /app
EXPOSE 853/tcp
ENTRYPOINT ["python3", "-u", "/app/resolver.py"]

# ---------------------------------------------------------------- hwmdm-admin
# Gestao do servidor pela web (configuracao, containers, aplicar mudancas). Fica fora do
# Tomcat para sobreviver ao restart/recreate/build dos outros containers que ele comanda.
FROM eclipse-temurin:21-jdk-alpine AS admin-build
COPY admin/HwmdmAdmin.java /build/HwmdmAdmin.java
COPY vendor/postgresql-42.7.3.jar /build/lib/postgresql.jar
WORKDIR /build
RUN javac -cp lib/postgresql.jar HwmdmAdmin.java

FROM eclipse-temurin:21-jre-alpine AS admin
RUN apk add --no-cache docker-cli docker-cli-compose tzdata wget
COPY --from=admin-build /build/*.class /app/
COPY --from=admin-build /build/lib/postgresql.jar /app/lib/postgresql.jar
WORKDIR /app
EXPOSE 8090/tcp
HEALTHCHECK --interval=30s --timeout=5s CMD wget -qO- http://127.0.0.1:${ADMIN_PORT:-8090}/health || exit 1
ENTRYPOINT ["java", "-cp", "/app:/app/lib/postgresql.jar", "HwmdmAdmin"]
