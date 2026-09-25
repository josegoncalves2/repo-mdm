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
