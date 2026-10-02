#!/bin/sh
# Entrypoint completo do HWMDM.
# - Aplica overlay do custom-webapp (APPLY_CUSTOM_WEBAPP_ON_BOOT=true) sem destruir plugins
# - Preserva edicoes ao vivo em webapps/ROOT/ (NUNCA recopia o WAR se ja existe)
# - Corrige permissoes no final para o usuario do host (HOST_UID:HOST_GID)
# - Versao 2026-10-02

HMDM_DIR=/opt/hmdm
TEMPLATE_DIR=$HMDM_DIR/templates
TOMCAT_DIR=/usr/local/tomcat
BASE_DIR=$TOMCAT_DIR/work
CACHE_DIR=$BASE_DIR/cache
PUBLIC_PROTOCOL="${PUBLIC_PROTOCOL:-$PROTOCOL}"
CUSTOM_WEBAPP_DIR=/opt/custom-webapp
APPLY_CUSTOM_WEBAPP_ON_BOOT="${APPLY_CUSTOM_WEBAPP_ON_BOOT:-false}"
PROXY_ADDRESSES="${PROXY_ADDRESSES:-}"
AUTO_UPDATE_WEBAPP="${AUTO_UPDATE_WEBAPP:-false}"
HOST_UID="${HOST_UID:-}"
HOST_GID="${HOST_GID:-}"

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

if [ "$AUTO_UPDATE_WEBAPP" = "true" ]; then
    $HMDM_DIR/update-web-app-docker.sh
else
    rm -f "$TOMCAT_DIR/work/files/hmdm_web_update_manifest.txt"
fi

# ---------------------------------------------------------------------------
# Overlay do custom-webapp (APPLY_CUSTOM_WEBAPP_ON_BOOT)
# ---------------------------------------------------------------------------
# Aplica overlay SOMENTE NA PRIMEIRA INICIALIZACAO (quando js/hwmdm-runtime.js
# ainda nao existe). Depois disso, as edicoes ao vivo em webapps/ROOT/ sao
# PRESERVADAS -- o overlay NAO sobrescreve arquivos existentes.
# ---------------------------------------------------------------------------
if [ "$APPLY_CUSTOM_WEBAPP_ON_BOOT" = "true" ] && [ -d "$CUSTOM_WEBAPP_DIR" ] && [ -d "$TOMCAT_DIR/webapps/ROOT" ]; then
    if [ ! -f "$TOMCAT_DIR/webapps/ROOT/js/hwmdm-runtime.js" ]; then
        echo "Aplicando overlay direto em webapps/ROOT/ (primeira inicializacao)..."
        # Preserva plugins compilados
        if [ -d "$TOMCAT_DIR/webapps/ROOT/app/components/plugins" ]; then
            cp -a "$TOMCAT_DIR/webapps/ROOT/app/components/plugins" /tmp/plugins-keep
        fi
        # Apenas copia o que existe nas fontes (nao remove nada)
        cp -a "$CUSTOM_WEBAPP_DIR/." "$TOMCAT_DIR/webapps/ROOT/"
        # Restaura plugins
        if [ -d /tmp/plugins-keep ]; then
            rm -rf "$TOMCAT_DIR/webapps/ROOT/app/components/plugins"
            cp -a /tmp/plugins-keep "$TOMCAT_DIR/webapps/ROOT/app/components/plugins"
            rm -rf /tmp/plugins-keep
        fi
        echo "Overlay direto concluido."
    else
        echo "Overlay pulado: webapps/ROOT/ ja foi inicializado (edicoes ao vivo preservadas)."
    fi
    # Gera/configura runtime.js sempre (e' seguro, nunca quebra edicoes)
    mkdir -p "$TOMCAT_DIR/webapps/ROOT/js"
    echo "window.HWMDM_RUNTIME = {adminPort: '${ADMIN_PORT:-}'};" > "$TOMCAT_DIR/webapps/ROOT/js/hwmdm-runtime.js"
fi

