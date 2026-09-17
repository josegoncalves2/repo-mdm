# Ordem de Serviço - Fatia A (Ambiente e linha de base)

## Dados da OS

- **Change:** add-webfilter-module
- **Fatia:** A - Ambiente e linha de base
- **Data:** 2026-09-15
- **FO:** Fiscal Orquestrador (esta sessão)
- **FQ:** Fiscal de Qualidade e Conformidade (independente, a ser designado)
- **Executor:** EA (Executor de Ambiente) para A.1; ET para A.2; EJ para A.3

## Tarefa

Executar a Fatia A conforme `execucao/fatias/fatia-A-ambiente.md`.

## Requisitos e cenários (copiados de spec)

- A.1: Registrar identidade da homologação (URL, host, conta admin, tablet piloto, evidências temporárias).
- A.2: Validar planejamento com `openspec validate --strict --json`; aceitar apenas `valid: true`, `issues: []`.
- A.3: Executar J00 (linha de base do MDM existente) conforme `03-provas-de-uso-real.md`; evidência: login, Devices, perfil, tablet, app permitido, site permitido.

## Escopo permitido

- Arquivos em `fiscal/` (estado.md, ledger.md, os/, relatorios/, pareceres/, fila/, escalonamentos/).
- Comandos de leitura (`openspec status/validate`, `fiscal_guard.py snapshot`).
- Registro de evidências temporárias em `<tmp>/fiscal-probes/add-webfilter-module/`.
- Nenhuma alteração em `repo-mdm/` ou `openspec/changes/add-webfilter-module/` além de `fiscal/` e `tasks.md` (marcação [x] apenas após ACEITA).

## Escopo protegido

- Nenhum arquivo do MDM existente (`repo-mdm/`) pode ser alterado.
- Nenhum arquivo de `openspec/changes/add-webfilter-module/` além de `fiscal/` pode ser alterado pelo executor.
- Nenhum comando de produção pode ser executado.
- Nenhum `commit`, `push`, `merge`, `archive` sem ordem explícita.

## Comandos de aceite

- `python3 /mnt/c/Users/40446686808/projetos/MDM/.agents/skills/openspec-fiscal/scripts/openspec_cli.py --cwd /mnt/c/Users/40446686808/projetos/MDM validate add-webfilter-module --strict --json` → `valid: true`, `issues: []`.
- J00 executado com evidência de vídeo/trace, capturas numeradas, diário com horário, hashes sha256.
- Snapshot fiscal registrado.

## Definição de pronto

- `fiscal/estado.md` atualizado com todas as tarefas de `tasks.md` em PENDENTE.
- `fiscal/ledger.md` atualizado com histórico.
- `fiscal/os/` contém esta OS.
- `fiscal/fila/para-fq/` contém pedido de auditoria para FQ.
- J00 comprovado com evidência real.
- OpenSpec strict passa.

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

**Status:** EM_TESTES (aguardando ET para A.2; EA para A.1; EJ para A.3)
**Próxima OS:** A.2 (validação strict) após A.1 (registro de ambiente)