# Parecer — Web Filter no DEV — tentativa 2

Validador: VAL-WEBFILTER-DEV-t2
Executor: sessao-principal
Evidencia: /opt/projetos/hwmdm/evidencias/webfilter/validador-t2/

- Data: 2026-09-18, 18:02–18:40 (horário de Brasília).
- Ambiente: somente DEV (`http://localhost:8080`, 192.168.1.65). Nenhum comando meu acessou o servidor de produção.
- Pedido: `fiscal/fila/para-validador/WEBFILTER-DEV-t2.md`, com as regras do t1 e o parecer `pareceres/WEBFILTER-DEV-t1.md`. O relatório do executor foi tratado como alegação.
- Decisões humanas respeitadas:
  - (1) item próprio no menu e correção do F5 em `app.js`;
  - (2) commit e versionamento são do responsável;
  - (3) `.btn-primary` do núcleo fica como observação;
  - (4) alterações de terceiros em paralelo.
- Método:
  - Chromium (Playwright) dirigido só por gestos de pessoa: login pela tela, clique no menu, digitação, F5, botão voltar do navegador, botão de tema.
  - Chamadas de API feitas pelo próprio navegador logado (mesma credencial da sessão), para ler os códigos HTTP.
  - Sonda DoT com SNI por perfil. Uso uma cópia, `roteiros/consulta_dot_ns.py`, que mostra também a seção de autoridade: resposta bloqueada pelo Blocky = NXDOMAIN com 1 registro de autoridade.
  - Sync simulado com `X-Request-Signature`, conferindo `X-Response-Signature` pela regra do launcher.
  - Banco somente leitura. Roteiros descartáveis em `validador-t2/roteiros/`.
- Estado inicial e final:
  - Arquivos: `00-estado-inicial-db.txt`, `Z-estado-final-db.txt` e `Z-dns-final.txt`.
  - Políticas, categorias, listas, categorização de apps, domínio DNS e plugins ativos voltaram ao estado inicial, pela tela.
  - O `blocky.yml` final tem o mesmo sha256 do inicial (`e3c3df9b…`).
  - Efeitos que não se desfazem pela tela: o histórico `plugin_webfilter_locked_history` da política 2 ganhou `br.com.empresa.jogo` (teste C.3). Por isso o sync do perfil 11 passa a trazer `unlocked_packages=br.com.empresa.jogo`, o que é o comportamento especificado.
  - Houve 4 `configUpdated` a mais por salvamento do perfil 11.
- Conta sem permissão: `v.observer.t1` (Observer, id 333), criada no t1, reutilizada sem alteração.
- O endereço de produção apareceu no navegador do DEV como origem do favicon (requisição recusada). Essa configuração é do núcleo e fica fora do escopo. O endereço foi mascarado em `A10-sem-permissao.log`.

## Tabela de evidências

