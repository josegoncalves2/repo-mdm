#!/bin/bash
# ROLLBACK do deploy de 04/08/2026 (plugin messaging).
#
# Uso:  bash ~/rollback.sh                 -> usa o backup mais recente
#       bash ~/rollback.sh 20260804-093647 -> usa um backup especifico
#
# Restaura o WAR. O banco NAO e' tocado: o deploy nao alterou schema, e reverter o banco
# perderia tudo que foi gravado depois do backup. O comando para isso fica no fim, se
# realmente for preciso.
set -e

STAMP="${1:-$(cat "$HOME/hwmdm-backup/ULTIMO")}"
case "$STAMP" in
  /*) BK="$STAMP" ;;
   *) BK="$HOME/hwmdm-backup/$STAMP" ;;
esac
[ -d "$BK" ] || { echo "backup nao encontrado: $BK"; exit 1; }

VOLUMES=/tmp/repo-mdm/source/volumes
CACHE_NAME=$(cat "$BK/cache-war-nome.txt")

echo "RESTAURANDO DE: $BK"
echo "  WAR de cache alvo: $CACHE_NAME"
echo

echo "=== parando o container ==="
docker stop source-hmdm-1

# Os arquivos sob volumes/ pertencem ao root (criados de dentro do container), por isso
# as escritas passam por um container em vez de sudo.
echo "=== restaurando o WAR de cache ==="
docker run --rm -v "$VOLUMES":/v -v "$BK":/bk alpine \
  sh -c "cp /bk/cache-war-original.war /v/work/cache/$CACHE_NAME && md5sum /v/work/cache/$CACHE_NAME"

echo "=== limpando o webapp publicado (sera refeito do cache no boot) ==="
docker run --rm -v "$VOLUMES/webapps":/w alpine \
  sh -c 'rm -rf /w/ROOT /w/ROOT.war && ls -la /w'

echo "=== subindo ==="
docker start source-hmdm-1

echo "=== aguardando responder ==="
for i in $(seq 1 60); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ 2>/dev/null || echo 000)
  echo "  t+$((i*10))s HTTP=$code"
  if [ "$code" = "200" ]; then echo "  PRONTO"; break; fi
  sleep 10
done

cat <<EOF

ROLLBACK DO WAR CONCLUIDO.

Se tambem precisar reverter o BANCO (so se algo o corrompeu):
  docker exec -i source-postgresql-1 sh -c \\
    'PGPASSWORD="\$POSTGRES_PASSWORD" psql -U "\$POSTGRES_USER" -d "\$POSTGRES_DB"' \\
    < $BK/hmdm-db.sql

Para reverter tambem o codigo:
  git -C /mnt/c/Users/40446686808/projetos/hwmdm-dev/repo-mdm reset --hard $(cat "$BK/git-head.txt")
EOF
