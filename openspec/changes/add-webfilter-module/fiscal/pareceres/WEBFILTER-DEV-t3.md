# Parecer — Web Filter no DEV — tentativa 3

Validador: VAL-WEBFILTER-DEV-t3
Executor: sessao-principal
Evidencia: /opt/projetos/hwmdm/evidencias/webfilter/validador-t3/

- Data: 2026-09-18, 18:45–19:47 (horário de Brasília).
- Ambiente: somente DEV (`http://localhost:8080`, 192.168.1.65). Nenhum comando meu acessou o servidor de produção. O favicon externo recusado aparece mascarado nos logs.
- Pedido: `fiscal/fila/para-validador/WEBFILTER-DEV-t3.md`, com as regras do t1 e do t2 e os pareceres t1 e t2. O relatório do executor foi tratado como alegação.
- Decisões humanas do t2 respeitadas:
  - (1) item próprio no menu e F5 em `app.js`;
  - (2) commit e versionamento são do responsável;
  - (3) `.btn-primary` do núcleo fica como observação;
  - (4) alterações de terceiros em paralelo.
- Método:
  - Chromium (Playwright) dirigido só por gestos de pessoa: login pela tela, clique no menu, digitação, F5, voltar do navegador, botão de tema e navegador em pt-BR e en-US.
  - Sonda DoT com SNI por perfil, usando só domínios que existem no DNS público. `roteiros/consulta_dot_ns.py` é uma cópia da sonda do pedido que também mostra a seção de autoridade. `sonda-seg.sh` e `sonda-passiva.sh` chamam a sonda do pedido.
  - Sync simulado com `X-Request-Signature`. A `X-Response-Signature` foi conferida pela regra do launcher.
  - Banco somente leitura. Roteiros descartáveis em `validador-t3/roteiros/`.
- Ações permitidas que executei:
  - UMA recarga a quente do MDM (`touch ROOT.war` às 18:47:25);
  - SIGKILL do Blocky dentro do `webfilter-dns`;
  - `docker stop` e `docker start` do `webfilter-dns`.
  - Ao final, a tela de login do MDM abre (`Z-tela-login.log`, `Z-tela-login.png`, 19:46:51).
- Estado inicial e final:
  - Arquivos: `00-estado-inicial-db.txt`, `00-estado-inicial-dns.txt`, `Z-estado-final-db.txt` e `Z-dns-final.txt`.
  - Voltaram ao estado inicial, pela tela: políticas, categorias, listas, domínio DNS e `pluginsdisabled`. As entradas ganharam ids novos porque o salvamento regrava as linhas; os valores são os mesmos.
  - Os sha256 de `blocky.yml` (`e3c3df9b…`), `sources.json` e das 4 listas de perfil são iguais aos iniciais.
  - `plugin_webfilter_locked_history` não mudou.
  - Os `configUpdated` que enviei são efeito normal dos salvamentos. A contagem de `pushmessages` caiu de 72 para 16 porque o núcleo limpa mensagens antigas.
- Pendência de ambiente: a recarga a quente deixou a instância antiga do MDM viva em memória, como o runbook avisa. Ela já não grava nada (ver defeito 2). Não reiniciei o container do MDM porque o pedido só autorizou a recarga. Recomendo ao responsável `docker restart hwmdm-hmdm-1` no DEV.

## Tabela de evidências

