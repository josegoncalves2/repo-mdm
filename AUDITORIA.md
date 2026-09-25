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
