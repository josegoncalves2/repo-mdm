# Tabela de Dispositivos

Executor: trava-executor. Data: 2026-09-25. Escopo: layout da tabela do menu "Dispositivos" (só HTML/CSS).
Não houve deploy, reinício de contêiner, alteração em `dist/`, em JS, em `settings.columnDisplayed*` nem em outras telas. Nada tocado em 192.168.1.75.

## Resumo

- **Antes:** com o menu aberto em 1366–1440 px, a tabela media **2481 px** num quadro de 1035–1126 px. Ações começava em x=2292, sempre fora da tela.
- **Depois:** com as 17 colunas da queixa, a tabela precisa de no mínimo **1015 px**. Cabe sem rolagem horizontal de 1349 a 2560 px de viewport. A coluna **Ações ficou fixa à direita** (sticky) em qualquer largura: com 21 colunas, em 900 px e em 390 px ela continua visível e o resto rola por baixo.

## O QUE FOI FEITO

Caminhos relativos a `server-source/server/src/main/webapp/`.

### 1. `app/components/main/view/devices.html` (583 → 588 linhas)

| Linha | Antes | Depois |
|---|---|---|
| 2 | `class="hwmdm-list-page"` | `class="hwmdm-list-page devices-page"` |
| 234 | `class="table-responsive"` (style inline mantido) | `class="table-responsive devices-table-wrap"` |
| 235 | `<table class='table' ...>` | `<table class='table devices-table' ...>` |
| 236–237 | — | comentário HTML explicando o colgroup |
| 239 | `<col ng-if="hasPermission('edit_devices')" style="width: 30px;">` | `... class="devices-col-check">` |
| 240, 246, 247, 248 | `<col ng-if="...Status / PermissionsStatus / AppInstallStatus / FilesStatus" style="width: 30px;">` | `... class="devices-col-dot">` |
| 241–245, 249–264 | `style="width: 100/110/120/90/80px;"` ou `style="min-width: 80/100px;"` | atributo `style` removido (o `ng-if` ficou igual) |
| 265 | `<col style="width: 60px;">` | `<col>` |
| 275, 305, 310, 315 | `<th ... sortData('STATUS'/'PERMISSIONS'/'INSTALLATIONS'/'FILES') class="pointer">` | `class="pointer devices-th-dot"` |
| 494–495 | — | `<div class="devices-actions">` (e um comentário) aberto dentro de `td.deviceActions` |
| 559 | — | `</div>` fechando a grade antes de `</td>` |

Soma das larguras fixas do `<colgroup>` antigo, só nas colunas da queixa: 30+30+120+100+30+30+30+110+90+80+80+100+120+110+110+60 = **1230 px**. Sozinha, essa soma já passava do quadro (1035–1126 px).

Nenhuma coluna, botão, filtro ou ação saiu. As contagens são iguais antes e depois: th=27, td=30, col=27, button=12, menuitem=11, ng-click=50, ng-if=140, settings.columnDisplayed=78. O hash das 256 expressões Angular também é idêntico.

### 2. `css/hwmdm-ui.css` (1020 → 1146 linhas; só acréscimos, nenhuma linha antiga alterada)

- Linha 32: item "21. Tabela de Dispositivos" acrescentado ao índice do cabeçalho.
- Linhas 1022–1146: seção **21**, com 20 regras. Todas usam uma classe própria (`devices-page`, `devices-table`, `devices-th-dot`, `devices-col-*` ou `devices-actions`). Essas classes só existem em `devices.html` e `hwmdm-ui.css` (conferido com `grep -rlE` em todo o webapp).

| Regra nova | Efeito (antes → depois) |
|---|---|
| `.hwmdm-list-page.devices-page { max-width:none }` | limite de 1500 px → nenhum (só nesta tela) |
| `.hwmdm-workspace:has(.devices-page) { max-width:none }` | casca limitada a 1680 px e centralizada → largura toda, só com Dispositivos aberta. Em 1920 px: casca x=120/w=1680 → x=0/w=1920 |
| `.hwmdm-main .devices-table { font-size:12px }` | 13.5 px → 12 px |
| `... > thead > tr > th` | padding 11px 14px → 8px 5px; fonte 12 px caixa-alta com letter-spacing → 10.5 px/600 sem caixa-alta; `white-space: nowrap` → `normal`; alinhamento vertical embaixo |
| `... > tbody > tr > td` | padding 10px 14px → 8px 5px; `white-space: normal`; line-height 1.35 |
| `.devices-table col.devices-col-check, col.devices-col-dot { width:1px }` | colunas de seleção e de bolinha ficam só com o mínimo que o conteúdo pede |
| `th.devices-th-dot` e `th.devices-th-dot > span:first-child` | rótulo das 4 colunas de bolinha na vertical, em 2 linhas (`writing-mode: vertical-rl` + `rotate(180deg)`, `max-height: 6em`, `min-height: min-content` para não cortar palavra longa, como em de_DE). Largura de 177–185 px → 35 px por coluna |
| `td:has(> img.device-indicator) { text-align:center }` e `.device-indicator { margin-top:0 }` | bolinha centralizada |
| `.devices-table .device-address-secondary` | nota do IP: `nowrap` 12 px → quebra entre palavras, 11 px (`!important` para vencer o `12px !important` do main.css) |
| `th.actions-column, td.deviceActions` (conjunto) | `position: sticky; right: 0; z-index: 2`; `min-width: 190px` → `0`; `width: 1%`; sombra: `inset 1px` na cor `--hwmdm-border` + sombra difusa à esquerda |
| `th.actions-column` | fundo `--hwmdm-surface-subtle`, z-index 3 |
| `td.deviceActions` e `tr:hover > td.deviceActions` | fundo opaco `--hwmdm-surface` / `--hwmdm-surface-hover` |
| `[data-theme="dark"] ...` | sombra mais escura no tema escuro |
| `.devices-table .devices-actions` | grade de 4 × 26 px com gap de 3 px (antes: botões inline dentro de uma célula de 190 px, em 2–3 fileiras irregulares) |
| `td.deviceActions .btn`, `.btn-group` e `.dropdown-toggle .caret` | botões de 30×36 px (o min-height de 36 px vencia o height de 30) → 26×26 px; margem 0; o caret do "…" cabe no botão |

### 3. Versão de asset (para o navegador não usar o arquivo velho do cache)

O mecanismo existe: `scripts/stamp-assets.py` gera um token com o hash do conteúdo. Não rodei o script inteiro: ele reescreveria 69 referências em 5 arquivos, a maioria de trabalho de outros agentes (app.js, remote, kiosk etc.). Usei **a mesma função `token()` do script** só nas duas referências aos arquivos que eu alterei:

- `index.html` linha 127: `css/hwmdm-ui.css?v=hwmdm-1.0.0-dev` → `?v=ha10015c8e2`
- `app/components/main/view/content.html` linha 95: `devices.html?v=h515e11b80b` → `?v=h5be4d56e5b`

`main.css` não foi alterado.

## Cálculo explícito da largura mínima (colunas da queixa: seleção + 15 colunas + Ações = 17)

**Quadro disponível** (1025–1440 px, menu aberto): viewport − 16 − 264 − 16 − 16 − 2 = **viewport − 314**
(padding da casca 16+16, menu 264, gap 16, borda 2; regra `@media (max-width:1440px)` do main.css).
Exemplos: 1366 → 1052; 1366 com barra de rolagem vertical de 17 px → 1035; 1390 → 1076 (1059 com barra); 1440 → 1126.

**Antes**: em cada coluna vale a palavra mais larga do cabeçalho em uma linha só (nowrap) + 28 px de padding.

| Coluna | Antes (px) | Depois (px) |
|---|---|---|
| seleção | 44 | 26 |
| Status | 79 | 25 |
| Conectados | 118 | 71 |
| Número do dispositivo | 192 | 99 |
| Status de permissão | 177 | 35 |
| Status de instalação | 185 | 35 |
| Status dos arquivos | 181 | 35 |
| Configuração | 131 | 78 |
| Versão do lançador | 179 | 54 |
| Nível de bateria | 144 | 46 |
| Modo MDM | 104 | 39 |
| Modo quiosque | 138 | 57 |
| Versão do Android | 165 | 51 |
| Data de inscrição | 156 | 61 |
| Número de série | 143 | 99 |
| Device IP | 156 | 80 |
| Ações | 190 | 123 |
| **Total** | **2481** | **1015** |

- **Condição para caber:** viewport ≥ 1015 + 314 = **1329 px** sem barra vertical, ou **1346 px** com barra de 17 px. Portanto 1366–1440 cabe nos dois casos, com folga de 20 a 111 px.
- **Cenário B** (queixa + Modelo, IMEI, Descrição e Grupo, 21 colunas): mínimo de **1296 px**. De 1366 a 1440 há rolagem horizontal, com Ações fixa e visível. Em 1920 cabe.
- Os números da tabela foram **medidos** no Chromium headless, com o template real e o CSS real (ver abaixo). A soma confere com o total medido.

## COMO VERIFIQUEI (máquina; não é o teste humano)

1. **Balanço de CSS e HTML** (`output/tabela-dispositivos/validar.py`):
   ```
   CSS hwmdm-ui.css: abre=284 fecha=284 profundidade_final=0 minimo=0     (original: 264/264)
   Bloco 21: 20 regras — todas as declaracoes no formato propriedade: valor
   HTML app/components/main/view/devices.html: pilha_final=[] erros=[]
   RESULTADO: OK
   ```
2. **Parser do próprio Chromium** carregando o `hwmdm-ui.css` real: `total_regras=277 regras_devices=20`. Nenhuma regra nova foi descartada, incluindo as duas com `:has()`.
3. **Estrutura intacta** (`contar.py`, antes = depois): `th=27 td=30 col=27 button=12 menuitem=11 ng-click=50 ng-if=140 settings.columnDisplayed=78 glyphicon-=9`.
4. **Asset:** `python3 scripts/stamp-assets.py --check` passou de 69 para 68 tokens defasados. Nenhum dos restantes é `hwmdm-ui.css` ou `devices.html`.
5. **Medição de layout** (`gen.py` + `medir.sh`). A página estática em Chromium headless (chrome-headless-shell do Playwright, sem barras de rolagem) contém:
   - o `devices.html` real, renderizado por um mini-renderizador de `ng-if`/`ng-repeat`/`{{}}`/`localized` com os textos de `pt_PT.js`;
   - todos os CSS na ordem do `index.html`;
   - a casca da `content.html` (menu de 264/300 px);
   - dados dos 4 tablets do DEV (R9XT200AMYY… , "Kiosk Total (6.37.3)", 192.168.1.100/.128/.165/.123).

   Saída completa em `output/tabela-dispositivos/medicao-final.txt`. Recorte:
   ```
   ANTES  1366 | quadro 1052 | tabela 2481 | rolagemH True  | Acoes visivel False x=2292 pos=static | linha 97px
   ANTES  1920 | quadro 1324 | tabela 2481 | rolagemH True  | Acoes visivel False x=2292 | workspace [120, 1680]
   DEPOIS  900 | quadro  870 | tabela 1015 | rolagemH True  | Acoes visivel True  x= 748 pos=sticky | linha 72px
   DEPOIS 1349 | quadro 1035 | tabela 1035 | rolagemH False | Acoes visivel True  x= 913 pos=sticky
   DEPOIS 1366 | quadro 1052 | tabela 1052 | rolagemH False | Acoes visivel True  x= 930 pos=sticky
   DEPOIS 1390 | quadro 1076 | tabela 1076 | rolagemH False | Acoes visivel True  x= 954 pos=sticky
   DEPOIS 1440 | quadro 1126 | tabela 1126 | rolagemH False | Acoes visivel True  x=1004 pos=sticky
   DEPOIS 1920 | quadro 1564 | tabela 1564 | rolagemH False | Acoes visivel True  | workspace [0, 1920]
   DEPOIS B 1366 | tabela 1296 | rolagemH True | Acoes visivel True x=930 pos=sticky
   ```
   Também medido em 390, 768, 1000 e 2560 px, nos temas claro e escuro, com a coluna ordenada (ícone de ordenação num cabeçalho vertical) e com rótulos longos em alemão:
   - em nenhum caso algum botão ficou fora da célula de Ações;
   - com a tabela rolada até o fim, Ações fica no mesmo x (a posição sticky se mantém);
   - o texto vertical ficou contido no cabeçalho em todos os casos;
   - fundo de Ações: claro `rgb(255,255,255)` = `--hwmdm-surface`; escuro `rgb(28,33,40)` = `--hwmdm-surface` do tema escuro.
6. **Capturas** em `output/tabela-dispositivos/`: `antes-1390-claro.png` (reproduz a queixa: vai só até "Status dos arquivos"), `depois-1390-claro.png`, `depois-1390-escuro.png`, `depois-1366-claro.png` e `depois-900-claro-rolagem.png` (Ações fixa com a sombra).

## O QUE NÃO VERIFIQUEI

- **O console real num navegador.** Isso cabe ao teste humano. A medição usa um renderizador próprio, não o Angular. Dados reais diferentes (nome de configuração ou serial mais longo, outro idioma) mudam as larguras.
- **Métrica de fonte no Windows/macOS.** A medição foi feita com o FreeType no Linux. O DirectWrite pode variar alguns px; a folga no pior caso (1366 com barra vertical) é de 20 px.
- **Firefox e Safari.** `:has()` exige Firefox 121+ ou Safari 15.4+; sem suporte, só perdem a centralização das bolinhas e a largura extra acima de 1680 px. Também não testei `min-height: min-content` em texto vertical nem sticky em tabela com `border-collapse`.
- Não cliquei em nada: menu "…" (ele é anexado ao `body`, então o sticky não deve cortá-lo), hover e dicas do intro.js.
- **Quais colunas o usuário tem de fato ligadas.** No banco do DEV (`userrolesettings`, só leitura), os 3 papéis têm apenas 6 colunas ligadas, o que não bate com a queixa. Usei a lista da queixa (cenário A) e um cenário maior (B).

## PENDÊNCIAS

1. **Para chegar à tela do usuário:** o console é o overlay de `server-source/.../webapp` aplicado no boot. É preciso que **quem está publicando reinicie o contêiner** `hwmdm-hmdm-1` (ou copie os 4 arquivos). Eu não reiniciei nem copiei nada.
2. **Cache:**
   - O servidor não envia `Cache-Control`, só ETag e Last-Modified; conferi com `curl -D -` em 127.0.0.1:8080. O navegador faz cache heurístico.
   - Com o token novo, `hwmdm-ui.css` vira uma URL nova.
   - `devices.html` também tem token novo dentro de `content.html`. Mas a própria `content.html` continua referenciada no `app/app.js` com o token manual `?v=hwmdm-20260924-1` (8 ocorrências) e em `webfilter.module.js` com `?v=h1d21823701`. Não mexi no `app.js`: outro agente o alterou hoje às 10:02 e ele está fora do escopo.
   - Na primeira vez, o usuário deve dar **Ctrl+Shift+R**.
   - O definitivo é quem coordena o deploy rodar `python3 scripts/stamp-assets.py`, que reescreve 68 referências em 5 arquivos.
3. **Decisões para o humano validar:**
   - rótulos **verticais** nas 4 colunas de bolinha, sem os quais a tabela não cabe em 1366–1440 (mínimo de 1100 px com rótulos horizontais);
   - cabeçalho sem caixa-alta e com 10.5 px, só nesta tabela;
   - **IP sempre em uma linha**, desviando do "IP podendo quebrar": com quebra livre aparecia "192.168.1.1 / 00", que se lê como outro endereço. O que alargava a coluna era a nota em `nowrap`, e ela agora quebra;
   - botões de Ações em 2 fileiras (4+3);
   - acima de 1680 px o menu lateral se desloca para a esquerda ao entrar em Dispositivos. Se incomodar, basta apagar a regra `.hwmdm-workspace:has(.devices-page)`.
