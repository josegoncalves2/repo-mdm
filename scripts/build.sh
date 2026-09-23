#!/usr/bin/env bash
# Comando unico de build: scripts/build.sh <alvo>
#
# Por que este script existe: havia sete scripts de build soltos na raiz do repo
# (build.py, build_maven.py, build_direct.py, build_with_jdk.py, final_build.py,
# build.bat, build.log) e nenhum deles era "o" caminho oficial -- todos escritos
# para Windows (caminhos C:\Users\...), nenhum funciona neste host Linux, e
# nenhum carimbava versao nenhuma no que produzia. Este script substitui os sete:
# ele e' o unico lugar que compila alguma coisa, sempre carimba a versao (via
# scripts/versao-artefato.py) e sempre deixa um manifesto em scripts/builds/.
# Os scripts antigos foram marcados como obsoletos (aviso no topo de cada um),
# nao apagados -- o responsavel ja foi prejudicado por agente que apaga coisas.
#
# Alvos:
#   console        carimba a versao no console (SEM recompilar o nucleo Java --
#                  ver aviso abaixo) e grava o manifesto contra o dist/hmdm.war atual.
#   plugin <nome>  compila UM plugin isolado (mvn -pl plugins/<nome>, sem -am,
#                  sem clean) -- e' como o Web Filter foi construido. O jar sai em
#                  server-source/plugins/<nome>/target/ e precisa ser adicionado
#                  ao WAR original a mao; este script nao faz isso sozinho.
#   agente-remoto  compila o APK do agente remoto com Gradle (--offline).
#   launcher       compila o APK do launcher com Gradle (--offline).
#   tudo           console + agente-remoto + launcher (nao inclui plugin nem o
#                  alvo perigoso abaixo).
#   nucleo-perigoso recompila server-source/server (o nucleo do WAR). DESLIGADO
#                  por padrao -- leia o aviso quando pedir este alvo.
#
# IMPORTANTE -- por que "console" nao compila Java:
#   O fonte em server-source/server NAO corresponde ao build que esta rodando em
#   producao no DEV (build de 2026-08-11). Uma sessao anterior recompilou o
#   nucleo, publicou o resultado e quebrou o MDM. A regra da casa, desde entao,
#   e' NUNCA recompilar o nucleo; plugins novos entram como jar proprio somado ao
#   WAR original (alvo "plugin"). O alvo "console" deste script respeita essa
#   regra: ele so' escreve um build-info.json em
#   server-source/server/src/main/webapp/ (um asset estatico, igual a qualquer
#   outro arquivo do console) e grava o manifesto. Esse build-info.json so' entra
#   no WAR quando o container aplica a sobreposicao de
#   ../server-source/server/src/main/webapp (ver APPLY_CUSTOM_WEBAPP_ON_BOOT em
#   source/docker-compose.yaml) -- este script NAO mexe em dist/hmdm.war.
#
# IMPORTANTE -- Gradle e' um recurso compartilhado:
#   agente-remoto e launcher usam o MESMO daemon Gradle (~/.gradle). Rodar os dois
#   ao mesmo tempo -- ou ao mesmo tempo que outra sessao/agente -- faz um build
#   esperar o outro ou brigar por memoria. Este script recusa rodar esses dois
#   alvos se ja existir um gradlew rodando (veja checar_gradle_livre abaixo); isso
#   NAO substitui olhar `ps aux | grep -i gradle` antes de rodar, porque a checagem
#   so' pega processos que ja apareceram no `ps` no instante exato da checagem.
#
# Uso:
#   scripts/build.sh console
#   scripts/build.sh plugin webfilter
#   scripts/build.sh agente-remoto
#   scripts/build.sh launcher
#   scripts/build.sh tudo
#   HWMDM_RECOMPILAR_NUCLEO=sim scripts/build.sh nucleo-perigoso
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m  ok\033[0m %s\n' "$*"; }
erro() { printf '\033[1;31mERRO\033[0m %s\n' "$*" >&2; }

VERSAO_PY="$RAIZ/scripts/versao-artefato.py"
MVN="$RAIZ/.maven/bin/mvn"
# O JDK proprio do repo (.jdk21) esta incompleto nesta maquina (falta lib/modules --
# "Failed setting boot class path" ao chamar java diretamente). O JDK 21 do sistema
# funciona e foi o que validou este script; se algum dia .jdk21 for corrigido, troque
# aqui.
JAVA_HOME_BUILD="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"

BUILDS_DIR="$RAIZ/scripts/builds"
mkdir -p "$BUILDS_DIR"

ALVO="${1:-}"
[ -n "$ALVO" ] || { erro "uso: $0 <console|plugin NOME|agente-remoto|launcher|tudo|nucleo-perigoso>"; exit 2; }

# ------------------------------------------------------------- publicacao atomica
# "Nunca deixa artefato pela metade em dist/": compoe no destino final so' depois
# que o arquivo de origem existe por inteiro; nunca copia em cima do nome final.
publicar_em_dist() {
    local origem="$1" destino="$2"
    [ -s "$origem" ] || { erro "origem vazia ou inexistente, nao publico: $origem"; exit 1; }
    local tmp="${destino}.building.$$"
    cp -f "$origem" "$tmp"
    mv -f "$tmp" "$destino"
}

checar_gradle_livre() {
    if pgrep -af 'gradlew' 2>/dev/null | grep -qE 'remote-agent|android-source'; then
        erro "ja existe um gradlew de remote-agent/android-source rodando agora."
        erro "nao vou disputar o daemon com ele -- confira com: pgrep -af gradlew"
        exit 1
    fi
}

# --------------------------------------------------------------------- console
build_console() {
    info "console: carimbando versao (NAO recompila o nucleo -- ver cabecalho deste script)"

    local versao
    versao="$(python3 "$VERSAO_PY" fonte console | python3 -c 'import json,sys; print(json.load(sys.stdin)["versao"] or "")')"
    [ -n "$versao" ] || { erro "scripts/versao-console.txt esta vazio ou ausente -- edite-o antes de continuar"; exit 1; }

    local destino="$RAIZ/server-source/server/src/main/webapp/build-info.json"
    local manifesto
    manifesto="$(python3 "$VERSAO_PY" fonte console)"
    printf '%s\n' "$manifesto" | python3 -c '
import json, sys
d = json.load(sys.stdin)
d = {"versao": d["versao"], "commit": d["commit"], "data_build_utc": d["data_build_utc"], "alvo": "console"}
print(json.dumps(d, indent=2, ensure_ascii=False))
' > "${destino}.building.$$"
    mv -f "${destino}.building.$$" "$destino"
    ok "carimbo escrito em ${destino#$RAIZ/}"
    ok "so' entra no WAR quando o container subir com APPLY_CUSTOM_WEBAPP_ON_BOOT=true"

    local artefato_manifesto=""
    local nota="build-info.json carimbado nos fontes estaticos; NAO foi feita recompilacao Java."
    if [ -f "$RAIZ/dist/hmdm.war" ]; then
        artefato_manifesto="$RAIZ/dist/hmdm.war"
        nota="$nota O sha256 abaixo e' do dist/hmdm.war ATUAL (nao alterado por este comando)."
    else
        nota="$nota dist/hmdm.war nao existe neste checkout -- sha256 nao registrado."
    fi

    local caminho_manifesto
    if [ -n "$artefato_manifesto" ]; then
        caminho_manifesto="$(python3 "$VERSAO_PY" manifesto --alvo console --artefato "$artefato_manifesto" --nota "$nota")"
    else
        caminho_manifesto="$(python3 "$VERSAO_PY" manifesto --alvo console --nota "$nota")"
    fi
    ok "manifesto: $caminho_manifesto"
}

# --------------------------------------------------------------------- plugin
build_plugin() {
    local nome="${1:-}"
    [ -n "$nome" ] || { erro "uso: $0 plugin <nome>  (ex.: webfilter, audit, push, xtra)"; exit 2; }
    [ -d "$RAIZ/server-source/plugins/$nome" ] || { erro "plugin nao existe: server-source/plugins/$nome"; exit 1; }

    info "plugin $nome: compilando isolado (mvn -pl plugins/$nome, sem -am, sem clean)"
    if ! ( cd "$RAIZ/server-source" && JAVA_HOME="$JAVA_HOME_BUILD" "$MVN" -o -q -pl "plugins/$nome" package -DskipTests ); then
        erro "build do plugin $nome falhou -- nada foi publicado"
        exit 1
    fi

    local jar
    jar="$(ls -1t "$RAIZ/server-source/plugins/$nome"/target/*.jar 2>/dev/null | grep -v -- '-sources.jar$' | head -1 || true)"
    [ -n "$jar" ] && [ -f "$jar" ] || { erro "build terminou sem gerar jar em plugins/$nome/target/"; exit 1; }
    ok "jar gerado: ${jar#$RAIZ/}"

    local nota="jar isolado do plugin $nome. NAO foi incorporado a nenhum WAR -- isso e' feito"
    nota="$nota a mao, somando o jar ao WAR original (regra da casa: nunca recompilar o nucleo)."
    local caminho_manifesto
    caminho_manifesto="$(python3 "$VERSAO_PY" manifesto --alvo "plugin-$nome" --artefato "$jar" --nota "$nota")"
    ok "manifesto: $caminho_manifesto"
}

# --------------------------------------------------------------------- agente-remoto
build_agente_remoto() {
    checar_gradle_livre
    info "agente-remoto: compilando com Gradle (--offline)"

    local fonte_json
    fonte_json="$(python3 "$VERSAO_PY" fonte agente-remoto)"
    local versao
    versao="$(printf '%s\n' "$fonte_json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["versao"] or "")')"
    [ -n "$versao" ] || { erro "nao consegui ler versionName de remote-agent/app/build.gradle"; exit 1; }
    ok "versao a construir (ja definida em build.gradle): $versao"

    local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
    if ! (
        cd "$RAIZ/remote-agent" &&
        ANDROID_HOME="$sdk" ANDROID_SDK_ROOT="$sdk" ./gradlew --offline :app:assembleRelease
    ); then
        erro "build do agente-remoto falhou -- nada foi publicado em dist/"
        exit 1
    fi

    local apk="$RAIZ/remote-agent/app/build/outputs/apk/release/app-release.apk"
    [ -f "$apk" ] || { erro "gradle terminou mas o apk esperado nao apareceu: ${apk#$RAIZ/}"; exit 1; }

    local destino="$RAIZ/dist/hwmdm-remote-${versao}.apk"
    publicar_em_dist "$apk" "$destino"
    ok "publicado: ${destino#$RAIZ/}"

    local caminho_manifesto
    caminho_manifesto="$(python3 "$VERSAO_PY" manifesto --alvo agente-remoto --artefato "$destino" \
        --nota "versionName/versionCode ja vinham definidos em remote-agent/app/build.gradle; este comando nao os alterou.")"
    ok "manifesto: $caminho_manifesto"
}