| Item | O que fiz | Onde está a evidência | Resultado |
|---|---|---|---|
| A.1 Menu, título, abas, textos | Entrei com `f.webfilter` e cliquei em "Web Filter" no menu | `A01-navegar.log`, `A01-lista.png` | OK: título igual ao item do menu; 3 abas em português; faixa "Perfis 3 / Filtro ativo 2 / filtro.hwmdm.dev.local" |
| A.2 Categorias (def. 7) | Abri o editor de "Common - Minimal" | `A01-navegar.log`, `A02-editor.png` | OK: 14 categorias com nome e descrição; `doh` marcada e fixa |
| A.3 F5, Voltar, voltar do navegador | Dei F5 no editor, usei "Voltar ao Web Filter", fui a Dispositivos e usei o voltar do navegador | `A01-navegar.log`, `A03-apos-F5-editor.png` | OK: F5 permanece em `#/plugin-webfilter`; Voltar leva à lista; o voltar do navegador retorna ao Web Filter |
| A.4 Carga do módulo | Contei as requisições de `webfilter.module.js` | `A01-navegar.log` | OK: 1 carga após o login e 1 após o F5 |
| A.5 Outras telas | Abri Dispositivos, Perfis, Aplicativos, Plugins e Usuários, com F5 em cada uma | `A01-navegar.log`, `A04-tela-*.png` | OK: cada tela continua após o F5 |
| A.6 Sem permissão (def. 1 e 3) | Entrei como `v.observer.t1`, li o menu, digitei `#/plugin-webfilter` e fiz 8 chamadas pelo navegador dele | `A10-sem-permissao.log`, `A10-menu-observer.png`, `A11-observer-url-digitada.png` | OK: sem item no menu; "Você não tem permissão…"; as 8 chamadas voltam **HTTP 403** |
| A.7 Validação (def. 3) | Em 2 temas × 3 larguras, digitei `*.com`, `facebook.com` nas duas listas, `exemplo com`, `com.br`, `com.hmdm.launcher` e `candy crush`, salvei e reabri | `A20-validacao-temas-larguras.log`, `A2E-*-editor-erros.png` | OK: HTTP 400, erros junto aos campos, "Nada foi salvo." e política reaberta idêntica |
| A.8 mainApp na blocklist (def. 5) | Incluí `com.android.chrome` no Kiosk Total e salvei | `A20-…log`, `A2E-dark-1280-kiosk-chrome.png` | OK: HTTP 400 "app principal do quiosque…"; nada salvo; a lista de bloqueados não mostra o chrome |
| A.9 Aba Aplicativos | Tentei incluir `com.hmdm.launcher` em Jogos nas 6 combinações | `A20-…log`, `A2E-*-aplicativos-erro.png` | OK: HTTP 400 com mensagem; nenhuma linha criada |
| A.10 404 | Pedi o perfil 99999 (GET e PUT) e o app 99999 (DELETE) | `A40-404.log` | OK: HTTP 404 |
| A.11 Plugin desativado | Desliguei "Web Filter" na tela Plugins, dei F5 e religuei | `A41-plugin-desativado.log`, `A41`/`A42*.png` | OK: o item some e volta; `pluginsdisabled` final igual ao inicial |
| A.12 Domínio inválido (def. 12) | Digitei `dns empresa` e salvei nas 6 combinações; apaguei o campo sem salvar; dei F5 | `A20-…log`, `A2E-*-configuracao-erro.png`, `A2E-*-1280-dominio-apagado-sem-salvar.png` | OK: HTTP 400; a faixa continua `filtro.hwmdm.dev.local` durante a digitação e após o erro; o campo apagado sem salvar não mostra aviso; após o F5 o domínio salvo está intacto |
| A.13 Domínio vazio salvo (def. 12) | Salvei vazio, olhei as abas, dei F5, digitei o domínio original sem salvar e depois salvei | `A60-dominio-vazio.log`, `A60-dominio-vazio-salvo.png`, `A61-dominio-restaurado.png` | OK: o aviso e a faixa "não configurado" só aparecem depois de salvar vazio e persistem após o F5; digitar sem salvar não os altera; somem após salvar o domínio |
| A.14 Texto de ajuda (def. 14) | Li a aba Configuração em pt-BR e en-US, antes e depois do F5 | `A50-ajuda-idiomas-e-doh.log`, `A50-ajuda-pt-BR.png`, `A50-ajuda-en-US.png` | OK: "id-c(cliente)-p(perfil).(domínio), por exemplo id-c1-p11.filtro.empresa.com.br…" e o equivalente em inglês, sem truncamento |
| A.15 Contraste do cartão `doh` (def. 11) | Medi o editor em escuro e claro, a 1280, 768 e 360 px | `A50-ajuda-idiomas-e-doh.log`, `A51-doh-*.png` | OK: descrição do `doh` com 6,31:1 no escuro e 4,97:1 no claro; a menor das 14 descrições é 6,31 (escuro) e 4,97 (claro) |
| A.16 Contraste de todo texto visível | Rodei a medição por texto em lista, editor, editor com erros, Aplicativos e Configuração (com erro), nas 6 combinações | `A20-validacao-temas-larguras.log` | Escuro: tudo ≥ 4,5 exceto `.btn-primary` (2,96, decisão 3) e o placeholder `form-control` (4,41, núcleo). Claro: aba ativa `.kiosk-tab-active` com 2,73:1 e placeholder com 2,85:1, ambos do núcleo (ver Observações) |
| A.17 Larguras | Comparei `scrollWidth` com `innerWidth` em cada aba e estado | `A20-…log` | OK: nenhum estouro. Em 360 px, "Editar" só aparece rolando a tabela (observação mantida) |
| B.1 Filtro por SNI | Consultei 10 domínios reais com os SNIs de p1, p11, p2 (sem política) e sem SNI | `B30-filtro-por-sni.txt` | OK: categorias, `doh` e blocklist dão NXDOMAIN, inclusive `www.roblox.com`; a allowlist `linkedin.com` vence `social_media` (o domínio está na lista); perfis isolados; p2 e sem SNI resolvem tudo |
| B.2 Defeito 2 (causa) | Fiz a recarga a quente às 18:47:25 e observei até 19:00:54: logs do MDM e do resolvedor, sonda a cada ~2 s, `.owner` e `blocky.yml` a cada 30 s | `B01`–`B06` | CORRIGIDO: a instância nova grava e assume às 18:47:41; a antiga registra "owned by a newer instance… stops writing" às 18:54:48 e não grava mais (19:04:48 sem linha); a nova grava às 18:57:41. Resolvedor: 0 linhas no log, 373 rodadas da sonda sem falha, maior intervalo 3 s, `blocky.yml` intocado (18:35:26) |
| B.3 Mudança só de lista | Pela tela: +www.bing.com (allow), +hwmdm.dev.local, +wikipedia.org, −wikipedia.org, −hwmdm.dev.local, −www.bing.com, −bing.com | `B39-sequencia.log`, `B40-log-seq.txt`, `B41-sonda-seq.txt` | OK sem nova tentativa pendente: 7 de 7 com "listas recarregadas sem reinicio" em cerca de 2 s, sem lacuna |
| B.4 Pai de protegido e precedência (def. 6) | Coloquei `bing.com` e `hwmdm.dev.local` na blocklist e `www.bing.com` na allowlist de p1 | `B39-sequencia.log` (arquivos gerados), `B41-sonda-seq.txt`, `B42`/`B43*.png` | OK: aceito; deny tem `*.hwmdm.dev.local` e allow tem `*.filtro.hwmdm.dev.local`; `bing.com` e `cn.bing.com` dão NXDOMAIN e `www.bing.com` resolve |
| B.5 Mudança de categoria | Desmarquei `streaming` em p1 (restauração) | `B40-log-seq.txt`, `B41-sonda-seq.txt` | OK: troca com 1 s sem resposta (19:41:21) e nenhuma resposta sem filtro |
| B.6 Queda do Blocky sem nova tentativa pendente | SIGKILL no Blocky em uso às 19:37:28 | `B39`, `B40`, `B41` | OK: "Blocky caiu" e volta filtrando às 19:37:37 (cerca de 9 s) |
| B.7 Defeito 13 | Marquei `streaming` em p1 pela tela. SIGKILL na 1ª carga (19:06:19) e nos 3 ensaios seguintes (19:08:25, 19:12:28, 19:20:31); depois deixei de matar | `B20-log-d13.txt`, `B21-sonda-d13.txt`, `B22-kills-d13.txt`, `B25-resumo-d13-e-espera.txt`, `B24-marcar-streaming-p1.png` | CORRIGIDO: só a 1ª carga derruba o DNS (19:06:19–19:06:21); nos 3 ensaios em portas alternativas, **0** rodadas sem resposta; espera de 120 → 240 → 480 → 960 s; a candidata entra às 19:36:36 quando deixa de falhar (def. 8 mantido) e `youtube.com` passa a NXDOMAIN em p1 |
| B.8 Lista alterada durante a espera | Com a nova tentativa pendente, incluí `bing.com` na blocklist de p1 às 19:12:53 | `B30-add-bing-durante-espera.log`/`.png`, `B31-sonda-lista-durante-espera.txt`, `B25` | FALHA: a tela diz "Salvo"; o arquivo do perfil tem `*.bing.com`, mas `bing.com` resolve nas 400 rodadas (até 19:20:17), com 0 "listas recarregadas". Só passa a NXDOMAIN às 19:36:36, 23 min 43 s depois (defeito 16) |
| B.9 Queda do Blocky durante a espera | SIGKILL na instância boa em uso às 19:20:40, com a nova tentativa marcada para 960 s | `B32-kill-boa-durante-espera.txt`, `B21-sonda-d13.txt`, `B20-log-d13.txt`, `B25` | FALHA: sem nenhuma linha "Blocky caiu"; nenhum perfil tem DNS de 19:20:41 a 19:36:33 (15 min 52 s), até a nova tentativa agendada (defeito 15). Em B.6 o mesmo SIGKILL se recupera em 9 s |
| B.10 Container parado | `docker stop` / `docker start webfilter-dns` | `B50-container-parado.txt` | OK: conexão recusada; volta filtrando em 3 s; no host só a 853/TCP (a 53 é do systemd-resolved) |
| B.11 Perfil desativado | Desativei o Kiosk Total pela tela | `C10-desativar.txt` | OK: p11 resolve roblox, youtube e cloudflare-dns; volta a bloquear ao reativar (`C20-reativar.txt`) |
| C.1 Sync inicial (amostra) | Sync de R9XT200AMYY | `C01-sync-inicial.txt`, `C-sync-01-inicial.json` | OK: assinatura confere; `webfilterDnsHost=id-c1-p11…`; 15 pacotes em locked, sem launcher, remote ou chrome; `unlocked_packages=br.com.empresa.jogo` (histórico do t2) |
| C.2 Desativar e reativar (amostra) | Desativei, contei os push, fiz sync, reativei e fiz sync | `C10-desativar.txt`, `C20-reativar.txt`, `C-sync-10/20*.json` | OK: 4 `configUpdated`, só para os 4 aparelhos do perfil 11; sem `webfilterDnsHost` e tudo em unlocked; ao reativar, as settings do launcher são iguais às iniciais e os demais campos não mudam; assinatura confere nos 3 syncs |
| D.1 APK | `aapt2 dump badging`, certificado e strings do dex do v1.0 e do v1.1 | `D01-apk.txt` | Conferido: versionCode 15380 > 15379; mesmo certificado SHA-256 `44:37:2F:…:D5:10`; dex com `webfilterDnsHost`, `setGlobalPrivateDnsModeSpecifiedHost`/`Opportunistic`, `disallow_config_private_dns` e `PrivateDnsDecision`/`Manager`; nenhuma fonte do launcher mais nova que o APK; o APK de `dist` é igual ao build padrão |
| D.2 Aplicação no tablet | — | — | BLOQUEADO: não há tablet matriculado no DEV |
| E.1 WAR e imagem | sha256 de `dist/hmdm.war`; extraí a WAR e a `ROOT.war` servida e comparei; procurei fontes mais novas; comparei `resolver.py` | `E01-war-imagem-qualidade.txt` | OK: `dist/hmdm.war` = `393d5380…c419` (o do pedido); a `ROOT.war` servida tem outro sha (`b1bae85b…`, reempacotada pelo container), mas 0 diferenças de conteúdo; nenhuma fonte mais nova; plugin da WAR = fonte; `resolver.py` da imagem = árvore |
| E.2 Qualidade | Procurei TODO, FIXME, stub e catch vazio; conferi a paridade do i18n e `<`/`>` nas mensagens | `E01-war-imagem-qualidade.txt` | OK: nada encontrado; 87 = 87 chaves; nenhuma mensagem com `<`/`>` |
| E.3 Runbook `docs/WEBFILTER.md` | Conferi cada afirmação contra o código e o comportamento observado | este parecer, `B*` | Coerente, com três ressalvas: (a) "Operação" diz que lista vale em segundos e que a configuração que não sobe não derruba o DNS em uso; durante a espera de nova tentativa isso não vale (defeitos 15 e 16); (b) faltam itens exigidos pela tarefa 10.1, ainda aberta (ver Observações); (c) o aviso de "não faça só a troca a quente" confere com B.2 |

