# 01 — Prompts dos papéis

Cada prompt tem **uma única fonte**, para que ninguém edite uma cópia e esqueça a outra:

| Papel | Fonte do prompt | Como é usado |
|---|---|---|
| FISCAL | `.claude/commands/fiscal-permissoes.md` | o humano digita `/fiscal-permissoes` numa sessão do Claude Code aberta em `/opt/projetos/hwmdm` ou em `repo-mdm/` |
| EXECUTOR | `.claude/agents/hwmdm-executor.md` | `Agent` com `subagent_type: hwmdm-executor`; prompt = OS inteira (M1) + rótulo |
| AMBIENTE | `.claude/agents/hwmdm-ambiente.md` | `Agent` com `subagent_type: hwmdm-ambiente`; prompt = OS de ambiente + caminho do relatório do EXECUTOR + rótulo |
| USUÁRIO | `.claude/agents/hwmdm-usuario.md` | `Agent` com `subagent_type: hwmdm-usuario`; prompt = OS com a jornada literal + recorte do inventário + rótulo |
| VALIDADOR | `.claude/agents/hwmdm-validador.md` | `Agent` com `subagent_type: hwmdm-validador`; prompt = **somente** o caminho do pedido M3 + rótulo designado |

`/opt/projetos/hwmdm/.claude/agents` e `.../commands/fiscal-permissoes.md` são links para os arquivos do repositório, para que a sessão aberta na pasta-mãe enxergue os mesmos agentes.

## Regras de despacho (FISCAL)

1. `guarda.py registrar "despacho <rótulo> <subagent_type>"` **antes** de cada chamada `Agent`.
2. `description` da chamada = rótulo (ex.: `EXE-1.1-t1`).
3. Sempre um sub-agente **novo** por OS. Nunca reaproveite um sub-agente de outro papel ou de outra tentativa (a devolução ao mesmo EXECUTOR usa `SendMessage` ao mesmo rótulo, registrada no ledger como `devolucao <rótulo>`).
4. O pedido ao VALIDADOR não contém opinião, resumo nem "já está pronto": só os caminhos.
5. Se a ferramenta `Agent` não existir ou falhar de forma permanente, o FISCAL **para** e escala. Ele nunca faz o papel do sub-agente.
