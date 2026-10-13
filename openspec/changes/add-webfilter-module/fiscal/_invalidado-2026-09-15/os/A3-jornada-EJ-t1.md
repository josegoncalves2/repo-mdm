# Ordem de Serviço - Tarefa A.3 (Executar J00)

## Dados da OS

- **Change:** add-webfilter-module
- **Fatia:** A - Ambiente e linha de base
- **Tarefa:** A.3 - Executar J00
- **Data:** 2026-09-15
- **FO:** Fiscal Orquestrador (esta sessão)
- **FQ:** Fiscal de Qualidade e Conformidade (independente)
- **Executor:** EJ (Executor de Jornada)

## Requisitos e cenários (copiados de spec)

**Tarefa A.3:** Executar J00 (linha de base do MDM existente)

**Objetivo:** provar que o MDM atual funciona antes do WebFilter.

**Pessoa:** administrador MDM e usuario do tablet.

**Passos:**

1. Abrir a URL de homologacao no navegador.
2. Fazer login pela tela.
3. Abrir Devices pelo menu lateral.
4. Abrir um dispositivo piloto.
5. Abrir o perfil do dispositivo.
6. No tablet, confirmar que o launcher esta ativo.
7. Abrir um app permitido.
8. Abrir o navegador e acessar `wikipedia.org`.
9. Acionar sync normal do tablet ou aguardar sync, conforme procedimento do ambiente.

**Constatacoes:**

- console autentica pela tela;
- dispositivo piloto aparece;
- perfil abre sem erro visual;
- tablet continua gerenciado;
- app permitido abre;
- site permitido resolve e carrega;
- nenhum erro novo aparece na tela.

## Escopo permitido

- Arquivos em `fiscal/` (estado.md, ledger.md, os/, relatorios/, pareceres/, fila/, escalonamentos/).
- Comandos de leitura (`openspec status/validate`, `fiscal_guard.py snapshot`).
- Registro de evidências temporárias em `<tmp>/fiscal-probes/add-webfilter-module/`.
- Uso do console de homologacao e tablet fisico para jornada J00.
- Nenhuma alteração em `repo-mdm/` ou `openspec/changes/add-webfilter-module/` além de `fiscal/`.

## Escopo protegido

- Nenhum arquivo do MDM existente (`repo-mdm/`) pode ser alterado.
- Nenhum arquivo de `openspec/changes/add-webfilter-module/` além de `fiscal/` pode ser alterado pelo executor.
- Nenhum comando de producao pode ser executado.
- Nenhum `commit`, `push`, `merge`, `archive` sem ordem explicita.

## Comandos de aceite

- J00 executado com evidencia de video/trace do login no console.
- Captura de Devices e perfil.
- Screenrecord ou capturas do tablet abrindo app permitido e site permitido.
- Diario da jornada com horario, acao e resultado visto.
- Hashes sha256 dos arquivos de evidencia.
- Confirmacao de que nenhum arquivo fora do escopo foi alterado.
- Confirmacao de que a linha de base J00 permanece funcional.

## Definicao de pronto

- Evidencia da jornada J00 coletada e registrada em `<tmp>/fiscal-probes/add-webfilter-module/A3-jornada/`.
- FQ reexecuta a jornada e confirma constatacoes.
- Tarefa A.3 marcada como EM_TESTES (aguardando FQ para auditoria).

## Proibições

- Não editar codigo do produto.
- Não alterar testes.
- Não alterar specs/design/proposal.
- Não aceitar entrega sem parecer do FQ.
- Não usar `--force`, `--no-verify`, skip, ignore, teste desabilitado ou placeholder.
- Não fazer commit, push, merge, archive ou producao sem autorizacao explicita.
- Não tratar relatorio de executor como evidencia.

## Formato obrigatorio do relatorio do executor

- Texto da tarefa executada.
- Comandos executados com saida completa.
- Evidencias (arquivos, hashes, capturas).
- Resultado do comando de aceite.
- Confirmacao de que nenhum arquivo fora do escopo foi alterado.
- Confirmacao de que a linha de base J00 permanece funcional.

## Devolucao (se reprovar)

- Defeitos numerados com evidencia (arquivo:linha ou comando + saida).
- Comportamento esperado.
- Critério de reaprovação.
- Reenvio ao mesmo executor ou novo executor conforme estrategia.

---

**Status:** EM_TESTES (aguardando EJ para executar J00)
**Próxima OS:** Após EJ executar e enviar relatorio, enviar ao FQ para auditoria TESTES.