## Status dos defeitos anteriores

| # | Defeito | Status | Evidência |
|---|---|---|---|
| 1 | Sem permissão vê e abre o Web Filter | Corrigido (não regrediu) | `A10-sem-permissao.log`, `A10`/`A11*.png` |
| 2 | Escritores antigos reiniciam o resolvedor | **Corrigido na causa**. Após a recarga a quente, a instância antiga parou de gravar e registrou isso. 13,5 min sem reinício nem falha de DNS. O runbook exige reinício completo | `B01`–`B06` |
| 3 | HTTP 200 no lugar de 400/403/404 | Corrigido (não regrediu) | `A10`, `A20`, `A40` |
| 4 | Contraste do erro e do selo | Corrigido (não regrediu): nenhum texto do plugin abaixo de 4,5 no escuro | `A20-validacao-temas-larguras.log` |
| 5 | mainApp aceito na blocklist | Corrigido (não regrediu) | `A20-…log`, `A2E-dark-1280-kiosk-chrome.png` |
| 6 | Pai de domínio protegido recusado | Corrigido (não regrediu) | `B39-sequencia.log`, `B41-sonda-seq.txt` |
| 7 | Categorias sem descrição | Corrigido (não regrediu) | `A01-navegar.log`, `A02-editor.png` |
| 8 | Falha transitória descarta configuração válida | Corrigido (não regrediu): a candidata entrou quando deixou de falhar | `B22`, `B20` (19:36:36) |
| 9 | O que roda ≠ entrega | Corrigido (não regrediu) | `E01-war-imagem-qualidade.txt`, `D01-apk.txt` |
| 10 | Core alterado fora do proposal | Resolvido por decisão humana 1 | `A01`, `A41` |
| 11 | Descrição do `doh` abaixo de 4,5 | **Corrigido**: 6,31 no escuro e 4,97 no claro, nas 3 larguras | `A50-ajuda-idiomas-e-doh.log`, `A51-doh-*.png` |
| 12 | Faixa e aviso mostram valor não salvo | **Corrigido**: a faixa e o aviso seguem o valor salvo (após erro 400, campo apagado, salvamento vazio, F5 e restauração) | `A20-…log`, `A60-dominio-vazio.log`, `A60`/`A61*.png` |
| 13 | Nova tentativa derruba o resolvedor | **Corrigido** no critério definido: 3 ensaios com falha e 0 lacunas depois da primeira volta à configuração boa. Porém a espera entre as tentativas criou os defeitos 15 e 16 | `B20`–`B25` |
| 14 | Texto de ajuda truncado | **Corrigido** em pt-BR e en-US, também após F5 | `A50-*` |

