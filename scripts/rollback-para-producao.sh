#!/usr/bin/env bash
# Volta o servidor ao estado funcional de producao (tag pre-fix-20260805).
#
# Por que isto funciona: desde o commit c59be2a o repositorio versiona
# source/volumes/, entao 'git reset --hard' na tag restaura tambem o ROOT.war
# e as APKs servidas aos devices -- nao so' o codigo.
#
# O banco NAO e' tocado por padrao. A WAR do fix tem os mesmos changelogs
# Liquibase da WAR de producao (verificado byte a byte), ou seja o teste nao
# migra schema e nao ha o que reverter no banco. Se ainda assim quiser
# restaurar os dados, use a flag --com-banco.
#
#   ./scripts/rollback-para-producao.sh              # so' codigo + webapp
#   ./scripts/rollback-para-producao.sh --com-banco  # tambem restaura o dump
set -euo pipefail

TAG=pre-fix-20260805
REPO=/opt/projetos/hwmdm
BK=/opt/projetos/hwmdm-backups/pre-fix-20260805
COM_BANCO=${1:-}

cd "$REPO"

echo "==> 1/5 Parando os containers"
docker compose -f source/docker-compose.yaml down

echo "==> 2/5 Removendo o override de compose (gambiarra de WSL, quebra o datadir aqui)"
rm -f source/docker-compose.override.yaml

echo "==> 3/5 Voltando a arvore para $TAG"
git reset --hard "$TAG"
# reset --hard nao remove arquivos nao rastreados; o override acima ja foi tratado.
git status --short

echo "==> 4/5 Subindo os containers"
docker compose -f source/docker-compose.yaml up -d

if [ "$COM_BANCO" = "--com-banco" ]; then
  echo "==> 5/5 Restaurando o banco a partir de $BK/hmdm-prefix.sql"
  echo "     aguardando o Postgres aceitar conexao..."
  for _ in $(seq 1 30); do
    docker exec source-postgresql-1 pg_isready -U hmdm -d hmdm >/dev/null 2>&1 && break
    sleep 2
  done
  docker exec -i source-postgresql-1 psql -U hmdm -d hmdm < "$BK/hmdm-prefix.sql" >/dev/null
  echo "     banco restaurado"
else
  echo "==> 5/5 Banco preservado (use --com-banco para restaurar o dump)"
fi

echo
echo "==> Conferencia"
docker compose -f source/docker-compose.yaml ps
echo -n "HTTP em localhost:8080 -> "
for _ in $(seq 1 30); do
  code=$(curl -s -o /dev/null -w '%{http_code}' -m 5 http://localhost:8080/ || true)
  [ "$code" = "200" ] && break
  sleep 2
done
echo "$code"
echo "commit atual: $(git rev-parse --short HEAD)  (esperado: $(git rev-parse --short "$TAG"))"