4. **Cenário com 21 colunas** não cabe em 1366–1440. Continua a rolagem horizontal, com Ações fixa e visível.
5. **Inconsistência pré-existente, não corrigida (é lógica Angular):** os `<col>` de Custom1–3 não têm `&& commonSettings.customPropertyNameN` e o último `<col>` não tem `hasDeviceActions()`, ao contrário dos `<th>`. Hoje não afeta nada, porque esses `<col>` não têm largura.

**Para reverter:** apague a seção 21 do `hwmdm-ui.css` (linhas 1022–1146, mais a linha 32 do índice) e desfaça as edições da tabela acima. sha256 dos originais: devices.html `515e11b8…`, hwmdm-ui.css `91f32b39…`, index.html `83e41dbe…`, content.html `8598b7d5…`.

---

# Fuso dos eventos DNS (25/09/2026, 14:56–15:05)

Papel: `trava-executor`. **Nenhum contêiner foi reiniciado.** O `source/volumes/webapps/ROOT.war` não foi trocado. No banco só rodei SELECT. Em `webfilter-dns/` e no contêiner `webfilter-dns` só fiz leitura: o `date` pedido, `docker inspect` e leitura do `Dockerfile`, do `docker-compose.yaml` e do `blocky.yml`. Nada foi apagado. Não rodei nenhum comando git de escrita. Nada foi enviado a 192.168.1.75.

> **Atenção: o relatório do deploy das 14:4x sumiu deste arquivo.** Às 14:57 este arquivo tinha 112 linhas e começava por `# Relatório: compilar e publicar no DEV (192.168.1.65), 25/09/2026`, com a seção do webfilter, os sha256, as cópias `.antes` e os horários dos reinícios. Entre 14:58 e 15:04, outra sessão (a da "Tabela de Dispositivos") reescreveu o arquivo e esse conteúdo foi perdido. `output/` não é versionado no git. Eu não apaguei nem restaurei esse conteúdo. Tentei recuperá-lo do meu transcript para o scratchpad, mas a trava anti-prova-falsa bloqueou a leitura. As referências abaixo a "deploy anterior" apontam para esse relatório perdido.

## Causa confirmada (saídas literais)
- `docker exec webfilter-dns date` → `Fri Sep 25 14:56:50 -03 2026`
- `date` no host → `Fri Sep 25 02:56:50 PM -03 2026`. `date -u` → `Fri Sep 25 05:56:50 PM UTC 2026`.
- O contêiner tem `TZ=America/Sao_Paulo` (`docker inspect`, `.Config.Env`) e o `date` dele mostra -03. **Mesmo assim, o Blocky grava em UTC.** Linha do log `2026-09-25_ALL.log` gerada pelo `dig` das 14:53:54 (verificação do deploy anterior):
  ```
  2026-09-25 17:53:54	192.168.1.65	192.168.1.65	0	BLOCKED (cat-social_media: *.facebook.com)	facebook.com.		NXDOMAIN	BLOCKED	A	9d216633a694
  ```
  A última linha do log no momento do `date` era `2026-09-25 17:56:45`, com o relógio do host em 14:56:50 -03.
- Banco (SELECT, 14:57:32 -03):
  ```
   id |       host       | source |   clientip   |       sourcekey        |   createdat   |         brt         |         utc
    5 | www.facebook.com | dns    | 192.168.1.65 | 2026-09-25_ALL.log:504 | 1790369634000 | 2026-09-25 17:53:54 | 2026-09-25 20:53:54
    4 | facebook.com     | dns    | 192.168.1.65 | 2026-09-25_ALL.log:366 | 1790369634000 | 2026-09-25 17:53:54 | 2026-09-25 20:53:54
  ```
- Importador (linha 100 antes da correção): `.atZone(ZoneId.of("America/Sao_Paulo"))`. Ele lê `17:53:54` como 17:53:54 -03 e grava 3 h adiantado.
- A mesma linha lida como UTC dá `1790358834000` = `2026-09-25 14:53:54 -0300`, o horário real.

## Existe fonte confiável do fuso? Não
- O `queryLog` do Blocky 0.34 (`blocky.yml`: `type: csv`, `target: /app/queries`, `logRetentionDays: 0`) não tem opção de fuso.
- A única variável é `TZ: America/Sao_Paulo`, definida em `webfilter-dns/docker-compose.yaml`. O próprio log a desmente: ela está definida e o Blocky grava em UTC. Além disso, o contêiner do app não enxerga o ambiente do `webfilter-dns`.
- Por isso o fuso ficou fixo em UTC. Não criei variável de ambiente nova.

## Diff exato do importador
Arquivo: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/resolver/DnsEventImporter.java`. Ele não é versionado (`??` no git). O original está em `scratchpad/DnsEventImporter.java.orig`, sha256 `a7328f7a…`.
```diff
@@ -96,8 +96,9 @@
             }
         }
         try {
+            // Blocky writes the query log in UTC without an offset, even with TZ=America/Sao_Paulo in its container.
             event.setCreatedAt(LocalDateTime.parse(fields[0], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
-                    .atZone(ZoneId.of("America/Sao_Paulo")).toInstant().toEpochMilli());
+                    .atZone(ZoneOffset.UTC).toInstant().toEpochMilli());
         } catch (RuntimeException e) { log.warn("Data DNS inválida em {}", key); return; }
```
`java.time.*` já era importado. Nenhum outro arquivo-fonte foi tocado.

## Build
- **Método do deploy anterior.** No transcript da sessão das 14:35 o comando foi `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ../.maven/bin/mvn -o -q -pl plugins/webfilter,plugins/deviceinfo package -DskipTests`. O binário é o Maven do projeto, **`.maven/bin/mvn` = Apache Maven 3.9.9**, e não o `mvn` do sistema (3.8.7), embora o relatório daquele deploy dissesse só `mvn`.
- **1ª tentativa, descartada (14:58:24–14:58:51):** usei o `mvn` do sistema, que é 3.8.7, com rc=0. As classes saíram iguais, mas o MANIFEST mudou (`Archiver-Version: Plexus Archiver`, `Created-By: Apache Maven 3.8.7`, `Built-By: sahw`), porque esse Maven usa o maven-jar-plugin 2.4. Esse jar não foi publicado. Ficou só como `scratchpad/webfilter-0.1.0.jar.mvn387-intermediario`.
- **Build final (15:02:12–15:02:32), rc=0:**
  ```
  touch server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/resolver/DnsEventImporter.java
  cd server-source && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ../.maven/bin/mvn -o -q -pl plugins/webfilter package -DskipTests
  ```
  - Sem `clean` e sem `-am`. O `touch` só atualiza o mtime do arquivo editado, para o compilador ver o fonte como mais novo.
  - `git status --porcelain` saiu igual antes e depois do build.
- **Conferência do jar novo** contra `target/webfilter-0.1.0.jar.antes-20260925-1435` (e41af45f…, o jar em produção até agora):
  - mesma lista, com 34 arquivos;
  - **só `com/hmdm/plugins/webfilter/resolver/DnsEventImporter.class` difere em bytes**, e o MANIFEST ficou igual (`Created-By: Maven JAR Plugin 3.4.1`, `Build-Jdk-Spec: 21`);
  - 28 classes, todas com `major version: 52`.
- `javap -v -cp webfilter-0.1.0.jar com.hmdm.plugins.webfilter.resolver.DnsEventImporter`:
  ```
    major version: 52
    #400 = Fieldref           #401.#402     // java/time/ZoneOffset.UTC:Ljava/time/ZoneOffset;
         364: getstatic     #400                // Field java/time/ZoneOffset.UTC:Ljava/time/ZoneOffset;
  ```
  A string `America/Sao_Paulo` não aparece mais na classe. No `javap -c -p`, a única troca é `ldc_w "America/Sao_Paulo"` + `invokestatic ZoneId.of` → `getstatic ZoneOffset.UTC`. O restante são deslocamentos de índice e offset.

## Colocação (15:03–15:04:07)
- **WAR:** fiz uma cópia de `dist/hmdm.war` no scratchpad e rodei `jar uf hmdm.war.novo -C stage WEB-INF/lib/webfilter-0.1.0.jar` nela.
  - Conferência: 698 → 698 entradas, e só o CRC de `WEB-INF/lib/webfilter-0.1.0.jar` mudou (`e44b03b7` → `90ed5a9a`). MANIFEST igual e `unzip -t` sem erros.
  - Depois copiei a cópia por cima de `dist/hmdm.war` com `cp`, mantendo o mesmo inode, porque o arquivo é bind mount ro em `/usr/local/tomcat/work/cache/hmdm-5.39.2-os.war`.
- **Overlay:** `cp server-source/plugins/webfilter/target/webfilter-0.1.0.jar server-source/server/src/main/webapp/WEB-INF/lib/webfilter-0.1.0.jar`.
- Antes de cada cópia, conferi com `sha256sum -c` que o alvo ainda era o esperado.

### sha256
| Arquivo | sha256 |
|---|---|
| server-source/plugins/webfilter/target/webfilter-0.1.0.jar (novo) | 44342f4f5dfd42a65e493e6ded1ecc7a69a7bf23f1c43927357eecc2d95dfef1 |
| `WEB-INF/lib/webfilter-0.1.0.jar` dentro de dist/hmdm.war | 44342f4f5dfd42a65e493e6ded1ecc7a69a7bf23f1c43927357eecc2d95dfef1 |
| server-source/server/src/main/webapp/WEB-INF/lib/webfilter-0.1.0.jar (overlay) | 44342f4f5dfd42a65e493e6ded1ecc7a69a7bf23f1c43927357eecc2d95dfef1 |
| dist/hmdm.war (novo) | f429811fa4f43965778789b7245fb9bd8be3f9c2ecc8fd20c5ae7bf395d627af |

Vistos de dentro do `hwmdm-hmdm-1` (`docker exec … sha256sum`, só leitura):
- `/usr/local/tomcat/work/cache/hmdm-5.39.2-os.war` = `f429811f…`
- `/opt/custom-webapp/WEB-INF/lib/webfilter-0.1.0.jar` = `44342f4f…`
- O servido `/usr/local/tomcat/webapps/ROOT/WEB-INF/lib/webfilter-0.1.0.jar` continua `e41af45f…` (antigo) até o reinício.
- `source/volumes/webapps/ROOT.war` continua `314096ca…` (14:52:36), sem mudança.

### Cópias `.antes`
- `dist/hmdm.war.antes-20260925-1503`: 692a340ce03cbfa963ad290ff703fb705c371d1cce71e94f903da5b9f5b76ad0, o `dist/hmdm.war` anterior.
- `dist/overlay-webfilter-0.1.0.jar.antes-20260925-1503`: e41af45fb6acb9d4fdd3557ced5b1d5b817f25e100685355c5ae1b1b6b21a7cd, o jar anterior do overlay.
  - Fica em `dist/`, e não ao lado do jar, porque o entrypoint copia **tudo** de `/opt/custom-webapp` para dentro do ROOT.war no boot. É o mesmo critério da cópia das 14:52.
- O jar anterior de `target/` já estava guardado em `target/webfilter-0.1.0.jar.antes-20260925-1435` (e41af45f…).

### Contêineres (StartedAt conferido depois da colocação; RestartCount=0 em todos)
`hwmdm-hmdm-1` 17:52:32Z · `hwmdm-postgresql-1` 17:31:18Z · `webfilter-dns` 17:48:29Z · `hwmdm-review-app-20260925` 12:57:19Z · `hwmdm-review-db-20260925` 12:53:41Z. São os mesmos horários de antes: nenhum reinício.

## Eventos com horário no futuro (SELECT às 15:04:19 -03)
```
 total | no_futuro | dns | menor_futuro  |     maior
     2 |         2 |   2 | 1790369634000 | 1790369634000