## Veredito por bloco

- **A — Console: ACEITA**
- **B — Resolvedor DNS: REPROVADA** (defeitos 15 e 16)
- **C — Sync do aparelho: ACEITA** (amostra declarada: sync inicial, desativar/reativar com push, assinatura)
- **D — Launcher: BLOQUEADA**: falta tablet matriculado no DEV. O APK foi conferido (versionCode, certificado, classes e strings no dex, build atualizado), mas a aplicação do DNS privado só se prova num aparelho.
- **E — Qualidade e escopo: ACEITA**

## Defeitos novos

15. **Se o Blocky em uso cai enquanto uma nova tentativa está agendada, o supervisor não o reinicia, e todos os perfis ficam sem DNS até a hora da nova tentativa (até 1 h).**
    - Evidência: `B32-kill-boa-durante-espera.txt`, `B21-sonda-d13.txt`, `B20-log-d13.txt` e `B25-resumo-d13-e-espera.txt`.
      - Às 19:20:40, com a nova tentativa marcada para 960 s, um SIGKILL na instância boa deixou a porta 853 sem resposta de 19:20:41 a 19:36:33 (15 min 52 s), sem nenhuma linha "Blocky caiu".
      - Sem nova tentativa pendente, o mesmo SIGKILL se recupera em 9 s (`B40`/`B41`, 19:37:28–19:37:37).
    - Causa lida: `resolver.py:278-283`. Com `current != running_hash` e `time.time() < self.retry_at`, `reconcile` retorna antes da verificação `self.proc.poll()` ("Blocky caiu", linhas 313-316). A espera cresce até `RETRY_MAX_SECONDS=3600`.
    - Justamente nesse período a falha é mais provável: a espera nasce de uma carga que falhou, por exemplo por falta de memória.
    - Esperado:
      - classification "Configuração inválida não derruba o resolvedor" (o resolvedor volta a operar com a última configuração válida);
      - D8 (o supervisor mantém o Blocky rodando em modo fail-closed);
      - o runbook, "Configuração que não sobe: o resolvedor volta para a última configuração boa… sem derrubar o DNS em uso".
    - Reaprovação: com uma nova tentativa pendente, o SIGKILL no Blocky em uso é seguido de "Blocky caiu" e de DNS filtrando de volta em segundos, como sem espera pendente. Depois disso a nova tentativa continua agendada e ensaiada sem lacuna (critério do defeito 13) e a candidata entra quando deixa de falhar (defeito 8).

