# Parecer — Web Filter no DEV — tentativa 1

Validador: VAL-WEBFILTER-DEV-t1
Executor: sessao-principal
Evidencia: /opt/projetos/hwmdm/evidencias/webfilter/validador-t1/

- Data: 2026-09-18, 17:15–17:55 (horário de Brasília)
- Ambiente: somente DEV (`http://localhost:8080`, 192.168.1.65). O servidor de produção não foi acessado.
- Pedido: `fiscal/fila/para-validador/WEBFILTER-DEV-t1.md`. O relatório e as capturas do executor foram tratados como alegação e não serviram de prova.
- Método: navegador Chromium (Playwright) dirigido só por gestos de pessoa: login pela tela, cliques, digitação, F5 (`page.reload`) e botão voltar do navegador. Também foram usadas a sonda DoT do pedido (`dns/consulta_dot.py`) e o sync simulado com `X-Request-Signature`, conferindo `X-Response-Signature` pela regra do launcher. O banco foi consultado somente para leitura. Os roteiros descartáveis estão em `validador-t1/roteiros/`. Nenhum arquivo do repositório foi alterado além deste parecer.
- Estado encontrado: `00-estado-inicial-db.txt`. Estado deixado: `Z-estado-final-db.txt` e `Z-dns-apos-restaurar.txt`. As políticas, as categorias, as listas, a categorização e o domínio DNS voltaram ao que eram, pela tela.
  - O histórico `plugin_webfilter_locked_history` da política 2 ganhou os pacotes de `streaming` no primeiro sync simulado. É efeito normal do sistema: qualquer sync de aparelho do perfil faria o mesmo, e não há como desfazer pela tela.
  - Conta criada para o teste de permissão, pela tela de usuários: `v.observer.t1`, papel Observer, id 333. Ela permaneceu no DEV.

## Tabela de evidências