```
- **2 eventos** (ids 4 e 5, facebook.com e www.facebook.com), ambos `source=dns` e 3 h adiantados. Não foram alterados.
- Eles deixam de estar "no futuro" às 17:53:54 de hoje, mas continuam com o horário errado para sempre.
- O importador não os corrige no reinício: `dns-events-offsets.json` = `{"2026-09-25_ALL.log":3224}` já passou dessas linhas, e `sourcekey` é UNIQUE.
- **Até o reinício, o app em execução continua com a classe antiga.** Qualquer consulta bloqueada importada antes disso também será gravada com +3 h.

## Como o jar chega ao app no reinício
O `hwmdm-hmdm-1` tem `FORCE_RECONFIGURE=false`, `APPLY_CUSTOM_WEBAPP_ON_BOOT=true` e `HMDM_URL=` vazio. Como o `ROOT.war` já existe, o entrypoint **não** copia o `dist/hmdm.war` no boot. Ele refaz o overlay sobre o `ROOT.war` atual, e é o jar do overlay (`44342f4f…`) que substitui o `WEB-INF/lib/webfilter-0.1.0.jar`. O `dist/hmdm.war` só entra se o `ROOT.war` sumir ou se `FORCE_RECONFIGURE=true`. Por isso os dois lugares agora têm o jar novo.

## O que NÃO foi verificado
- **A correção não está carregada.** Não houve reinício, que é proibido nesta tarefa. Não vi o Tomcat carregar o jar novo, nem o boot sem erro depois disso, nem um evento novo gravado com o horário certo.
- Não fiz nenhuma consulta DNS nova e não abri o painel "Tráfego bloqueado recentemente". Esse é o teste humano, depois do reinício: bloquear um domínio e conferir se aparece com o horário local.
- Não conferi o fuso em que o painel exibe `createdAt` no navegador. Conferi só o valor gravado.
- Não investiguei dentro do `webfilter-dns` por que o Blocky ignora o `TZ`, porque é proibido mexer nele. Se um dia a imagem passar a respeitar o `TZ`, o importador ficará 3 h **atrasado**. A suposição UTC está registrada no comentário do código.

## Pendências
1. `dist/hmdm.war.sha256` ainda registra `692a340c…`, que agora é o `dist/hmdm.war.antes-20260925-1503`. `source/volumes/webapps/ROOT.war.sha256` também registra `692a340c…`. Não atualizei nenhum dos dois porque estão fora do escopo. O valor novo do `dist/hmdm.war` é `f429811f…`.
2. O reinício final de `hwmdm-hmdm-1` fica com quem coordena o deploy. Outro agente alterou `index.html` e `app/components/main/view/content.html` no overlay durante esta tarefa, conforme o `git status`.

## Deploy 14:4x — compilar e publicar no DEV (seção RECONSTITUÍDA)
> O texto original (112 linhas, com comandos e saídas literais) foi sobrescrito às 14:58 por outra sessão e não pôde ser recuperado: o registro forense da trava está vazio e a leitura de transcrições é bloqueada. Esta seção foi reconstituída a partir da resposta final do agente que fez o deploy.

### Artefatos (sha256 no momento do deploy)
- dist/hmdm.war — 692a340ce03cbfa963ad290ff703fb705c371d1cce71e94f903da5b9f5b76ad0 (depois substituído às 15:03 pelo WAR com o fix de fuso, ver seção "Fuso dos eventos DNS")
- dist/hmdm-remote-agent-v1.21.apk — 8cbfa9ab45c188a0c2a8dd9c42db170cbafe6bbbde3f853138b16277b31068c0 (versionCode 22 / 1.21, recompilado)
- dist/hmdm-v1.2.apk — faca407bd8934d57b0669dd3bca24f0001f951d030a38d56c5253297d94a4df6 (versionCode 15381 / 1.2, copiado do build das 10:04)
- webfilter-0.1.0.jar (WAR e overlay) — e41af45fb6acb9d4fdd3557ced5b1d5b817f25e100685355c5ae1b1b6b21a7cd
- deviceinfo-0.1.0.jar — f4a5253d73d7d98be68f934442da52b155ea29b9ad4a3e92509661223c9316d1
- imagem hwmdm/webfilter-dns:0.34.0 nova — ID 2a0370c97e6a
- APKs assinados com a mesma chave dos anteriores (SHA-256 do certificado 44372f14…)

### Compilação
- `--release 8` falhou: `BackupResource.java:262: error: cannot find symbol pb.redirectOutput(ProcessBuilder.Redirect.DISCARD); symbol: variable DISCARD` (API do Java 9+). Código não alterado; compilado com `-source 8 -target 8` (igual ao pom.xml raiz), bytecode 52. JVM do contêiner: Java 11.0.31.

### Cópias guardadas
dist/hmdm.war.antes-20260925-1449; source/volumes/webapps/ROOT.war.antes-20260925-1449 e ROOT.war.antes-20260925-1452; dist/overlay-webfilter-0.1.0.jar.antes-20260925-1452; jar anterior do webfilter e APK das 10:09 com o mesmo sufixo; imagem hwmdm/webfilter-dns:0.34.0-antes-20260925-1439.

### Reinícios
- webfilter-dns recriado 14:48:13–14:48:30, healthy 14:48:42
- hwmdm-hmdm-1 docker restart 14:49:31–14:49:48 (Tomcat 14:50:20)
- hwmdm-hmdm-1 docker restart 14:52:20–14:52:32 (Tomcat 14:53:07)

### Verificado por máquina
Login HTTP 200; sem exceção de inicialização (só o `LongPollingServlet : Empty constructor called!` já existente); changeSet plugin-webfilter-2026-09-25-dns-events aplicado; BackupArchiveService agendado; dig google.com resolve, facebook.com NXDOMAIN; consulta bloqueada gravada em source/volumes/work/plugins/webfilter/queries/2026-09-25_ALL.log e importada para plugin_webfilter_events.

### Observações
- server-source/server/src/main/webapp/WEB-INF/lib/webfilter-0.1.0.jar é copiado pelo overlay por cima do jar do WAR a cada boot; foi atualizado. Restaurá-lo pelo git volta o plugin antigo.
- Às 14:30:58, alguém fora desta sessão recriou hwmdm-hmdm-1 e hwmdm-postgresql-1.
- Em deploys futuros, parar o contêiner antes de trocar o ROOT.war (o Tomcat recarrega sozinho ao detectar a troca).

---

# Reinício final 15:10 (25/09/2026)

Papel: `trava-executor`. Nenhum código-fonte foi editado à mão; os únicos arquivos-fonte alterados foram os 5 que o `scripts/stamp-assets.py` reescreveu (só tokens `?v=`). Não apaguei nada. No banco só rodei SELECT. Não mexi no `hwmdm-postgresql-1`, no `webfilter-dns`, nas instâncias `hwmdm-review-*` nem em `/tmp/hwmdm-fixes-20260925/`. Não rodei git de escrita. Nada foi enviado a 192.168.1.75.

**Resultado: o DEV está no ar.** `docker restart hwmdm-hmdm-1` às **15:10:28**, contêiner de pé às 15:10:43 (StartedAt 18:10:43Z), overlay refeito às 15:10:50 e **Tomcat no ar às 15:11:15** (`Server startup in [20921] milliseconds`).

## 1. Seção do deploy 14:4x
A seção "Deploy 14:4x — compilar e publicar no DEV (seção RECONSTITUÍDA)", logo acima, foi acrescentada ao final do arquivo às 15:08:58, com o texto exato recebido. Conferência: os primeiros 25.608 bytes do arquivo depois da edição têm o mesmo sha256 do arquivo antes (`cb8dc7f9…`). Nada do conteúdo anterior mudou.

## 2. Versão dos assets (`scripts/stamp-assets.py`)
**Análise do script (lido inteiro).**
- Só lê e reescreve `*.html` e `*.js` sob `server-source/server/src/main/webapp/`, exceto `lib/` na raiz.
- Não apaga, não renomeia e não cria arquivos. A única escrita é `fonte.write_text(atualizado)`, e só quando o texto mudou.
- A troca é `REFERENCIA.sub`, que devolve `aspa + caminho + ?v=<hash> + aspa`. Aspas e caminho saem iguais; muda só o sufixo `?…`.
- Riscos avaliados:
  - `read_text`/`write_text` em modo texto converteriam CRLF em LF. Os 5 arquivos afetados têm `crlf=0 cr=0`, não têm BOM e terminam em `\n`. Portanto não há reformatação.
  - O `--check` listou 68 trocas, todas de `?v=…` para `?v=…`. Nenhuma referência sem token ganhou token e não havia `?timestamp=`.
- **Ensaio antes de rodar.** Rodei o script numa cópia do webapp no scratchpad (`--raiz …/dryrun/webapp`) e comparei com o original. Nos 5 arquivos, trocando todo `?v=[A-Za-z0-9_.=%-]*` por `?v=X`, os bytes ficam idênticos:
  ```
  app/components/main/view/content.html: iguais_fora_dos_tokens=True linhas 137->137 bytes 10072->10076 tokens 20->20 alterados=9
  index.html: iguais_fora_dos_tokens=True linhas 134->134 bytes 9459->9385 tokens 104->104 alterados=35
  app/app.js: iguais_fora_dos_tokens=True linhas 683->683 bytes 27454->27356 tokens 31->31 alterados=22
  app/components/header/header.controller.js: iguais_fora_dos_tokens=True linhas 133->133 bytes 4565->4565 tokens 1->1 alterados=1
  app/components/plugins/webfilter/webfilter.module.js: iguais_fora_dos_tokens=True linhas 303->303 bytes 12679->12679 tokens 1->1 alterados=1
  total referencias alteradas: 68
  ```
  Conclusão: o script só altera tokens `?v=`, e por isso foi rodado.

**Cópias `.antes`**, em `output/stamp-assets-antes/`, feitas às 15:09 com `cp -p`. Ficam fora do webapp, para o overlay não levá-las ao WAR.
| Cópia | sha256 (= arquivo antes do script) |
|---|---|
| `output/stamp-assets-antes/index.html.antes-20260925-1509` | ded5f9b50da414a1007e0f57266fdca8b965048f489cc89cd840ba985b89d191 |
| `output/stamp-assets-antes/app/app.js.antes-20260925-1509` | 10f06cbc104d357093ae3ced47193d5356282edca081577f68f011c4e67dd83b |
| `output/stamp-assets-antes/app/components/main/view/content.html.antes-20260925-1509` | cff5760056c0da7137811d724bcf83a484393785f3628a72aea1fcd31b1deb1c |
| `output/stamp-assets-antes/app/components/header/header.controller.js.antes-20260925-1509` | 11606d6fd2c77a83caeca3f9713f3124508f01dfeaf628d93c1d3e6a76bb0ae6 |
| `output/stamp-assets-antes/app/components/plugins/webfilter/webfilter.module.js.antes-20260925-1509` | 635d2e1c0340d1cfee079276f5054d62c5359c7d51a146d33e374752c30e73c5 |

**Execução** (15:10:00): `python3 scripts/stamp-assets.py` terminou com rc=0.
```
Tokens defasados: 68
...
Referencias sem arquivo correspondente (nao tocadas): 2
  index.html: lib/jsencrypt/bin/jsencrypt.min.js
  index.html: lib/angular-intro.js/build/angular-intro.min.js

Arquivos reescritos: 5
  app/components/main/view/content.html
  index.html
  app/app.js
  app/components/header/header.controller.js
  app/components/plugins/webfilter/webfilter.module.js
```
- A saída é idêntica à do ensaio, e os 5 arquivos ficaram byte a byte iguais aos do ensaio (`cmp`).
- **Arquivos e referências alterados:**
  - `index.html`: 35
  - `app/app.js`: 22 (20 de `content.html`, mais `qr.html` e `login.html`)
  - `app/components/main/view/content.html`: 9
  - `app/components/header/header.controller.js`: 1
  - `app/components/plugins/webfilter/webfilter.module.js`: 1
  - **Total: 68 referências em 5 arquivos.**
- `content.html` agora é chamada como `content.html?v=h3bb6f5eba8` nas 20 ocorrências do `app.js` e no `webfilter.module.js`, em vez de `hwmdm-20260924-1`, `hwmdm-20260925` e `h1d21823701`.
- Os tokens de `devices.html` (`h5be4d56e5b`) e `hwmdm-ui.css` (`ha10015c8e2`) já estavam certos e não mudaram.

| Arquivo (webapp) | sha256 depois |
|---|---|
| index.html | 3c217d228169df8491ad4bf3ddcf3f97c5404471cf8b50414d06fe5732b9a731 |
| app/app.js | 18540773f1e63273f5b4201e5733f92cdfa4b00e2ec5e935f4b1ebf509714138 |
| app/components/main/view/content.html | 3bb6f5eba82bd1dad5d796ed112af6315d8e1da1ffab83d948547d9d971c1a15 |
| app/components/header/header.controller.js | 813fe68a73aa4b4f28515a9cbc17166747e476eface159a78e5117b8c2c54431 |
| app/components/plugins/webfilter/webfilter.module.js | cc7a9b6de20b32043f3797d04e30d55d3699e6413f8d05742bc339dad95ac8ee |

**Sobra do script.** O `--check` depois da execução ainda aponta 2 tokens:
```
  index.html: app/app.js
      ?v=h10f06cbc10  ->  ?v=h18540773f1
  index.html: app/components/header/header.controller.js
      ?v=h11606d6fd2  ->  ?v=h813fe68a73
```
- **Causa:** o script processa todos os `*.html` antes dos `*.js`. O `index.html` recebeu o hash do `app.js` e do `header.controller.js` de **antes** de esses dois serem reescritos na mesma execução.
- **Não afeta o cache:** as duas URLs são novas (antes eram `?v=hwmdm-20260924-2` e `?v=h9c1b33eb25`), então o navegador busca o arquivo de novo.
- Não rodei o script uma segunda vez, porque a tarefa pedia uma execução.

## 3. Reinício único
**Antes (15:10:10).** Nada mudou nos últimos 10 minutos fora dos horários desta tarefa:
```
-rw-rw-r-- 1 sahw sahw 44351863 2026-09-25 15:04:07.086037350 -0300 dist/hmdm.war          <- jar do fuso (15:03–15:04:07)
-rw-r--r-- 1 root root 44356763 2026-09-25 14:52:36.516634533 -0300 source/volumes/webapps/ROOT.war   <- deploy 14:52
f429811fa4f43965778789b7245fb9bd8be3f9c2ecc8fd20c5ae7bf395d627af  dist/hmdm.war
314096caf65c3b0bbfad5f71b2ec502c8d3d1bf460d800ece9b213f0ce526454  source/volumes/webapps/ROOT.war
NAMES                       IMAGE                        STATUS                    CREATED AT
webfilter-dns               hwmdm/webfilter-dns:0.34.0   Up 21 minutes (healthy)   2026-09-25 14:48:14 -0300 -03
hwmdm-hmdm-1                headwindmdm/hmdm:0.1.8       Up 17 minutes             2026-09-25 14:30:53 -0300 -03
hwmdm-postgresql-1          postgres:12-alpine           Up 38 minutes             2026-09-25 14:30:53 -0300 -03
hwmdm-review-app-20260925   headwindmdm/hmdm:0.1.8       Up 5 hours                2026-09-25 09:54:43 -0300 -03
hwmdm-review-db-20260925    postgres:12-alpine           Up 5 hours                2026-09-25 09:53:32 -0300 -03
```
- StartedAt antes do reinício, com RestartCount=0 em todos:
  - hmdm-1: 17:52:32Z
  - postgresql-1: 17:31:18Z
  - webfilter-dns: 17:48:29Z
  - review-app: 12:57:19Z
  - review-db: 12:53:41Z
- No overlay, os únicos arquivos alterados desde 14:55 foram:
  - `devices.html` (14:57:18) e `hwmdm-ui.css` (14:57:46), da tarefa da tabela;
  - o jar do webfilter (15:04:07), do fuso;
  - os 5 arquivos do stamp (15:10:00).
- Ambiente do contêiner: `FORCE_RECONFIGURE=false`, `APPLY_CUSTOM_WEBAPP_ON_BOOT=true`, `HMDM_URL=` (vazio).

**Reinício:**
```
inicio: 2026-09-25 15:10:28 -0300
hwmdm-hmdm-1
rc=0 fim_restart: 2026-09-25 15:10:47 -0300
StartedAt=2026-09-25T18:10:43.982182614Z Status=running RestartCount=0
```
- Log do boot: `Usando WAR existente: hmdm-5.39.2-os.war`, `Custom webapp overlay rebuilt from /opt/custom-webapp (stale files removed)`, `[HMDM-INITIALIZER]: Application initialization was successful`.
- `25-Sep-2026 15:11:15.880 INFO [main] org.apache.catalina.startup.Catalina.start Server startup in [20921] milliseconds`.
- O ROOT.war não foi trocado por mim. O entrypoint o reconstruiu a partir dele mesmo mais o overlay: agora está com `15:10:50`, 44358480 bytes, sha256 `e85fc8d0e689a1110cc4dafaafe5b293df6d2b332243c2318400ad513f562b53`.
- Os outros 4 contêineres continuam com o mesmo StartedAt de antes.

## 4. Verificação por máquina
| Verificação | Resultado | Evidência |
|---|---|---|
| `http://192.168.1.65:8080/` responde a tela de login | **sim** | Às 15:11:44: `HTTP/1.1 200`, `Content-Length: 9385`, `<title>Headwind MDM</title>`. O corpo tem o mesmo sha256 do `index.html` do fonte (`3c217d22…`). A rota de login `app/components/main/view/login.html?v=h32e5930c2a` (o token do `app.js`) responde `HTTP 200 2687B`, com `<form name='loginForm' ng-submit='onLogin()'>`, `input type='password'` e o botão `login.submit`. |
| Nenhuma exceção nova no boot | **sim** | Log desde 18:10:40Z: 40.970 linhas, só `[DEBUG]` e `[INFO]` até o startup. Nenhuma linha `WARN`, `SEVERE`, `WARNING`, `Caused by` ou `at …`. A única `[ERROR]` é `2026-09-25 15:11:39 [ERROR] com.hmdm.notification.rest.LongPollingServlet : Empty constructor called!`, que já existia. Os `Exception` que aparecem são só nomes de classe em linhas DEBUG do liquibase e do swagger. O plugin subiu: `ResolverConfigWriter : Web filter resolver configuration written: 2 active profile(s), categories [doh, games, social_media]`. |
| `WEB-INF/lib/webfilter-0.1.0.jar` no ROOT explodido = `44342f4f…` | **sim** | `docker exec hwmdm-hmdm-1 sha256sum /usr/local/tomcat/webapps/ROOT/WEB-INF/lib/webfilter-0.1.0.jar` → `44342f4f5dfd42a65e493e6ded1ecc7a69a7bf23f1c43927357eecc2d95dfef1` |
| `devices.html` = fonte | **sim** | `5be4d56e5bc4edb3fdc2014e1214c46d46839b7861fb84db2635e96999f148be` nos dois |
| `css/hwmdm-ui.css` = fonte | **sim** | `a10015c8e28ef2a95d0c5ca697a38aad02983b235d77165f805b6ee302def7e4` nos dois |
| `index.html` = fonte | **sim** | `3c217d228169df8491ad4bf3ddcf3f97c5404471cf8b50414d06fe5732b9a731` nos dois |
| `app/app.js` = fonte | **sim** | `18540773f1e63273f5b4201e5733f92cdfa4b00e2ec5e935f4b1ebf509714138` nos dois |
| (extra) `content.html` e `header.controller.js` = fonte | **sim** | `3bb6f5eb…` e `813fe68a…` nos dois |
| (extra) `app/components/plugins/webfilter/webfilter.module.js` = fonte | **não** | Fonte `cc7a9b6d…` (com o token novo); servido `635d2e1c…`, que é a versão de antes do stamp. Ver pendência 1. Os outros 3 arquivos de `app/components/plugins/webfilter/` estão iguais. |
| Fuso do evento DNS novo | **sim** | Detalhes logo abaixo. |