16. **Allowlist ou blocklist salva durante a espera de nova tentativa não é aplicada até a candidata entrar (medido: 23 min 43 s; pode chegar a mais de 1 h), enquanto a tela diz "Salvo".**
    - Evidência: `B30-add-bing-durante-espera.log`/`.png`, `B31-sonda-lista-durante-espera.txt` e `B25-resumo-d13-e-espera.txt`.
      - `bing.com` foi incluído na blocklist de p1 pela tela às 19:12:53. A resposta foi HTTP 200 "Salvo. Os tablets do perfil foram avisados…" e `c1-p1-deny.txt` já continha `*.bing.com`.
      - Mesmo assim, a sonda recebeu RESOLVE nas 400 rodadas, até 19:20:17, com 0 "listas recarregadas".
      - O bloqueio só começou às 19:36:36, quando a candidata entrou.
    - Causa lida: é o mesmo retorno antecipado de `resolver.py:282-283`. O `api_refresh()` (linhas 317-320) nunca é alcançado enquanto `blocky.yml` difere do que está rodando.
    - Esperado:
      - classification "Domínio adicionado à blocklist" (NXDOMAIN em até 5 minutos, sem reinício);
      - D8 (mudança só de `profiles/` → `POST /api/lists/refresh`);
      - o runbook, "Alteração de allowlist/blocklist… vale em segundos".
    - Reaprovação: com uma nova tentativa pendente, uma inclusão e uma remoção feitas pela tela na blocklist de um perfil passam a valer na instância em uso em até 5 minutos (o esperado são segundos), com "listas recarregadas sem reinicio" e sem lacuna na sonda. O fail-closed e os critérios dos defeitos 8 e 13 continuam valendo.

