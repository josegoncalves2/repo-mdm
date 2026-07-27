#!/usr/bin/env bash
# Provisiona o HWMDM numa maquina nova (outro IP, outro DNS).
#
# Idempotente: rodar de novo nao duplica nada. Nao sobrescreve source/.env se ja existir
# (use --force-env para isso).
#
#   ./scripts/provision.sh                       # pergunta os valores
#   ./scripts/provision.sh --dominio mdm.x.com.br --ip 192.168.0.10 --proxy 10.1.1.1
#   ./scripts/provision.sh --check               # so verifica pre-requisitos e sai
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

DOMINIO=""; IP=""; PROXY=""; SENHA=""; SEGREDO=""; EMAIL=""
FORCE_ENV=0; SO_CHECK=0; RESTAURAR_DUMP=1

while [ $# -gt 0 ]; do
    case "$1" in
        --dominio) DOMINIO="$2"; shift 2 ;;
        --ip) IP="$2"; shift 2 ;;
        --proxy) PROXY="$2"; shift 2 ;;
        --senha) SENHA="$2"; shift 2 ;;
        --segredo) SEGREDO="$2"; shift 2 ;;
        --email) EMAIL="$2"; shift 2 ;;
        --force-env) FORCE_ENV=1; shift ;;
        --sem-dump) RESTAURAR_DUMP=0; shift ;;
        --check) SO_CHECK=1; shift ;;
        -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
        *) echo "opcao desconhecida: $1" >&2; exit 2 ;;
    esac