| Item | O que fiz | Onde está a evidência | Resultado |
|---|---|---|---|
| A.1 Menu e tela | Entrei com `f.webfilter` (Admin), cliquei em "Web Filter" no menu lateral e li título, abas e textos | `A01-explorar.log`, `A02-webfilter-lista.png` | OK: item presente, título "Web Filter" igual ao do menu, abas Políticas por perfil / Aplicativos / Configuração, textos em português |
| A.2 Editar, ativar, categorias, listas, salvar e reabrir | Editei "Common - Minimal", marquei `adult`, incluí `example.com` na blocklist e `instagram.com` na allowlist, salvei, voltei e reabri | `A03-salvar.log`, `A06-salvo.png`, `A07-lista-apos-salvar.png`, `A09-reaberto.png` | OK: confirmação exibida, lista com 4 categorias e 6 entradas, valores persistidos |
| A.3 `doh` fixa | Abri o editor | `A02-validacao.log`, `A03-editor-aberto.png` | OK: marcada e desabilitada |
| A.4 Erros de validação | Digitei `*.com` e `facebook.com` na blocklist, `com.br` e `facebook.com` na allowlist, `com.hmdm.launcher` na blocklist de apps e salvei; depois reabri | `A02-validacao.log`, `A04-erros-validacao.png`, `A05-lista-apos-erro.png` | OK: 4 erros junto aos campos, aviso "Nada foi salvo" e política reaberta idêntica. O servidor respondeu HTTP 200 com `status:ERROR`, e não 400 (defeito 3) |
| A.5 F5 | Recarreguei na lista e dentro do editor | `A03b-f5-editor.log`, `A08-apos-F5.png`, `A09b-F5-no-editor.png` | OK: permanece em `#/plugin-webfilter`. No editor, volta para a lista |
| A.6 Voltar | Usei "Voltar ao Web Filter", o link "< Plugins" do topo e o voltar do navegador | `A02-validacao.log`, `A03b-f5-editor.log`, `A10-voltar-plugins-topo.png` | OK: "Voltar ao Web Filter" leva à lista. Observação: o link "< Plugins" do console, visível acima do editor, leva a `#/extensions` |
| A.7 Aba Aplicativos | Incluí `br.com.empresa.jogo` em Jogos, tentei `com.hmdm.launcher` e removi o pacote do cliente | `A05-abas.log`, `A30`–`A32*.png` | OK: inclusão e remoção funcionam, o pacote protegido é recusado com mensagem e o catálogo inicial não tem botão de remover |
| A.8 Aba Configuração | Tentei salvar `dns empresa` e recarreguei | `A05-abas.log`, `A33`/`A34*.png` | OK: erro exibido e domínio anterior mantido. As três fontes aparecem com licença e link |
| A.9 Larguras 360/768/1280 | Abri lista, editor, Aplicativos e Configuração em cada largura | `A06-larguras-claro.log`, `A4C-*.png` | OK: `scrollWidth` igual a `innerWidth` em todas. Em 360 px, o botão "Editar" só aparece rolando a tabela na horizontal |
| A.10 Tema escuro | Cliquei no botão de tema e medi o contraste do texto visível, inclusive de uma mensagem de erro | `A06-larguras-escuro.log`, `A08-erro-escuro.log`, `A08-erro-tema-escuro.png`, `A07-contraste-erro-vermelho.txt` | FALHA: erro de campo com 2,88:1, selo "Ativo" com 2,48:1, botões primários com 2,96:1 (defeito 4) |
| A.11 Outras telas | Abri Dispositivos, Perfis de dispositivo, a edição de um perfil, Aplicativos e Plugins | `A09-outras-telas.log`, `A50-*.png`, `A51-*.png` | OK |
| A.12 Usuário sem permissão | Criei `v.observer.t1` (Observer, sem `plugin_webfilter_access`) pela tela, entrei com ele, li o menu, digitei a URL da tela e chamei a API pelo navegador dele | `A04-sem-permissao.log`, `A20`–`A23*.png`, `A-papeis-usuarios.txt` | FALHA: "Web Filter" aparece no menu e a tela abre (defeito 1). A API recusa, porém com HTTP 200 `error.permission.denied`, e não 403 (defeito 3) |
| A.13 App de quiosque na blocklist | Incluí `com.android.chrome` (mainApp do perfil Kiosk Total) na blocklist e salvei | `C-02-chrome-na-blocklist.log`, `C-02-chrome-na-blocklist.png` | FALHA: foi aceito sem erro e a tela lista o pacote como bloqueado (defeito 5) |
| A.14 Domínio pai de domínio protegido | Incluí `*.hwmdm.dev.local` na blocklist | `A10-pai-protegido.log`, `A60-*.png` | FALHA diante da spec: a entrada é recusada, quando a spec manda aceitá-la e manter permitido só o domínio protegido (defeito 6) |
| A.15 Descrição das categorias | Olhei o editor | `A03-editor-aberto.png` | FALHA: só aparecem os nomes, sem descrição (defeito 7) |
| B.1 Filtro por SNI | Fiz consultas DoT com os SNIs `id-c1-p1`, `id-c1-p11`, `id-c1-p2` (sem política) e sem SNI | `B04-dominios-nas-listas.txt`, `B05-consultas-dot.txt` | OK: categoria bloqueada dá NXDOMAIN, inclusive em subdomínios; a allowlist vence a categoria; a blocklist bloqueia; site comum resolve; `doh` bloqueada nos dois perfis; os perfis ficam isolados; perfil sem política e conexão sem SNI resolvem |
| B.2 Mudança de lista sem reinício | Pela tela, incluí `wikipedia.org` na blocklist (duas vezes, em momentos diferentes) | `B06-*.txt`, `B15-refresh-sem-reinicio.txt` | FALHA INTERMITENTE: às 17:35 o Blocky REINICIOU, com cerca de 18 s sem DNS. Às 17:51 houve refresh sem reinício, com efeito em menos de 2 s. Causa no defeito 2 |
| B.3 Mudança de categoria | Desmarquei Jogos pela tela e sondei `roblox.com` a cada segundo | `B07-*.txt` | OK: reinício sem responder durante a troca (fail-closed) e efeito em cerca de 17 s |
| B.4 Estabilidade sem ação do administrador | Sondei o DNS por 150 s sem mexer em nada | `B08-*.txt`, `B03-reinicios-blocky.txt`, `B09-blocky-apos-1739.yml`, `E01-contextos-zumbis-e-git.txt` | FALHA: às 17:39:07 o `blocky.yml` foi regravado com outro conteúdo, o Blocky reiniciou e ficou 17 s sem DNS. Isso se repete a cada poucos minutos (defeito 2) |
| B.5 Resolvedor parado | Parei o container `webfilter-dns`, consultei e subi de novo | `B10-queda.txt`, `B10-sonda-volta.txt` | OK: conexão recusada com o container parado; volta filtrando cerca de 16 s depois do start |
| B.6 Queda do processo Blocky | Matei o processo com SIGKILL dentro do container | `B11-*.txt` | Volta sozinho filtrando, mas marca como "configuração nova recusada" um `blocky.yml` válido e não tenta de novo (defeito 8) |
| B.7 Perfil desativado e portas | Desativei a política do Kiosk Total e conferi as portas do host | `B14-perfil-desativado.txt`, `B13-portas.txt`, `B12-memoria.txt` | OK: o perfil desativado resolve sem filtro; só a 853/TCP fica exposta; memória em 687 MiB de 1,5 GiB |
| C.1 Sync com política ativa | Simulei o sync do aparelho R9XT200AMYY (perfil 11) | `C-sync-01-inicial.*` | OK: `locked_packages` traz jogos, streaming e `com.whatsapp`; sem `com.hmdm.launcher`, `com.hwmdm.remote` ou `com.android.chrome` (mainApp); `webfilterDnsHost=id-c1-p11.filtro.hwmdm.dev.local`; assinatura confere |
| C.2 mainApp na blocklist | Sync depois de A.13 | `C-sync-02*` | OK no sync: o chrome não aparece em `locked_packages` |
| C.3 Allowlist | Incluí `com.roblox.client` na allowlist de apps e sincronizei | `C-sync-03*` | OK: sai de `locked_packages` e entra em `unlocked_packages` |
| C.4 Desativar política | Desativei pela tela e sincronizei | `C-sync-04*`, `C-05-comparacao-campos.txt` | OK: `unlocked_packages` com tudo o que foi bloqueado, sem `locked_packages`, sem `webfilterDnsHost`, assinatura confere, demais campos idênticos |
| C.5 Push | Contei `pushmessages` antes e depois de salvar o Kiosk Total | `C-08-push-antes-depois.txt` | OK: 4 `configUpdated`, um para cada aparelho do perfil 11 |
| C.6 Restauração | Reativei pela tela e sincronizei | `C-sync-06*`, `C-06-restaurar.log` | OK: igual ao estado inicial |
| D.1 APK | Rodei `aapt dump badging` e `apksigner verify --print-certs` no v1.0, no v1.1 e no 6.36 do DEV; procurei classes no dex | `D-apk.txt`, `D-launcher-em-uso.txt` | OK: versionCode 15380, maior que 15379 (v1.0) e que 15360 (6.36); mesmo certificado do v1.0 (SHA-256 44372f14…); dex contém `webfilterDnsHost`/`setGlobalPrivateDnsModeSpecifiedHost` |
| D.2 Aplicação no tablet | — | — | BLOQUEADO: não há tablet matriculado no DEV |
| E.1 Código implantado e contextos | Comparei o jar implantado com a fonte, li os logs do Tomcat e o SQL executado | `E01-contextos-zumbis-e-git.txt` | FALHA: 4 recargas a quente do contexto ROOT e 3 relógios de regeração do resolvedor vivos, com versões de código diferentes; mudanças da fonte ainda não commitadas (defeito 2 e defeito 9) |
| E.2 Escopo | Rodei `git diff HEAD~1 HEAD` no console do core | `E02-diff-console-core.txt` | FALHA: `content.html`, `app.js`, `index.html` e `hwmdm_modules.js` do core foram alterados, fora da lista fechada do proposal (Impact) e contra o D13 ("sem alterar o console") (defeito 10) |
| E.3 Qualidade | Procurei TODO, stub, catch vazio e valores chumbados; li todo o código do plugin, do resolvedor e do launcher | este parecer (seção de defeitos) | Sem TODO, stub ou catch vazio. Chumbados: cor `#c62828` (defeito 4), upstreams 1.1.1.1/8.8.8.8 (observação). Chave privada de DEV versionada (observação) |

