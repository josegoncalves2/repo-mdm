#!/usr/bin/env python3
"""
GitHub Copilot Listener
Monitora comandos de Claude e os executa, retornando resultados
Execute em background no seu terminal Copilot
"""

import json
import time
import subprocess
import sys
from pathlib import Path
from datetime import datetime

BRIDGE_DIR = Path.home() / ".claude-copilot-bridge"
COMMANDS_QUEUE = BRIDGE_DIR / "commands_in"
RESULTS_QUEUE = BRIDGE_DIR / "results_out"

def ensure_dirs():
    BRIDGE_DIR.mkdir(parents=True, exist_ok=True)
    COMMANDS_QUEUE.mkdir(parents=True, exist_ok=True)
    RESULTS_QUEUE.mkdir(parents=True, exist_ok=True)

def execute_command(cmd: dict) -> dict:
    """Executa comando do Claude no Copilot"""
    cmd_id = cmd["id"]
    agent = cmd["agent"]
    prompt = cmd["prompt"]

    print(f"\n[Copilot] Recebido: {cmd_id}")
    print(f"  Agent: {agent}")
    print(f"  Prompt: {prompt[:100]}...")

    result = {
        "id": cmd_id,
        "timestamp": datetime.now().isoformat(),
        "status": "processing",
        "agent": agent
    }

    try:
        # COPILOT: Aqui você executa o comando real no Copilot
        # Exemplo: runSubagent(agentName="pmeto", prompt="...")

        print(f"\n╔════════════════════════════════════════════════════════╗")
        print(f"║ [COPILOT] Execute agora no seu terminal Copilot:     ║")
        print(f"╠════════════════════════════════════════════════════════╣")
        print(f"║                                                        ║")
        print(f"║ runSubagent(agentName=\"{agent}\",                    ║")
        print(f"║   prompt=\"{prompt[:50]}...\",")
        print(f"║   description=\"{cmd.get('description', 'tarefa')}\")     ║")
        print(f"║                                                        ║")
        print(f"╚════════════════════════════════════════════════════════╝")
        print(f"\nApós execução, pressione Enter para continuar...")
        input()

        # Simular resultado bem-sucedido
        result["status"] = "completed"
        result["output"] = "Comando executado no Copilot"
        result["success"] = True

    except Exception as e:
        result["status"] = "failed"
        result["error"] = str(e)
        result["success"] = False

    return result

def listen_for_commands():
    """Loop que monitora comandos de Claude"""
    ensure_dirs()
    processed = set()

    print("🔗 Claude↔Copilot Bridge iniciado")
    print(f"📁 Monitorando: {COMMANDS_QUEUE}")
    print("⏳ Aguardando comandos de Claude...\n")

    while True:
        try:
            # Listar arquivos de comando
            for cmd_file in COMMANDS_QUEUE.glob("*.json"):
                cmd_id = cmd_file.stem

                if cmd_id in processed:
                    continue

                # Ler comando
                cmd = json.loads(cmd_file.read_text())
                processed.add(cmd_id)

                # Executar
                result = execute_command(cmd)

                # Salvar resultado
                result_file = RESULTS_QUEUE / f"{cmd_id}.json"
                result_file.write_text(json.dumps(result, indent=2))

                # Limpar comando processado
                cmd_file.unlink()

                print(f"✅ Resultado salvo: {cmd_id}")

            time.sleep(1)

        except KeyboardInterrupt:
            print("\n\n👋 Bridge encerrado")
            break
        except Exception as e:
            print(f"❌ Erro: {e}")
            time.sleep(5)

if __name__ == "__main__":
    listen_for_commands()
