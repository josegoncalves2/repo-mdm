# Manual de execucao do change `add-webfilter-module`

Este manual define como o modulo WebFilter e desenvolvido por agentes de IA. Ele e so documentacao e nao inicia implementacao, publicacao em homologacao ou publicacao em producao.

O que construir esta em `../proposal.md`, `../design.md`, `../specs/**` e `../tasks.md`. Em caso de conflito, os artefatos OpenSpec prevalecem e o conflito vai ao responsavel humano.

## Arquivos

| Arquivo | Conteudo | Quem le |
|---|---|---|
| `README.md` | visao geral, papeis, estados, ordem das fatias, regras de ouro | fiscais |
| `01-papeis-e-prompts.md` | responsabilidades, limites e prompts de sistema | fiscais; FO copia o prompt do executor na OS |
| `02-ciclo-da-microtarefa.md` | ciclo E0-E14, comandos e modelos | fiscais |
| `03-provas-de-uso-real.md` | politica de prova e jornadas J00-J13 | fiscais e EJ |
| `fatias/*.md` | passo a passo de cada microtarefa | fiscais |

Registros gerados durante execucao ficam em `../fiscal/`. Registros anteriores a 2026-09-16 que nao tenham sub-agente real, FQ independente, vermelho confirmado e `fiscal_guard.py` sao invalidos e devem permanecer arquivados em `../fiscal/_invalidado-2026-09-15/`.

Evidencias pesadas nunca entram no repositorio. Ficam em `<tmp>/fiscal-probes/add-webfilter-module/<tarefa>/`, com sha256 registrado no parecer.

## Papeis

| Papel | Sigla | Faz | Nunca faz |
|---|---|---|---|
| Agente Fiscal Orquestrador | FO | emite OS, despacha executores, mantem estado/ledger, marca `[x]` apos parecer ACEITA | codificar, testar, julgar qualidade, aprovar sem FQ |
| Agente Fiscal de Qualidade | FQ | reexecuta gates, confere diff/escopo, verifica prova de uso real, veta | codificar, despachar executor, aceitar relatorio como prova |
| Executor de Testes | ET | escreve testes de construcao | implementar comportamento |
| Executor de Implementacao | EI | implementa no escopo ate testes passarem | tocar testes protegidos ou sair do escopo |
| Executor de Ambiente | EA | prepara homologacao | tocar producao ou alterar codigo |
| Executor de Jornada | EJ | usa console/tablet como pessoa real e grava evidencia | alterar codigo, banco ou configuracao fora do roteiro |

Se o FQ for sub-agente do FO, toda jornada de console/tablet exige J13 (assinatura humana da evidencia principal) antes de `ACEITA`.

## Estados

```text
PENDENTE -> EM_TESTES -> VERMELHO_CONFIRMADO -> EM_IMPLEMENTACAO -> CONSTRUIDA -> EM_JORNADA -> ACEITA
```

Desvios: `REPROVADA` volta com devolucao; `BLOQUEADA` exige decisao humana. `[x]` em `tasks.md` so com parecer `ACEITA`.

## Ordem de execucao por fatias

| Fatia | Resultado visivel | Tarefas | Jornada | Depende de |
|---|---|---|---|---|
| A | homologacao pronta e linha de base registrada | baseline fiscal | J00 | - |
| B | resolvedor DNS-over-TLS passa gate tecnico isolado | 1.1-1.6 | J01 | A |
| C | plugin entra no WAR, schema aplica e catalogos carregam | 2.1, 2.2, 3.1, 3.2, 3.3 | J02, J06 | B |
| D | politica valida entradas e calcula apps | 4.1-4.3 | J05A/J05B | C |
| E | salvar politica gera arquivos do Blocky | 5.1, 5.2 | J04 | C, D |
| F | REST, push e sync funcionam no caminho real | 6.1-6.4 | J02, J05A/J05B | C, D |
| G | launcher novo aplica DNS privado | 7.1, 7.2 | J07 | E, F |
| H | console Web Filter completo e responsivo | 8.1-8.4 | J02, J03, J06, J08 | C, D |
| I | ponta a ponta em tablet real | 9.1-9.5 | J04, J05B, J10, J11, J13 quando aplicavel | G, H |
| J | runbook, rollback e producao humana | 10.1-10.4 | J12, J13 quando aplicavel | I |

Depois da Fatia C, o FO pode despachar trabalho preparatorio em paralelo, mas integracao e aceite respeitam dependencias e parecer do FQ.

## Regras de ouro

1. Planejamento e lei. Nenhum executor edita `openspec/**`.
2. Uma OS, uma entrega verificavel, um executor.
3. Quem escreve teste nao implementa; quem implementa nao prova uso real.
4. Relatorio e alegacao; evidencia e o que o FQ reexecuta ou ve.
5. Uso real nao e aceito por `curl`, porta, healthcheck, `ping`, `dumpsys`, log ou teste unitario isolado.
6. So os arquivos listados no escopo da OS podem mudar.
7. Nada e publicado por agentes em producao.
8. Falta de ambiente, evidencia ou comando reproduzivel bloqueia.
9. OS de implementacao sem `VERMELHO_CONFIRMADO` e invalida.
10. Fatia B nao comeca sem J00 aceita.

## Quando chamar o humano

- falta tablet, conta, dominio, certificado ou homologacao;
- spec/design contraditorio;
- requisito exige escopo novo;
- cinco reprovas na mesma microtarefa;
- qualquer passo toca producao;
- FQ sub-agente precisa de assinatura J13 para jornada.
