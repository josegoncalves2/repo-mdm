#!/usr/bin/env bash
# Diz o que esta REALMENTE no ar no DEV agora -- sem confiar em dist/, sem confiar
# na memoria de ninguem. So' le (docker exec + SELECT); nunca publica, nunca
# reinicia container, nunca altera banco.
#
# Por que este script existe: em 2026-09-21 um agente concorrente substituiu
# dist/hmdm.war por um backup antigo e ninguem percebeu ate o webfilter sumir do
# console. Nao havia como checar "o que esta rodando bate com o que a gente acha
# que publicou" sem entrar no container a mao. Este script e' esse "a mao",
# automatizado: ele compara o hash do WAR dentro do container, o hash do WAR
# efetivamente publicado (webapps/ROOT.war, depois da sobreposicao de webapp) e
# o apkhash que o banco esta entregando no QR de matricula, contra os manifestos
# que scripts/build.sh grava em scripts/builds/. Quando as tres pontas nao
# batem, este script diz isso alto em vez de deixar passar.
#
# Uso: scripts/versao-publicada.sh
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

CONTAINER_APP="hwmdm-hmdm-1"
CONTAINER_PG="hwmdm-postgresql-1"
VERSAO_PY="$RAIZ/scripts/versao-artefato.py"

titulo() { printf '\n\033[1;36m=== %s ===\033[0m\n' "$*"; }
linha()  { printf '  %-32s %s\n' "$1:" "$2"; }
bate()   { printf '\033[1;32m  bate\033[0m %s\n' "$*"; }
nao_bate() { printf '\033[1;31m  NAO BATE\033[0m %s\n' "$*"; }
sem_rastro() { printf '\033[1;33m  sem rastro\033[0m %s\n' "$*"; }

for c in "$CONTAINER_APP" "$CONTAINER_PG"; do
    if ! docker inspect -f '{{.State.Running}}' "$c" >/dev/null 2>&1; then
        echo "ERRO: container '$c' nao esta rodando -- este script so' funciona com o DEV de pe (docker compose up -d)." >&2
        exit 1
    fi
done

manifesto_por_sha() {
    python3 "$VERSAO_PY" manifesto-por-sha "$1"
}

mostrar_manifesto() {
    # $1 = saida (possivelmente "null") de manifesto-por-sha
    local m="$1"
    if [ "$m" = "null" ] || [ -z "$m" ]; then
        sem_rastro "nenhum manifesto em scripts/builds/ tem este sha256 -- este build nao passou por scripts/build.sh"
        return
    fi
    local versao commit data arq
    versao="$(printf '%s' "$m" | python3 -c 'import json,sys;print(json.load(sys.stdin).get("versao"))')"
    commit="$(printf '%s' "$m" | python3 -c 'import json,sys;print(json.load(sys.stdin).get("commit"))')"
    data="$(printf '%s' "$m" | python3 -c 'import json,sys;print(json.load(sys.stdin).get("data_build_utc"))')"
    arq="$(printf '%s' "$m" | python3 -c 'import json,sys;print(json.load(sys.stdin).get("_manifesto"))')"
    bate "versao=$versao  commit=$commit  data=$data  ($arq)"
}

# ============================================================== CONSOLE (WAR)
titulo "CONSOLE -- WAR dentro de $CONTAINER_APP"

SHA_CACHE="$(docker exec "$CONTAINER_APP" sh -c 'sha256sum /usr/local/tomcat/work/cache/*.war 2>/dev/null' | awk '{print $1; exit}')"
NOME_CACHE="$(docker exec "$CONTAINER_APP" sh -c 'basename $(ls -1 /usr/local/tomcat/work/cache/*.war 2>/dev/null | head -1)' || true)"
if [ -n "$SHA_CACHE" ]; then
    linha "arquivo em work/cache" "$NOME_CACHE"
    linha "sha256 (cache, fonte que o entrypoint usa)" "$SHA_CACHE"
    mostrar_manifesto "$(manifesto_por_sha "$SHA_CACHE")"
else
    echo "  nao encontrei nenhum .war em work/cache dentro do container" >&2
fi