| Item | O que fiz | Onde está a evidência | Resultado |
|---|---|---|---|
| A.1 Menu, título, abas, textos | Entrei com `f.webfilter` (Admin) e cliquei em "Web Filter" no menu lateral | `A01-navegar.log`, `A01-lista.png` | OK: título "Web Filter" igual ao item do menu, 3 abas em português, faixa de status, 3 perfis |
| A.2 Categorias com descrição e `doh` fixa (def. 7) | Abri o editor de "Common - Minimal" | `A01-navegar.log`, `A02-editor.png` | OK: 14 categorias com nome e descrição em português; `doh` marcada e desabilitada |
| A.3 F5, Voltar e voltar do navegador | F5 no editor, "Voltar ao Web Filter", ida a Dispositivos e voltar do navegador | `A01-navegar.log`, `A03-apos-F5-editor.png` | OK: F5 mantém `#/plugin-webfilter` (o editor fecha e volta à lista); Voltar leva à lista; voltar do navegador retorna ao Web Filter |
| A.4 Carga dupla do módulo | Contei as requisições de `webfilter.module.js` | `A01-navegar.log`, `E02-war-git-chave-escritores.txt` | OK: 1 carga por carregamento de página (1 após o login e 1 após o F5); `index.html` não carrega mais o módulo |
| A.5 Outras telas | Abri Dispositivos, Perfis, Aplicativos, Plugins e Usuários e dei F5 em cada uma | `A01-navegar.log`, `A04-tela-*.png` | OK: cada tela permanece após o F5, sem erro de JavaScript (só o favicon externo recusado) |
| A.6 Usuário sem permissão (def. 1 e 3) | Entrei como `v.observer.t1`, li o menu, digitei `#/plugin-webfilter` e chamei 8 endpoints pelo navegador dele | `A10-sem-permissao.log`, `A10-menu-observer.png`, `A11-observer-url-digitada.png` | OK: sem "Web Filter" no menu; a URL digitada mostra "Você não tem permissão…" e não carrega o controlador; GET, PUT e DELETE respondem **HTTP 403** |
| A.7 Validação (def. 3) | Digitei `*.com`, `facebook.com` nas duas listas, `exemplo com`, `com.br`, `com.hmdm.launcher` e `candy crush` e salvei; reabri a política | `A20-validacao-escuro.log`, `A2E-1280-editor-erros.png` | OK: **HTTP 400**, 6 erros junto aos campos, "Nada foi salvo." e política reaberta idêntica |
| A.8 App de quiosque na blocklist (def. 5) | Incluí `com.android.chrome` (mainApp do Kiosk Total) na blocklist e salvei; reabri | `A20-validacao-escuro.log`, `A2E-1280-kiosk-chrome.png` | OK: HTTP 400 "app principal do quiosque deste perfil, não pode ser bloqueado"; nada salvo; os bloqueados mostrados não contêm o chrome |
| A.9 App de quiosque em categoria bloqueada | Na aba Aplicativos, categorizei `com.android.chrome` em Jogos (bloqueada no perfil 11) | `C30-apps.log` | OK: a tela não o lista como bloqueado e o sync não o põe em `locked_packages` |
| A.10 Aba Aplicativos | Incluí e removi `br.com.empresa.jogo` e tentei `com.hmdm.launcher` | `C30-apps.log`, `A20-validacao-escuro.log` | OK: incluir e remover funcionam; o protegido responde HTTP 400 com mensagem |
| A.11 Domínio inválido e 404 | Salvei `dns empresa`; pedi o perfil 99999 e o app 99999 | `A20-validacao-escuro.log`, `A40-404-e-plugins.log` | OK no servidor: HTTP 400 e domínio salvo mantido (confirmado após F5); **HTTP 404** para perfil e app inexistentes. FALHA na tela: a faixa de status mostra "dns empresa" como domínio do filtro (defeito 12) |
| A.12 Aviso de domínio DNS vazio | Apaguei o campo sem salvar; depois F5 | `A20-validacao-escuro.log`, `A2E-1280-dominio-apagado-sem-salvar.png` | O aviso existe, mas segue o campo digitado e não o valor salvo: aparece sem nada ter sido salvo e some após o F5 (defeito 12). Não apaguei o domínio salvo, para não derrubar o hostname dos perfis |
| A.13 Texto de ajuda da aba Configuração | Li a tela | `A2E-1280-configuracao-erro.png` | FALHA: aparece "id-c-p.." e "*. apontando", porque `<cliente>`, `<perfil>` e `<domínio>` são engolidos como HTML (defeito 14) |
| A.14 Plugin desativado para o cliente | Na tela Plugins, desliguei "Web Filter" e salvei; F5; religuei | `A41-plugin-desativado.log`, `A41-*.png`, `A42-*.png` | OK: o item some do menu e volta ao religar; `pluginsdisabled` voltou ao estado inicial |
| A.15 Contraste no tema escuro (def. 4), 3 larguras | Botão de tema; mediu-se todo texto visível de lista, editor, editor com erros, Aplicativos (com erro) e Configuração (com erro) em 1280, 768 e 360 px; também no tema claro | `A20-validacao-escuro.log`, `A21-validacao-escuro-768-360.log`, `A30-contraste-medidas.log`, `A2E-*.png` | Erro de campo 7,15:1; selo "Ativo" 7,52:1; "Inativo" 9,96:1. FALHA: descrição da categoria `doh` com 4,17:1 no escuro e 3,06:1 no claro (defeito 11). Botões primários com 2,96:1 (núcleo, decisão 3). Placeholder `form-control` com 4,41:1 (observação) |
| A.16 Larguras 360/768/1280 | `scrollWidth` × `innerWidth` em cada aba e estado | mesmos logs | OK: nenhum estouro. Em 360 px, o botão "Editar" fica fora da janela dentro da tabela rolável (observação do t1, mantida) |
| B.1 Filtro por SNI | Consultei 8 domínios reais com os SNIs de p1, p11, p2 (sem política) e sem SNI | `B30-filtro-por-sni.txt` | OK: categoria bloqueada, subdomínio de `doh` e blocklist dão NXDOMAIN; a allowlist vence a categoria; perfis isolados; p2 e sem SNI resolvem tudo |
| B.2 Sem ação por 14 min (def. 2) | Não alterei política de 18:02:25 a 18:16:24; sonda DoT a cada 2 s e `docker logs -f` do resolvedor | `B01-inicio-observacao.txt`, `B02-log-passivo-14min.txt` (vazio), `B03-sonda-passiva-14min.txt`, `B04-resumo-janela-passiva.txt` | OK no DEV: 392 consultas sem falha nem lacuna; zero linhas no log (nem reinício, nem refresh). O plugin regravou às 18:10:27 com o mesmo conteúdo e sha igual |
| B.3 Mudança só de lista (def. 2) | Pela tela: +wikipedia.org, +bing.com, −wikipedia.org, −bing.com; depois +`*.hwmdm.dev.local`, +bing.com, allow +www.bing.com e as três remoções | `B10-log-mudancas-lista.txt`, `B11-sonda-mudancas-lista.txt`, `B12`–`B15*.png` | OK: 10 de 10 com "listas recarregadas sem reinicio", nenhum "iniciando Blocky", efeito em cerca de 1 s e sonda sem lacuna |
| B.4 Causa do defeito 2 | Li o código e o log do Tomcat | `E02-war-git-chave-escritores.txt` | NÃO CORRIGIDA: a tarefa de 10 min do plugin continua sem parada na descarga do contexto (o núcleo só a encerra no desligamento da JVM). Não há runbook que exija reinício do container: `docs/WEBFILTER.md` não existe. O próprio DEV fez recarga a quente às 17:58:59 |
| B.5 Pai de domínio protegido (def. 6) | Incluí `*.hwmdm.dev.local` na blocklist e conferi os arquivos gerados. Para provar a mesma precedência com domínio real: blocklist `bing.com` e allowlist `www.bing.com` | `B21-pai-protegido.log`, `B24-pai-e-filho-permitido.txt`, `B20-antes-pai-protegido.txt` | OK: aceito; deny tem `*.hwmdm.dev.local` e allow tem `*.filtro.hwmdm.dev.local`. `bing.com` e `cn.bing.com` bloqueados e `www.bing.com` resolvido em p1; tudo resolve em p2. Nomes `.local` não servem de prova direta, porque o Blocky os responde como uso especial |
| B.6 Mudança de categoria | Marquei e desmarquei streaming e adult em p1 | `B40`/`B41`, `B44`/`B45`, `B50-log-adult.txt`, `B51-sonda-adult.txt`, `B53-memoria-adult.txt` | OK (fail-closed, nenhuma resposta sem filtro): troca com listas pequenas em cerca de 1–3 s; com `adult` (140 MB), 23 s sem DNS para todos os perfis; RSS 688 MiB de 1,5 GiB |
| B.7 Queda do Blocky em execução | SIGKILL no Blocky já pronto (2×) | `B40-log-queda-carga.txt`, `B41-sonda-queda-carga.txt`, `B43-kills.txt` | OK: "Blocky caiu", volta com a configuração nova em cerca de 9 s, sem resposta sem filtro |
| B.8 Queda durante a carga (def. 8) | SIGKILL 0,5 s após iniciar a candidata, na 1ª tentativa e na nova tentativa | `B44-log-queda-na-carga.txt`, `B45-sonda-queda-na-carga.txt`, `B46-kills-na-carga.txt` | CORRIGIDO: volta à última boa, tenta de novo a cada 120 s e a 3ª tentativa põe a regra nova em vigor (youtube passa a resolver às 18:31:49). FALHA NOVA: cada nova tentativa derruba a instância boa (18:29:43–18:29:46 sem DNS) (defeito 13) |
| B.9 Container parado | `docker stop` / `docker start webfilter-dns` | `B60-queda-container.txt`, `B61-sonda-volta.txt`, `B62-portas.txt` | OK: conexão recusada com o container parado; volta filtrando em cerca de 2 s; no host só a 853/TCP (a 53 é do systemd-resolved) |
| B.10 Perfil desativado | Desativei o Kiosk Total pela tela | `C10-desativar.log`, `B63-perfil-desativado.txt` | OK: p11 resolve roblox, youtube e cloudflare-dns |
| C.1 Sync inicial | Sync de R9XT200AMYY (perfil 11) | `C01-sync-inicial.txt`, `C-sync-01-inicial.json` | OK: assinatura confere; `webfilterDnsHost=id-c1-p11.filtro.hwmdm.dev.local`; 15 pacotes em `locked_packages`, sem launcher, remote ou chrome |
| C.2 Desativar, push e demais campos | Desativei pela tela, contei `configUpdated` e fiz sync | `C10-desativar.log`, `C11-sync-desativado.txt`, `C12-comparacao-campos.txt` | OK: 4 pushes, só para os 4 aparelhos do perfil 11; sem `webfilterDnsHost`; tudo em `unlocked_packages`; nenhum outro campo difere; assinatura confere |
| C.3 Reativar, pacote do cliente, allowlist | Reativei; categorizei `br.com.empresa.jogo`; allowlist de `com.roblox.client`; removi; syncs | `C20-reativar.log`, `C21`, `C30-apps.log`, `C32`, `C40-allow.log`, `C41`, `C50-sync-final.txt` | OK: pacote do cliente em locked; allowlist tira de locked e põe em unlocked; ao remover, o pacote vai para unlocked; assinatura sempre confere |
| D.1 APK | `aapt dump badging`, certificado `CERT.RSA` e strings do dex do v1.0 e do v1.1 | `D01-apk.txt` | Conferido: versionCode 15380 > 15379; mesmo certificado SHA-256 `44:37:2F:…:D5:10`; dex com `webfilterDnsHost`, `setGlobalPrivateDnsModeSpecifiedHost`/`Opportunistic`, `disallow_config_private_dns` e `PrivateDnsDecision`/`Manager`; APK (17:22) mais novo que as fontes do launcher (17:15–17:21) |
| D.2 Aplicação no tablet | — | — | BLOQUEADO: não há tablet matriculado no DEV |
| E.1 WAR servida | sha256 de `dist/hmdm.war`; extraí a WAR e o `webapps/ROOT.war` servido e comparei | `E02-war-git-chave-escritores.txt` | OK: sha256 `3eb58b07…137a5` igual ao do pedido; 0 diferenças entre a WAR e a servida; nenhuma fonte mais nova que a WAR; `resolver.py` da imagem igual ao da árvore |
| E.2 Chave TLS | `git status`, `check-ignore` e `git log` | `E02-war-git-chave-escritores.txt` | OK na árvore: `key.pem` e `cert.pem` removidos do índice (staged) e ignorados. Observação: a chave continua no histórico publicado (`9852d24b`, `c1433e71` em `origin/main-v2`, commits do responsável) |
| E.3 Qualidade | Procurei TODO, FIXME, stub, catch vazio e código morto no plugin, no resolvedor e no launcher; conferi a paridade do i18n | este parecer | OK: nada encontrado; en_US e pt_PT com as mesmas 87 chaves |
| E.4 D2, D4, D6, D7, D8, D9 | Conferi no sync, na sonda, nos arquivos gerados e nas restrições do banco | `C*`, `B24`, `E01-d7-restricoes.txt` | OK: D2 (mescla e histórico), D4 (formato do hostname), D6 (protegidos no allow e mainApp recusado), D7 (FK cascade e unicidades), D8 (arquivos e refresh sem reinício), D9 (hook com assinatura válida). D13: decisão humana 1 |