**Fuso.** Comandos:
```
date antes: 2026-09-25 15:12:27 -0300 epoch_ms=1790359947088
dig @192.168.1.65 facebook.com  ->  status: NXDOMAIN ... ;; WHEN: Fri Sep 25 15:12:27 -03 2026
```
Linha gravada pelo Blocky, em UTC como antes: `2026-09-25 18:12:27	192.168.1.65	192.168.1.65	0	BLOCKED (cat-social_media: *.facebook.com)	facebook.com.		NXDOMAIN	BLOCKED	A	9d216633a694`.

SELECT às 15:12:36 (`date` = `2026-09-25 15:12:36 -0300 epoch_ms=1790359956621`):
```
 id |       host       | source |   clientip   |        sourcekey        |   createdat   |         brt         |         utc         | now_menos_createdat_ms
----+------------------+--------+--------------+-------------------------+---------------+---------------------+---------------------+------------------------
  6 | facebook.com     | dns    | 192.168.1.65 | 2026-09-25_ALL.log:5298 | 1790359947000 | 2026-09-25 15:12:27 | 2026-09-25 18:12:27 |                   9690
  5 | www.facebook.com | dns    | 192.168.1.65 | 2026-09-25_ALL.log:504  | 1790369634000 | 2026-09-25 17:53:54 | 2026-09-25 20:53:54 |               -9677310
  4 | facebook.com     | dns    | 192.168.1.65 | 2026-09-25_ALL.log:366  | 1790369634000 | 2026-09-25 17:53:54 | 2026-09-25 20:53:54 |               -9677310
```
- O evento novo (id 6) tem `createdat` = 15:12:27 de Brasília.
- Ele fica 88 ms antes do `date` do `dig` e 9,7 s antes do `date` do SELECT, bem menos que 2 min, e **não** 3 h à frente.
- O importador corrigido está carregado.
- `dns-events-offsets.json` passou de `{"2026-09-25_ALL.log":5298}` para `{"2026-09-25_ALL.log":5436}`.

**`dist/hmdm.war.sha256`:**
- valor antigo: `692a340ce03cbfa963ad290ff703fb705c371d1cce71e94f903da5b9f5b76ad0  dist/hmdm.war`;
- valor novo: `f429811fa4f43965778789b7245fb9bd8be3f9c2ecc8fd20c5ae7bf395d627af  dist/hmdm.war`.
- Mesmo formato: uma linha, dois espaços e `\n` no final. `sha256sum -c dist/hmdm.war.sha256` → `dist/hmdm.war: OK`.
- Cópia do arquivo antigo em `scratchpad/hmdm.war.sha256.antes`.

## O que NÃO foi verificado
- **A interface num navegador real.** Esse é o teste humano. Não verifiquei:
  - se o navegador do usuário realmente pede as URLs novas;
  - a tabela de Dispositivos;
  - o painel "Tráfego bloqueado recentemente" com o horário local.
- **Login com usuário e senha.** Só conferi o HTTP 200 da página e do template de login.
- **Os eventos 4 e 5 (+3 h) continuam errados no banco.** Não foram alterados, porque a tarefa só permitia SELECT.

## Pendências
1. **`webfilter.module.js` do overlay não chega ao app.**
   - O entrypoint guarda `app/components/plugins/` do WAR e o põe de volta por cima do overlay. Veja o comentário no `docker-entrypoint.sh`: "app/components/plugins/ is assembled by Maven … NOT part of the mounted sources".
   - Por isso o servido continua com `content.html?v=h1d21823701`. A troca desse token feita pelo stamp só chega ao app se o plugin for empacotado no WAR, o que está fora do escopo.
   - Impacto: só a rota do plugin Web Filter pede `content.html` com o token antigo e pode receber do cache uma `content.html` velha. As 20 rotas do `app.js` já usam `h3bb6f5eba8`.
2. **2 tokens do `index.html` ficaram com o hash anterior** (`app.js` e `header.controller.js`), pela ordem de processamento do script (seção 2). Funciona para o cache. Uma segunda execução de `python3 scripts/stamp-assets.py` os alinha, mas exige um novo reinício para chegar ao app.
3. `source/volumes/webapps/ROOT.war.sha256` ainda registra `692a340c…`. O `ROOT.war` agora tem `e85fc8d0…`, reconstruído no boot. Não atualizei o arquivo porque ele não estava no pedido.
4. Na primeira abertura depois deste reinício, recomendo **Ctrl+Shift+R**. O servidor continua sem `Cache-Control`, e o `index.html` não tem token. Ele depende de ETag e Last-Modified (`ETag: W/"9385-1790359800000"`, `Last-Modified: Fri, 25 Sep 2026 18:10:00 GMT`) e está sujeito ao cache heurístico do navegador.

---

## Restauração do acesso remoto (15:2x)

**Pedido:** devolver `remote.controller.js` e `remoteSupport.service.js` ao conteúdo do HEAD `0f18200d` ("fim do dia 24/09/26"), preservar a versão de hoje e reiniciar só o app. Nenhum outro arquivo-fonte foi alterado. O agente do tablet não foi tocado.

Papel: `./trava papel trava-executor` → `papel registrado: trava-executor` (15:19:55).

### 1. Versão de hoje preservada (sem apagar)
Destino: `output/remoto-versao-25-09-codex/`. Nada foi colocado dentro de `webapp/`.
```
$ cp -p <controller> <service> output/remoto-versao-25-09-codex/
$ sha256sum <fonte> <cópia>
23fae5dc98d5dd975dd68047cf8e7e3e823fa3bfe7801d7e5a6856f60395e6ca  server-source/.../controller/remote.controller.js
0fbbb7e4ca7abae39241f5e00fd90d82564ac54b9d192bc83eec9118eef83441  server-source/.../service/remoteSupport.service.js
23fae5dc98d5dd975dd68047cf8e7e3e823fa3bfe7801d7e5a6856f60395e6ca  output/remoto-versao-25-09-codex/remote.controller.js
0fbbb7e4ca7abae39241f5e00fd90d82564ac54b9d192bc83eec9118eef83441  output/remoto-versao-25-09-codex/remoteSupport.service.js
```
Tamanhos: 56512 e 34978 bytes, mtime original 2026-09-25 09:50:08 (preservado com `cp -p`).

A diferença de hoje em relação ao HEAD (`git diff --stat`, antes da restauração):
```
 .../main/controller/remote.controller.js           | 37 ++++++++++++++++------
 .../main/service/remoteSupport.service.js          | 17 ++++++----
 2 files changed, 38 insertions(+), 16 deletions(-)
```
Resumo do que a versão de hoje mudou (só para referência; nada disso foi mexido):
- **controller:** `viewGeneration` e `destroyed`; `closePlayer()` incrementa a geração; `openPlayer` reagenda a si mesmo a cada 50 ms enquanto não houver canvas; o `start` deixou de parar a sessão quando o operador troca de aparelho.
- **service:** `socket.onclose` passou a cair para HTTP em vez de fechar; `close()` zera `handlers`; guarda em `startHttp`; ajuste de `nalEnd`.

### 2. Restauração com o conteúdo exato do HEAD
```
$ git show HEAD:server-source/server/src/main/webapp/app/components/main/controller/remote.controller.js > (mesmo caminho)
$ git show HEAD:server-source/server/src/main/webapp/app/components/main/service/remoteSupport.service.js > (mesmo caminho)
$ git diff --stat -- <os dois caminhos>
(saída vazia)
$ sha256sum <arquivos>            ; git show HEAD:<arquivo> | sha256sum
a017dcffbf1e2a104e15cf057280de551ecacae2f6171ca3671a57219c139703  remote.controller.js     = HEAD a017dcffbf1e...
658beb74c3f89d36b59ae5efcfe84c2ca76e64aff88ea091b20e5c03a10da110  remoteSupport.service.js = HEAD 658beb74c3f8...
```
Não usei `git checkout/restore/stash/reset`.

### 3. Tokens de cache (`stamp-assets.py` duas vezes)
Backup feito antes em `output/stamp-assets-antes-2/`:
- `index.html`, sha `3c217d22…`;
- `app/app.js`, sha `18540773…`.

O `--check` prévio indicou que só `index.html` seria reescrito. Os dois JS restaurados não contêm referências de asset (`grep -c` = 0), então o script não os altera.

A causa dos 2 tokens extras defasados: `index.html`, `app.js` e `header.controller.js` têm mtime 15:10:00.393, .401 e .402. A passada única das 15:09/15:10 carimbou `index.html` antes de reescrever os JS.
```
=== passada 1 (15:20:52)
Tokens defasados: 4
  index.html: app/app.js
      ?v=h10f06cbc10  ->  ?v=h18540773f1
  index.html: app/components/header/header.controller.js
      ?v=h11606d6fd2  ->  ?v=h813fe68a73
  index.html: app/components/main/controller/remote.controller.js
      ?v=h23fae5dc98  ->  ?v=ha017dcffbf
  index.html: app/components/main/service/remoteSupport.service.js
      ?v=h0fbbb7e4ca  ->  ?v=h658beb74c3
Referencias sem arquivo correspondente (nao tocadas): 2
  index.html: lib/jsencrypt/bin/jsencrypt.min.js
  index.html: lib/angular-intro.js/build/angular-intro.min.js
Arquivos reescritos: 1
  index.html
=== passada 2 (15:20:52)
Todos os tokens ja batem com o conteudo dos arquivos.
Arquivos reescritos: 0
=== --check
--check: nada foi escrito. Arquivos que mudariam: 0
```
| referência em `index.html` | antes | depois |
|---|---|---|
| `remote.controller.js` | `h23fae5dc98` | `ha017dcffbf` |
| `remoteSupport.service.js` | `h0fbbb7e4ca` | `h658beb74c3` |
| `app/app.js` | `h10f06cbc10` | `h18540773f1` |
| `header.controller.js` | `h11606d6fd2` | `h813fe68a73` |

`diff output/stamp-assets-antes-2/index.html index.html` mostra só as linhas 55, 58, 64 e 93 (os 4 tokens). O `app.js` não foi reescrito.

Isso também resolve a **pendência 2** da seção anterior (tokens de `app.js` e `header.controller.js`).

### 4. Checagem de concorrência (15:20:59)
```
$ docker ps
9d216633a694 hwmdm/webfilter-dns:0.34.0 ... Up 32 minutes (healthy)  webfilter-dns
d7f5e782a40d headwindmdm/hmdm:0.1.8     ... Up 10 minutes             hwmdm-hmdm-1
a34c46516f1e postgres:12-alpine         ... Up 49 minutes             hwmdm-postgresql-1
a1b661d01e6b headwindmdm/hmdm:0.1.8     ... Up 5 hours                hwmdm-review-app-20260925
2e36a004461e postgres:12-alpine         ... Up 5 hours                hwmdm-review-db-20260925
StartedAt: webfilter-dns 17:48:29Z (14:48:29) | hwmdm-hmdm-1 18:10:43Z (15:10:43)
$ ls -la --time-style=full-iso dist/hmdm.war source/volumes/webapps/ROOT.war
-rw-rw-r-- 1 sahw sahw 44351863 2026-09-25 15:04:07.086037350 -0300 dist/hmdm.war
-rw-r--r-- 1 root root 44358480 2026-09-25 15:10:50.117975178 -0300 source/volumes/webapps/ROOT.war
```
Os reinícios batem com os esperados (15:10 app, 14:48 dns). Não há reinício mais recente, então segui.

### 5. Reinício só do app
```
inicio restart: 2026-09-25 15:21:10
$ docker restart hwmdm-hmdm-1
restart retornou: 2026-09-25 15:21:27   (StartedAt=2026-09-25T18:21:25.928Z)
2026-09-25T18:21:30Z Custom webapp overlay rebuilt from /opt/custom-webapp (stale files removed)
25-Sep-2026 15:22:09.943 INFO [main] org.apache.catalina.startup.Catalina.start Server startup in [34311] milliseconds
```
- **Tomcat no ar às 15:22:09.**
- Não troquei o `ROOT.war`: o entrypoint o reconstruiu do overlay às 15:21:30, como em todo boot.
- Não usei `FORCE_RECONFIGURE`.
- Não toquei no postgres, no webfilter-dns nem nos contêineres `review-*`.

### 6. Verificação por máquina (15:22:43)
```
$ curl -s -o /dev/null -w 'HTTP %{http_code}\n' http://192.168.1.65:8080/
HTTP 200
--- sha256 HEAD (git show)
a017dcffbf1e2a104e15cf057280de551ecacae2f6171ca3671a57219c139703  HEAD:remote.controller.js
658beb74c3f89d36b59ae5efcfe84c2ca76e64aff88ea091b20e5c03a10da110  HEAD:remoteSupport.service.js
--- docker exec hwmdm-hmdm-1 sha256sum (ROOT explodido)
a017dcffbf1e2a104e15cf057280de551ecacae2f6171ca3671a57219c139703  app/components/main/controller/remote.controller.js
658beb74c3f89d36b59ae5efcfe84c2ca76e64aff88ea091b20e5c03a10da110  app/components/main/service/remoteSupport.service.js
--- servido por HTTP (curl | sha256sum)
a017dcffbf1e2a104e15cf057280de551ecacae2f6171ca3671a57219c139703  -
658beb74c3f89d36b59ae5efcfe84c2ca76e64aff88ea091b20e5c03a10da110  -
--- index.html servido
55:    <script src='app/app.js?v=h18540773f1'></script>
58:    <script src='app/components/header/header.controller.js?v=h813fe68a73'></script>
64:    <script src='app/components/main/controller/remote.controller.js?v=ha017dcffbf'></script>
93:    <script src='app/components/main/service/remoteSupport.service.js?v=h658beb74c3'></script>
--- cabeçalhos: ETag: W/"9385-1790360452000"  Last-Modified: Fri, 25 Sep 2026 18:20:52 GMT
```
**Log do boot desde 18:21:25Z** (linhas `[ERROR]`, `[WARN]`, `SEVERE`, `Exception:`, `Caused by` e stack; excluí os DEBUG de carga de classe do liquibase e do swagger, cujo texto contém "Exception"):
```
2026-09-25T18:22:15.570977Z http-nio-8080-exec-10 ERROR Log4j API could not find a logging provider.
```
- **Não é nova.** Ela também aparece nos boots anteriores de hoje: 17:32:33Z, 17:55:55Z e 18:16:20Z.
- Não houve nenhuma exceção com stack.
- `LongPollingServlet : Empty constructor called!` ainda não tinha aparecido até 15:23:09. Ele surge no primeiro long-poll de um aparelho; no boot anterior veio 24 s depois do startup.

### Observação (não mexida)
No log do boot das 15:10, o aparelho 46 (R9XT200AMYY) recebeu `remoteScreenStart`, `remoteScreenStop` e `remoteScreenStart` num mesmo push às 15:16:50. O agente respondeu `Encoder indisponivel em 752x1280: java.lang.SecurityException: Cannot create VirtualDisplay with non-current MediaProjection` / `Sessao falhou: nenhum encoder H.264 utilizavel`.

