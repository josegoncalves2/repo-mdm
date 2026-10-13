# Laudo de invalidacao fiscal - 2026-09-16

## Veredito

Todos os registros movidos para `_invalidado-2026-09-15/` sao invalidos como evidencia de progresso, aceite ou estado do change `add-webfilter-module`.

## Motivos

1. `fila/para-fo.md` e os escalonamentos registram que o 429 impediu a criacao de sub-agentes reais.
2. Mesmo sem sub-agentes reais, `estado.md` marcava tarefas como `EM_IMPLEMENTACAO` e `EM_JORNADA`.
3. O parecer da tarefa 1.1 declarou auditoria realizada pelo FO. O FO nao pode emitir parecer de qualidade.
4. Houve OS de implementacao sem parecer FQ `VERMELHO_CONFIRMADO`.
5. A Fatia B foi iniciada sem J00 aceita.
6. O ledger citava snapshot em caminho de exemplo (`<tmp>/...`), nao evidencia real.
7. Havia OS duplicadas para a mesma tarefa com nomes diferentes.
8. `ORDEM-AO-AGENTE-PRINCIPAL.md` e `PROMPT-CURTO-EXECUTAR-AGORA.md` impunham modelo/provedor e 400 sub-agentes, e o prompt curto mandava implementar 1.1 diretamente, pulando o ciclo de testes.

## Efeito operacional

- Nenhum checkbox de `tasks.md` pode ser marcado com base nesses registros.
- Nenhuma OS, parecer, pedido de fila, ledger ou estado arquivado pode ser reaproveitado.
- A proxima execucao deve recomecar pela Fatia A, J00, `openspec validate --strict` e snapshot real pelo `fiscal_guard.py`.
- O novo estado fiscal deve ser derivado de ledger encadeado pelo guard.
