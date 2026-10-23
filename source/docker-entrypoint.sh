#!/bin/sh
# Entrypoint MINIMO: sobe o Tomcat. NADA destrutivo.
# O webapp esta no volume webapps/ e e' editado ao vivo.
# O hwmdm-runtime.js com adminPort e' escrito pelo entrypoint original
# que esta em /docker-entrypoint.original.sh dentro da imagem.

TOMCAT_DIR=/usr/local/tomcat
BASE_DIR=$TOMCAT_DIR/work

for DIR in cache files plugins logs; do
   [ -d "$BASE_DIR/$DIR" ] || mkdir "$BASE_DIR/$DIR"
done

# Aguarda o banco ficar pronto
until PGPASSWORD=$SQL_PASS psql -h "$SQL_HOST" -U "$SQL_USER" -d "$SQL_BASE" -c '\q' 2>/dev/null; do
  echo "Waiting for PostgreSQL..."
  sleep 3
done

# Libera lock do Liquibase que ficou preso em boot anterior
if PGPASSWORD=$SQL_PASS psql -h "$SQL_HOST" -U "$SQL_USER" -d "$SQL_BASE" -tAc \
     "select to_regclass('public.databasechangeloglock') is not null;" 2>/dev/null | grep -q '^t$'; then
    STALE=$(PGPASSWORD=$SQL_PASS psql -h "$SQL_HOST" -U "$SQL_USER" -d "$SQL_BASE" -tAc \
            "select coalesce(lockedby,'?') from databasechangeloglock where locked;" 2>/dev/null)
    if [ -n "$STALE" ]; then
        echo "Releasing stale Liquibase lock held by: $STALE"
        PGPASSWORD=$SQL_PASS psql -h "$SQL_HOST" -U "$SQL_USER" -d "$SQL_BASE" -c \
          "update databasechangeloglock set locked=false, lockgranted=null, lockedby=null where locked;"
    fi
fi

# ROOT.xml gerado do .env em TODA subida: um arquivo que sobrevive entre subidas carrega o
# endereco (base.url) e o segredo de outro servidor.
CONTEXT_DIR=$TOMCAT_DIR/conf/Catalina/localhost
mkdir -p "$CONTEXT_DIR"
TEMPLATE=/opt/hmdm/templates/conf/context_template.xml
if [ -f "$TEMPLATE" ]; then
    sed -e "s|_SQL_HOST_|${SQL_HOST}|g" \
        -e "s|_SQL_PORT_|${POSTGRES_INTERNAL_PORT}|g" \
        -e "s|_SQL_BASE_|${SQL_BASE}|g" \
        -e "s|_SQL_USER_|${SQL_USER}|g" \
        -e "s|_SQL_PASS_|${SQL_PASS}|g" \
        -e "s|_PROTOCOL_|${PROTOCOL}|g" \
        -e "s|_BASE_DOMAIN_|${BASE_DOMAIN}|g" \
        -e "s|_SHARED_SECRET_|${SHARED_SECRET}|g" \
        -e "s|_PROXY_ADDRESSES_|${PROXY_ADDRESSES}|g" \
        -e "s|_MQTT_SERVER_URI_|${MQTT_SERVER_URI}|g" \
        -e "s|_SESSION_RANDOM_ALGORITHM_|${MDM_SESSION_RANDOM_ALGORITHM}|g" \
        "$TEMPLATE" > "$CONTEXT_DIR/ROOT.xml"
    echo "Generated ROOT.xml from template (base.url=${PROTOCOL}://${BASE_DOMAIN})"
fi

cp /opt/java/openjdk/conf/security/java.security /tmp/java.security
sed "s|securerandom.source=file:/dev/random|securerandom.source=file:/dev/urandom|g" /tmp/java.security > /opt/java/openjdk/conf/security/java.security
rm /tmp/java.security

