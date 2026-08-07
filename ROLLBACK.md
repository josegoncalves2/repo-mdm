# Rollback do deploy de 04/08/2026

Deploy do plugin `messaging` corrigido (payload de push em JSON valido) sobre a instancia
local. Este documento descreve como voltar atras.

## O que foi trocado

| Alvo | Antes | Depois |
|---|---|---|
| `volumes/work/cache/hmdm-5.39.2-os.war` | md5 `7b6b2802067f8a84e59838fb2f1843d7` | md5 `2de209478d430a21bb7e6b90df8f16af` |
| Unica diferenca entre os dois WARs | `WEB-INF/lib/messaging-0.1.0.jar` | idem |
| Unica diferenca dentro desse jar | `com/hmdm/plugins/messaging/rest/MessagingResource.class` | idem |

Nada mais do WAR mudou: as libs, o changelog do Liquibase e o mapper do MyBatis ficaram
byte a byte identicos. O jar foi compilado de fontes normalizadas em LF justamente para
que `liquibase/messaging.changelog.xml` nao mudasse de checksum e derrubasse o boot.

O painel (HTML/JS/CSS) **nao** vem do WAR: o entrypoint reaplica o overlay de
`/opt/custom-webapp`, que aponta para `/tmp/repo-mdm/server-source/server/src/main/webapp`.

## Ponto de restauracao

    ~/hwmdm-backup/20260804-093647

| Arquivo | Conteudo |
|---|---|
| `cache-war-original.war` | WAR de cache original (identico ao `dist/hmdm.war` versionado) |
| `ROOT.war` | WAR publicado antes da troca, ja com overlay |
| `ROOT-servido-agora/` | copia exata do webapp servido antes da troca |
| `hmdm-db.sql` | `pg_dump --clean --if-exists` completo |
| `work-cache.tar.gz`, `hmdm-config.tar.gz` | cache e config do Tomcat |
| `git-head.txt` | commit `eaef3b0` |
| `imagem.txt` | `headwindmdm/hmdm:0.1.8` |

Ha duas vias independentes de volta: este backup e o `dist/hmdm.war` do proprio repositorio,
que e' byte a byte igual ao WAR de cache original.

## Rollback do servidor

    bash ~/rollback.sh

Ou manualmente:

    docker stop source-hmdm-1
    BK=~/hwmdm-backup/20260804-093647
    cp "$BK/cache-war-original.war" /tmp/repo-mdm/source/volumes/work/cache/hmdm-5.39.2-os.war
    docker run --rm -v /tmp/repo-mdm/source/volumes/webapps:/w alpine \
      sh -c 'rm -rf /w/ROOT /w/ROOT.war'
    docker start source-hmdm-1

O `rm` precisa rodar como root (os arquivos sao do root dentro do container), por isso o
`docker run alpine` em vez de `rm` direto.

## Rollback do banco (so se algo o corromper)

    docker exec -i source-postgresql-1 sh -c \
      'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
      < ~/hwmdm-backup/20260804-093647/hmdm-db.sql

Nao foi necessario neste deploy: o boot rodou o Liquibase sem alterar schema nem checksums.

## Rollback do codigo

    git reset --hard eaef3b0        # commit anterior a este deploy

## Armadilhas deste ambiente

1. **O restart reconstroi o painel a partir de `/tmp/repo-mdm`.** O container monta
   `/tmp/repo-mdm/server-source/server/src/main/webapp` como `/opt/custom-webapp` e, com
   `APPLY_CUSTOM_WEBAPP_ON_BOOT=true`, refaz o `ROOT.war` no boot. Editar o painel apenas
   no clone do Windows e fazer `docker cp` para o container em execucao **nao sobrevive a
   um restart**. Sincronize antes:

       rsync -a --delete \
         /mnt/c/Users/40446686808/projetos/hwmdm-dev/repo-mdm/server-source/server/src/main/webapp/ \
         /tmp/repo-mdm/server-source/server/src/main/webapp/

2. **`FORCE_RECONFIGURE=true` no `.env`** faz o entrypoint copiar o WAR do *cache* por cima
   de `webapps/ROOT.war` a cada boot. Trocar so o `webapps/ROOT.war` e' desfeito no boot
   seguinte; o alvo correto e' `volumes/work/cache/`.

3. **Nunca rodar `mvn clean`** neste projeto: o `maven-clean-plugin` do
   `server-source/server/pom.xml` apaga `src/main/webapp/lib`, onde ficam AngularJS e as
   demais libs do painel.

4. **`mvn package` completo falha em `npm install`** (exit 254) no modulo `server`. Foi por
   isso que o deploy trocou apenas o jar do plugin, em vez de reconstruir o WAR inteiro --
   o que, alem de destravar o deploy, manteve o raio de impacto em uma unica classe.

5. **A arvore de trabalho esta toda em CRLF** (clone feito no Windows) enquanto o
   repositorio guarda LF. Compilar direto dela empacota recursos com bytes diferentes; no
   caso do `messaging.changelog.xml` isso muda o checksum dos changesets do Liquibase e
   pode derrubar o boot. Normalize antes de compilar.
