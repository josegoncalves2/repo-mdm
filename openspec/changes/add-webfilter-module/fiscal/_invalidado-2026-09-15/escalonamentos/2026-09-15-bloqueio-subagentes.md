# Escalonamento - Historico superado de sub-agentes

## Data
2026-09-15

## Status atual

SUPERADO COMO RESTRICAO DE QUANTIDADE.

A proposta, a skill local e os guias de execucao foram revisados para remover limite documental de quantidade, paralelismo, lote ou fan-out. O FO pode convocar 400 ou mais executores quando a ferramenta suportar.

## Contexto historico

O bloqueio original foi registrado por interpretacao incorreta do FO sobre a quantidade de executores e sobre a convocacao ampla.

Essa interpretacao nao deve mais ser usada.

## Regras de Times
1. Todo agente e sub-agente deve resgistrar toda e qualquer operação que for feita, a fim de compartilhar tudo que está acontecendo com todos os demais.
2. Todo agente e Sub-agente deve manter atualizado esse registro.
3. É obrigatorio o acompanhamento desses registros por todos agentes e sub-agentes.

## Regra revisada

1. Nao ha limite documental de quantidade de sub-agentes.
2. O FO pode convocar 400 ou mais executores se a ferramenta aceitar.
3. Dependencia entre tarefas nao impede convocar executor para probe, teste, revisao, documentacao, implementacao isolada ou trabalho preparatorio.
4. Escopo sobreposto nao impede convocar executor; nesses casos o FO deve usar worktree, branch, pasta isolada, patch separado ou integracao fiscal posterior.
5. O FQ continua obrigatorio para aceitar entregas.
6. Producao continua proibida para agentes sem autorizacao explicita do responsavel humano.

## O que ainda pode bloquear

- ferramenta real de sub-agentes ausente na sessao;
- OpenSpec strict falhando;
- falta de ambiente, tablet, dominio, certificado ou credencial;
- tentativa de publicar em producao sem autorizacao explicita;
- entrega sem evidencia real ou sem parecer do FQ.

## O que nao pode mais bloquear

- quantidade 400 ja solicitada pelo responsavel humano;
- FQ nao designado quando `runSubagent` ou ferramenta equivalente existe: o FO cria FQ automatico como sub-agente real;
- pedido de nova confirmacao para o ciclo padrao TESTES -> CONSTRUCAO -> USO_REAL -> ACEITA;
- 429/rate limit transitorio: o FO registra, reduz lote, aguarda/backoff quando possivel e continua a fila.

## Proximo passo

Se `runSubagent` ou ferramenta equivalente estiver disponivel, iniciar imediatamente: criar FQ automatico se necessario, gerar OS, despachar executores ate atingir a quantidade solicitada e manter retry/backoff em caso de 429.