# Uma unica gravacao no boot: sem ROOT.war (webapps/ e' imutavel) o Tomcat nao
# re-explode nada, entao nao ha o que reescrever depois.
if [ -d "$TOMCAT_DIR/webapps/ROOT/js" ]; then
  INFRA_JS=$(printf '%s' "${INFRASTRUCTURE_IPS}" | tr -d ' ' | sed "s/[^,][^,]*/'&'/g")
  echo "window.HWMDM_RUNTIME = {adminPort: '${ADMIN_PORT}', infrastructureIps: [${INFRA_JS}], deviceOnlineSeconds: ${DEVICE_ONLINE_SECONDS}, deviceAwaySeconds: ${DEVICE_AWAY_SECONDS}, gpsStopSeconds: ${GPS_STOP_SECONDS}};" > "$TOMCAT_DIR/webapps/ROOT/js/hwmdm-runtime.js"
  chown 1000:1000 "$TOMCAT_DIR/webapps/ROOT/js/hwmdm-runtime.js" 2>/dev/null
fi

# build-info.json (rodape e Sobre) gerado a cada subida: versao do .env, commit do .git
# montado, componentes lidos do que esta de fato instalado (app.js, jars, banco, imagem).
ROOT_DIR=$TOMCAT_DIR/webapps/ROOT
if [ -d "$ROOT_DIR" ]; then
  GIT_REF=""; GIT_COMMIT=""; GIT_EPOCH=""
  if [ -f "$HWMDM_GIT_MOUNT/HEAD" ]; then
    GIT_REF=$(sed -n 's/^ref: //p' "$HWMDM_GIT_MOUNT/HEAD")
    if [ -n "$GIT_REF" ]; then
      GIT_COMMIT=$(cat "$HWMDM_GIT_MOUNT/$GIT_REF" 2>/dev/null || sed -n "s| $GIT_REF\$||p" "$HWMDM_GIT_MOUNT/packed-refs" 2>/dev/null)
    else
      GIT_COMMIT=$(cat "$HWMDM_GIT_MOUNT/HEAD")
    fi
    GIT_EPOCH=$(tail -n 1 "$HWMDM_GIT_MOUNT/logs/HEAD" 2>/dev/null | sed -n 's/^.*> \([0-9]*\) [-+][0-9]*\t.*$/\1/p')
  fi
  GIT_SHORT=$(printf '%.8s' "$GIT_COMMIT")
  COMMIT_UTC=""
  [ -n "$GIT_EPOCH" ] && COMMIT_UTC=$(date -u -d "@$GIT_EPOCH" +%Y-%m-%dT%H:%M:%SZ)
  CORE_VERSION=$(sed -n 's/.*"APP_VERSION", *"\([^"]*\)".*/\1/p' "$ROOT_DIR/app/app.js" | head -n 1)
  jar_version() { ls "$ROOT_DIR/WEB-INF/lib" | sed -n "s/^$1-\(.*\)\.jar\$/\1/p" | head -n 1; }
  PKGS_SQL=$(printf '%s' "${HWMDM_AGENT_PKGS}" | tr -d ' ' | sed "s/[^,][^,]*/'&'/g")
  AGENTS_JSON=$(PGPASSWORD=$SQL_PASS psql -h "$SQL_HOST" -U "$SQL_USER" -d "$SQL_BASE" -tA -c \
    "select coalesce(json_object_agg(a.pkg, json_build_object('nome', a.name, 'versao', v.version) order by a.pkg), '{}')
       from applications a join applicationversions v on v.id = a.latestversion
      where a.pkg in (${PKGS_SQL:-''})" 2>/dev/null)
  [ -n "$AGENTS_JSON" ] || AGENTS_JSON='{}'
  cat > "$ROOT_DIR/build-info.json" <<EOF
{
  "versao": "${HWMDM_VERSION}",
  "commit": "${GIT_SHORT}",
  "ref": "${GIT_REF#refs/heads/}",
  "data_commit_utc": "${COMMIT_UTC}",
  "componentes": {
    "nucleo": {"nome": "Núcleo Headwind MDM", "versao": "${CORE_VERSION}"},
    "plugin_webfilter": {"nome": "Plugin Web Filter", "versao": "$(jar_version webfilter)"},
    "plugin_moduleregistry": {"nome": "Plugin Module Registry", "versao": "$(jar_version moduleregistry)"},
    "resolvedor_dns": {"nome": "Resolvedor DNS do Web Filter", "versao": "${WEBFILTER_IMAGE##*:}"}
  },
  "agentes": ${AGENTS_JSON}
}
EOF
  chown 1000:1000 "$ROOT_DIR/build-info.json" 2>/dev/null
  echo "Generated build-info.json (HWMDM ${HWMDM_VERSION}, commit ${GIT_SHORT:-?})"
fi

catalina.sh run