Isso é compatível com o sintoma "dezenas de cliques" na versão de hoje: o start/stop em sequência invalida o MediaProjection. Não investiguei além disso.

### O que NÃO foi verificado
- **O uso real no navegador.** É o teste humano:
  - a sessão abre no primeiro clique;
  - o teclado físico digita;
  - o Backspace apaga, sem duplicar.
- Recomendo Ctrl+Shift+R na primeira abertura, porque o `index.html` depende de ETag/Last-Modified.

## Botões Módulos/Integrações (16:00–16:08, 25/09/2026)

### Defeito
Nas telas Módulos e Integrações, "Configurar" e "Copiar" apareciam com uma letra por linha.

A renderização com o CSS de antes reproduz o defeito: 6 linhas de texto por botão, e 7 em "Copiado".

A medição mostrou uma causa um pouco diferente da descrita no pedido:
- A célula **não** encolhia até um caractere. A célula de ações dos Módulos media de 170 a 316 px, e o Chromium respeita o `min-width: 170px` nela. A última célula das Integrações media de 94 a 121 px.
- O que prendia o texto era o **botão**. `hwmdm-ui.css:473-480` fixa `.table .btn { width: 30px; height: 30px; padding: 0 }`, o quadrado do botão só-ícone. Com `white-space: normal; overflow-wrap: anywhere` da regra global `.btn`, o texto quebra dentro de uns 12 px úteis. Por isso sai uma letra por linha, vazando para cima e para baixo do botão.

### Alteração (só `css/hwmdm-modules.css`, só acréscimo, regras restritas a `.ext-hub`)
- Antes: 27 linhas. Depois: 36 linhas.
- As linhas 1–27 estão intactas.
- Acrescentei as linhas 28–36: um comentário (28–33) e três regras:
```
+.ext-hub .table .btn { width: auto; white-space: nowrap; overflow-wrap: normal; word-break: normal; max-width: none; }
+.ext-hub .table td.ext-hub-row-actions { width: 1%; white-space: nowrap; }
+.ext-hub .table td:last-child:has(> .btn) { width: 1%; white-space: nowrap; }
```

**Desvio da lista literal: `width: auto`.**
- Sem ele, o texto fica numa linha, mas o botão continua com 30 px. "Configurar" vaza por baixo do interruptor, nas 6 linhas com botão.
- Evidência em `output/botoes-ext-hub/modulos-1366-sem-width-auto-tabela.png` e em `medidas-sem-width-auto.json`: `textoVazaBotao=6` e `textoSobreInterr=6`.
- A regra continua restrita a `.ext-hub .table`. Não alterei `.btn` nem `.table .btn` de `hwmdm-ui.css`.

**A última célula das Integrações** não tem classe, e não mexi no HTML. Por isso o seletor é `td:last-child:has(> .btn)`.
- Assim a coluna "Estado" da tabela "Extensões instaladas", que também é a última célula, não é afetada.
- `.ext-hub` só existe em `extensionsHub.html` e `integrations.html`, conferido com grep.
- O painel já usa `:has()` em `hwmdm-ui.css`.

Nenhuma regra, botão, coluna ou texto foi removido.

### Publicação
- **Cópias de antes** em `output/botoes-ext-hub-antes/`:
  - `css/hwmdm-modules.css`: sha256 cddf9294…, 27 linhas.
  - `index.html`: sha256 9cc80bb7….
- **`stamp-assets.py`, 1ª execução (16:06:10):** 1 token defasado. Reescreveu só `index.html`, linha 124:
  - antes: `css/hwmdm-modules.css?v=hcddf9294b3`
  - depois: `css/hwmdm-modules.css?v=hdebc1f4a64`
- **2ª execução:** "Todos os tokens ja batem", 0 arquivos reescritos.
- **`docker ps` (16:06:15):**
  - `hwmdm-hmdm-1` StartedAt 18:21:25Z (15:21:25). É o esperado, sem reinício mais recente.
  - postgres 17:31:18Z, webfilter-dns 17:48:29Z.
  - Havia um processo `codex` (extensão do VS Code) vivo desde 14:23. Não interagi com ele.
- **`docker restart hwmdm-hmdm-1`:**
  - início 16:06:22, retorno 16:06:36 (StartedAt 19:06:34Z);
  - overlay do webapp reconstruído pelo entrypoint às 16:06:47;
  - **Tomcat no ar às 16:07:22** ("Server startup in [19669] milliseconds").
- Não troquei o ROOT.war. O entrypoint o regravou às 16:06:46, como em todo boot.
- Não toquei no postgres nem no webfilter-dns, cujos StartedAt não mudaram.

### Verificação por máquina (16:07:37–16:08:21)
- `http://192.168.1.65:8080/` → **HTTP 200**, conferido duas vezes.
- **Boot desde 19:06:30Z:** nenhuma exceção nova.
  - Única linha de erro: `[ERROR] LongPollingServlet : Empty constructor called!` às 16:07:32. Ela aparece em todos os boots de hoje (17:32, 17:50, 17:53, 18:11 e 18:24Z).
  - As 7 linhas com "Exception" são DEBUG de carga de classe do liquibase e do swagger.
  - Não houve nenhum stack.
- **`css/hwmdm-modules.css` servido = disco:**
  - sha256 debc1f4a… nos três lugares: disco, curl e ROOT explodido no contêiner;
  - `cmp`: idêntico;
  - HTTP 200, 2720 bytes.
- **`index.html` servido:**
  - linha 124 = `css/hwmdm-modules.css?v=hdebc1f4a64`;
  - sha256 a8b20fbd…, igual ao do disco.

### Renderização headless
Usei o Chromium 149 do Playwright, com o HTML e o CSS reais lidos do disco, sem login e sem tocar no servidor.
- A página de teste foi montada fora do repositório, no scratchpad. O script ficou copiado em `output/botoes-ext-hub/render.js`.
- Os templates reais (`extensionsHub.html` e `integrations.html`) foram compilados por AngularJS local, com controllers de mentira.
- Catálogo real de módulos (25) e pontos de integração reais (5).
- Uma linha foi posta em manutenção para forçar a coluna Estado.
- As folhas de estilo seguem a ordem do `index.html`.

| variante | tela/largura | linhas por botão | largura do botão | texto vaza do botão | texto sobre o interruptor |
|---|---|---|---|---|---|
| antes | Módulos 1366/1920 | 6 | 30 px | 6 de 6 | 0 |
| antes | Integrações 1366/1920 | 6 (Copiado: 7) | 30 px | 5 de 5 | – |
| só a lista literal | Módulos 1366/1920 | 1 | 30 px | 6 de 6 | **6 de 6** |
| só a lista literal | Integrações 1366/1920 | 1 | 30 px | 5 de 5 | – |
| **depois** | Módulos 1366/1920 | **1** | 81 px | **0** | **0** |
| **depois** | Integrações 1366/1920 | **1** (Copiado: 1) | 73/82 px | **0** | – |

No "depois":
- botão e interruptor lado a lado (mesma linha, interruptor à direita, sem interseção);
- botão dentro da célula;
- 0 rolagem horizontal na página e nas tabelas;
- 0 erro de página.

Capturas em `output/botoes-ext-hub/`:
- `{modulos,integracoes}-{1366,1920}-{antes,depois}.png` (página inteira);
- `...-tabela.png` (recorte da tabela);
- `medidas-*.json`.

### O que NÃO foi verificado
- **A tela real logada no navegador.** É o teste humano: Configurações → Módulos e Integrações.
- Recomendo Ctrl+Shift+R na primeira abertura.
- Os dados da renderização são fixos. Na tela real, quais linhas mostram "Configurar" depende dos plugins instalados e das permissões.

---

# Listas DNS, backup e F5 (25/09/2026, 16:35–17:02)

Papel: `trava-executor` (16:35:16). Não criei, alterei nem atribuí papel ou usuário em nenhum banco. No banco do DEV só rodei SELECT; a coluna nova entrou pelo Liquibase no boot. Não toquei no `hwmdm-postgresql-1`, no `webfilter-dns` nem em 192.168.1.75. Não rodei git de escrita.

**Resultado: o DEV está no ar com as três correções.**
- Parada: `docker stop hwmdm-hmdm-1` das 17:00:03 às 17:00:19.
- Subida: `docker start` às 17:00:45 (StartedAt 20:00:46Z), overlay refeito às 17:00:51 e **Tomcat no ar às 17:01:22**.

Antes disso, as correções passaram na cópia isolada com o usuário `review_20260925`, de papel **Admin** (userroleid 2), que **não** é super admin naquele banco.
- 1ª rodada: 18 verificações, 18 SIM.
- 2ª rodada: 24 verificações, 24 SIM.
- Nenhum erro de console nas duas rodadas.

## 1. Web Filter › Configuração: gestão das listas de DNS

**O que mudou**
- **Tela.** O bloco `<div class="kiosk-panel" ng-repeat="category in catalog.categories">`, com as URLs travadas, foi trocado por "Listas de DNS". Só esse bloco mudou; o resto da aba ficou igual: Domínio DNS do filtro, a dica e "Fontes das listas de sites".
  - Uma faixa recolhível por categoria mostra nome, "N de M ativas" e "não salvo" quando há alteração pendente.
  - Aberta, a categoria mostra uma tabela compacta. Em cada linha: checkbox **Ativa**, **URL** editável e botão **Remover**.
  - Embaixo da tabela ficam **Adicionar lista** e **Salvar listas**. Salvar só habilita quando há alteração. A mensagem de sucesso ou de erro aparece ao lado do botão.
  - Uma URL que não começa com `https://` fica com a borda vermelha e não é enviada.
  - Um recarregamento do catálogo (por exemplo, depois de categorizar um app) mantém o que estava aberto e as edições ainda não salvas.
- **Duas cópias da tela** (`plugins/webfilter/src/main/webapp` e `server/src/main/webapp/app/components/plugins/webfilter`): `cmp` confirma que `views/main.html` e `webfilter.module.js` estão idênticos.
  - Para isso, o token `content.html?v=` da cópia do plugin passou de `h1d21823701` para `h3bb6f5eba8`, o mesmo da cópia do overlay.
- **Persistência.** Criei o changeSet NOVO `plugin-webfilter-2026-09-25-sources-inactive`, que roda `ALTER TABLE plugin_webfilter_sources ADD COLUMN inactiveUrls TEXT NOT NULL DEFAULT ''`.
  - Nenhum changeSet existente foi alterado e nada foi apagado.
  - A lista desativada continua em `urls`, cadastrada e visível. `inactiveUrls` guarda, uma por linha, as que ficam fora do filtro.
- **Quem gera o resolvedor ignora as inativas.** `WebFilterCatalog.getSiteSources()` agora devolve só as ativas. `ResolverConfigWriter.sourcesJson()` já usava esse método, então o `sources.json` sai sem as listas desativadas.
- **Permissão.** O `PUT /sources/{category}` usa só `denied()`, que é `!hasPermission("plugin_webfilter_access")`: a mesma verificação do `PUT /settings`, que salva o domínio DNS.
  - `canManageSources` passou a ser `!denied()`.
  - Não sobrou nenhum `isSuperAdmin()` no plugin: `grep` → 0.
- **Ao salvar:**
  1. grava no banco;
  2. roda `sourceWriter.writeAll()`, o mesmo `ResolverConfigWriter.writeAll()` que salvar uma política chama via `applyChanges`;
  3. devolve a categoria como ficou salva.

  O `resolver.py` do `webfilter-dns` lê o `sources.json` a cada 5 s. Quando a impressão das URLs de uma categoria muda, ele baixa a lista e pede `POST /api/lists/refresh` ao Blocky. Não mexi nele.

**Build do plugin**
- Comando: `cd server-source && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ../.maven/bin/mvn -o -q -pl plugins/webfilter package -DskipTests`, das 16:44:41 às 16:46:01, rc=0.
- Resultado, contra o jar anterior (`44342f4f…`):
  - mudaram só `WebFilterCatalog`, `WebFilterMapper`, `WebFilterResource` e `liquibase/webfilter.changelog.xml`;
  - entrou uma classe nova, `rest/json/SourceView.class`;
  - todas as classes têm `major version: 52`;
  - o MANIFEST é o mesmo (`Maven JAR Plugin 3.4.1`).
- Jar novo: `dc38668a0b5e7b3c0709894e1c3730335554d014b869b34d187db6dc881b0ca2`.