# --------------------------------------------------------------------- launcher
build_launcher() {
    checar_gradle_livre
    info "launcher: compilando com Gradle (--offline, flavor opensource)"

    local fonte_json
    fonte_json="$(python3 "$VERSAO_PY" fonte launcher)"
    local versao
    versao="$(printf '%s\n' "$fonte_json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["versao"] or "")')"
    [ -n "$versao" ] || { erro "nao consegui ler versionName de android-source/app/build.gradle"; exit 1; }
    ok "versao a construir (ja definida em build.gradle): $versao"

    local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
    if ! (
        cd "$RAIZ/android-source" &&
        ANDROID_HOME="$sdk" ANDROID_SDK_ROOT="$sdk" ./gradlew --offline :app:assembleOpensourceRelease
    ); then
        erro "build do launcher falhou -- nada foi publicado em dist/"
        exit 1
    fi

    local apk="$RAIZ/android-source/app/build/outputs/apk/opensource/release/app-opensource-release.apk"
    [ -f "$apk" ] || { erro "gradle terminou mas o apk esperado nao apareceu: ${apk#$RAIZ/}"; exit 1; }

    local destino="$RAIZ/dist/hmdm-v${versao}.apk"
    publicar_em_dist "$apk" "$destino"
    ok "publicado: ${destino#$RAIZ/}"

    local caminho_manifesto
    caminho_manifesto="$(python3 "$VERSAO_PY" manifesto --alvo launcher --artefato "$destino" \
        --nota "versionName/versionCode ja vinham definidos em android-source/app/build.gradle; este comando nao os alterou.")"
    ok "manifesto: $caminho_manifesto"
}