## Veredito por bloco

- **A — Console: REPROVADA** (defeitos 1, 3, 4, 5, 6, 7)
- **B — Resolvedor DNS: REPROVADA** (defeitos 2, 8)
- **C — Sync do aparelho: ACEITA**
- **D — Launcher: BLOQUEADA** — falta tablet matriculado no DEV. O APK foi conferido (versionCode e assinatura), mas a aplicação do DNS privado não pode ser provada por leitura de código.
- **E — Qualidade e escopo: REPROVADA** (defeitos 2, 9, 10; o 4 e o 6 também afetam D13 e D6)

## Defeitos

1. **Usuário sem `plugin_webfilter_access` vê e abre o Web Filter.**
   - Evidência: `A04-sem-permissao.log` (menu do Observer contém "Web Filter"), `A22-menu-observer.png`, `A23-observer-url-direta.png` (tela aberta com "Sem direitos de execução", zero perfis e "não configurado"), `A-papeis-usuarios.txt` (Observer sem a permissão).
   - Causa lida: `content.html:60` mostra o item só por `functionsPlugins | filter:{identifier:'webfilter'}`, sem conferir a permissão.
   - Esperado (admin-console "Usuário sem permissão"): o menu não exibe "Web Filter".
   - Reaprovação: entrando como Observer ou Guest, o item não aparece no menu, e a URL `#/plugin-webfilter` digitada não mostra a tela do plugin. Entrando como Admin, o item continua aparecendo.

