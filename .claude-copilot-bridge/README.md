# Claude ↔ GitHub Copilot Bridge

Conexão bidirecional entre Claude Code e GitHub Copilot via IPC (Inter-Process Communication).

## Como Funciona

```
Claude Code              Bridge IPC              GitHub Copilot
    │                   (arquivos)                    │
    ├─ send_to_copilot()─┐                            │
    │                    ├─ commands_in/*.json   ─────┤
    │                    │                             │
    │                    │              copilot_listener.py
    │                    │                    (monitora)
    │                    │                             │
    │  wait_for_result() ├─ results_out/*.json  ◄─────┤
    │  ◄────────────────┘  (feedback)                  │
    │                                                   │
```

## Setup

### 1. No Claude Code (aqui)

```python
from claude_to_copilot import send_to_copilot, wait_for_result

# Enviar comando para Copilot
cmd_id = send_to_copilot(
    agent_name="pmeto",
    prompt="provisione e garanta a aplicação...",
    description="correção de bugs"
)

# Aguardar resultado
result = wait_for_result(cmd_id, timeout=3600)
```

### 2. No GitHub Copilot

Em outro terminal, inicie o listener:

```bash
cd ~/.claude-copilot-bridge
python copilot_listener.py
```

Ele vai:
1. Monitorar comandos de Claude
2. Exibir instruções para você executar
3. Registrar resultado
4. Retornar para Claude

## Arquitetura

```
~/.claude-copilot-bridge/
├── commands_in/         # Fila de comandos de Claude → Copilot
│   └── {cmd_id}.json
├── results_out/         # Fila de resultados de Copilot → Claude
│   └── {cmd_id}.json
├── claude_to_copilot.py # Cliente para Claude
└── copilot_listener.py  # Servidor para Copilot
```

## Uso Real

```python
# Em Claude Code
cmd_id = send_to_copilot("pmeto", "provisione /openspec/", "fix bugs")
result = wait_for_result(cmd_id)
```

No terminal Copilot, você verá:

```
╔════════════════════════════════════════════════════════╗
║ [COPILOT] Execute agora no seu terminal Copilot:     ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║ runSubagent(agentName="pmeto",                        ║
║   prompt="provisione /openspec/",                     ║
║   description="fix bugs")                             ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

Execute o comando, pressione Enter, e o resultado volta para Claude.

## Status

- ✅ Fila de comandos
- ✅ Fila de resultados  
- ✅ Timeout de espera
- ✅ Monitoramento automático
- ⏳ WebSocket (futuro)
- ⏳ REST API (futuro)

## Próximas Melhorias

1. Auto-start do listener no Copilot
2. Timeout inteligente por tipo de tarefa
3. Logging centralizado
4. Interface Web para monitorar
5. Suporte a múltiplos agentes paralelos