# --------------------------------------------------------------------- nucleo-perigoso
build_nucleo_perigoso() {
    cat >&2 <<'AVISO'

=============================================================================
ATENCAO -- recompilar o nucleo do WAR (server-source/server, com -am) e'
PERIGOSO neste repositorio.

Historico: o fonte em server-source/server NAO corresponde ao build que esta
em producao no DEV (build de 2026-08-11). Uma sessao anterior recompilou o
nucleo, publicou o resultado e quebrou o MDM. A regra da casa desde entao e':
NUNCA recompilar o nucleo; plugins novos entram como jar proprio somado ao
WAR original (veja: scripts/build.sh plugin <nome>).

Em 2026-09-23, durante a propria construcao deste script, este alvo foi
acionado sem essa ressalva: um `mvn -pl server -am clean package` rodou e o
`clean` apagou arquivos VERSIONADOS sob server-source/**/target/ (5
deletados, 82 modificados) -- tiveram que ser restaurados a mao com
`git checkout`. Por isso este alvo aqui embaixo NUNCA usa `clean`.

Se voce tem certeza de que precisa mesmo assim, rode de novo com:
    HWMDM_RECOMPILAR_NUCLEO=sim scripts/build.sh nucleo-perigoso
=============================================================================

AVISO
    if [ "${HWMDM_RECOMPILAR_NUCLEO:-}" != "sim" ]; then
        erro "recusado -- leia o aviso acima. Defina HWMDM_RECOMPILAR_NUCLEO=sim para prosseguir."
        exit 3
    fi

    info "HWMDM_RECOMPILAR_NUCLEO=sim -- prosseguindo sob sua responsabilidade (sem 'clean')"
    if ! ( cd "$RAIZ/server-source" && JAVA_HOME="$JAVA_HOME_BUILD" "$MVN" -o -pl server -am package -DskipTests ); then
        erro "build do nucleo falhou"
        exit 1
    fi
    local war="$RAIZ/server-source/server/target/launcher.war"
    [ -f "$war" ] || { erro "maven terminou mas o war esperado nao apareceu: ${war#$RAIZ/}"; exit 1; }
    ok "war gerado: ${war#$RAIZ/} -- NAO foi copiado para dist/hmdm.war automaticamente."
    erro "publicacao em dist/hmdm.war e' manual e deliberada -- este script nao faz isso sozinho."

    local caminho_manifesto
    caminho_manifesto="$(python3 "$VERSAO_PY" manifesto --alvo nucleo-perigoso --artefato "$war" \
        --nota "build do NUCLEO, alvo perigoso. Confirme com o responsavel antes de copiar para dist/hmdm.war.")"
    ok "manifesto: $caminho_manifesto"
}

# --------------------------------------------------------------------- despacho
case "$ALVO" in
    console)        build_console ;;
    plugin)         build_plugin "${2:-}" ;;
    agente-remoto)  build_agente_remoto ;;
    launcher)       build_launcher ;;
    nucleo-perigoso) build_nucleo_perigoso ;;
    tudo)
        build_console
        build_agente_remoto
        build_launcher
        ;;
    *)
        erro "alvo desconhecido: $ALVO"
        erro "use: console | plugin <nome> | agente-remoto | launcher | tudo | nucleo-perigoso"
        exit 2
        ;;
esac