## Status dos defeitos do t1

| # | Defeito | Status | Evidência |
|---|---|---|---|
| 1 | Sem permissão vê e abre o Web Filter | **Corrigido** | `A10-sem-permissao.log`, `A10`/`A11*.png` |
| 2 | Reinícios sozinhos (escritores antigos) | **Não corrigido na causa**. O sintoma sumiu no DEV após o reinício completo: 14 min sem reinício e 10 de 10 mudanças de lista sem reinício. O critério (a) não foi atendido: o plugin continua deixando a tarefa viva numa recarga a quente, que o próprio DEV fez às 17:58:59, e nenhum runbook exige reinício | `B01`–`B04`, `B10`/`B11`, `E02-war-git-chave-escritores.txt` |
| 3 | HTTP 200 no lugar de 400/403/404 | **Corrigido** | `A10-sem-permissao.log` (403), `A20-validacao-escuro.log` (400), `A40-404-e-plugins.log` (404) |
| 4 | Contraste do erro e do selo | **Corrigido** para erro (7,15) e selos (7,52 e 9,96). Surgiu o defeito novo 11 | `A30-contraste-medidas.log` |
| 5 | mainApp aceito na blocklist | **Corrigido** | `A20-validacao-escuro.log`, `A2E-1280-kiosk-chrome.png` |
| 6 | Pai de domínio protegido recusado | **Corrigido** | `B21-pai-protegido.log`, `B24-pai-e-filho-permitido.txt` |
| 7 | Categorias sem descrição | **Corrigido** | `A01-navegar.log`, `A02-editor.png` |
| 8 | Queda transitória descarta configuração válida | **Corrigido**. A correção introduziu o defeito 13 | `B44`–`B46` |
| 9 | O que roda ≠ entrega | **Corrigido** no que cabe ao executor (decisão 2): a WAR servida é igual a `dist/hmdm.war` (sha do pedido), o resolvedor é igual à árvore e o APK é igual a `dist/hmdm-v1.1-webfilter.apk`. A chave saiu do índice | `E02-war-git-chave-escritores.txt`, `D01-apk.txt` |
| 10 | Core alterado fora do proposal | **Resolvido por decisão humana 1**. O menu respeita a permissão e o plugin ativo; o F5 funciona sem quebrar as outras telas; a carga dupla foi removida | `A01-navegar.log`, `A41-plugin-desativado.log` |

