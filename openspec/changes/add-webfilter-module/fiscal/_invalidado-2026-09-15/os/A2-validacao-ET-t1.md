# Ordem de Serviço - Tarefa A.2 (Validar planejamento)

## Dados da OS

- **Change:** add-webfilter-module
- **Fatia:** A - Ambiente e linha de base
- **Tarefa:** A.2 - Validar planejamento
- **Data:** 2026-09-15
- **FO:** Fiscal Orquestrador (esta sessão)
- **FQ:** Fiscal de Qualidade e Conformidade (independente)
- **Executor:** ET (Executor de Testes)

## Requisitos e cenários (copiados de spec)

**Tarefa A.2:** Validar planejamento

Comando obrigatorio:

```powershell
python C:/Users/40446686808/projetos/MDM/.agents/skills/openspec-fiscal/scripts/openspec_cli.py --cwd C:/Users/40446686808/projetos/MDM validate add-webfilter-module --strict --json
```

**Aceite:** `valid: true`, sem issues.

## Escopo permitido

- Arquivos em `fiscal/` (estado.md, ledger.md, os/, relatorios/, pareceres/, fila/, escalonamentos/).
- Comandos de leitura (`openspec status/validate`, `fiscal_guard.py snapshot`).
- Nenhuma alteração em `repo-mdm/` ou `openspec/changes/add-webfilter-module/` além de `fiscal/`.

## Escopo protegido

- Nenhum arquivo do MDM existente (`repo-mdm/`) pode ser alterado.
- Nenhum arquivo de `openspec/changes/add-webfilter-module/` além de `fiscal/` pode ser alterado pelo executor.
- Nenhum comando de produção pode ser executado.
- Nenhum `commit`, `push`, `merge`, `archive` sem ordem explícita.

## Comandos de aceite

- `openspec validate add-webfilter-module --strict --json` executado pelo FO.
- Saída JSON com `valid: true` e `issues: []`.
- `fiscal/estado.md` atualizado com tarefa A.2 em PENDENTE.
- `fiscal/ledger.md` atualizado com registro.
- `fiscal/os/` contém esta OS.
- `fiscal/fila/para-fq/` contém pedido de auditoria para FQ.

## Definição de pronto

- OpenSpec strict passa (`valid: true`, `issues: []`).
- FQ reexecuta e confirma validação.
- Tarefa A.2 marcada como EM_TESTES (aguardando EJ para A.3).

## Proibições

- Não editar código do produto.
- Não alterar testes.
- Não alterar specs/design/proposal.
- Não aceitar entrega sem parecer do FQ.
- Não usar `--force`, `--no-verify`, skip, ignore, teste desabilitado ou placeholder.
- Não fazer commit, push, merge, archive ou produção sem autorização explícita.
- Não tratar relatório de executor como evidência.

## Formato obrigatório do relatório do executor

- Texto da tarefa executada.
- Comandos executados com saída completa.
- Evidências (arquivos, hashes, capturas).
- Resultado do comando de aceite.
- Confirmação de que nenhum arquivo fora do escopo foi alterado.
- Confirmação de que a linha de base J00 permanece funcional.

## Devolução (se reprovar)

- Defeitos numerados com evidência (arquivo:linha ou comando + saída).
- Comportamento esperado.
- Critério de reaprovação.
- Reenvio ao mesmo executor ou novo executor conforme estratégia.

---

**Status:** EM_TESTES (aguardando EJ para A.3)
**Próxima OS:** A.3 (executar J00) após A.2 (validação strict)