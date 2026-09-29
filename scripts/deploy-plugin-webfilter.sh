#!/usr/bin/env bash
# Compila o plugin Web Filter (Java 8, contra as libs do WAR no ar) e grava jar + UI do plugin
# nos TRES lugares que o MDM le: volumes/webapps/ROOT.war, dist/hmdm.war e o jar do overlay
# (server-source/server/src/main/webapp/WEB-INF/lib), que sobrepoe o do WAR no boot.
# Guarda antes uma copia de tudo em source/volumes/backups/deploy-webfilter-<data>/.
# O hwmdm-mdm e' parado durante a troca (o Tomcat recarrega sozinho quando o ROOT.war muda).
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
P="$RAIZ/server-source/plugins/webfilter/src/main"
W="$(mktemp -d)"
trap 'docker run --rm -v "$W:/w" alpine rm -rf /w/war /w/out /w/jar /w/ui >/dev/null 2>&1; rm -rf "$W"' EXIT
B="$RAIZ/source/volumes/backups/deploy-webfilter-$(date +%Y%m%d-%H%M%S)"

mkdir -p "$W/war" "$W/out" "$W/jar" "$W/tlib" "$W/ui/app/components/plugins/webfilter" "$W/ui/WEB-INF/lib"
(cd "$W/war" && unzip -q "$RAIZ/source/volumes/webapps/ROOT.war" 'WEB-INF/*')
(cd "$W/jar" && unzip -q "$W/war/WEB-INF/lib/webfilter-0.1.0.jar")
docker cp hwmdm-mdm:/usr/local/tomcat/lib/servlet-api.jar "$W/tlib/"

docker run --rm -v "$W:/w" -v "$P:/src:ro" eclipse-temurin:8-jdk sh -c \
  'find /src/java -name "*.java" > /tmp/f && javac -nowarn -encoding UTF-8 -source 8 -target 8 \
     -cp "/w/war/WEB-INF/classes:/w/war/WEB-INF/lib/*:/w/tlib/*" -d /w/out @/tmp/f'
rm -rf "$W/jar/com" && cp -a "$W/out/com" "$W/jar/"
cp "$P/resources/liquibase/webfilter.changelog.xml" "$W/jar/liquibase/"
cp "$P"/resources/*.json "$W/jar/"
cp -a "$P/webapp/." "$W/ui/app/components/plugins/webfilter/"
docker run --rm -v "$W:/w" eclipse-temurin:8-jdk sh -c \
  'cd /w/jar && jar cfm /w/ui/WEB-INF/lib/webfilter-0.1.0.jar META-INF/MANIFEST.MF $(ls | grep -v META-INF)'

(cd "$RAIZ/source" && docker compose stop hmdm)
docker run --rm -v "$RAIZ:$RAIZ" -v "$W:/w" eclipse-temurin:8-jdk sh -c "
  set -e
  mkdir -p '$B'
  cp '$RAIZ/source/volumes/webapps/ROOT.war' '$B/ROOT.war'
  cp '$RAIZ/dist/hmdm.war' '$B/dist-hmdm.war'
  cp '$RAIZ/server-source/server/src/main/webapp/WEB-INF/lib/webfilter-0.1.0.jar' '$B/overlay-webfilter.jar'
  cd /w/ui
  jar uf '$RAIZ/source/volumes/webapps/ROOT.war' WEB-INF/lib/webfilter-0.1.0.jar app/components/plugins/webfilter
  jar uf '$RAIZ/dist/hmdm.war' WEB-INF/lib/webfilter-0.1.0.jar app/components/plugins/webfilter
  cp WEB-INF/lib/webfilter-0.1.0.jar '$RAIZ/server-source/server/src/main/webapp/WEB-INF/lib/webfilter-0.1.0.jar'
  chown $(id -u):$(id -g) '$RAIZ/dist/hmdm.war' '$RAIZ/server-source/server/src/main/webapp/WEB-INF/lib/webfilter-0.1.0.jar'"
sha256sum "$RAIZ/dist/hmdm.war" > "$RAIZ/dist/hmdm.war.sha256"
(cd "$RAIZ/source" && docker compose up -d hmdm)
echo "backup: $B"
echo "jar: $(sha256sum "$RAIZ/server-source/server/src/main/webapp/WEB-INF/lib/webfilter-0.1.0.jar" | cut -c1-16)"