## Veredito por bloco

- **A — Console: REPROVADA** (defeitos 11, 12, 14)
- **B — Resolvedor DNS: REPROVADA** (defeito 2 na causa, defeito 13)
- **C — Sync do aparelho: ACEITA**
- **D — Launcher: BLOQUEADA** — falta tablet matriculado no DEV. O APK foi conferido (versionCode, certificado e classes no dex), mas a aplicação do DNS privado não foi provada em aparelho, e leitura de código não substitui essa prova.
- **E — Qualidade e escopo: ACEITA**

## Defeitos novos

11. **Descrição da categoria `doh` abaixo de 4,5:1.**
    - Evidência: `A30-contraste-medidas.log`, `A20`/`A21-*.log` (1280, 768 e 360 px). "Servidores DNS externos que contornariam o filtro. Sempre bloqueada." tem 4,17:1 no tema escuro e 3,06:1 no claro. A causa lida é `.wf-cat-required { opacity: .75 }` em `views/main.html`, aplicada sobre `--hwmdm-muted`.
    - Esperado: admin-console "Tema escuro" (todo texto visível ≥ 4,5:1). O texto é informativo, não é só rótulo de controle inativo.
    - Reaprovação: o editor aberto, nas 3 larguras e nos dois temas, mede ≥ 4,5:1 em todo texto, inclusive no cartão `doh`.