### Observações (não bloqueiam isoladamente)

- **Tema claro, núcleo:**
  - A aba ativa (`.kiosk-tab-active`, `var(--hwmdm-accent)` = rgb(14,165,183) sobre rgb(243,246,248)) tem 2,73:1. O estilo vem de `css/main.css` do núcleo, é o mesmo da tela Quiosque e usa a mesma cor de destaque dos `.btn-primary` da decisão 3.
  - O placeholder de `form-control` tem 2,85:1.
  - O cenário quantificado da spec (≥ 4,5:1) é o do tema escuro. Por isso registrei como observação para o responsável, sem reprovar. Se o responsável quiser o tema claro com a mesma régua, isso vira defeito do núcleo.
- `.btn-primary` tem 2,96:1 no escuro (decisão 3). O placeholder tem 4,41:1 no escuro (núcleo).
- **Runbook incompleto diante da tarefa 10.1 (aberta) e dos riscos de `design.md`:** não traz os limites (IP literal, portal cativo de Wi-Fi, Android anterior ao 10, app não listado), a memória medida na tarefa 1.6, o ensaio em homologação do D12 nem o commit/tag no backup.
- No passo 5 do runbook, `scripts/publicar-apk.sh` sem `--apk` publica o build padrão. Hoje o build padrão é idêntico a `dist/hmdm-v1.1-webfilter.apk` (mesmo sha256), mas o runbook deveria dizer `--apk dist/hmdm-v1.1-webfilter.apk`.
- **Base URL por IP:** no DEV o `base.url` é um IP, então nenhuma entrada do host do MDM entra na allowlist gerada; só `*.filtro.hwmdm.dev.local` entra. Em produção com nome de domínio, a entrada deve aparecer. Não testado lá.
- **Memória durante o ensaio:** o ensaio em portas alternativas roda um segundo Blocky ao lado do que está em uso. Com as listas atuais, o pico ficou baixo (`B23-memoria-d13.txt`). Com `adult` nas duas instâncias (cerca de 690 MiB cada, medido no t2) e o limite de 1,5 GiB, pode haver OOM da instância em uso, o que cai no defeito 15. Não provoquei esse caso.
- A recarga a quente deixou a instância antiga do MDM viva em memória (esperado; ver runbook). Recomenda-se reiniciar o container do MDM no DEV.
- Em 360 px, o "Editar" da lista só aparece rolando a tabela. A chave de DEV continua no histórico publicado do git (observações do t2, mantidas).

Veredito geral: REPROVADA — A ACEITA, B REPROVADA (defeitos 15 e 16), C ACEITA, D BLOQUEADA (sem tablet matriculado no DEV), E ACEITA.
