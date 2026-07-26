#!/bin/sh
HMDM_DIR=/opt/hmdm
TEMPLATE_DIR=$HMDM_DIR/templates
TOMCAT_DIR=/usr/local/tomcat
BASE_DIR=$TOMCAT_DIR/work
CACHE_DIR=$BASE_DIR/cache
PASSWORD=123456
PUBLIC_PROTOCOL="${PUBLIC_PROTOCOL:-$PROTOCOL}"
CUSTOM_WEBAPP_DIR=/opt/custom-webapp
APPLY_CUSTOM_WEBAPP_ON_BOOT="${APPLY_CUSTOM_WEBAPP_ON_BOOT:-false}"
AUTO_UPDATE_WEBAPP="${AUTO_UPDATE_WEBAPP:-false}"

for DIR in cache files plugins logs; do
   [ -d "$BASE_DIR/$DIR" ] || mkdir "$BASE_DIR/$DIR"
done

if [ ! -z "$LOCAL_IP" ]; then
    EXISTS=`grep $BASE_DOMAIN /etc/hosts`
    if [ -z "$EXISTS" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
        grep -v $BASE_DOMAIN /etc/hosts > /etc/hosts~
	cp /etc/hosts~ /etc/hosts
	echo "$LOCAL_IP $BASE_DOMAIN" >> /etc/hosts
	rm -f /etc/hosts~
    fi
fi

if [ -z "$HMDM_URL" ]; then
    # Se não tem URL, procura arquivo existente no cache
    HMDM_WAR=$(ls -1 $CACHE_DIR/*.war 2>/dev/null | head -1 | xargs basename)
    if [ -z "$HMDM_WAR" ]; then
        echo "ERROR: HMDM_URL não configurado e nenhum WAR encontrado em $CACHE_DIR"
        exit 1
    fi
    echo "Usando WAR existente: $HMDM_WAR"
else
    HMDM_WAR="$(basename -- $HMDM_URL)"

    if [ -f "$CACHE_DIR/$HMDM_WAR" ] && [ "$FORCE_RECONFIGURE" = "true" ]; then
        rm -f $CACHE_DIR/$HMDM_WAR
    fi

    if [ ! -f "$CACHE_DIR/$HMDM_WAR" ]; then
        if ! wget $DOWNLOAD_CREDENTIALS $HMDM_URL -O $CACHE_DIR/$HMDM_WAR; then
            echo "Failed to retrieve $HMDM_URL!"
            exit 1
        fi
    fi
fi

if [ ! -f "$TOMCAT_DIR/webapps/ROOT.war" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
    cp $CACHE_DIR/$HMDM_WAR $TOMCAT_DIR/webapps/ROOT.war
fi

if [ "$AUTO_UPDATE_WEBAPP" = "true" ]; then
    $HMDM_DIR/update-web-app-docker.sh
else
    rm -f "$TOMCAT_DIR/work/files/hmdm_web_update_manifest.txt"
fi

if [ "$APPLY_CUSTOM_WEBAPP_ON_BOOT" = "true" ] && [ -d "$CUSTOM_WEBAPP_DIR" ]; then
    # 'jar uf' only adds and overwrites -- it can never remove. Overlaying the custom webapp
    # that way leaves every file ever deleted from the sources still inside the WAR, which is
    # why retired screens kept coming back from the dead after they had been deleted.
    # So the overlay is rebuilt from scratch: explode the WAR, drop the whole app/ tree that
    # the overlay owns, copy the current sources in, and repack.
    # Only 'jar' is available in this image (no zip/unzip), so it does both ends.
    # 'cfM' keeps the MANIFEST.MF that came out of the original WAR instead of generating a new one.
    STAGE_DIR=$(mktemp -d)
    NEW_WAR=$(mktemp -u)   # kept outside STAGE_DIR so it is not packed into itself
    PLUGINS_KEEP=$(mktemp -d)
    if (
        cd "$STAGE_DIR" || exit 1
        jar xf "$TOMCAT_DIR/webapps/ROOT.war" || exit 1
        # app/components/plugins/ is assembled by Maven out of plugins/*/src/main/webapp and only
        # ever exists inside the built WAR -- it is NOT part of the mounted sources. Dropping the
        # whole app/ tree without setting it aside first silently deletes the UI of every plugin
        # (audit, deviceinfo, devicelog, messaging, push, xtra), leaving those menu entries dead.
        if [ -d ./app/components/plugins ]; then
            cp -a ./app/components/plugins "$PLUGINS_KEEP/plugins" || exit 1
        fi
        rm -rf ./app
        cp -a "$CUSTOM_WEBAPP_DIR/." . || exit 1
        if [ -d "$PLUGINS_KEEP/plugins" ]; then
            mkdir -p ./app/components || exit 1
            rm -rf ./app/components/plugins
            cp -a "$PLUGINS_KEEP/plugins" ./app/components/plugins || exit 1
        fi
        jar cfM "$NEW_WAR" -C "$STAGE_DIR" . || exit 1
    ) && [ -s "$NEW_WAR" ]; then
        mv "$NEW_WAR" "$TOMCAT_DIR/webapps/ROOT.war"
        echo "Custom webapp overlay rebuilt from $CUSTOM_WEBAPP_DIR (stale files removed)"
    else
        echo "ERROR: failed to rebuild the custom webapp overlay; keeping the existing ROOT.war" >&2
    fi
    rm -rf "$STAGE_DIR" "$NEW_WAR" "$PLUGINS_KEEP"
fi

if [ ! -f "$BASE_DIR/log4j.xml" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
    cp $TEMPLATE_DIR/conf/log4j_template.xml $BASE_DIR/log4j-hmdm.xml
fi

if [ ! -d "$BASE_DIR/emails" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
    cp -r $TEMPLATE_DIR/emails $BASE_DIR/emails
fi

if [ ! -d $TOMCAT_DIR/conf/Catalina/localhost ]; then
    mkdir -p $TOMCAT_DIR/conf/Catalina/localhost
fi

if [ ! -f "$TOMCAT_DIR/conf/Catalina/localhost/ROOT.xml" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
    cat $TEMPLATE_DIR/conf/context_template.xml | sed "s|_SQL_HOST_|$SQL_HOST|g; s|_SQL_PORT_|$SQL_PORT|g; s|_SQL_BASE_|$SQL_BASE|g; s|_SQL_USER_|$SQL_USER|g; s|_SQL_PASS_|$SQL_PASS|g; s|_PROTOCOL_|$PUBLIC_PROTOCOL|g; s|_BASE_DOMAIN_|$BASE_DOMAIN|g; s|_SHARED_SECRET_|$SHARED_SECRET|g;" > $TOMCAT_DIR/conf/Catalina/localhost/ROOT.xml 
fi

for DIR in cache files plugins logs; do
   [ -d "$BASE_DIR/$DIR" ] || mkdir "$BASE_DIR/$DIR"
done

if [ "$INSTALL_LANGUAGE" != "ru" ]; then
    INSTALL_LANGUAGE=en
fi

FILES_TO_DOWNLOAD=""
if [ ! -f "$BASE_DIR/init.sql" ] || [ "$FORCE_RECONFIGURE" = "true" ]; then
    cat $TEMPLATE_DIR/sql/hmdm_init.$INSTALL_LANGUAGE.sql | sed "s|_ADMIN_EMAIL_|$ADMIN_EMAIL|g; s|_HMDM_VERSION_|$CLIENT_VERSION|g; s|_HMDM_VARIANT_|$HMDM_VARIANT|g" > $BASE_DIR/init1.sql

    FILES_TO_DOWNLOAD=$(grep https://h-mdm.com $BASE_DIR/init1.sql | awk '{ print $4 }' | sed "s/'//g; s/)//g; s/,//g")

    cat $BASE_DIR/init1.sql | sed "s|https://h-mdm.com|$PUBLIC_PROTOCOL://$BASE_DOMAIN|g" > $BASE_DIR/init.sql
    rm $BASE_DIR/init1.sql
fi

cd $BASE_DIR/files
for FILE in $FILES_TO_DOWNLOAD; do
    FILENAME=$(basename $FILE)
    if [ ! -f "$BASE_DIR/files/$FILENAME" ]; then
	wget $FILE
    fi
done

# Waiting for the database
until PGPASSWORD=$SQL_PASS psql -h "$SQL_HOST" -U "$SQL_USER" -d "$SQL_BASE" -c '\q'; do
  echo "Waiting for the PostgreSQL database..."
  sleep 5
done

# Liquibase takes and releases the changelog lock ~90 times during boot, once per plugin
# changelog. Kill the JVM in any of those windows and the lock row stays set forever: the next
# boot then blocks in "Waiting for changelog lock...." and Tomcat never finishes starting, so
# the proxy answers 504 and every tablet reads as offline. Nothing else uses this database, so
# at boot time no live process can legitimately hold the lock -- whatever is there is a leftover.
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

# Avoid delays due to an issue with a random number
cp /opt/java/openjdk/conf/security/java.security /tmp/java.security
cat /tmp/java.security | sed "s|securerandom.source=file:/dev/random|securerandom.source=file:/dev/urandom|g" > /opt/java/openjdk/conf/security/java.security
rm /tmp/java.security

catalina.sh run

#sleep 100000