# ---------------------------------------------------------------------------
# Templates e configuracao
# ---------------------------------------------------------------------------
if [ ! -f "$BASE_DIR/log4j.xml" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
    cp "$TEMPLATE_DIR/conf/log4j_template.xml" "$BASE_DIR/log4j-hmdm.xml"
fi
if [ ! -d "$BASE_DIR/emails" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
    cp -r "$TEMPLATE_DIR/emails" "$BASE_DIR/emails"
fi
if [ ! -d "$TOMCAT_DIR/conf/Catalina/localhost" ]; then
    mkdir -p "$TOMCAT_DIR/conf/Catalina/localhost"
fi
if [ ! -f "$TOMCAT_DIR/conf/Catalina/localhost/ROOT.xml" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
    cat "$TEMPLATE_DIR/conf/context_template.xml" | sed "s|_SQL_HOST_|$SQL_HOST|g; s|_SQL_PORT_|$SQL_PORT|g; s|_SQL_BASE_|$SQL_BASE|g; s|_SQL_USER_|$SQL_USER|g; s|_SQL_PASS_|$SQL_PASS|g; s|_PROTOCOL_|$PUBLIC_PROTOCOL|g; s|_BASE_DOMAIN_|$BASE_DOMAIN|g; s|_SHARED_SECRET_|$SHARED_SECRET|g; s|_PROXY_ADDRESSES_|$PROXY_ADDRESSES|g;" > "$TOMCAT_DIR/conf/Catalina/localhost/ROOT.xml"
fi

for DIR in cache files plugins logs; do
   [ -d "$BASE_DIR/$DIR" ] || mkdir "$BASE_DIR/$DIR"
done

if [ "$INSTALL_LANGUAGE" != "ru" ]; then
    INSTALL_LANGUAGE=en
fi

FILES_TO_DOWNLOAD=""
if [ ! -f "$BASE_DIR/init.sql" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
    cat "$TEMPLATE_DIR/sql/hmdm_init.$INSTALL_LANGUAGE.sql" | sed "s|_ADMIN_EMAIL_|$ADMIN_EMAIL|g; s|_HMDM_VERSION_|$CLIENT_VERSION|g; s|_HMDM_VARIANT_|$HMDM_VARIANT|g" > "$BASE_DIR/init1.sql"
    FILES_TO_DOWNLOAD=$(grep 'https://h-mdm.com' "$BASE_DIR/init1.sql" | awk '{ print $4 }' | sed "s/'//g; s/)//g; s/,//g")
    cat "$BASE_DIR/init1.sql" | sed "s|https://h-mdm.com|$PUBLIC_PROTOCOL://$BASE_DOMAIN|g" > "$BASE_DIR/init.sql"
    rm "$BASE_DIR/init1.sql"
fi

cd "$BASE_DIR/files" || true
for FILE in $FILES_TO_DOWNLOAD; do
    FILENAME=$(basename "$FILE")
    if [ ! -f "$BASE_DIR/files/$FILENAME" ]; then
        wget "$FILE" || true
    fi
done

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

# ---------------------------------------------------------------------------
# Corrige permissoes dos volumes montados
# ---------------------------------------------------------------------------
if [ -d "$TOMCAT_DIR/webapps/ROOT" ]; then
    find "$TOMCAT_DIR/webapps/ROOT" -type d -exec chmod 775 {} \; 2>/dev/null || true
    find "$TOMCAT_DIR/webapps/ROOT" -type f -exec chmod 664 {} \; 2>/dev/null || true
fi
chmod 775 "$TOMCAT_DIR/webapps" 2>/dev/null || true
chmod 775 "$TOMCAT_DIR/webapps/ROOT.war" 2>/dev/null || true
if [ -d "$BASE_DIR/logs" ]; then
    find "$BASE_DIR/logs" -type d -exec chmod 775 {} \; 2>/dev/null || true
    find "$BASE_DIR/logs" -type f -exec chmod 664 {} \; 2>/dev/null || true
fi
if [ -d "$BASE_DIR/files" ]; then
    find "$BASE_DIR/files" -type d -exec chmod 775 {} \; 2>/dev/null || true
    find "$BASE_DIR/files" -type f -exec chmod 664 {} \; 2>/dev/null || true
fi

# Se HOST_UID/HOST_GID foram passados, tenta chown
if [ -n "$HOST_UID" ] && [ -n "$HOST_GID" ]; then
    if getent group "$HOST_GID" >/dev/null 2>&1 || addgroup -g "$HOST_GID" hostgroup 2>/dev/null; then
        if getent passwd "$HOST_UID" >/dev/null 2>&1 || adduser -u "$HOST_UID" -G hostgroup -D hostuser 2>/dev/null; then
            chown -R "$HOST_UID:$HOST_GID" "$TOMCAT_DIR/webapps/ROOT" 2>/dev/null || true
            chown "$HOST_UID:$HOST_GID" "$TOMCAT_DIR/webapps" 2>/dev/null || true
        fi
    fi
fi

echo "Entrypoint concluido. Iniciando Tomcat..."
exec catalina.sh run
