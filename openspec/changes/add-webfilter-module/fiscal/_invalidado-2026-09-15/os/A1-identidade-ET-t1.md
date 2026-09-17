# Ordem de Serviço - Tarefa A.1 (Registrar identidade da homologacao)

## Dados da OS

- **Change:** add-webfilter-module
- **Fatia:** A - Ambiente e linha de base
- **Tarefa:** A.1 - Registrar identidade da homologacao
- **Data:** 2026-09-15
- **FO:** Fiscal Orquestrador (esta sessão)
- **FQ:** Fiscal de Qualidade e Conformidade (independente)
- **Executor:** EA (Executor de Ambiente)

## Requisitos e cenários (copiados de spec)

**Tarefa A.1:** Registrar identidade da homologacao

1. registrar URL do console de homologacao;
2. registrar host/usuario que roda servidor;
3. registrar como distinguir homologacao de producao;
4. registrar conta admin de teste;
5. registrar tablet piloto: serial, Android, launcher atual, perfil, cliente;
6. registrar onde ficam evidencias temporarias.

## Escopo permitido

- Arquivos em `fiscal/` (estado.md, ledger.md, os/, relatorios/, pareceres/, fila/, escalonamentos/).
- Comandos de leitura (`openspec status/validate`, `fiscal_guard.py snapshot`).
- Registro de evidências temporárias em `<tmp>/fiscal-probes/add-webfilter-module/`.
- Nenhuma alteração em `repo-mdm/` ou `openspec/changes/add-webfilter-module/` além de `fiscal/`.

## Escopo protegido

- Nenhum arquivo do MDM existente (`repo-mdm/`) pode ser alterado.
- Nenhum arquivo de `openspec/changes/add-webfilter-module/` além de `fiscal/` pode ser alterado pelo executor.
- Nenhum comando de produção pode ser executado.
- Nenhum `commit`, `push`, `merge`, `archive` sem ordem explícita.

## Comandos de aceite

- `fiscal/estado.md` atualizado com tarefa A.1 em PENDENTE.
- `fiscal/ledger.md` atualizado com registro.
- `fiscal/os/` contém esta OS.
- `fiscal/fila/para-fq/` contém pedido de auditoria para FQ.
- Evidências de registro (URL, host, conta, tablet) com hashes sha256.

## Definição de pronto

- Todos os 6 itens de registro completados com evidência.
- FQ reexecuta e confirma registro.
- Tarefa A.1 marcada como EM_TESTES (aguardando ET para A.2).

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

**Status:** EM_TESTES (aguardando ET para A.2)
**Próxima OS:** A.2 (validação strict) após A.1 (registro de ambiente)