2. **Resolvedor reinicia sozinho a cada poucos minutos, com cerca de 17–18 s sem DNS para todos os perfis. Uma simples mudança de lista também chegou a reiniciar o Blocky.**
   - Evidência: `B08-sonda-sem-alteracao.txt` (17:39:10–17:39:27 sem resposta, sem nenhuma ação), `B06-sonda-wikipedia.txt` e `B06-log-resolvedor-wikipedia.txt` (mudança só de blocklist às 17:35:44 gerou "Terminating… iniciando Blocky" e 18 s sem DNS), `B03-reinicios-blocky.txt` (o hash alterna e3c3df9b / d3ee34c0 / 1235e086… sem mudança de política), `B09-blocky-apos-1739.yml` (`refreshPeriod: 0`, contra `0m` da fonte).
   - Causa em `E01-contextos-zumbis-e-git.txt`: o contexto ROOT foi recarregado a quente (16:48, 16:52, 16:58, 17:11). A tarefa repetitiva do plugin (`WebFilterTaskModule`) dos contextos antigos não para, e três instâncias de `ResolverConfigWriter` com código diferente gravam `blocky.yml` alternadamente (x1:16, x3:10, x9:07). Às 17:23:10 ainda rodava o SQL sem `ORDER BY`, que não existe mais na fonte.
   - O mesmo acontece em produção se a WAR for trocada a quente, como diz o D12 passo 3.
   - Esperado: D8 e classification "Domínio adicionado à blocklist … sem reinício do resolvedor"; nenhuma perda de DNS sem mudança de categoria ou de perfil ativo.
   - Reaprovação:
     - (a) o plugin não pode deixar escritor vivo depois de recarga do contexto, ou o runbook passa a exigir reinício do container e isso é provado;
     - (b) o DEV é reimplantado limpo;
     - (c) uma sonda DoT de 30 min sem ação do administrador mostra zero reinícios do Blocky e zero falhas;
     - (d) três mudanças só de lista feitas pela tela mostram "listas recarregadas sem reinicio" em todas.

