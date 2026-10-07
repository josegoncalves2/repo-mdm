#!/bin/sh
# Entrypoint MINIMO: sobe o Tomcat. NADA destrutivo.
# O webapp esta no volume webapps/ e e' editado ao vivo.
# O hwmdm-runtime.js com adminPort e' escrito pelo entrypoint original
# que esta em /docker-entrypoint.original.sh dentro da imagem.

TOMCAT_DIR=/usr/local/tomcat
BASE_DIR=$TOMCAT_DIR/work
SQL_HOST="${SQL_HOST:-postgresql}"
SQL_USER="${SQL_USER:-hmdm}"
SQL_PASS="${SQL_PASS:-hmdm}"
SQL_BASE="${SQL_BASE:-hmdm}"

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

# Gera ROOT.xml se não existir (necessário para Liquibase/JDBC)
CONTEXT_DIR=$TOMCAT_DIR/conf/Catalina/localhost
if [ ! -f "$CONTEXT_DIR/ROOT.xml" ]; then
  mkdir -p "$CONTEXT_DIR"
  TEMPLATE=/opt/hmdm/templates/conf/context_template.xml
  if [ -f "$TEMPLATE" ]; then
    sed -e "s|_SQL_HOST_|${SQL_HOST}|g" \
        -e "s|_SQL_PORT_|5432|g" \
        -e "s|_SQL_BASE_|${SQL_BASE}|g" \
        -e "s|_SQL_USER_|${SQL_USER}|g" \
        -e "s|_SQL_PASS_|${SQL_PASS}|g" \
        -e "s|_PROTOCOL_|${PROTOCOL:-http}|g" \
        -e "s|_BASE_DOMAIN_|${BASE_DOMAIN:-localhost}|g" \
        -e "s|_SHARED_SECRET_|${SHARED_SECRET:-}|g" \
        -e "s|_PROXY_ADDRESSES_|${PROXY_ADDRESSES:-}|g" \
        "$TEMPLATE" | sed 's|<Context>|<Context docBase="/usr/local/tomcat/webapps/ROOT">|' > "$CONTEXT_DIR/ROOT.xml"
    echo "Generated ROOT.xml from template"
  fi
fi

cp /opt/java/openjdk/conf/security/java.security /tmp/java.security
sed "s|securerandom.source=file:/dev/random|securerandom.source=file:/dev/urandom|g" /tmp/java.security > /opt/java/openjdk/conf/security/java.security
rm /tmp/java.security

# Fix runtime.js: Tomcat explode o WAR como root e sobrescreve o runtime.js.
# Escreve imediatamente (sem sleep) e re-escreve sempre que o Tomcat recriar o diretório.
(
  while true; do
    if [ -d "$TOMCAT_DIR/webapps/ROOT/js" ]; then
      echo "window.HWMDM_RUNTIME = {adminPort: '${ADMIN_PORT:-}'};" > "$TOMCAT_DIR/webapps/ROOT/js/hwmdm-runtime.js"
      chown 1000:1000 "$TOMCAT_DIR/webapps/ROOT/js/hwmdm-runtime.js" 2>/dev/null
    fi
    sleep 5
  done
) &

catalina.sh run