**Diffs do backend (webfilter)**
```diff
--- a/server-source/plugins/webfilter/src/main/resources/liquibase/webfilter.changelog.xml
+++ b/server-source/plugins/webfilter/src/main/resources/liquibase/webfilter.changelog.xml
@@ -158,4 +158,13 @@
             CREATE TABLE plugin_webfilter_sources (category VARCHAR(50) PRIMARY KEY, urls TEXT NOT NULL);
         </sql>
     </changeSet>
+
+    <!-- Listas desativadas continuam em "urls" (cadastradas e visiveis na tela); esta coluna guarda,
+         uma por linha, as que ficam fora do filtro. Vazia = todas ativas. -->
+    <changeSet id="plugin-webfilter-2026-09-25-sources-inactive" author="hwmdm" context="common">
+        <comment>Column,new: plugin_webfilter_sources.inactiveUrls</comment>
+        <sql>
+            ALTER TABLE plugin_webfilter_sources ADD COLUMN inactiveUrls TEXT NOT NULL DEFAULT '';
+        </sql>
+    </changeSet>
 </databaseChangeLog>
--- a/server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/persistence/mapper/WebFilterMapper.java
+++ b/server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/persistence/mapper/WebFilterMapper.java
@@ -120,8 +120,13 @@
 
     @Select("SELECT urls FROM plugin_webfilter_sources WHERE category = #{category}")
     String sourceUrls(@Param("category") String category);
-    @Insert("INSERT INTO plugin_webfilter_sources(category,urls) VALUES(#{category},#{urls}) ON CONFLICT(category) DO UPDATE SET urls = EXCLUDED.urls")
-    void saveSourceUrls(@Param("category") String category, @Param("urls") String urls);
+    // Subset of "urls" kept registered but left out of the filter, one per line
+    @Select("SELECT inactiveUrls FROM plugin_webfilter_sources WHERE category = #{category}")
+    String inactiveSourceUrls(@Param("category") String category);
+    @Insert("INSERT INTO plugin_webfilter_sources(category,urls,inactiveUrls) VALUES(#{category},#{urls},#{inactiveUrls}) " +
+            "ON CONFLICT(category) DO UPDATE SET urls = EXCLUDED.urls, inactiveUrls = EXCLUDED.inactiveUrls")
+    void saveSourceUrls(@Param("category") String category, @Param("urls") String urls,
+                        @Param("inactiveUrls") String inactiveUrls);
 
     // ------------------------------------------------------------------------------------------------- events
     @Insert("INSERT INTO plugin_webfilter_events (customerId, deviceId, configurationId, host, url, category, source, " +
--- a/server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/catalog/WebFilterCatalog.java
+++ b/server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/catalog/WebFilterCatalog.java
@@ -3,11 +3,14 @@
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.google.inject.Singleton;
+import com.hmdm.plugins.webfilter.rest.json.SourceView;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
+import java.util.Arrays;
 import java.util.Collections;
+import java.util.HashSet;
 import java.util.LinkedHashMap;
 import java.util.LinkedHashSet;
 import java.util.List;
@@ -84,12 +87,42 @@
         return siteSources.containsKey(id);
     }
 
+    /**
+     * <p>The lists of a category that go to the resolver: the active ones only.</p>
+     */
     public List<String> getSiteSources(String category) {
+        List<String> result = new ArrayList<>();
+        for (SourceView source : getSourceEntries(category)) {
+            if (source.isActive()) {
+                result.add(source.getUrl());
+            }
+        }
+        return result;
+    }
+
+    /**
+     * <p>Every registered list of a category, active or not, in the saved order. While a category was never saved,
+     * its lists are the ones of the catalog, all active.</p>
+     */
+    public List<SourceView> getSourceEntries(String category) {
+        List<String> urls = siteSources.getOrDefault(category, Collections.emptyList());
+        Set<String> inactive = Collections.emptySet();
         if (sourceMapper != null) {
             String saved = sourceMapper.sourceUrls(category);
-            if (saved != null) { return saved.isEmpty() ? Collections.emptyList() : java.util.Arrays.asList(saved.split("\\n")); }
+            if (saved != null) {
+                urls = lines(saved);
+                inactive = new HashSet<>(lines(sourceMapper.inactiveSourceUrls(category)));
+            }
         }
-        return siteSources.getOrDefault(category, Collections.emptyList());
+        List<SourceView> result = new ArrayList<>();
+        for (String url : urls) {
+            result.add(new SourceView(url, !inactive.contains(url)));
+        }
+        return result;
+    }
+
+    private static List<String> lines(String value) {
+        return value == null || value.isEmpty() ? Collections.<String>emptyList() : Arrays.asList(value.split("\\n"));
     }
 
     /**
@@ -107,17 +140,36 @@
         return Collections.unmodifiableSet(protectedPackages);
     }
 
-    public void saveSources(String category, List<String> urls) {
-        if (!isCategory(category) || urls == null || urls.size() > 30) { throw new IllegalArgumentException("Categoria ou lista inválida"); }
-        Set<String> normalized = new LinkedHashSet<>();
-        for (String value : urls) {
-            java.net.URI uri = java.net.URI.create(value.trim());
+    /**
+     * <p>Replaces the lists of a category. Blank rows are ignored; a URL repeated in the request is kept once and
+     * stays active if any of its rows is active.</p>
+     */
+    public void saveSources(String category, List<SourceView> sources) {
+        if (!isCategory(category) || sources == null || sources.size() > 30) { throw new IllegalArgumentException("Categoria ou lista inválida"); }
+        Map<String, Boolean> normalized = new LinkedHashMap<>();
+        for (SourceView source : sources) {
+            String value = source == null || source.getUrl() == null ? "" : source.getUrl().trim();
+            if (value.isEmpty()) {
+                continue;
+            }
+            java.net.URI uri;
+            try {
+                uri = java.net.URI.create(value);
+            } catch (IllegalArgumentException e) {
+                throw new IllegalArgumentException("Endereço inválido: " + value);
+            }
             if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null || value.length() > 2000) {
-                throw new IllegalArgumentException("Use um endereço HTTPS válido para cada fonte");
+                throw new IllegalArgumentException("Use um endereço https:// válido: " + value);
             }
-            normalized.add(uri.toString());
+            normalized.merge(uri.toString(), source.isActive(), Boolean::logicalOr);
         }
-        sourceMapper.saveSourceUrls(category, String.join("\n", normalized));
+        List<String> inactive = new ArrayList<>();
+        normalized.forEach((url, active) -> {
+            if (!active) {
+                inactive.add(url);
+            }
+        });
+        sourceMapper.saveSourceUrls(category, String.join("\n", normalized.keySet()), String.join("\n", inactive));
     }
 
     public JsonNode getAttribution() {
--- a/server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/rest/WebFilterResource.java
+++ b/server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/rest/WebFilterResource.java
@@ -7,6 +7,7 @@
 import com.hmdm.plugins.webfilter.persistence.domain.WebFilterAppCategory;
 import com.hmdm.plugins.webfilter.rest.json.PolicyView;
 import com.hmdm.plugins.webfilter.rest.json.SettingsView;
+import com.hmdm.plugins.webfilter.rest.json.SourceView;
 import com.hmdm.plugins.webfilter.rest.json.ValidationError;
 import com.hmdm.plugins.webfilter.service.WebFilterService;
 import com.hmdm.rest.json.Response;
@@ -92,19 +93,30 @@
             ObjectNode c = categories.addObject();
             c.put("id", id);
             c.put("required", WebFilterCatalog.REQUIRED_CATEGORY.equals(id));
-            c.put("sourceCount", catalog.getSiteSources(id).size());
-            ArrayNode sources = c.putArray("sources");
-            catalog.getSiteSources(id).forEach(sources::add);
+            putSources(c, id);
             ArrayNode a = c.putArray("apps");
             apps.getOrDefault(id, java.util.Collections.<String>emptySet()).forEach(a::add);
         }
         ArrayNode prot = root.putArray("protectedPackages");
         catalog.getProtectedPackages().forEach(prot::add);
         root.set("attribution", catalog.getAttribution());
-        root.put("canManageSources", SecurityContext.get().isSuperAdmin());
+        // Same check as saving the settings (DNS domain): whoever reaches this point may manage the lists
+        root.put("canManageSources", !denied());
         return ok(Response.OK(root));
     }
 
+    /** Every list of the category (active or not) and how many of them go to the resolver. */
+    private void putSources(ObjectNode node, String category) {
+        List<SourceView> entries = catalog.getSourceEntries(category);
+        node.put("sourceCount", (int) entries.stream().filter(SourceView::isActive).count());
+        ArrayNode sources = node.putArray("sources");
+        for (SourceView s : entries) {
+            ObjectNode item = sources.addObject();
+            item.put("url", s.getUrl());
+            item.put("active", s.isActive());
+        }
+    }
+
     @GET
     @Path("/dashboard")
     public javax.ws.rs.core.Response dashboard() {
@@ -200,12 +212,17 @@
     @PUT
     @Path("/sources/{category}")
     @Consumes(MediaType.APPLICATION_JSON)
-    public javax.ws.rs.core.Response saveSources(@PathParam("category") String category, java.util.List<String> urls) {
-        if (denied() || !SecurityContext.get().isSuperAdmin()) { return forbidden(); }
+    public javax.ws.rs.core.Response saveSources(@PathParam("category") String category, java.util.List<SourceView> sources) {
+        // Same check as saving the settings (DNS domain)
+        if (denied()) { return forbidden(); }
         try {
-            catalog.saveSources(category, urls);
+            catalog.saveSources(category, sources);
+            // Rewrites sources.json/blocky.yml; the resolver downloads the changed lists and reloads Blocky
             sourceWriter.writeAll();
-            return ok(Response.OK());
+            ObjectNode saved = new ObjectMapper().createObjectNode();
+            saved.put("id", category);
+            putSources(saved, category);
+            return ok(Response.OK(saved));
         } catch (IllegalArgumentException e) { return http(400, Response.ERROR(e.getMessage())); }
     }
 
```
Arquivo novo `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/rest/json/SourceView.java` (sha256 `fd8107cd…`): POJO com `url` e `active` (padrão `true`), getters e setters, construtor vazio e `(url, active)`.

## 2. Backup e restauração: permissão
- **Diff:** as 9 ocorrências voltaram de `isSuperAdmin()` para `hasPermission("settings")`. Isso inclui `schedule`, `saveSchedule`, `upload` e `inspect`. Nenhuma outra linha mudou (`git diff --stat`: 9 inserções, 9 remoções).
- **Compilação:** `javac -source 8 -target 8 -implicit:none` contra o `WEB-INF/classes` e o `WEB-INF/lib` extraídos do `dist/hmdm.war` do DEV, mais `/tmp/hwmdm-fixes-20260925/tomcat-lib/*.jar`, com rc=0.
  - Saiu um único `BackupResource.class`, com `major version: 52` e sha256 `1517ffaf88c60188a8debd064f2e80e87d454961afcebbdbf6aecc48296023b9`.
  - Não recompilei nenhuma outra classe do core. `BackupArchiveService` continua a do WAR.
- **Bytecode contra a classe do WAR (`javap -c -p`, índices normalizados):** as únicas trocas são 9× `invokevirtual SecurityContext.isSuperAdmin:()Z` → `ldc "settings"` + `invokevirtual SecurityContext.hasPermission:(Ljava/lang/String;)Z`. O resto são deslocamentos de offset.

## 3. F5 nas telas de plugin
- **`app/app.js`:** criei 6 estados depois de `integrations`, no mesmo padrão:
  - template `content.html?v=h3bb6f5eba8`;
  - controller `TabController`;
  - `ncyBreadcrumb` com a chave de nome do plugin;
  - `resolve.openTab` com o id da aba.

  | estado | URL | openTab |
  |---|---|---|
  | `webfilterModule` | `/webfilter` | `plugin-webfilter` |
  | `devicelogModule` | `/logs` | `plugin-devicelog` |
  | `auditModule` | `/acessos` | `plugin-audit` |
  | `pushModule` | `/push` | `plugin-push` |
  | `deviceinfoModule` | `/informacao-detalhada` | `plugin-deviceinfo` |
  | `messagingSettingsModule` | `/mensagens-config` | `plugin-settings-messaging` |
- **`tabs.controller.js`:** o `openTab` resolvido já vira `activeTab` na inicialização (`$scope.activeTab = openTab`). O `content.html` mostra a tela do plugin quando `functionsPlugins` ou `settingsPlugins` chegam.
  - O risco numa entrada direta estava em outro lugar. O módulo JS do plugin é carregado em paralelo pelo `app.js`, por `$ocLazyLoad`. Se a lista de plugins chegasse antes dele, o `ng-controller` do template ainda não existiria.
  - Mudança mínima: `getAvailablePlugins` agora passa por `waitForPluginModules`, que espera `$ocLazyLoad.load(javascriptModuleFile)` de cada plugin antes de preencher as listas.
    - O ocLazyLoad compartilha a promessa do arquivo que já está carregando e resolve na hora o que já foi carregado.
    - Uma falha de carga é ignorada, e a lista é entregue do mesmo jeito.
  - Injetei `$q` e `$ocLazyLoad`. O resto do arquivo não mudou.
- **`index.html`:** mudaram só os dois tokens trocados pelo `stamp-assets.py`.

**Diffs (backup, F5, index e tela do Web Filter)**

