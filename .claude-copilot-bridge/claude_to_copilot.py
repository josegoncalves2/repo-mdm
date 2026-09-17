#!/usr/bin/env python3
"""
Claude → GitHub Copilot Bridge
Permite Claude Code enviar comandos para GitHub Copilot via arquivo IPC
"""

import json
import time
import os
import sys
from pathlib import Path
from datetime import datetime
import uuid

BRIDGE_DIR = Path.home() / ".claude-copilot-bridge"
COMMANDS_QUEUE = BRIDGE_DIR / "commands_in"
RESULTS_QUEUE = BRIDGE_DIR / "results_out"

def ensure_dirs():
    """Criar diretórios se não existem"""
    BRIDGE_DIR.mkdir(parents=True, exist_ok=True)
    COMMANDS_QUEUE.mkdir(parents=True, exist_ok=True)
    RESULTS_QUEUE.mkdir(parents=True, exist_ok=True)

def send_to_copilot(agent_name: str, prompt: str, description: str) -> str:
    """
    Envia comando para GitHub Copilot executar
    Retorna: ID do comando para rastrear resultado
    """
    ensure_dirs()

    cmd_id = str(uuid.uuid4())[:8]

    command = {
        "id": cmd_id,
        "timestamp": datetime.now().isoformat(),
        "agent": agent_name,
        "prompt": prompt,
        "description": description,
        "status": "pending"
    }

    # Escrever arquivo de comando
    cmd_file = COMMANDS_QUEUE / f"{cmd_id}.json"
    cmd_file.write_text(json.dumps(command, indent=2))

    print(f"[Claude->Copilot] Comando enviado: {cmd_id}")
    print(f"  Agent: {agent_name}")
    print(f"  Description: {description}")

    return cmd_id

def wait_for_result(cmd_id: str, timeout: int = 300) -> dict:
    """
    Aguarda resultado do comando executado pelo Copilot
    timeout em segundos
    """
    start = time.time()

    while time.time() - start < timeout:
        result_file = RESULTS_QUEUE / f"{cmd_id}.json"

        if result_file.exists():
            result = json.loads(result_file.read_text())
            print(f"[Copilot->Claude] Resultado recebido:")
            print(f"  Status: {result.get('status')}")
            if result.get('error'):
                print(f"  Erro: {result['error']}")
            return result

        time.sleep(1)

    raise TimeoutError(f"Timeout aguardando resultado de {cmd_id}")

def main():
    """Exemplo de uso"""
    if len(sys.argv) < 2:
        print("Uso: python claude_to_copilot.py <agent_name> [prompt]")
        print("\nExemplo:")
        print("  python claude_to_copilot.py pmeto 'provisione e garanta...'")
        return

    agent = sys.argv[1]
    prompt = sys.argv[2] if len(sys.argv) > 2 else "execute com prompt padrão"

    # Enviar comando
    cmd_id = send_to_copilot(agent, prompt, "tarefa automática")

    # Aguardar resultado
    try:
        result = wait_for_result(cmd_id, timeout=3600)
        print(f"\n✅ Completado: {result.get('status')}")
    except TimeoutError:
        print(f"\n⏳ Ainda aguardando resultado...")

if __name__ == "__main__":
    main()