3. **Códigos HTTP diferentes da spec.**
   - Evidência: `A02-validacao.log`, `A05-abas.log` e `A04-sem-permissao.log`. Validação, domínio inválido e pacote protegido respondem HTTP 200 com `status:"ERROR"`. Sem permissão também responde HTTP 200 com `error.permission.denied`.
   - Esperado: HTTP 400 (policy-management, classification, admin-console "Domínio inválido") e HTTP 403 (admin-console "Usuário sem permissão"). O HTTP 404 de perfil de outro cliente não foi testável (há um só cliente) e o código devolve `PERMISSION_DENIED`.
   - Reaprovação: as chamadas feitas pela tela e pelo navegador sem permissão mostram 400 e 403 reais, com a tela ainda exibindo as mensagens junto aos campos. A outra saída aceita é a spec ser revista formalmente (openspec-update aprovado) para o padrão 200+ERROR do Headwind.

4. **Contraste abaixo de 4,5:1 no tema escuro.**
   - Evidência: `A08-erro-escuro.log` e `A07-contraste-erro-vermelho.txt`. O erro de campo usa `#c62828`, chumbado em `views/main.html` (`.wf-field-error`), sobre `rgb(28,33,40)`, com 2,88:1.
   - Evidência: `A06-larguras-escuro.log`. O selo "Ativo" (`label-success`) tem 2,48:1 e "Inativo" 4,48:1. Os botões "Editar", "Salvar" e "Adicionar" têm 2,96:1.
   - Esperado: admin-console "Tema escuro" (todo texto visível ≥ 4,5:1) e D13 (reusar as variáveis de tema, sem cor própria).
   - Reaprovação: a medição por texto visível em cada aba e no editor com erro exibido, nas três larguras, mostra tudo ≥ 4,5:1 e o erro usa variável do tema. Para os botões, que vêm do tema global do console, vale uma correção limitada à tela do plugin ou a revisão formal da spec.

5. **App principal de quiosque é aceito na blocklist e mostrado como bloqueado.**
   - Evidência: `C-02-chrome-na-blocklist.log` e `.png`. `com.android.chrome`, o mainApp do perfil 11, foi salvo sem erro e aparece em "Aplicativos bloqueados por esta política". O sync o exclui corretamente (`C-sync-02*`), então a tela informa algo falso ao administrador.
   - Causa lida: `WebFilterService.savePolicy` recusa só `catalog.getProtectedPackages()`.
   - Esperado: policy-management "O filtro nunca bloqueia o próprio MDM" ("SHALL recusar em blocklist … o aplicativo principal de quiosque do perfil").
   - Reaprovação: incluir o mainApp do perfil na blocklist pela tela mostra erro de pacote protegido e nada é salvo; a lista de bloqueados da tela nunca mostra o mainApp.

6. **Domínio pai de domínio protegido é recusado, contra a spec.**
   - Evidência: `A10-pai-protegido.log` e `A60-*.png`. `*.hwmdm.dev.local` é recusado como "domínio do próprio MDM".
   - Esperado: policy-management "Domínio do MDM em blocklist por curinga": aceitar, bloquear `outro.empresa.com.br` e manter `mdm.empresa.com.br` permitido (D6: domínios protegidos sempre na allowlist gerada).
   - Reaprovação: o domínio pai é aceito, a sonda DoT mostra NXDOMAIN para outro subdomínio e resposta para o domínio protegido. A outra saída aceita é revisar a spec formalmente.

7. **Categorias sem descrição no editor.**
   - Evidência: `A03-editor-aberto.png`.
   - Esperado: admin-console "Configurar política por perfil" ("as 14 categorias do catálogo com nome e descrição").
   - Reaprovação: cada categoria mostra nome e descrição em português.