done

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m  ok\033[0m %s\n' "$*"; }
erro() { printf '\033[1;31mERRO\033[0m %s\n' "$*" >&2; }

# ---------------------------------------------------------------- pre-requisitos
info "Conferindo pre-requisitos"
FALTA=0
for cmd in docker git; do
    if command -v "$cmd" >/dev/null 2>&1; then ok "$cmd: $(command -v $cmd)"
    else erro "$cmd nao encontrado"; FALTA=1; fi
done
if docker compose version >/dev/null 2>&1; then ok "docker compose: $(docker compose version --short)"
elif command -v docker-compose >/dev/null 2>&1; then ok "docker-compose: $(docker-compose version --short)"
else erro "docker compose nao encontrado"; FALTA=1; fi
if ! docker info >/dev/null 2>&1; then
    erro "o daemon do Docker nao responde (o usuario esta no grupo 'docker'?)"; FALTA=1
fi
[ "$FALTA" -eq 1 ] && exit 1
[ "$SO_CHECK" -eq 1 ] && { ok "pre-requisitos atendidos"; exit 0; }

DC="docker compose"; docker compose version >/dev/null 2>&1 || DC="docker-compose"

# ---------------------------------------------------------------- .env
if [ -f source/.env ] && [ "$FORCE_ENV" -eq 0 ]; then
    info "source/.env ja existe -- mantido (use --force-env para regerar)"
else
    info "Gerando source/.env"
    [ -n "$DOMINIO" ] || read -rp "  Dominio pelo qual os tablets acessam o servidor: " DOMINIO
    [ -n "$IP" ]      || read -rp "  IP desta maquina na rede dos tablets: " IP
    if [ -z "$PROXY" ]; then
        echo "  IPs do proxy reverso/tunel entre o tablet e este servidor, separados por virgula."
        echo "  Sem isto o painel mostra o IP do proxy no lugar do IP do dispositivo."
        read -rp "  (vazio se nao houver proxy): " PROXY
    fi
    [ -n "$SENHA" ]   || read -rp "  Senha do banco [gerar automaticamente]: " SENHA
    [ -n "$EMAIL" ]   || read -rp "  E-mail do admin: " EMAIL
    [ -n "$SENHA" ]   || SENHA="$(openssl rand -base64 24 | tr -d '/+=' | head -c 24)"

    if [ -z "$SEGREDO" ]; then
        echo "  SHARED_SECRET: ao restaurar um dump existente, use o MESMO valor do ambiente"
        echo "  de origem, senao os dispositivos ja matriculados param de ser reconhecidos."
        read -rp "  SHARED_SECRET [gerar novo]: " SEGREDO
    fi
    [ -n "$SEGREDO" ] || SEGREDO="$(openssl rand -hex 24)"

    sed -e "s|^BASE_DOMAIN=.*|BASE_DOMAIN=${DOMINIO}|" \
        -e "s|^LOCAL_IP=.*|LOCAL_IP=${IP}|" \
        -e "s|^PROXY_ADDRESSES=.*|PROXY_ADDRESSES=${PROXY}|" \
        -e "s|^SQL_PASS=.*|SQL_PASS=${SENHA}|" \
        -e "s|^SHARED_SECRET=.*|SHARED_SECRET=${SEGREDO}|" \
        -e "s|^ADMIN_EMAIL=.*|ADMIN_EMAIL=${EMAIL}|" \
        -e "s|^FORCE_RECONFIGURE=.*|FORCE_RECONFIGURE=true|" \
        source/.env.example > source/.env
    chmod 600 source/.env
    ok "source/.env criado (FORCE_RECONFIGURE=true para a primeira subida)"
fi

# shellcheck disable=SC1091
set -a; . ./source/.env; set +a

# ---------------------------------------------------------------- WAR
info "Conferindo o WAR da aplicacao"
mkdir -p source/volumes/work/cache source/volumes/work/files source/volumes/webapps source/volumes/hmdm-config source/volumes/db
if ls source/volumes/work/cache/*.war >/dev/null 2>&1; then
    ok "WAR presente: $(basename "$(ls -1 source/volumes/work/cache/*.war | head -1)")"
elif [ -f dist/hmdm.war ]; then
    cp dist/hmdm.war source/volumes/work/cache/hmdm-5.39.2-os.war
    ok "WAR copiado de dist/"
else
    erro "Nenhum WAR em source/volumes/work/cache/ nem em dist/hmdm.war."
    erro "O WAR carrega as alteracoes Java deste projeto (BaseIPFilter, endpoint /command)"
    erro "e NAO pode ser substituido pelo WAR oficial do h-mdm.com."
    exit 1
fi

# ---------------------------------------------------------------- APK
info "Publicando o APK do agente"
if ls dist/*.apk >/dev/null 2>&1; then
    for apk in dist/*.apk; do
        cp -n "$apk" source/volumes/work/files/ 2>/dev/null || true
        ok "$(basename "$apk")"
    done
else
    erro "nenhum APK em dist/ -- a matricula de dispositivos nao vai funcionar"
fi

# ---------------------------------------------------------------- subir
info "Subindo os containers"
( cd source && $DC up -d )

info "Aguardando o Postgres aceitar conexao"
for _ in $(seq 1 60); do
    if docker exec source-postgresql-1 pg_isready -U "$SQL_USER" >/dev/null 2>&1; then break; fi
    sleep 2
done
docker exec source-postgresql-1 pg_isready -U "$SQL_USER" >/dev/null 2>&1 \
    && ok "Postgres respondendo" || { erro "Postgres nao subiu"; exit 1; }

# ---------------------------------------------------------------- dump
TABELAS=$(docker exec source-postgresql-1 psql -U "$SQL_USER" -d "$SQL_BASE" -tAc \
          "select count(*) from information_schema.tables where table_schema='public';" 2>/dev/null || echo 0)
if [ "$RESTAURAR_DUMP" -eq 1 ] && [ "${TABELAS:-0}" -lt 5 ] && [ -f source/sql/hmdm-dump.sql.gz ]; then
    info "Banco vazio -- restaurando source/sql/hmdm-dump.sql.gz"
    gunzip -c source/sql/hmdm-dump.sql.gz | \
        docker exec -i source-postgresql-1 psql -U "$SQL_USER" -d "$SQL_BASE" >/dev/null 2>&1
    ok "dump restaurado"

    # As URLs do dump apontam para o dominio/IP de ORIGEM. Sem reescrever, os tablets
    # tentam baixar o APK de um host que nao existe nesta rede.
    info "Reapontando URLs do dump para ${PUBLIC_PROTOCOL}://${BASE_DOMAIN}"
    docker exec -i source-postgresql-1 psql -U "$SQL_USER" -d "$SQL_BASE" <<SQL >/dev/null
UPDATE applicationversions
   SET url = regexp_replace(url, '^https?://[^/]+', '${PUBLIC_PROTOCOL}://${BASE_DOMAIN}')
 WHERE url ~ '^https?://';
UPDATE applications SET icontext = icontext WHERE false;
SQL
    RESTAM=$(docker exec source-postgresql-1 psql -U "$SQL_USER" -d "$SQL_BASE" -tAc \
             "select count(*) from applicationversions where url ~ '^https?://' and url not like '%${BASE_DOMAIN}%';")
    [ "${RESTAM:-0}" -eq 0 ] && ok "todas as URLs reapontadas" \
                             || erro "${RESTAM} URL(s) ainda apontam para outro host -- confira applicationversions"
elif [ "${TABELAS:-0}" -ge 5 ]; then
    ok "banco ja populado (${TABELAS} tabelas) -- dump nao restaurado"
fi

# ---------------------------------------------------------------- esperar Tomcat
info "Aguardando o Tomcat subir (pode levar ~1 min)"
for _ in $(seq 1 90); do
    if docker logs source-hmdm-1 2>&1 | tail -40 | grep -q 'Server startup in'; then break; fi
    sleep 3
done
if docker logs source-hmdm-1 2>&1 | tail -40 | grep -q 'Server startup in'; then
    ok "Tomcat no ar"
else
    erro "Tomcat nao sinalizou startup. Veja: docker logs source-hmdm-1"
    exit 1
fi

# ---------------------------------------------------------------- conferencias
info "Conferencias finais"
ROOTXML=source/volumes/hmdm-config/ROOT.xml
if [ -f "$ROOTXML" ]; then
    grep -q "base.url\" value=\"${PUBLIC_PROTOCOL}://${BASE_DOMAIN}\"" "$ROOTXML" \
        && ok "base.url = ${PUBLIC_PROTOCOL}://${BASE_DOMAIN}" \
        || erro "base.url no ROOT.xml nao bate com o .env (FORCE_RECONFIGURE=true e suba de novo)"
    grep -q "proxy.addresses\" value=\"${PROXY_ADDRESSES}\"" "$ROOTXML" \
        && ok "proxy.addresses = '${PROXY_ADDRESSES}'" \
        || erro "proxy.addresses no ROOT.xml nao bate com o .env"
fi
docker exec source-hmdm-1 sh -c 'ls /usr/local/tomcat/webapps/ROOT/app/components/main/view/kiosk.html' >/dev/null 2>&1 \
    && ok "layout novo aplicado (kiosk.html servido)" \
    || erro "kiosk.html ausente -- o overlay do webapp nao rodou"

echo
info "Pronto. Proximos passos:"
echo "  1. Aponte o DNS/proxy de ${BASE_DOMAIN} para esta maquina (porta 8080)."
echo "  2. Abra ${PUBLIC_PROTOCOL}://${BASE_DOMAIN} e entre com o admin do dump."
echo "  3. Depois da primeira subida, ponha FORCE_RECONFIGURE=false em source/.env."
echo "  4. Confira as pendencias conhecidas em PROVISIONAMENTO.md."
