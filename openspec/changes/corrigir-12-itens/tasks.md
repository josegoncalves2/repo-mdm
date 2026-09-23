# Tarefas — corrigir-12-itens

Regra de paralelismo: duas tarefas que tocam o MESMO arquivo nunca correm juntas.

## Onda 1 (paralela — conjuntos de arquivos disjuntos)

- [ ] **A — Acesso remoto no aparelho (itens 2 e 5).** Arquivos: `remote-agent/**`.
      A1: `canRetrieveWindowContent` + `type()` que insere no cursor, apaga, e envia Enter/Tab.
      A2: sessão sobrevive à tela apagada e à tela de bloqueio (wake lock, dispensa de keyguard,
      MediaProjection que não morre quando a tela desliga).
- [ ] **B — Acesso remoto no servidor e no console (itens 3 e 4).** Arquivos:
      `server-source/server/src/main/java/com/hmdm/remote/**`,
      `.../rest/resource/RemoteSupportResource.java`,
      `.../webapp/app/components/main/{controller/remote.controller.js,service/remoteSupport.service.js,view/remote.html}`.
      B1: sessão persiste a F5 e a troca de aba (reatar em vez de encerrar).
      B2: pedido de início enfileirado quando o aparelho está offline; ata sozinho quando volta.
- [ ] **C — Versão, build e compilação (item 10).** Arquivos: `scripts/**`, `VERSION`, `CHANGELOG.md`.

## Onda 2 (paralela entre si, depois da Onda 1)

- [ ] **D — Web Filter real no aparelho (item 1).** Arquivos: `remote-agent/**` (pacote novo
      `webfilter`), `server-source/plugins/webfilter/**`, `docs/WEBFILTER.md`.
      Imposição no dispositivo, não só no servidor.
- [ ] **E — Módulos, menus, botões e Plugins (itens 6, 7, 8, 9).** Arquivos:
      `server-source/server/src/main/webapp/**` (exceto os da tarefa B),
      `server-source/server/src/main/java/com/hmdm/rest/resource/**` (registro de módulos).

## Fora do alcance do agente

- [ ] **F — Ativar a trava de entrega (itens 11 e 12).** Editar hooks/settings é proibido ao
      agente pelo Artigo 11 do CONTRATO-DE-ENTREGA. Só o humano ativa. Ver `fiscal/ativar-trava.md`.

## Prova final (CLAUDE.md)

- [ ] Print da tela de acesso remoto funcionando, tirado do navegador do responsável.
- [ ] Laudo de bancada humana (`./trava bancada`), sem o qual o estado é NÃO ENTREGUE.