12. **A aba Configuração mostra como "domínio do filtro" um valor que não foi salvo.**
    - Evidência: `A20-validacao-escuro.log`, `A2E-1280-configuracao-erro.png` e `A2E-1280-dominio-apagado-sem-salvar.png`.
      - Depois do HTTP 400 para `dns empresa`, a faixa mostra "DOMÍNIO DNS DO FILTRO dns empresa", embora o salvo continue `filtro.hwmdm.dev.local` (confirmado após F5).
      - Apagar o campo sem salvar faz aparecer o aviso "não configurado" e a faixa "não configurado".
    - Causa lida: a faixa e o aviso usam `settings.dnsDomain`, o mesmo modelo do campo digitado.
    - Esperado: admin-console "Domínio inválido" (o domínio anterior permanece) e "Salvar domínio válido" (o aviso deixa de ser exibido após salvar). A tela não pode informar ao administrador um estado que não existe.
    - Reaprovação: após um 400, a faixa continua com o domínio salvo; o aviso só muda depois de um salvamento bem-sucedido ou de um F5 que reflita o banco.

13. **Cada nova tentativa de uma configuração que falha derruba o resolvedor para todos os perfis, a cada 120 s, sem fim.**
    - Evidência: `B44-log-queda-na-carga.txt` e `B45-sonda-queda-na-carga.txt`. Às 18:29:42, com a última configuração boa servindo, o supervisor parou a instância boa para tentar a candidata. A sonda ficou sem resposta de 18:29:43 a 18:29:46, para todos os perfis.
    - Causa lida: `resolver.py:211` (`_start` chama `self.stop()` antes de subir a candidata) e `:240` (nova tentativa a cada `RETRY_FAILED_SECONDS=120`, sem limite).
    - Cada ciclo custa a carga da candidata (até `HEALTH_TIMEOUT=180` s) mais a recarga da boa. Com `adult`, a carga medida foi de 23 s (`B50`/`B51`). Um OOM persistente ou uma configuração realmente inválida deixaria a frota sem DNS por cerca de 46 s a cada cerca de 2,8 min, indefinidamente.
    - Esperado: classification "Configuração inválida não derruba o resolvedor" e D8 (voltar à última boa sem deixar o serviço cair).
    - Reaprovação: com uma candidata que falhe em toda tentativa (por exemplo, SIGKILL em cada carga), a sonda DoT não mostra lacuna depois da primeira volta à configuração boa, por pelo menos 3 ciclos de nova tentativa. Continuam valendo o defeito 8 (a candidata válida entra quando deixa de falhar) e o fail-closed.