A cópia do overlay de `webfilter.module.js` recebeu o mesmo diff, exceto a linha 7 do token, que ali já era `h3bb6f5eba8`. A cópia do overlay de `views/main.html` recebeu diff idêntico.
```diff
--- a/server-source/server/src/main/java/com/hmdm/rest/resource/BackupResource.java
+++ b/server-source/server/src/main/java/com/hmdm/rest/resource/BackupResource.java
@@ -89,7 +89,7 @@
     @Path("/list")
     @Produces(MediaType.APPLICATION_JSON)
     public Response list() {
-        if (!SecurityContext.get().isSuperAdmin()) {
+        if (!SecurityContext.get().hasPermission("settings")) {
             return Response.PERMISSION_DENIED();
         }
         try {
@@ -118,7 +118,7 @@
     @Path("/create")
     @Produces(MediaType.APPLICATION_JSON)
     public Response create(@javax.ws.rs.QueryParam("scope") @javax.ws.rs.DefaultValue("full") String scope) {
-        if (!SecurityContext.get().isSuperAdmin()) {
+        if (!SecurityContext.get().hasPermission("settings")) {
             return Response.PERMISSION_DENIED();
         }
         try {
@@ -139,7 +139,7 @@
     @Path("/{filename}")
     @Produces(MediaType.APPLICATION_JSON)
     public Response remove(@PathParam("filename") String filename) {
-        if (!SecurityContext.get().isSuperAdmin()) {
+        if (!SecurityContext.get().hasPermission("settings")) {
             return Response.PERMISSION_DENIED();
         }
         File file = resolve(filename);
@@ -162,7 +162,7 @@
     @Path("/{filename}/download")
     @Produces(MediaType.APPLICATION_OCTET_STREAM)
     public javax.ws.rs.core.Response download(@PathParam("filename") @ApiParam("The backup file name") String filename) {
-        if (!SecurityContext.get().isSuperAdmin()) {
+        if (!SecurityContext.get().hasPermission("settings")) {
             return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.FORBIDDEN).build();
         }
         File file = resolve(filename);
@@ -190,7 +190,7 @@
     @Produces(MediaType.APPLICATION_JSON)
     @Path("/{filename}/restore")
     public Response restore(@PathParam("filename") String filename) {
-        if (!SecurityContext.get().isSuperAdmin()) {
+        if (!SecurityContext.get().hasPermission("settings")) {
             return Response.PERMISSION_DENIED();
         }
         File target = resolve(filename);
@@ -304,22 +304,22 @@
     }
     @GET @Path("/schedule") @Produces(MediaType.APPLICATION_JSON)
     public Response schedule() {
-        if (!SecurityContext.get().isSuperAdmin()) { return Response.PERMISSION_DENIED(); }
+        if (!SecurityContext.get().hasPermission("settings")) { return Response.PERMISSION_DENIED(); }
         try { return Response.OK(archives.getSchedule()); } catch (Exception e) { return Response.ERROR(e.getMessage()); }
     }
     @javax.ws.rs.PUT @Path("/schedule") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
     public Response saveSchedule(com.fasterxml.jackson.databind.node.ObjectNode value) {
-        if (!SecurityContext.get().isSuperAdmin()) { return Response.PERMISSION_DENIED(); }
+        if (!SecurityContext.get().hasPermission("settings")) { return Response.PERMISSION_DENIED(); }
         try { return Response.OK(archives.saveSchedule(value)); } catch (Exception e) { return Response.ERROR(e.getMessage()); }
     }
     @POST @Path("/upload") @Consumes(MediaType.APPLICATION_OCTET_STREAM) @Produces(MediaType.APPLICATION_JSON)
     public Response upload(InputStream data) {
-        if (!SecurityContext.get().isSuperAdmin()) { return Response.PERMISSION_DENIED(); }
+        if (!SecurityContext.get().hasPermission("settings")) { return Response.PERMISSION_DENIED(); }
         try { return Response.OK(toBackupInfo(archives.upload(data))); } catch (Exception e) { return Response.ERROR(e.getMessage()); }
     }
     @GET @Path("/{filename}/inspect") @Produces(MediaType.APPLICATION_JSON)
     public Response inspect(@PathParam("filename") String filename) {
-        if (!SecurityContext.get().isSuperAdmin()) { return Response.PERMISSION_DENIED(); }
+        if (!SecurityContext.get().hasPermission("settings")) { return Response.PERMISSION_DENIED(); }
         File file = resolve(filename);
         if (file == null || !file.isFile()) { return Response.ERROR("Backup não encontrado"); }
         try { return Response.OK(archives.inspect(file)); } catch (Exception e) { return Response.ERROR(e.getMessage()); }
--- a/server-source/server/src/main/webapp/app/app.js
+++ b/server-source/server/src/main/webapp/app/app.js
@@ -234,6 +234,49 @@
                 ncyBreadcrumb: {label: '{{"nav.integrations" | localize}}'},
                 resolve: {openTab: function () { return 'INTEGRATIONS'; }}
             })
+            // Telas de plugin abertas pelo menu (PLUGIN_STATES em tabs.controller.js): URL propria, para F5 e Voltar
+            .state('webfilterModule', {
+                url: '/webfilter',
+                templateUrl: 'app/components/main/view/content.html?v=h3bb6f5eba8',
+                controller: 'TabController',
+                ncyBreadcrumb: {label: '{{"plugin.webfilter.localization.key.name" | localize}}'},
+                resolve: {openTab: function () { return 'plugin-webfilter'; }}
+            })
+            .state('devicelogModule', {
+                url: '/logs',
+                templateUrl: 'app/components/main/view/content.html?v=h3bb6f5eba8',
+                controller: 'TabController',
+                ncyBreadcrumb: {label: '{{"plugin.devicelog.localization.key.name" | localize}}'},
+                resolve: {openTab: function () { return 'plugin-devicelog'; }}
+            })
+            .state('auditModule', {
+                url: '/acessos',
+                templateUrl: 'app/components/main/view/content.html?v=h3bb6f5eba8',
+                controller: 'TabController',
+                ncyBreadcrumb: {label: '{{"plugin.audit.localization.key.name" | localize}}'},
+                resolve: {openTab: function () { return 'plugin-audit'; }}
+            })
+            .state('pushModule', {
+                url: '/push',
+                templateUrl: 'app/components/main/view/content.html?v=h3bb6f5eba8',
+                controller: 'TabController',
+                ncyBreadcrumb: {label: '{{"plugin.push.localization.key.name" | localize}}'},
+                resolve: {openTab: function () { return 'plugin-push'; }}
+            })
+            .state('deviceinfoModule', {
+                url: '/informacao-detalhada',
+                templateUrl: 'app/components/main/view/content.html?v=h3bb6f5eba8',
+                controller: 'TabController',
+                ncyBreadcrumb: {label: '{{"plugin.deviceinfo.localization.key.name" | localize}}'},
+                resolve: {openTab: function () { return 'plugin-deviceinfo'; }}
+            })
+            .state('messagingSettingsModule', {
+                url: '/mensagens-config',
+                templateUrl: 'app/components/main/view/content.html?v=h3bb6f5eba8',
+                controller: 'TabController',
+                ncyBreadcrumb: {label: '{{"plugin.messaging.localization.key.name" | localize}}'},
+                resolve: {openTab: function () { return 'plugin-settings-messaging'; }}
+            })
             .state('applications', {
                 url: '/applications',
                 templateUrl: 'app/components/main/view/content.html?v=h3bb6f5eba8',
--- a/server-source/server/src/main/webapp/app/components/main/controller/tabs.controller.js
+++ b/server-source/server/src/main/webapp/app/components/main/controller/tabs.controller.js
@@ -1,7 +1,7 @@
 // Localization completed
 angular.module('headwind-kiosk')
     .controller('TabController', function ($scope, $rootScope, $timeout, $state, userService, authService, openTab,
-                                           pluginService, moduleRegistry, localization, hintService) {
+                                           pluginService, moduleRegistry, localization, hintService, $q, $ocLazyLoad) {
 
         $scope.localization = localization;
         $scope.moduleRegistry = moduleRegistry;
@@ -74,8 +74,24 @@
             INTEGRATIONS: 'nav.integrations'
         };
 
+        // Entrada direta pela URL (F5) numa tela de plugin: o app.js carrega o modulo JS do plugin em
+        // paralelo, e o template da tela nao pode ser montado antes dele (o ng-controller ainda nao
+        // existiria). Espera esses modulos antes de entregar a lista; os ja carregados resolvem na hora.
+        var waitForPluginModules = function (callback) {
+            return function (response) {
+                var plugins = response.status === 'OK' && response.data ? response.data.filter(function (plugin) {
+                    return plugin.javascriptModuleFile;
+                }) : [];
+                $q.all(plugins.map(function (plugin) {
+                    return $ocLazyLoad.load(plugin.javascriptModuleFile).catch(angular.noop);
+                })).then(function () {
+                    callback(response);
+                });
+            };
+        };
+
         var loadData = function () {
-            pluginService.getAvailablePlugins(function (response) {
+            pluginService.getAvailablePlugins(waitForPluginModules(function (response) {
                 if (response.status === 'OK') {
                     if (response.data) {
                         // Plugins available for Functions tab
@@ -100,7 +116,7 @@
                     $scope.functionsPlugins = [];
                     $scope.settingsPlugins = [];
                 }
-            });
+            }));
         };
 
         $scope.currentUser = {};
--- a/server-source/server/src/main/webapp/index.html
+++ b/server-source/server/src/main/webapp/index.html
@@ -52,7 +52,7 @@
     <script src='lib/angular-intro.js/build/angular-intro.min.js'></script>
 -->
 
-    <script src='app/app.js?v=h18540773f1'></script>
+    <script src='app/app.js?v=h92744cc72a'></script>
     <script src='app/spinner.js?v=h3c932d6e43'></script>
 
     <script src='app/components/header/header.controller.js?v=h813fe68a73'></script>
@@ -78,7 +78,7 @@
     <script src='app/components/main/controller/users.controller.js?v=ha7187881c3'></script>
     <script src='app/components/main/controller/roles.controller.js?v=h029004ddc0'></script>
     <script src='app/components/main/controller/icons.controller.js?v=h6fe24063f2'></script>
-    <script src='app/components/main/controller/tabs.controller.js?v=h9043c0047c'></script>
+    <script src='app/components/main/controller/tabs.controller.js?v=hfa7ab21d97'></script>
     <script src='app/components/main/controller/passwordreset.controller.js?v=h01e84d8e8f'></script>
     <script src='app/components/main/controller/passwordrecovery.controller.js?v=h7034d8629c'></script>
     <script src='app/components/main/controller/twofactorauth.controller.js?v=hd765939de8'></script>
--- a/server-source/plugins/webfilter/src/main/webapp/views/main.html
+++ b/server-source/plugins/webfilter/src/main/webapp/views/main.html
@@ -329,16 +329,79 @@
         <div class="wf-field-error" ng-repeat="m in fieldErrors.dnsDomain">{{m}}</div>
         <p class="wf-muted" localized>plugin.webfilter.dns.domain.hint</p>
 
-        <div class="kiosk-panel" ng-repeat="category in catalog.categories">
-            <h5>{{categoryName(category.id)}}</h5>
-            <p class="wf-muted">Fontes DNS compartilhadas pelo servidor. Uma URL de lista de domínios por campo.</p>
-            <div class="wf-add-app" ng-repeat="source in category.sources track by $index">
-                <input class="form-control" type="url" ng-model="category.sources[$index]" aria-label="URL da lista" ng-disabled="!catalog.canManageSources">
-                <button class="btn btn-default" ng-if="catalog.canManageSources" ng-click="removeSource(category, $index)" title="Retira esta fonte da categoria; use Salvar listas para aplicar.">Remover</button>
-            </div>
-            <div class="kiosk-action-row" ng-if="catalog.canManageSources">
-                <button class="btn btn-default" ng-click="addSource(category)" title="Adiciona uma fonte de domínios a esta categoria.">Adicionar lista</button>
-                <button class="btn btn-primary" ng-click="saveSources(category)" ng-disabled="saving" title="Salva as fontes e solicita a atualização do resolvedor DNS.">Salvar listas</button>
+        <div class="wf-src" data-testid="wf-sources">
+            <style>
+                .wf-src { margin: 16px 0; }
+                .wf-src-cat { border: 1px solid var(--hwmdm-border); border-radius: 8px; background: var(--hwmdm-surface); margin-bottom: 4px; }
+                .wf-src-head { display: flex; align-items: center; gap: 8px; width: 100%; padding: 5px 10px; border: 0; background: transparent;
+                               text-align: left; color: var(--hwmdm-text-strong); cursor: pointer; }
+                .wf-src-head .hw-icon { width: 14px; height: 14px; color: var(--hwmdm-muted); }
+                .wf-src-name { font-weight: 600; }
+                .wf-src-count { margin-left: auto; color: var(--hwmdm-muted); font-size: 12px; white-space: nowrap; }
+                .wf-src-dirty { color: var(--hwmdm-danger); font-size: 12px; font-weight: 600; white-space: nowrap; }
+                .wf-src-body { padding: 0 10px 10px; }
+                .wf-src-table { width: 100%; table-layout: fixed; margin: 0 0 8px; }
+                .wf-src-table th, .wf-src-table td { padding: 4px 6px !important; vertical-align: middle !important; }
+                .wf-src-table th { font-size: 12px; font-weight: 600; color: var(--hwmdm-muted); text-transform: none !important; letter-spacing: normal !important; }
+                .wf-src-table .wf-src-col-active { width: 56px; text-align: center; }
+                .wf-src-table .wf-src-col-actions { width: 100px; text-align: right; }
+                .wf-src-table .wf-src-col-active input { margin: 0; }
+                .wf-src-table .form-control { height: 30px; padding: 0 8px; font-family: monospace; font-size: 12px; }
+                .wf-src-table .btn { width: auto; min-width: 0; height: 30px; padding: 0 10px; white-space: nowrap; }
+                .wf-src-off .form-control { opacity: .55; }
+                .wf-src-bad .form-control { border-color: var(--hwmdm-danger); }
+                .wf-src-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
+                .wf-src-ok { color: #15803d; font-size: 12px; font-weight: 600; }
+                [data-theme="dark"] .wf-src-ok { color: #86efac; }
+                .wf-src-err { color: var(--hwmdm-danger); font-size: 12px; font-weight: 600; }
+            </style>
+            <h5><strong>Listas de DNS</strong></h5>
+            <p class="wf-muted">Lista desmarcada fica cadastrada, mas fora do filtro.</p>
+            <div class="wf-src-cat" ng-repeat="category in catalog.categories" data-testid="wf-src-{{category.id}}">
+                <button type="button" class="wf-src-head" ng-click="toggleSources(category)" aria-expanded="{{!!category.open}}"
+                        data-testid="wf-src-toggle-{{category.id}}">
+                    <span class="hw-icon" ng-class="category.open ? 'hw-icon-chevron-down' : 'hw-icon-chevron-right'"></span>
+                    <span class="wf-src-name">{{categoryName(category.id)}}</span>
+                    <span class="wf-src-dirty" ng-if="category.dirty">não salvo</span>
+                    <span class="wf-src-count">{{activeSources(category)}} de {{category.sources.length}} ativas</span>
+                </button>
+                <div class="wf-src-body" ng-if="category.open">
+                    <table class="table wf-src-table" ng-if="category.sources.length">
+                        <thead>
+                        <tr>
+                            <th class="wf-src-col-active">Ativa</th>
+                            <th>URL</th>
+                            <th class="wf-src-col-actions"></th>
+                        </tr>
+                        </thead>
+                        <tbody>
+                        <tr ng-repeat="source in category.sources" data-testid="wf-src-row"
+                            ng-class="{'wf-src-off': !source.active, 'wf-src-bad': invalidSource(source)}">
+                            <td class="wf-src-col-active">
+                                <input type="checkbox" ng-model="source.active" ng-change="markSourcesDirty(category)"
+                                       aria-label="Ativa" data-testid="wf-src-active">
+                            </td>
+                            <td>
+                                <input type="text" class="form-control" ng-model="source.url" ng-change="markSourcesDirty(category)"
+                                       placeholder="https://" aria-label="URL da lista" spellcheck="false" data-testid="wf-src-url">
+                            </td>
+                            <td class="wf-src-col-actions">
+                                <button type="button" class="btn btn-default btn-sm" ng-click="removeSource(category, $index)"
+                                        data-testid="wf-src-remove">Remover</button>
+                            </td>
+                        </tr>
+                        </tbody>
+                    </table>
+                    <p class="wf-muted" ng-if="!category.sources.length">Nenhuma lista nesta categoria.</p>
+                    <div class="wf-src-actions">
+                        <button type="button" class="btn btn-default btn-sm" ng-click="addSource(category)"
+                                data-testid="wf-src-add">Adicionar lista</button>
+                        <button type="button" class="btn btn-primary btn-sm" ng-click="saveSources(category)"
+                                ng-disabled="category.saving || !category.dirty" data-testid="wf-src-save">Salvar listas</button>
+                        <span class="wf-src-ok" ng-if="category.sourceMessage" data-testid="wf-src-ok">{{category.sourceMessage}}</span>
+                        <span class="wf-src-err" ng-if="category.sourceError" data-testid="wf-src-err">{{category.sourceError}}</span>
+                    </div>
+                </div>
             </div>
         </div>
         <h5><strong localized>plugin.webfilter.sources</strong></h5>
--- a/server-source/plugins/webfilter/src/main/webapp/webfilter.module.js
+++ b/server-source/plugins/webfilter/src/main/webapp/webfilter.module.js
@@ -4,7 +4,7 @@
         try {
             $stateProvider.state('plugin-webfilter', {
                 url: '/plugin-webfilter',
-                templateUrl: 'app/components/main/view/content.html?v=h1d21823701',
+                templateUrl: 'app/components/main/view/content.html?v=h3bb6f5eba8',
                 controller: 'TabController',
                 ncyBreadcrumb: {
                     label: '{{"plugin.webfilter.localization.key.name" | localize}}'
@@ -120,20 +120,77 @@
             if ($scope.activeWfTab === 'dashboard' && !$scope.dashboardLoading) { loadDashboard(); }
         }, 10000);
         $scope.$on('$destroy', function () { $interval.cancel(refreshTimer); });
-        $scope.addSource = function (category) { category.sources.push(''); };
-        $scope.removeSource = function (category, index) { category.sources.splice(index, 1); };
+        // ------------------------------------------------------------------------------------------------ DNS lists
+        // Each category carries its rows ({url, active}) plus, only here, whether it is expanded and has unsaved edits.
+        var HTTPS_URL = /^https:\/\/[^\s\/?#]+\S*$/i;
+        $scope.activeSources = function (category) {
+            return (category.sources || []).filter(function (s) { return s.active; }).length;
+        };
+        $scope.toggleSources = function (category) { category.open = !category.open; };
+        $scope.markSourcesDirty = function (category) {
+            category.dirty = true;
+            category.sourceMessage = undefined;
+            category.sourceError = undefined;
+        };
+        $scope.addSource = function (category) {
+            category.sources.push({url: '', active: true});
+            category.open = true;
+            $scope.markSourcesDirty(category);
+        };
+        $scope.removeSource = function (category, index) {
+            category.sources.splice(index, 1);
+            $scope.markSourcesDirty(category);
+        };
+        $scope.invalidSource = function (source) {
+            var url = (source.url || '').trim();
+            return url.length > 0 && !HTTPS_URL.test(url);
+        };
+        var sourceErrorText = function (body) {
+            return body && body.message ? localization.localize(body.message) : localization.localize('error.request.failure');
+        };
         $scope.saveSources = function (category) {
-            clearMessages();
-            $scope.saving = true;
-            pluginWebFilterService.saveSources({category: category.id}, category.sources, function (response) {
-                $scope.saving = false;
-                if (response.status === 'OK') { $scope.successMessage = localization.localize('plugin.webfilter.saved'); loadCatalog(); }
-                else { showErrors(response); }
-            }, onFailure);
+            // Blank rows are simply dropped
+            var rows = category.sources.filter(function (s) { return (s.url || '').trim().length > 0; });
+            category.sourceMessage = undefined;
+            category.sourceError = undefined;
+            if (rows.some($scope.invalidSource)) {
+                category.sourceError = 'Use endereços que comecem com https://';
+                return;
+            }
+            category.saving = true;
+            pluginWebFilterService.saveSources({category: category.id}, rows.map(function (s) {
+                return {url: s.url.trim(), active: !!s.active};
+            }), function (response) {
+                category.saving = false;
+                if (response.status === 'OK') {
+                    category.sources = response.data.sources;
+                    category.sourceCount = response.data.sourceCount;
+                    category.dirty = false;
+                    category.sourceMessage = 'Listas salvas. O filtro DNS vai recarregá-las.';
+                } else {
+                    category.sourceError = sourceErrorText(response);
+                }
+            }, function (httpResponse) {
+                category.saving = false;
+                category.sourceError = sourceErrorText(httpResponse && httpResponse.data);
+            });
         };
         var loadCatalog = function () {
             pluginWebFilterService.getCatalog(function (response) {
                 if (response.status === 'OK') {
+                    // A reload (e.g. after categorizing an app) keeps what is expanded and any list edit not saved yet
+                    var previous = {};
+                    ($scope.catalog.categories || []).forEach(function (c) { previous[c.id] = c; });
+                    (response.data.categories || []).forEach(function (c) {
+                        var old = previous[c.id];
+                        if (old) {
+                            c.open = old.open;
+                            if (old.dirty) {
+                                c.sources = old.sources;
+                                c.dirty = true;
+                            }
+                        }
+                    });
                     $scope.catalog = response.data;
                 } else {
                     showErrors(response);
```

