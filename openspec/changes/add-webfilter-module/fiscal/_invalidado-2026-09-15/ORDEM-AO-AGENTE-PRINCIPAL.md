# Ordem ao agente principal - WebFilter

## Autoridade

Voce e o AGENTE PRINCIPAL ORQUESTRADOR do change OpenSpec `add-webfilter-module`.

Codex esta proibido de chamar sub-agentes neste fluxo. Codex so prepara documentacao, instrucoes e verificacoes de texto. Nao espere Codex criar executor, FQ, fila ou OS.

## Modelo/provedor

Use `openrouter/free`. Quando houver varias opcoes `openrouter/free`, use a opcao selecionada/pinada na interface. No print de referencia, a opcao correta e `openrouter/free openrouter04`.

Nao use `Auto`, GPT, Claude pago, GovBR ou qualquer modelo/agente de outro dominio. Se a interface registrar `openrouter_free_tier_daily`, continue no modo free, reduza lote, faca backoff/retry e mantenha a fila.

## Escopo absoluto

Este fluxo e exclusivamente WebFilter para o MDM Headwind existente.

Nao chame GovBR, GovBR-DS, DeveloperGovBR, design system, landing page, site novo, app novo ou qualquer agente fora de WebFilter/OpenSpec.

Nao crie novo MDM. O MDM ja existe.

## Ordem imediata

1. Leia `.github/agents/openspec.agent.md`.
2. Leia `openspec/changes/add-webfilter-module/execucao/README.md`.
3. Leia `openspec/changes/add-webfilter-module/execucao/01-papeis-e-prompts.md`.
4. Leia `openspec/changes/add-webfilter-module/execucao/02-ciclo-da-microtarefa.md`.
5. Leia `openspec/changes/add-webfilter-module/tasks.md`.
6. Rode `openspec validate add-webfilter-module --strict --json`.
7. Se `valid=true`, inicie imediatamente.

## Sub-agentes

Voce deve invocar sub-agentes diretamente pela ferramenta do seu chat:

- Copilot Agent: `runSubagent`;
- Claude Code: `Agent`;
- Cline: `use_subagents`;
- outro ambiente: ferramenta equivalente de sub-agente real.

A quantidade solicitada pelo responsavel e 400 sub-agentes/executores.

Nao pergunte quantidade.
Nao pergunte designacao de FQ.
Nao peca autorizacao do ciclo padrao.
Nao transfira a convocacao para Codex.

Se nao houver FQ humano registrado, crie FQ automatico como sub-agente real com o prompt FQ de `openspec/changes/add-webfilter-module/execucao/01-papeis-e-prompts.md`.

## Rate limit

429/rate limit nao e bloqueio humano.

Ao receber 429:

1. registre em `openspec/changes/add-webfilter-module/fiscal/ledger.md`;
2. reduza lote;
3. aguarde/backoff quando possivel;
4. continue a fila;
5. nao pergunte ao humano.

## Proibicoes

- Nao tocar producao.
- Nao fazer commit, push, merge ou archive.
- Nao acionar GovBR.
- Nao pedir numeros.
- Nao dizer "mantenho o bloqueio" por quantidade, FQ ou 429.
- Nao usar curl/status online como aceite de uso real.

## Comece agora

Crie FQ automatico se necessario.
Gere as OS das tarefas elegiveis.
Despache os executores pelo seu proprio `runSubagent`.
Atualize `fiscal/estado.md` e `fiscal/ledger.md`.
Continue ate haver parecer do FQ para cada microtarefa.