14. **Texto de ajuda da aba Configuração truncado.**
    - Evidência: `A2E-1280-configuracao-erro.png`. A tela mostra "Cada perfil recebe o DNS privado id-c-p.. Exige DNS curinga *. apontando…".
    - Causa lida: a chave `plugin.webfilter.dns.domain.hint` (pt_PT e en_US) contém `<cliente>`, `<perfil>` e `<domínio>`, que a diretiva `localized` insere como HTML e o navegador descarta.
    - Esperado: admin-console "Título e idioma" (rótulos e mensagens legíveis em português).
    - Reaprovação: a aba mostra o formato completo do hostname, por exemplo `id-c<cliente>-p<perfil>.<domínio>`, nos dois idiomas.

### Observações (não bloqueiam isoladamente)

- Os botões `.btn-primary` têm 2,96:1 no tema escuro. É estilo do núcleo (decisão humana 3).
- O placeholder de `form-control` tem 4,41:1 no escuro (aba Aplicativos). A cor também é do núcleo.
- Em 360 px, o "Editar" da lista só aparece rolando a tabela. O link "< Plugins" acima da tela continua levando a `#/extensions`.
- A chave privada de DEV continua no histórico publicado em `origin/main-v2`. Commits são decisão do responsável; recomenda-se tratá-la como exposta e nunca reutilizá-la.
- O tempo sem DNS numa troca de categoria depende do tamanho das listas: 23 s com `adult`. O gate de 60 s da tarefa 1.6 (catálogo completo) segue em aberto.
- Na tela Plugins, "Web Filter" aparece também como cartão em "Consultar por dispositivo". Não foi avaliado.

Veredito geral: REPROVADA — A REPROVADA, B REPROVADA, C ACEITA, D BLOQUEADA (sem tablet matriculado no DEV), E ACEITA.