## Teste na cópia isolada (sem tocar no DEV)

**Preparação**
- Só o contêiner de revisão foi parado e iniciado: `docker stop hwmdm-review-app-20260925` das 16:49:31 às 16:50:31 e `docker start` às 16:50:54. O Tomcat subiu às 16:52:26.
- O ROOT explodido da revisão (`/tmp/hwmdm-fixes-20260925/review/webapps/ROOT`), que era das 09:5x, foi atualizado com o estado servido hoje pelo DEV (`source/volumes/webapps/ROOT`) e, por cima, com os arquivos novos:
  - o webapp do overlay;
  - a UI dos plugins webfilter e deviceinfo;
  - o jar novo do webfilter;
  - o `BackupResource.class` novo.
- `diff -rq` contra o ROOT servido pelo DEV mostrou exatamente 7 diferenças:
  - `app.js`, `tabs.controller.js` e `index.html`;
  - `webfilter/views/main.html` e `webfilter/webfilter.module.js`;
  - `BackupResource.class` e `webfilter-0.1.0.jar`.
- No boot da revisão, o Liquibase aplicou `plugin-webfilter-2026-09-25-dns-events` (que faltava naquele banco) e `plugin-webfilter-2026-09-25-sources-inactive`.
  - Única linha SEVERE: o conector HTTPS 8443 sem `hmdm.jks`. Ela já aparece no boot das 12:54 da revisão.
- **Usuário:** `review_20260925`, papel "Admin" (userroleid 2, `superadmin=false` no banco da revisão). Não criei usuário e não mexi em papéis.
- **Roteiro:** fora do repositório, em `/tmp/claude-1002/-opt-projetos-hwmdm/5eef6554-fd12-4c0c-a71d-4f279183552c/scratchpad/teste-revisao.js`.
  - Usa o Playwright pedido, com Chromium headless em 1366×768 e locale pt-BR.
  - Gestos: login digitado na tela, cliques no menu lateral, cliques em checkbox e botões, digitação na URL.
  - Resultado bruto em `output/listas-dns-backup-f5/r{1,2}-resultado.json`.

**Rodadas**
- **Rodada 1 (16:54):**
  - A primeira execução parou na medição de rolagem, por um erro do **roteiro** (`document.scrollingElement` nulo), e não da tela. Corrigi o roteiro e repeti: 18 de 18 SIM.
  - Pelas capturas, ajustei só a aparência do bloco novo, nas duas cópias:
    - a seta `glyphicon-chevron-right` aparecia como um quadrado cinza e virou `hw-icon-chevron-right/down`;
    - o cabeçalho "ATIVA / URL" vinha em caixa-alta do CSS global e agora sai "Ativa / URL";
    - as faixas ficaram mais baixas.
- **Rodada 2 (16:57), já com a versão publicada:** 24 de 24 SIM. Os passos, com as capturas `r2-*`, estão na tabela abaixo.

| # | Passo (Admin, gestos de pessoa) | Resultado | Captura / evidência |
|---|---|---|---|
| 1a | Web Filter › Configuração: 14 categorias recolhidas; o bloco inteiro ocupa 570 px de altura; sem rolagem horizontal; domínio DNS e "Fontes das listas de sites" presentes | **SIM** | `r2-03-configuracao-listas-recolhidas.png` |
| 1b | Abrir "Jogos", desmarcar a 1ª lista, editar a URL da 2ª, adicionar uma lista, adicionar outra e **Remover** | **SIM** (a linha removida sai da tela) | `r2-04-jogos-aberta-antes.png`, `r2-05-jogos-editada-antes-de-salvar.png` |
| 1c | **Salvar listas** → "Listas salvas. O filtro DNS vai recarregá-las." | **SIM** | `r2-06-jogos-salva.png` |
| 1d | `sources.json` da revisão (`/usr/local/tomcat/work/plugins/webfilter/dns/sources.json`): a desativada saiu, a editada e a nova entraram, a antiga 2ª saiu | **SIM**: `games` = [`…editada-r2.txt`, `…nova.txt`, `…nova-r2.txt`] | r2-resultado.json |
| 1e | Banco da revisão: a desativada continua em `urls` e aparece em `inactiveurls` | **SIM** | r2-resultado.json |
| 1f | **F5** na página → continua em `#/webfilter`; em Configuração › Jogos: 1ª desmarcada e visível, 2ª editada, nova presente, removida ausente | **SIM** | `r2-07-jogos-depois-f5.png` |
| 1g | **Marcar** de novo a 1ª e salvar → ela volta ao `sources.json` | **SIM** | `r2-08-jogos-remarcada.png` |
| 1h | Erro visível: URL `http://…` → "Use endereços que comecem com https://", nada salvo | **SIM** | `r2-09-jogos-erro-http.png` |
| 2 | Backup e restauração: `GET /rest/private/backup/list` → HTTP 200, `status: OK`; nenhum "permissão negada" na tela | **SIM** | `r2-12-backup.png` |
| 3a | Clicar Web Filter no menu → `#/webfilter`; F5 → continua em Web Filter (tela e item ativo no menu) | **SIM** | `r2-01-webfilter-menu.png`, `r2-02-webfilter-depois-f5.png` |
| 3b | Logs pelo menu → `#/logs`; F5 → continua em Logs | **SIM** | `r2-10-devicelog-menu.png`, `r2-11-devicelog-depois-f5.png` |
| 3c | Informação detalhada pelo menu → `#/informacao-detalhada`; F5 → continua | **SIM** | `r2-10-deviceinfo-menu.png`, `r2-11-deviceinfo-depois-f5.png` |
| 3d | (extra) Entrada direta pela URL + 3× F5 em `#/webfilter`, `#/logs`, `#/acessos`, `#/push`, `#/informacao-detalhada`, `#/mensagens-config`: a tela do plugin abre nas 18 recargas, sem erro de console | **SIM** | `r2-11b-mensagens-config-direto.png` |
| 4 | Dispositivos, Quiosque, Módulos e Integrações abrem sem erro de console | **SIM** (0 erros em cada uma) | `r2-13-dispositivos.png`, `r2-13-quiosque.png`, `r2-13-modulos.png`, `r2-13-integracoes.png` |

**Erros de console do navegador:** nenhum, nem `pageerror` nem `console.error`, nas rodadas 1 e 2. No log da revisão depois dos testes só apareceu um `ClientAbortException: Broken pipe`, que é o navegador cancelando uma requisição durante o F5.

**Observação fora do escopo, não corrigida.** Na tela de Backup, `GET /rest/private/backup/schedule` responde HTTP 200 com `{"status":"ERROR","message":null}`. Não é permissão: a mensagem de permissão negada seria outra.
- Hipótese: `BackupResource` recebe `BackupArchiveService` por `@com.google.inject.Inject` num campo. Os resources são criados pelo HK2 (`GuiceIntoHK2Bridge` em `HMDMApplication`), que só honra `@javax.inject.Inject`. O campo fica nulo e a chamada cai em `NullPointerException`, que no Java 11 não tem mensagem.
- Se a hipótese estiver certa, também falhariam agendamento, "Create backup now" (zip), importação, inspeção e restauração de `.zip`.
- A tarefa mandava não mudar mais nada no arquivo, por isso não corrigi.

## Publicação no DEV
- **Concorrência (16:59:35):** o `docker ps` mostrou `hwmdm-hmdm-1` com StartedAt 19:06:34Z (16:06:34), que é o último reinício esperado. Não havia reinício mais recente. Postgres 17:31:18Z e webfilter-dns 17:48:29Z, sem mudança. Processos vivos: `codex`, desde 14:23, e duas sessões `claude`.
- **Commits do usuário durante a tarefa:** `35055a13` (16:53:27) e `46b6146e` (16:58:05), ambos "fim do dia 25-09-26", de Jose Carlos. Eles levaram os meus arquivos-fonte e as capturas `r1/r2` como estavam. Não rodei git de escrita. O fonte de agora é igual ao do HEAD e ao que foi publicado.
- **Cópias `.antes` (17:00, `cp -p`):**

  | Cópia | sha256 (= arquivo substituído) |
  |---|---|
  | `dist/hmdm.war.antes-20260925-1700` | f429811fa4f43965778789b7245fb9bd8be3f9c2ecc8fd20c5ae7bf395d627af |
  | `dist/ROOT.war.antes-20260925-1700` (cópia de `source/volumes/webapps/ROOT.war`; fica em `dist/` para o Tomcat não a ver) | 660205b9307fafc990ece4976296ffc39f4bf6d46c4374ef216e9b2858abc6b1 |
  | `dist/overlay-webfilter-0.1.0.jar.antes-20260925-1700` | 44342f4f5dfd42a65e493e6ded1ecc7a69a7bf23f1c43927357eecc2d95dfef1 |
  | `output/listas-dns-backup-f5-antes/…/*.antes-20260925-1645`, os 14 arquivos-fonte e webapp de antes da edição | ver a pasta |
- **Remendo dos WARs:** feito com `jar uf` numa cópia no scratchpad, com 4 entradas:
  - `WEB-INF/lib/webfilter-0.1.0.jar`;
  - `WEB-INF/classes/com/hmdm/rest/resource/BackupResource.class`;
  - `app/components/plugins/webfilter/views/main.html`;
  - `app/components/plugins/webfilter/webfilter.module.js`.

  Conferência com Python `zipfile`:
  - `dist/hmdm.war` foi de 698 para 698 entradas e `ROOT.war` de 701 para 701;
  - só essas 4 entradas mudaram de CRC;
  - o MANIFEST é o mesmo e o `testzip` passou.

  Com o app parado, conferi cada alvo com `sha256sum -c` antes de copiar:
  - `cp` sobre `dist/hmdm.war`, mantendo o inode 917524, que é bind mount de arquivo;
  - `cp` e depois `mv` sobre `source/volumes/webapps/ROOT.war`;
  - `cp` do jar novo sobre `server-source/server/src/main/webapp/WEB-INF/lib/webfilter-0.1.0.jar`.
- **`stamp-assets.py`:**
  - Às 16:49:14, antes do teste, a 1ª passada reescreveu só `index.html`, com 2 tokens:
    - `app/app.js`: `h18540773f1` → `h92744cc72a`;
    - `tabs.controller.js`: `h9043c0047c` → `hfa7ab21d97`.

    A 2ª passada não reescreveu nada.
  - Às 17:00:37, com o app parado, as duas passadas deram "Arquivos reescritos: 0".
- **Reinício:**
  - `docker stop` das 17:00:03 às 17:00:19;
  - `docker start` às 17:00:45 (StartedAt 2026-09-25T20:00:46Z, RestartCount 0);
  - `Usando WAR existente: hmdm-5.39.2-os.war` e `Custom webapp overlay rebuilt … (stale files removed)`;
  - **`Server startup in [18908] milliseconds` às 17:01:22**.

  O entrypoint regravou o `ROOT.war` às 17:00:51 (sha256 `5af32c1a…`).

## Verificação por máquina no DEV (17:01:41–17:01:49)
| Verificação | Resultado | Evidência |
|---|---|---|
| HTTP 200 | **sim** | `GET http://192.168.1.65:8080/` → 200, 9385 B; `login.html` → 200 |
| Nenhuma exceção nova no boot | **sim** | Log desde 20:00:40Z (41.078 linhas), sem nenhum `Caused by` ou stack. Sobraram 2 linhas de erro ou aviso:<br>• `[ERROR] LongPollingServlet : Empty constructor called!`, que aparece em todos os boots de hoje;<br>• `[WARN] RemoteSessionHub : Fluxo recusado para 'R9XT200AMYY': nenhuma sessao foi pedida`, que é o tablet empurrando vídeo remoto sem sessão depois do reinício. Não é exceção, e a mesma classe já avisou às 15:16 e às 15:17. |
| changeSet novo aplicado (SELECT) | **sim** | `plugin-webfilter-2026-09-25-sources-inactive`, `dateexecuted 2026-09-25 17:01:14.854994`, md5 `8:e4ad2613…`. Coluna `inactiveurls text NOT NULL DEFAULT ''`. `plugin_webfilter_sources` com 0 linhas: nada foi gravado à mão, e o DEV continua com as listas padrão do catálogo. |
| sha256 servido = disco | **sim** | Disco, `curl` e ROOT explodido iguais:<br>• `index.html` 72735bec…;<br>• `app/app.js` 92744cc7…;<br>• `tabs.controller.js` fa7ab21d…;<br>• `webfilter/views/main.html` 84bb1f7f…, igual às duas cópias-fonte;<br>• `webfilter/webfilter.module.js` 9de8c23a…, igual às duas cópias-fonte.<br>`webfilter-0.1.0.jar` = dc38668a… em `target/`, no ROOT explodido e em `/opt/custom-webapp`. `BackupResource.class` = 1517ffaf… no ROOT explodido. |
| `index.html` com os tokens novos | **sim** | Linha 55 `app/app.js?v=h92744cc72a`; linha 81 `tabs.controller.js?v=hfa7ab21d97` |
| Resolvedor sem mudança indevida | **sim** | O boot regenerou `2 active profile(s), categories [doh, games, social_media]`. O `sources.json` do DEV continua com mtime 08:28:07 e o mesmo conteúdo lido às 16:3x. |
| Contêineres | **sim** | Só o `hwmdm-hmdm-1` mudou de StartedAt. Postgres 17:31:18Z e webfilter-dns 17:48:29Z seguem iguais. |

## O que NÃO foi verificado
- **A tela no DEV com o `admin` real, num navegador.** Não tenho a senha do DEV; esse é o teste humano. Desde as 16:40 o papel Admin do DEV tem `superadmin=true`, alterado pelo orquestrador. Por isso o teste que prova que nada exige super admin é o da revisão, onde o Admin tem `superadmin=false`.
- **O caminho completo no DEV** (adicionar ou desativar uma URL e ver o `sources.json` do DEV e o Blocky recarregar).
  - Não salvei listas no DEV, para não mudar o filtro real.
  - Na revisão, o `sources.json` saiu certo, mas lá não existe `webfilter-dns`. Portanto o download e o `POST /api/lists/refresh` do Blocky não foram observados.
  - O mecanismo está descrito no item 1.
- **Cache do navegador.** O `webfilter.module.js` e o `views/main.html` do plugin são pedidos sem `?v=`, porque os caminhos vêm da tabela `plugins`. Um navegador com a versão antiga em cache pode mostrar a tela velha. Recomendo **Ctrl+Shift+R** na primeira abertura.
- **Backup além da lista:** agendamento, criação de `.zip`, importação e restauração não foram testados. Veja a observação da NPE acima: não é a permissão, mas pode afetar o uso.
- Não atualizei `dist/hmdm.war.sha256` nem `source/volumes/webapps/ROOT.war.sha256`, que ficaram defasados. O `dist/hmdm.war` novo tem `72806349358ac9f38ba8b637fc5b565918abb2172bfaba0f3197b96df8bfae57`.
- Testei só o Chromium headless em 1366×768. Não testei Firefox, tela estreita de celular nem tema escuro.
- Na entrada direta em `#/acessos`, um instante logo depois de montar a tela mostrou a chave `plugin.audit.tab.title` sem tradução. 4 s depois, a mesma tela no F5 estava traduzida ("Registro de Acesso"). Isso vem do carregamento do pacote de idioma do plugin, que não mexi.
