#!/bin/sh
# Entrypoint MINIMO do HWMDM.
# - NUNCA sobrescreve webapps/ROOT/ (edicoes ao vivo sao feitas no volume)
# - NUNCA aplica overlay (o usuario deletou o entrypoint anterior por isso)
# - So espera o DB e inicia o Tomcat
# - Versao 2026-10-02

HMDM_DIR=/opt/hmdm
TEMPLATE_DIR=$HMDM_DIR/templates
TOMCAT_DIR=/usr/local/tomcat
BASE_DIR=$TOMCAT_DIR/work
CACHE_DIR=$BASE_DIR/cache
PUBLIC_PROTOCOL="${PUBLIC_PROTOCOL:-$PROTOCOL}"

for DIR in cache files plugins logs; do
   [ -d "$BASE_DIR/$DIR" ] || mkdir "$BASE_DIR/$DIR"
done

if [ ! -z "$LOCAL_IP" ]; then
    EXISTS=$(grep "$BASE_DOMAIN" /etc/hosts 2>/dev/null)
    if [ -z "$EXISTS" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
        grep -v "$BASE_DOMAIN" /etc/hosts > /etc/hosts~
        cp /etc/hosts~ /etc/hosts
        echo "$LOCAL_IP $BASE_DOMAIN" >> /etc/hosts
        rm -f /etc/hosts~
    fi
fi

if [ -z "$HMDM_URL" ]; then
    HMDM_WAR=$(ls -1 "$CACHE_DIR"/*.war 2>/dev/null | head -1 | xargs basename)
    if [ -z "$HMDM_WAR" ]; then
        echo "ERROR: HMDM_URL nao configurado e nenhum WAR encontrado em $CACHE_DIR"
        exit 1
    fi
    echo "Usando WAR existente: $HMDM_WAR"
else
    HMDM_WAR="$(basename -- "$HMDM_URL")"
    if [ -f "$CACHE_DIR/$HMDM_WAR" ] && [ "$FORCE_RECONFIGURE" = "true" ]; then
        rm -f "$CACHE_DIR/$HMDM_WAR"
    fi
    if [ ! -f "$CACHE_DIR/$HMDM_WAR" ]; then
        if ! wget $DOWNLOAD_CREDENTIALS "$HMDM_URL" -O "$CACHE_DIR/$HMDM_WAR"; then
            echo "Failed to retrieve $HMDM_URL!"
            exit 1
        fi
    fi
fi

# So copia o WAR se NAO existe nenhum. NUNCA recopia.
if [ ! -f "$TOMCAT_DIR/webapps/ROOT.war" ]; then
    cp "$CACHE_DIR/$HMDM_WAR" "$TOMCAT_DIR/webapps/ROOT.war"
    echo "WAR copiado para $TOMCAT_DIR/webapps/ROOT.war"
fi

# Waiting for the database
until PGPASSWORD=$SQL_PASS psql -h "$SQL_HOST" -U "$SQL_USER" -d "$SQL_BASE" -c '\q'; do
  echo "Waiting for the PostgreSQL database..."
  sleep 5
done

# Release stale Liquibase changelog lock
if PGPASSWORD=$SQL_PASS psql -h "$SQL_HOST" -U "$SQL_USER" -d "$SQL_BASE" -tAc \
     "select to_regclass('public.databasechangeloglock') is not null;" 2>/dev/null | grep -q '^t$'; then
    STALE=$(PGPASSWORD=$SQL_PASS psql -h "$SQL_HOST" -U "$SQL_USER" -d "$SQL_BASE" -tAc \
            "select coalesce(lockedby,'?') from databasechangeloglock where locked;" 2>/dev/null)
    if [ -n "$STALE" ]; then
        echo "Releasing a stale Liquibase changelog lock held by: $STALE"
        PGPASSWORD=$SQL_PASS psql -h "$SQL_HOST" -U "$SQL_USER" -d "$SQL_BASE" -c \
          "update databasechangeloglock set locked=false, lockgranted=null, lockedby=null where locked;"
    fi
fi

# Avoid delays due to random number issue
cp /opt/java/openjdk/conf/security/java.security /tmp/java.security
cat /tmp/java.security | sed "s|securerandom.source=file:/dev/random|securerandom.source=file:/dev/urandom|g" > /opt/java/openjdk/conf/security/java.security
rm /tmp/java.security

echo "Entrypoint concluido. Iniciando Tomcat..."
exec catalina.sh run