8. **Queda transitória do Blocky descarta uma configuração válida de forma permanente.**
   - Evidência: `B11-queda-processo.txt`. Depois de SIGKILL durante a carga, o supervisor registrou "configuracao nova recusada" e ficou rodando `blocky.good.yml`, diferente de `/app/dns/blocky.yml`.
   - Causa lida: `resolver.py` faz `running_hash = current` e não tenta de novo até o arquivo mudar. Com o limite de 1,5 GiB e listas grandes (adult com cerca de 5 M entradas), um OOM na carga faria uma ativação de perfil nunca valer.
   - Esperado: classification "Configuração inválida não derruba o resolvedor". O retorno à última boa vale para configuração inválida, não para falha transitória.
   - Reaprovação: depois de matar o Blocky durante a carga de uma configuração nova e válida, o supervisor volta a tentar essa configuração e a sonda mostra a regra nova em vigor.

9. **O que roda no DEV não corresponde à entrega versionada.**
   - Evidência: `E01-contextos-zumbis-e-git.txt`. `WebFilterMapper.java`, `ResolverConfigWriter.java` e `resolver.py` têm alterações não commitadas. O jar implantado corresponde à fonte alterada, mas o código antigo continua executando em contextos antigos. `PrivateDns*.java` e `dist/hmdm-v1.1-webfilter.apk` não estão versionados. O commit `c1433e71` mistura 18 mil arquivos: JDK, SDK, `.m2`, `target/`, `build/`, `.pyc` e a chave privada `webfilter-dns/certs/key.pem`.
   - Esperado: a validação incide sobre um estado identificável e reproduzível da entrega.
   - Reaprovação: a entrega fica em commit identificado, só com os arquivos da mudança, sem artefato de build nem chave privada; o DEV é implantado a partir dele com container reiniciado; o hash do jar em execução é registrado.

10. **Console do core alterado fora da lista fechada do proposal.**
    - Evidência: `E02-diff-console-core.txt`. Foram alterados `content.html` (item "Web Filter" chumbado no menu), `app.js` (reabertura de página de plugin após F5 e troca de hash de cache em todas as rotas), `index.html` (carga estática de `webfilter.module.js`, que também é carregado sob demanda pelo `pluginService`, ou seja, módulo carregado duas vezes) e `hwmdm_modules.js` (73 chaves do plugin duplicadas no pacote de idioma do core).
    - Esperado: proposal Impact (só `plugins/pom.xml`, `server/pom.xml` e launcher) e D13 ("cria a entrada … sem alterar o console").
    - Reaprovação: remover essas alterações do core, ou aprovar formalmente a revisão do proposal e do D13 (openspec-update) listando cada arquivo do core e o motivo. O item de menu deve respeitar a permissão (defeito 1) e o módulo não pode ser carregado duas vezes.

### Observações (não bloqueiam isoladamente)

- Em 360 px o botão "Editar" da lista só aparece rolando a tabela na horizontal, sem indicação visual (`A4C-360-lista.png`).
- O link "< Plugins" do console aparece acima do editor e leva a `#/extensions`, não à origem.
- Na aba Aplicativos, o pacote do cliente aparece também na linha "Catálogo inicial" (`A05-abas.log`).
- Upstreams DoT 1.1.1.1/8.8.8.8 estão chumbados em `ResolverConfigWriter`. O `docker-compose.yaml` usa por padrão o certificado autoassinado de DEV (`./certs`) quando `WEBFILTER_CERTS_DIR` não é definido.
- Launcher:
  - A classe ficou em `com.hmdm.launcher.helper`, e não no pacote novo `com.hmdm.launcher.webfilter` do proposal.
  - O código usa `disallow_config_private_dns`, que é o valor real de `UserManager.DISALLOW_CONFIG_PRIVATE_DNS`. A spec cita `no_config_private_dns`, então a spec precisa ser corrigida.
  - O launcher registrado no DEV (6.36, `hmdm-6.36-os.apk`) é assinado pela Headwind Solutions, com certificado diferente do v1.1. Um aparelho com esse launcher não atualiza para o v1.1. Isso deve ser conferido antes do piloto (`D-launcher-em-uso.txt`).
- Conta de teste `v.observer.t1` (Observer) permanece no DEV. Senha nos roteiros de evidência.

Veredito geral: REPROVADA — A REPROVADA, B REPROVADA, C ACEITA, D BLOQUEADA (sem tablet matriculado no DEV), E REPROVADA.
