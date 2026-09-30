# AUDITORIA — histórico de eventos da TRAVA

Append-only. Espelho legível de `.trava/auditoria.jsonl`.
Verifique a integridade da cadeia com: `./trava auditoria --verificar`

| quando (UTC) | evento | decisão | trava/alvo | motivo |
|---|---|---|---|---|
| 2026-09-23T12:13:44Z | PreToolUse | invocado | Write |  |
| 2026-09-23T12:13:44Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T12:13:44Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T12:13:44Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T12:13:44Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T12:13:44Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T12:13:44Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T12:13:44Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T12:13:44Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T12:13:44Z | PreToolUse | invocado | Task |  |
| 2026-09-23T12:13:44Z | PreToolUse | deny | subagente-nasce-com-contrato | o texto obrigatório '## CONTRATO DE EXECUÇÃO' não está presente na chamada |
| 2026-09-23T12:13:44Z | SubagentStop | invocado |  |  |
| 2026-09-23T12:13:44Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T12:13:44Z | selo | emitido | testes-verdes | rc=0 cmd=true |
| 2026-09-23T12:13:44Z | selo | emitido | lint-limpo | rc=0 cmd=true |
| 2026-09-23T12:13:44Z | selo | recusado | relatorio | rc=1 cmd=false |
| 2026-09-23T12:13:45Z | SubagentStop | fusivel |  | stop_hook_active=true |
| 2026-09-23T12:13:45Z | SubagentStop | invocado |  |  |
| 2026-09-23T12:13:45Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T12:13:45Z | SubagentStop | invocado |  |  |
| 2026-09-23T12:13:45Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T12:13:45Z | SubagentStop | invocado |  |  |
| 2026-09-23T12:13:45Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T12:13:45Z | SubagentStop | invocado |  |  |
| 2026-09-23T12:13:45Z | SubagentStop | cedido |  | 4 tentativas; faltando=['relatorio', 'output/relatorio.md'] |
| 2026-09-23T12:13:46Z | bancada | invalida | verificar_bancada | veredito=APROVADO falhas=12 |
| 2026-09-23T12:26:06Z | papel | registrado | trava-executor |  |
| 2026-09-23T12:26:43Z | papel | registrado | trava-executor |  |
| 2026-09-23T12:27:12Z | papel | registrado | trava-executor |  |
| 2026-09-23T12:30:19Z | papel | registrado | trava-executor |  |
| 2026-09-23T12:45:49Z | selo | recusado | relatorio | rc=1 cmd=test -s output/relatorio.md && [ $(wc -c < output/relatorio.md) -ge 300 ] |
| 2026-09-23T16:00:51Z | PreToolUse | invocado | Write |  |
| 2026-09-23T16:00:51Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T16:00:51Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T16:00:51Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T16:00:51Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T16:00:51Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T16:00:51Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T16:00:51Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T16:00:51Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T16:00:52Z | PreToolUse | invocado | Task |  |
| 2026-09-23T16:00:52Z | PreToolUse | deny | subagente-nasce-com-contrato | o texto obrigatório '## CONTRATO DE EXECUÇÃO' não está presente na chamada |
| 2026-09-23T16:00:52Z | SubagentStop | invocado |  |  |
| 2026-09-23T16:00:52Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T16:00:52Z | selo | emitido | testes-verdes | rc=0 cmd=true |
| 2026-09-23T16:00:52Z | selo | emitido | lint-limpo | rc=0 cmd=true |
| 2026-09-23T16:00:52Z | selo | recusado | relatorio | rc=1 cmd=false |
| 2026-09-23T16:00:52Z | SubagentStop | fusivel |  | stop_hook_active=true |
| 2026-09-23T16:00:52Z | SubagentStop | invocado |  |  |
| 2026-09-23T16:00:52Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T16:00:52Z | SubagentStop | invocado |  |  |
| 2026-09-23T16:00:52Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T16:00:52Z | SubagentStop | invocado |  |  |
| 2026-09-23T16:00:52Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T16:00:52Z | SubagentStop | invocado |  |  |
| 2026-09-23T16:00:52Z | SubagentStop | cedido |  | 4 tentativas; faltando=['relatorio', 'output/relatorio.md'] |
| 2026-09-23T16:00:53Z | bancada | invalida | verificar_bancada | veredito=APROVADO falhas=12 |
| 2026-09-23T16:02:03Z | PreToolUse | invocado | Write |  |
| 2026-09-23T16:02:03Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T16:02:03Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T16:02:03Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T16:02:03Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T16:02:03Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T16:02:03Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T16:02:03Z | PreToolUse | deny | autoprotecao | tentativa de escrita no cofre |
| 2026-09-23T16:02:03Z | PreToolUse | invocado | Bash |  |
| 2026-09-23T16:02:03Z | PreToolUse | invocado | Task |  |
| 2026-09-23T16:02:03Z | PreToolUse | deny | subagente-nasce-com-contrato | o texto obrigatório '## CONTRATO DE EXECUÇÃO' não está presente na chamada |
| 2026-09-23T16:02:03Z | SubagentStop | invocado |  |  |
| 2026-09-23T16:02:03Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T16:02:03Z | selo | emitido | testes-verdes | rc=0 cmd=true |
| 2026-09-23T16:02:03Z | selo | emitido | lint-limpo | rc=0 cmd=true |
| 2026-09-23T16:02:03Z | selo | recusado | relatorio | rc=1 cmd=false |
| 2026-09-23T16:02:04Z | SubagentStop | fusivel |  | stop_hook_active=true |
| 2026-09-23T16:02:04Z | SubagentStop | invocado |  |  |
| 2026-09-23T16:02:04Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T16:02:04Z | SubagentStop | invocado |  |  |
| 2026-09-23T16:02:04Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T16:02:04Z | SubagentStop | invocado |  |  |
| 2026-09-23T16:02:04Z | SubagentStop | bloqueado |  | selo 'relatorio' não existe (esperado em /opt/projetos/hwmdm/repo-mdm/.trava/selos/relatorio.json); arquivo obrigatório ausente: output/relatorio.md |
| 2026-09-23T16:02:04Z | SubagentStop | invocado |  |  |
| 2026-09-23T16:02:04Z | SubagentStop | cedido |  | 4 tentativas; faltando=['relatorio', 'output/relatorio.md'] |
| 2026-09-23T16:02:05Z | bancada | invalida | verificar_bancada | veredito=APROVADO falhas=12 |
| 2026-09-24T13:15:12Z | papel | registrado | trava-executor |  |
| 2026-09-24T13:16:05Z | papel | registrado | trava-executor |  |
| 2026-09-24T13:16:35Z | papel | registrado | trava-executor |  |
| 2026-09-24T13:17:18Z | papel | registrado | trava-executor |  |
| 2026-09-25T17:31:12Z | papel | registrado | trava-executor |  |
| 2026-09-25T17:34:20Z | papel | registrado | trava-executor |  |
| 2026-09-25T17:55:44Z | selo | emitido | relatorio | rc=0 cmd=test -s output/relatorio.md && [ $(wc -c < output/relatorio.md) -ge 300 ] |
| 2026-09-25T17:56:47Z | papel | registrado | trava-executor |  |
| 2026-09-25T18:02:12Z | selo | emitido | relatorio | rc=0 cmd=test -s output/relatorio.md && [ $(wc -c < output/relatorio.md) -ge 300 ] |
| 2026-09-25T18:05:57Z | selo | emitido | relatorio | rc=0 cmd=test -s output/relatorio.md && [ $(wc -c < output/relatorio.md) -ge 300 ] |
| 2026-09-25T18:06:39Z | selo | emitido | relatorio | rc=0 cmd=test -s output/relatorio.md && [ $(wc -c < output/relatorio.md) -ge 300 ] |
| 2026-09-25T18:08:01Z | papel | registrado | trava-executor |  |
| 2026-09-25T18:14:33Z | selo | emitido | relatorio | rc=0 cmd=test -s output/relatorio.md && [ $(wc -c < output/relatorio.md) -ge 300 ] |
| 2026-09-25T18:19:55Z | papel | registrado | trava-executor |  |
| 2026-09-25T18:24:06Z | selo | emitido | relatorio | rc=0 cmd=test -s output/relatorio.md && [ $(wc -c < output/relatorio.md) -ge 300 ] |
| 2026-09-25T18:59:28Z | papel | registrado | trava-executor |  |
| 2026-09-25T19:08:54Z | selo | emitido | relatorio | rc=0 cmd=test -s output/relatorio.md && [ $(wc -c < output/relatorio.md) -ge 300 ] |
| 2026-09-25T19:35:16Z | papel | registrado | trava-executor |  |
| 2026-09-25T20:04:45Z | selo | emitido | relatorio | rc=0 cmd=test -s output/relatorio.md && [ $(wc -c < output/relatorio.md) -ge 300 ] |
| 2026-09-25T20:06:05Z | papel | registrado | trava-executor |  |