SHA_ROOT="$(docker exec "$CONTAINER_APP" sh -c 'sha256sum /usr/local/tomcat/webapps/ROOT.war 2>/dev/null' | awk '{print $1; exit}')"
if [ -n "$SHA_ROOT" ]; then
    echo
    linha "sha256 (webapps/ROOT.war, o que o Tomcat serve de verdade)" "$SHA_ROOT"
    if [ "$SHA_ROOT" = "$SHA_CACHE" ]; then
        bate "ROOT.war e' identico ao WAR do cache (nenhuma sobreposicao de webapp foi aplicada, ou foi aplicada sem alterar bytes)"
    else
        sem_rastro "ROOT.war DIFERE do WAR do cache -- normal quando APPLY_CUSTOM_WEBAPP_ON_BOOT=true aplicou a sobreposicao de server-source/server/src/main/webapp por cima"
    fi
    mostrar_manifesto "$(manifesto_por_sha "$SHA_ROOT")"
fi

if [ -f "$RAIZ/dist/hmdm.war" ]; then
    SHA_DIST="$(sha256sum "$RAIZ/dist/hmdm.war" | cut -d' ' -f1)"
    echo
    linha "sha256 (dist/hmdm.war local, neste checkout)" "$SHA_DIST"
    if [ "$SHA_DIST" = "$SHA_CACHE" ]; then
        bate "dist/hmdm.war local e' o mesmo que esta montado no container"
    else
        nao_bate "dist/hmdm.war local NAO e' o que esta rodando no container -- foi trocado por outra sessao, ou o container esta desatualizado"
    fi
fi

# ============================================================== LAUNCHER e AGENTE REMOTO
consultar_apk_publicado() {
    local pkg="$1" alvo="$2" prefixo_dist="$3"
    titulo "$(echo "$alvo" | tr 'a-z' 'A-Z') ($pkg) -- o que o banco esta servindo"

    local linha_sql
    linha_sql="$(docker exec "$CONTAINER_PG" psql -U hmdm -d hmdm -tA -F $'\t' -c \
        "SELECT av.version, av.versioncode, coalesce(av.apkhash,''), coalesce(av.url,'') FROM applications a JOIN applicationversions av ON av.id = a.latestversion WHERE a.pkg = '${pkg}';")"

    if [ -z "$linha_sql" ]; then
        echo "  banco nao tem latestversion definido para $pkg" >&2
        return
    fi

    IFS=$'\t' read -r versao versioncode apkhash url <<<"$linha_sql"
    linha "versao (applications.latestversion)" "$versao"
    linha "versioncode" "$versioncode"
    linha "apkhash (base64, como o QR de matricula usa)" "${apkhash:-<vazio>}"
    linha "url" "$url"

    local sha_hex=""
    if [ -n "$apkhash" ]; then
        sha_hex="$(python3 "$VERSAO_PY" apkhash-para-hex "$apkhash")"
    fi
    if [ -n "$sha_hex" ]; then
        linha "apkhash convertido para sha256 hex" "$sha_hex"
        mostrar_manifesto "$(manifesto_por_sha "$sha_hex")"
    else
        sem_rastro "apkhash vazio ou nao decodificavel como sha256 -- nao da' pra cruzar com manifesto nenhum"
    fi

    local candidato="$RAIZ/dist/${prefixo_dist}${versao}.apk"
    if [ -f "$candidato" ]; then
        local sha_local
        sha_local="$(sha256sum "$candidato" | cut -d' ' -f1)"
        echo
        linha "arquivo local esperado" "${candidato#$RAIZ/}"
        if [ -n "$sha_hex" ] && [ "$sha_local" = "$sha_hex" ]; then
            bate "arquivo local bate com o apkhash do banco"
        elif [ -n "$sha_hex" ]; then
            nao_bate "arquivo local NAO bate com o apkhash do banco (sha local=$sha_local)"
        fi
    else
        sem_rastro "nao existe ${candidato#$RAIZ/} neste checkout para conferir"
    fi
}

consultar_apk_publicado "com.hmdm.launcher" "launcher" "hmdm-v"
consultar_apk_publicado "com.hwmdm.remote" "agente-remoto" "hwmdm-remote-"

echo
echo "Leitura somente -- nada foi publicado, reiniciado ou alterado no banco por este script."
