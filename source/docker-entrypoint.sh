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

# hwmdm-runtime.js: necessario para o iframe do Servidor (adminPort)
mkdir -p "$TOMCAT_DIR/webapps/ROOT/js"
echo "window.HWMDM_RUNTIME = {adminPort: '${ADMIN_PORT:-}'};" > "$TOMCAT_DIR/webapps/ROOT/js/hwmdm-runtime.js"

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

cp /opt/java/openjdk/conf/security/java.security /tmp/java.security
sed "s|securerandom.source=file:/dev/random|securerandom.source=file:/dev/urandom|g" /tmp/java.security > /opt/java/openjdk/conf/security/java.security
rm /tmp/java.security

# Fix permissões: Tomcat explode o WAR como root, mas o usuario do host (UID 1000)
# precisa poder editar os arquivos. Roda em background enquanto o Tomcat sobe.
(
  sleep 15
  if [ -d "$TOMCAT_DIR/webapps/ROOT" ]; then
    chown -R 1000:1000 "$TOMCAT_DIR/webapps/ROOT" 2>/dev/null
    chmod -R u+w "$TOMCAT_DIR/webapps/ROOT" 2>/dev/null
  fi
  chown 1000:1000 "$TOMCAT_DIR/webapps/ROOT/js/hwmdm-runtime.js" 2>/dev/null
) &

catalina.sh run