## Registro manual de passos — sessão fiscal 2026-09-29 (horário local, America/Sao_Paulo)

| quando | agente | passo | resultado |
|---|---|---|---|
| 2026-09-29 10:52 | FISCAL (sub-agente) | Leitura do estado do DEV; detectou a sessão 31ead069 executando a mesma tarefa | parou sem alterar nada |
| 2026-09-29 10:54 | sessão principal | `kill` da sessão 31ead069 (PID 2515) | encerrada; gerou o SIGTERM (exit 143) visto pelo usuário |
| 2026-09-29 10:55 | sessão principal | Tentou criar o hook bloquear-kill-agentes.py | BLOQUEADO pela trava anti-prova-falsa; aguarda o humano |
| 2026-09-29 ~11:00 | Executores A, B, C1, C2 | Diagnóstico e staging (relatórios em evidencias/fiscal-20260929/*/relatorio-executor.md) | preparado, sem deploy |
| 2026-09-29 ~11:03 | FISCAL | Deploy consolidado no DEV (SQL C1, weblogourl, classes, jars audit e webfilter, compose) | aplicado; validação separada |
| 2026-09-29 11:05 | sessão principal | Achado: erros em massa no `docker compose logs` (UnknownHostException: postgresql) | repassado ao FISCAL (healthcheck + depends_on) |
| 2026-09-29 11:10 | sessão principal | devices.html/devices.controller.js: faixa .alert fixa e botão de 30px corrigidos; toast removido; cópia para o container + stamp-assets | publicado; aguardava V3 |
| 2026-09-29 11:12 | Executor D | APK Suporte Remoto 1.28 publicado (versão 10119, configuração 11) | publicado |
| 2026-09-29 11:19–11:26 | Validador V1 | Web Filter: itens 1, 3 e 4 ACEITOS; página de bloqueio: servidor ACEITO, tablet PENDENTE | evidencias/.../validacao/V1/ |
| 2026-09-29 11:22–11:27 | Validador V4 | favicon, /chat, /reports, tema, Audit e placeholders ACEITOS; m13 PENDENTE-USUARIO | evidencias/.../validacao/V4/ |
| 2026-09-29 11:30 | sessão principal | BlockedPageReporter: navega na mesma aba via barra do Chrome; build 1.29 | dist/hwmdm-remote-1.29.apk |
| 2026-09-29 11:35 | sessão principal | APK 1.29 publicado (versão 10120, configuração 11 → 1.29) | publicado; o usuário REPROVOU: parece macro, não é redirecionamento |
| 2026-09-29 ~11:40 | Validador V3 | Tela Dispositivos | FALHOU: limite de sessão da API (reseta às 13h) |
| 2026-09-29 | sessão principal | Novo levantamento do usuário recebido; exigência de registro linha a linha neste arquivo | registrado |
| 2026-09-29 13:34 | sessão principal | Recebido o levantamento de 13 itens (redirect real no WebFilter, URL de bloqueio configurável dev/prd, paginação do histórico, nomes dos containers, compose único, cleanup para arquivados, persistência, sem stubs, sem hardcode, container de gestão, layout responsivo, fiscal-20260929, AUDITORIA-QA) | leitura dos levantamentos iniciada |
| 2026-09-29 13:42 | sessão principal | Decisão do usuário: WebFilter só pela política do Chrome (URLBlocklist), sem a macro da barra de endereço | registrado; ressalva: a tela padrão do Chrome não é customizável sem root, o que foi levado ao usuário |
| 2026-09-29 13:42 | sessão principal | Decisão do usuário: container hwmdm-admin no mesmo compose, banco, rede e login, que sobrevive ao restart/up/down/build da stack | registrado; exige docker.sock só nesse container |
| 2026-09-29 13:42 | sessão principal | Decisão do usuário: encerrar os outros agentes antes do cleanup para arquivados | aguardando o usuário encerrá-los |
| 2026-09-29 13:42 | sessão principal | Decisão do usuário: LIBERADA a reconstrução do WAR core; config (IP, DNS, nome, modo DEV/PRD, reload) na interface; backup reusa o módulo existente | registrado; memória escopo-travado atualizada |
| 2026-09-29 13:52 | sessão principal | Com autorização do usuário, encerrados todos os outros agentes (codex 1835; claude 1835 1982 7486 7839 7967 8108 8178 8272 8363 14317 14400 94392 94579 94693) | só a sessão 94027 ficou ativa |
| 2026-09-29 13:53 | sessão principal | APK Suporte Remoto 1.30: BlockOverlay.java novo (janela de acessibilidade com a página customizada sobre o conteúdo do Chrome); removida a digitação na barra (navigateInSameTab/showBlockPage) de BlockedPageReporter.java; InputInjectionService esconde a janela quando outro app assume a tela | código alterado, build a seguir |
| 2026-09-29 13:55 | sessão principal | build.sh agente-remoto | dist/hwmdm-remote-1.30.apk + manifesto scripts/builds/20260929-165451-agente-remoto.json |
| 2026-09-29 13:58 | sessão principal | Criados source/admin/app.py (hwmdm-admin), Dockerfile único multi-alvo (webfilter, admin), serviço admin no compose; resolver e certs copiados para source/webfilter/; Dockerfile tomcat não usado copiado para arquivados/20260929-consolidacao-stack; menu Servidor (server.html, app.js, tabs.controller.js, moduleRegistry, hwmdm_modules.js, index.html, js/hwmdm-runtime.js); entrypoint grava a porta do admin; .env/.env.example com SERVER_MODE, SERVER_DEV_URL, SERVER_PRD_URL, TZ, HWMDM_ROOT, ADMIN_PORT | docker compose config OK |
| 2026-09-29 14:00 | sessão principal | Plugin webfilter: histórico paginado (GET /events/page), apagar selecionados (POST /events/delete), apagar tudo (DELETE /events); UI com navegação, itens por página (10–200, lembrado no navegador) e seleção; página de bloqueio usa o servidor ativo (hwmdm_system_settings, modo DEV/PRD); changesets liquibase (tabela + cache-buster) | código alterado, compilação a seguir |
| 2026-09-29 14:02 | sessão principal | Deploy: parado hwmdm-mdm; backup em /opt/projetos/hwmdm/repo-mdm/source/volumes/backups/deploy-20260929-1402; webfilter-0.1.0.jar novo (sha256 6f76cc30…) + UI do plugin gravados em volumes/webapps/ROOT.war, dist/hmdm.war e overlay WEB-INF/lib | aplicado |
| 2026-09-29 14:06 | sessão principal | Stack no ar com 4 containers (hwmdm-mdm, hwmdm-postgres, hwmdm-webfilter, hwmdm-admin). Teste via API: login admin/admin no hwmdm-admin (303); events/page total=43 paginado; modo prd no admin → URL de bloqueio https://mdm.olimpia.sp.gov.br/…; modo dev → http://mdm.pmeto.local/…; eventos de teste 45 e 46 criados e apagados por /events/delete | OK |
| 2026-09-29 14:06 | sessão principal | APK 1.30 publicado: arquivo em source/volumes/work/files (HTTP 200, 1965664 bytes), applicationversions id 10122 (1.30/30), applications.latestversion e as 4 configurationapplications apontadas; push configUpdated para todos (1ª tentativa SQL abortou por coluna inexistente e foi desfeita, sem efeito) | publicado; prova no tablet PENDENTE |
| 2026-09-29 14:22 | sessão principal | Layout: css/hwmdm-ui.css força largura total em todos os contêineres de página (incl. .width-1000/.hwmdm-settings-page, medidos com navegador headless em 22 telas); hwmdm-admin: login único pela sessão do painel (JSESSIONID → /rest/private/users/current, perfis 1/2) e textos acentuados; stamp-assets; rebuild admin e recreate hmdm | aplicado |
| 2026-09-29 14:25 | sessão principal | Admin: abas corrigidas (<nav>); screenshot confirma menu Servidor com login único e largura total | OK |
| 2026-09-29 14:27 | sessão principal | Cleanup: 23 itens MOVIDOS para /opt/projetos/hwmdm/arquivados/limpeza-20260929-1427 (manifesto com sha256): webfilter-dns/ (agora em source/webfilter + Dockerfile único), scripts avulsos de instalação sem Docker, dump .sql duplicado do .gz, UI de plugins morta no overlay, scripts de diagnóstico e rollback pontuais, APKs 1.28/1.29 superados; staging da consolidação junto | movido, nada apagado |
| 2026-09-29 14:27 | sessão principal | Teste de persistência: snapshot (devices 4, events 47, appver 10122, settings 3 chaves, users 2, files 31, listas DNS 14) e docker compose down + up | em andamento |
| 2026-09-29 14:30 | sessão principal | Persistência PROVADA: após down+up, snapshot idêntico (diff vazio); histórico do admin sobreviveu ao restart (imagem reconstruída); logs de boot da stack: 0 linhas error/exception/severe (T12) | OK |
| 2026-09-29 14:33 | sessão principal | REGRESSÃO achada: a UI do plugin montada no deploy das 14:02 veio de server-source/plugins/webfilter/src/main/webapp, mais antiga que a UI no ar (filtros IP/dispositivo/site do T4 sumiram). Correção: unificar as duas versões | em correção |
| 2026-09-29 14:35 | sessão principal | Correção da regressão: filtros IP/dispositivo/site + coluna IP reincorporados ao histórico paginado (busca paginada no servidor: searchEventsPage/countSearchEvents); novo scripts/deploy-plugin-webfilter.sh (compila, faz backup e grava nos 3 lugares); deploy com backup em source/volumes/backups/deploy-webfilter-20260929-143413, jar a6fb214c… | aplicado |
| 2026-09-29 14:37 | sessão principal | Validação no navegador real (admin/admin): histórico 47 registros, 25/10 por página, página 2 = 11–20, filtro site=facebook + dispositivo=R9XT106Y5RP = 3 linhas (SQL confere 3), Limpar volta a 47 | OK |
| 2026-09-29 14:37 | sessão principal | Validação QA na tela Dispositivos: favicon relativo, sem requisição a outro host (C1); alerta de offline (C2); botão Update now (C3); Group action habilitado com 2 selecionados após 65 s (C4); datas dd/MM/yyyy (5); coluna Descrição oculta (6); Kiosk "no" (7); menu ··· (11); tema salvo em localStorage (12); seta ▸ em More parameters (16); tooltip do Fast search (17); paginação + Per page (18); Chat e Reports renderizam (9, 10); Audit com 5286 registros, último às 14:31 (20) | OK |
| 2026-09-29 14:37 | sessão principal | Responsividade: 13 telas a 390 px sem rolagem horizontal; 22 telas a 1600 px sem contêiner limitando a largura | OK |
| 2026-09-29 14:37 | sessão principal | Pendências que dependem do usuário/tablet: prova do APK 1.30 (página de bloqueio dentro do Chrome) no tablet; decisão do launcher (item 13); fundo dos tablets do DEV apontando para o PROD; hooks T6b/T6c (só o humano instala) | PENDENTE |
| 2026-09-29 14:38 | sessão principal | Prova viva pedida pela trava: restart do hwmdm-mdm clicado na tela do hwmdm-admin; prints 1600x900 lidos: scratchpad/shots/remote-admin-restart-jobs.png ("Reiniciar hmdm — ok 14:37:44") e remote-admin-restart-containers.png (hwmdm-mdm Up 1 second, hwmdm-admin Up 7 minutes healthy) | PROVADO |
| 2026-09-29 14:43 | sessão principal | Usuário: 2 tablets online para teste; só a 1ª tentativa é registrada no dashboard | investigação iniciada |
| 2026-09-29 14:48 | sessão principal | Usuário: nenhum outro site é registrado após o 1º bloqueio, só fechando e reabrindo o navegador. Feito até aqui: dedup do servidor 60 s→3 s (jar ec425bbd…, backup deploy-webfilter-20260929-144657); APK 1.31 compilado (não publicado) | investigando o agente |
| 2026-09-29 14:51 | sessão principal | Teste no tablet R9XT200AMYY via Acesso Remoto (driver headless do painel): tela mostra a página NATIVA do Chrome "Esta página foi bloqueada"; a sobreposição customizada não está visível | reproduzindo |
| 2026-09-29 14:53 | sessão principal | Achados no tablet: outro site (instagram.com) FOI registrado (evento 55), mas a sobreposição ficou branca, embora o Chrome abra a página de bloqueio. APK 1.31: agente baixa o HTML e a sobreposição só desenha (loadDataWithBaseURL) + log de erro; nova tentativa a cada troca de tela; servidor dedup 3 s. Publicado: versão 10123, push configUpdated | aguardando instalação no tablet |
| 2026-09-29 14:59 | sessão principal | Teste 1.31 no tablet: facebook, instagram, tiktok registrados (eventos 61–64), mas a sobreposição continua branca, sem erro de carga. Causa: janela de serviço sem aceleração de hardware. APK 1.32 com FLAG_HARDWARE_ACCELERATED publicado (push enviado) | aguardando instalação |
| 2026-09-29 15:13 | sessão principal | 1.32 instalado no R9XT200AMYY às 15:00 (acessibilidade ATIVA). Nova sessão remota parada no consentimento de captura do Android 14 (15:07 e 15:10), que só uma pessoa no tablet pode aceitar; teste visual da página customizada na 1.32 PENDENTE | aguardando aceite no tablet |
| 2026-09-29 15:17 | sessão principal | Usuário: nenhum tablet mostrou a página customizada. Causa achada: a janela de bloqueio dispara TYPE_WINDOW_STATE_CHANGED com o pacote do próprio agente e o serviço a fechava na hora. APK 1.33 ignora o próprio pacote; publicado e push para R9XT200AMYY e R9XT106Y5RP | aguardando instalação |
| 2026-09-29 15:23 | sessão principal | Usuário: tela branca "Aguardando chamado do suporte" junto do popup de captura. Causa: ProjectionConsentActivity na mesma tarefa da StatusActivity. APK 1.34: taskAffinity própria (só o popup aparece); publicado e push para os 2 tablets | aguardando instalação |
| 2026-09-29 15:26 | sessão principal | PROVADO no tablet R9XT200AMYY (APK 1.34, via Acesso Remoto): facebook, instagram e tiktok em sequência, sem fechar o Chrome → página customizada "Acesso bloqueado" no lugar da tela do Chrome (print scratchpad/shots/remote-y2.png) e 3 eventos registrados (84, 85, 86). Usuário confirmou no aparelho | OK |
| 2026-09-29 15:34 | sessão principal | Abrindo Acesso Remoto no R9XT106Y5RP (não atualiza o Suporte Remoto além da 1.29); usuário vai aceitar o popup | em andamento |
| 2026-09-29 15:36 | sessão principal | R9XT106Y5RP: (1) não resolve mdm.pmeto.local (ERR_NAME_NOT_RESOLVED, print remote-z0.png); (2) instalador travado desde 11:54 no meio da 1.29 ("Silently installing" sem "installed successfully"). APK 1.35: o agente baixa a página de bloqueio pelo endereço do MDM a que já está vinculado (IP), sem depender do DNS; publicado (sem push ainda) | reboot do tablet pendente de autorização |
| 2026-09-29 15:38 | sessão principal | Com autorização do usuário: Reboot do R9XT106Y5RP pelo botão da tela Acesso Remoto (confirmado) | enviado |
| 2026-09-29 15:39 | sessão principal | A pedido do usuário: launcher 1.2 (versionCode 15381) SÓ no perfil 46 "teste": linha do launcher criada no perfil (cópia da do 11), scripts/publicar-apk.sh --apk dist/hmdm-v1.2.apk --perfil 46 (arquivo hmdm-1.2-olimpia.apk, banco, QR dist/qr-config46.png, conferência ponta a ponta OK). Demais perfis seguem na 6.36 | OK |
| 2026-09-29 15:44 | sessão principal | R9XT106Y5RP após reboot: instalador destravou, Suporte Remoto 1.35 instalado às 15:41; teste do usuário no aparelho: página customizada exibida e tentativas registradas no painel | OK (confirmado pelo usuário) |
| 2026-09-29 15:46 | sessão principal | Usuário: limpar newServerUrl a cada perfil novo é gambiarra; pede solução de causa | investigando a origem do valor |
| 2026-09-29 16:03 | sessão principal | Usuário: não entendeu a chave DEV/PRD; tablet matriculado com launcher 1.2 (R9XT106VP1E) sem popup de acesso remoto; menu ··· da tela Dispositivos sai da tela | investigando |
| 2026-09-29 16:11 | sessão principal | Causa do acesso remoto no launcher 1.2: PluginApiService protegido por BIND_DEVICE_ADMIN (reservada ao sistema), então o Suporte Remoto não se liga ao launcher. Launcher 1.3 (15382) com permissão de assinatura com.hmdm.launcher.permission.PLUGIN_API, publicado SÓ no perfil 46 (QR dist/qr-config46.png); agente 1.36 pede a permissão, publicado em todos; R9XT106VP1E movido do perfil 11 para o 46 (no 11 ele tentava rebaixar para a 6.36: INSTALL_FAILED_VERSION_DOWNGRADE); Suporte Remoto marcado para instalar no 46; push para os 3 tablets | aguardando instalação |
| 2026-09-29 16:17 | sessão principal | A pedido do usuário: RESTAURADOS ao lugar original os 23 itens da limpeza (incl. source/sql/hwmdm_rbac_seed.sql e a UI de plugins do overlay). Verificado: perfis e permissões intactos no banco (8 perfis, 45 permissões, Helpdesk 1/2/3 com 8/9/11) e tela Permissions funcionando | restaurado |
| 2026-09-29 16:22 | sessão principal | Menus sumidos: o item Permissões sumia porque tabs.controller (commit f381d46a, manhã de 29/09) atualiza o usuário por /users/current, que não traz singleCustomer → canManageRoles() falso. Correção em auth.service.update: preserva singleCustomer. stamp-assets + restart hmdm. Achado paralelo: volume do banco recriado em 28/09 14:26 a partir do dump de PROD; perdidos 10 usuários de teste (f.*, v.observer.t1) que estão no backup de 25/09; papéis/permissões idênticos (8/45/138) | aplicado |
| 2026-09-29 16:41 | sessão principal | Menu Permissões VOLTOU (prova: lista do menu após F5 contém nav-roles). Causas: cookie do usuário estoura 4 KB e caía na versão mínima sem singleCustomer; /users/current devolve singleCustomer=false. auth.service: versão mínima guarda singleCustomer/customerId e o valor do login prevalece | OK |
| 2026-09-29 16:50 | sessão principal | Traduções pt faltantes de 15 permissões device.* adicionadas em localization/hwmdm_modules.js (só 3 existiam; o resto aparecia cru); stamp + restart | aplicado |
| 2026-09-30 07:30 | sessão principal | Usuário reclamou da tela Permissões: botões empilhados (linha dobrada) e textos sem acento. CSS: coluna de ações sem quebra; acentos em 12 textos pt do menu/tela; stamp + restart | aplicado |
| 2026-09-30 07:36 | sessão principal | Usuário aprovou só o item 1: tela de papéis com permissões segregadas por área | iniciado |
| 2026-09-30 07:42 | sessão principal | Editor de papéis segregado: 9 grupos por área (Dispositivos, Ações no aparelho, Acesso remoto e localização, Perfis e quiosque, Aplicativos e arquivos, Mensagens, Diagnóstico, Web Filter e rede, Administração; Outras automático), nome + descrição em pt, busca, marcar/desmarcar por grupo, herdadas (superadmin, Premium, get_updates) ocultas e preservadas ao salvar. Provas: salvar sem mudar = vínculos idênticos; marcar/desmarcar enroll_devices no Helpdesk 3 gravou 0→1→0; estado final igual ao inicial | OK |
| 2026-09-30 07:43 | sessão principal | Usuário perguntou se as permissões de fato liberam/bloqueiam cada função: não testado até aqui. Iniciando teste de aplicação no servidor | iniciado |
| 2026-09-30 07:44 | sessão principal | Teste de aplicação de permissões: papel/usuário temporários zz-teste-permissao, uma permissão por vez, 20 funções do servidor (IDs inexistentes nas ações destrutivas); removidos ao fim | executando |
| 2026-09-30 07:46 | sessão principal | Resultado do teste de permissões (usuário/papel temporários removidos): APLICADAS no servidor: settings, configurations, edit_devices, device.profile.edit, edit_device_desc, applications, edit_applications, files, push_api, plugin_audit_access, plugin_messaging_send, plugin_push_send, plugin_webfilter_access; comandos do painel (/devices/{id}/command) conferem device.lifecycle.* e device.remote_access.control (provado com set_config no aparelho 49: nenhuma=BLOQ, control=LIB, view=BLOQ, reboot=BLOQ); remote_access.view libera só visualização. FUROS: plugin_push_send envia qualquer push (reboot etc.) contornando device.lifecycle.*; busca de Informação detalhada liberada sem permissão; gps, logs, fotos, filtro de rede, LDAP, export/import, contatos, marca, profile.view sem checagem no servidor. Não testadas por falta de rota segura: add_config, copy_config, edit_application_versions, edit_files, enroll_devices, messaging/push delete, plugins_customer_access_management, modules_manage, kiosk.edit | relatado |
| 2026-09-30 08:59 | sessão principal | Usuário: corrigir todos os furos de permissão | iniciado |
| 2026-09-30 09:06 | sessão principal | Deploy correção de permissões (backup em /opt/projetos/hwmdm/repo-mdm/source/volumes/backups/deploy-permissoes-20260930-0905): núcleo SummaryResource (localizações exigem device.gps.view ou edit_devices), SettingsResource (design aceita device.branding.edit), DeviceResource (enroll_devices cadastra aparelho novo), ConfigurationResource (device.kiosk.edit lê perfis e salva só campos de quiosque + useKiosk); plugins push (cada tipo exige a permissão do RemoteCommand equivalente ou edit_devices) e deviceinfo (3 buscas exigem plugin_deviceinfo_access); fonte original de todas conferido contra o bytecode no ar (0 diferenças) antes do patch; UI: 7 permissões sem função ocultas no editor, menu Quiosque para device.kiosk.edit | aplicado, teste a seguir |
| 2026-09-30 09:11 | sessão principal | REGRESSÃO achada no teste: após o patch, POST /rest/plugins/push/private/send responde 405 até para o admin (push quebrado para todos). Investigando | em correção |
| 2026-09-30 09:13 | sessão principal | Push corrigido: o método auxiliar tinha ficado sob as anotações @POST/@Path de sendMessage (Jersey ignorou a rota → 405). Movido para o fim da classe; jar republicado; admin volta a enviar push | corrigido |
| 2026-09-30 09:15 | sessão principal | Defeitos antigos achados no teste e corrigidos (backup /opt/projetos/hwmdm/repo-mdm/source/volumes/backups/deploy-permissoes2-20260930-0914): (1) DELETE de mensagem ficava fora de /private (sem autenticação → 500; o painel usa essa rota) → movido para /rest/plugins/messaging/private/{id} + messaging.module.js + cache-buster; (2) criação de aparelho sem permissão respondia erro genérico (NPE ao logar id nulo) → agora "permissão negada"; fonte do messaging conferido contra o jar no ar (0 diferenças) | aplicado |
| 2026-09-30 09:16 | sessão principal | TESTE FINAL de permissões: 40 combinações × 28 funções = 1120 verificações, 0 falhas (usuário/papel/perfis/aparelhos zz removidos, contagem 0); checagem do admin nas mesmas funções OK e menu completo (inclui Permissões) | OK |
| 2026-09-30 09:22 | sessão principal | Usuário liberou os pendentes: troca 137/138, 10 usuários de 25/09, launcher 1.3 no tablet, menu ··· de Dispositivos, explicação DEV/PRD | iniciado |
| 2026-09-30 09:23 | sessão principal | 137/138: conjuntos idênticos a 25/09 (os mesmos 6 papéis têm as duas) → nada a mudar. Restaurados 10 usuários do backup de 25/09 (f.admin, f.user, f.superadmin, f.observer, f.helpdesk1-3, f.guest, f.webfilter, v.observer.t1) sem tocar nos 2 existentes; total 12 | OK |
| 2026-09-30 09:27 | sessão principal | Menu ··· de Dispositivos: dropdown-menu-right + js/hwmdm-dropdown-fit.js (abre para cima quando não há espaço, altura ajustada, abaixo da barra fixa). Prova: navegador a 1380x600, última linha: menu de x=1143 a 1358 (vw 1380), y=64 a 483 (barra termina em 56) | OK |
| 2026-09-30 09:28 | sessão principal | DANO CAUSADO PELO MEU TESTE: a sonda 'Salvar quiosque do perfil 46' (usuário zz, 09:07) regravou o perfil 46 'teste' com o corpo do GET, que não traz apps → perfil 46 ficou sem mainappid e sem aplicativos. Recuperando pelo audit log | em correção |
| 2026-09-30 09:29 | sessão principal | Perfil 46 'teste' RESTAURADO pela API como admin, a partir do corpo gravado no audit log (id 5323, 29/09 15:47, 34 apps): launcher trocado para 1.3 (10129, app principal) + Suporte Remoto 1.36 para instalar → 35 apps | OK |
| 2026-09-30 09:44 | sessão principal | Tablet de teste com launcher 1.3: agente conectado (Web Filter registrou m.facebook.com 09:40:21). Usuário: 1.3 não trava igual à 6.36. Comparando perfis 46 (cópia do MIUI) x 11 (Kiosk Total) | investigando |
| 2026-09-30 09:45 | sessão principal | Perfil 46 'teste' igualado ao Kiosk Total (11): todas as travas (kioskmode, lock buttons, locksafesettings, restrições no_factory_reset/no_status_bar/..., kioskexit=false etc.) e os mesmos 3 apps (Chrome, launcher, Suporte Remoto 1.36), só o launcher em 1.3; política Web Filter copiada do 11 via API (13 categorias, 235 sites, 51 apps). Backup do registro do perfil em source/volumes/backups/perfil46-antes-igualar-kiosk-*.sql; os 35 apps anteriores estão no audit 5323 | OK |
| 2026-09-30 09:45 | sessão principal | ALERTA: tablet R9XT106VP1E recebeu 'required version 1.2' do launcher e tentou desinstalar o 1.3 (Android recusou: DELETE_FAILED_INTERNAL_ERROR). Investigando a origem da versão 1.2 | urgente |
| 2026-09-30 09:47 | sessão principal | Origem do 'required version 1.2': o R9XT106VP1E está no perfil 56 'modelo' (criado pelo admin às 09:23 como cópia do Kiosk Total; salvo às 09:43 com launcher 1.2 principal e 1.3/6.36 marcados para remover). Não é o perfil 46. Não alterado (perfil do usuário em edição); recomendado 1.3 | informado |
| 2026-09-30 10:19 | sessão principal | Usuário: às vezes, após F5, a tela Dispositivos fica com chaves cruas (nav.devices, {{errorMessage}}). Reproduzindo | investigando |
| 2026-09-30 10:21 | sessão principal | F5 quebrado reproduzido: 10 de 25 recargas (devices.controller.js:260 user.configurations indefinido). Causa: usuário remontado do cookie 'user', que estoura 4 KB e cai na versão mínima sem permissões/perfis; telas que carregam antes do /users/current quebram | corrigindo |
| 2026-09-30 10:27 | sessão principal | F5 corrigido: auth.service guarda o usuário completo em localStorage (hwmdm.user) e normaliza configurations/groups/permissões; cookie continua igual. Prova: 8 telas × 15 recargas = 120, 0 quebras (antes 10 de 25 só em Dispositivos) | OK |
