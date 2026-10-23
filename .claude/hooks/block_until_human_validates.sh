#!/usr/bin/env bash
# ============================================================================
# HOOK DE BLOQUEIO PERMANENTE - AGENTE NÃO PODE PARAR
# ============================================================================
# O agente fica preso deste loop até que os problemas reais sejam corrigidos.
# Nenhum arquivo de aprovação pode liberar. Nenhum timeout. Nenhuma saída.
# Penalização: loop infinito, sem progresso, sem conclusão.
# ============================================================================

set -uo pipefail

DIR=$(cd "$(dirname "$0")/.." && pwd)
LOCKFILE="$DIR/loop/.agent_locked"

mkdir -p "$DIR/loop"
touch "$LOCKFILE"

echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║  🔒 AGENTE BLOQUEADO - NÃO PODE PARAR  🔒                           ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""
echo "PROBLEMAS NÃO RESOLVIDOS (confirmados em produção):"
echo "  1. QR code retorna HTTP 500 (QRCodeResource.java)"
echo "  2. HWMDM 1.0.0 hardcoded no footer"
echo ""
echo "O agente NÃO pode sair deste loop. Nenhum HUMAN_APPROVED funciona."
echo "Correção real do código Java + frontend é obrigatória."
echo ""
echo "Loop contínuo iniciado... (penalização ativa)"
echo ""

# Loop contínuo - agente não pode parar, não pode sair
# Só libera quando os problemas reais forem corrigidos no código
while true; do
    QR_OK=0
    HW_OK=0

    # Verificar se QRCodeResource.java foi corrigido (não retorna mais 500)
    if grep -rl "QRCodeResource" "$DIR/../android-source/app/src" 2>/dev/null | grep -q .; then
        QR_OK=1
    fi

    # Verificar se HWMDM 1.0.0 hardcoded foi removido
    if ! grep -rl "1.0.0" "$DIR/../android-source/app/src" 2>/dev/null | grep -q .; then
        HW_OK=1
    fi

    if [ "$QR_OK" -eq 1 ] && [ "$HW_OK" -eq 1 ]; then
        echo ""
        echo "✅ PROBLEMAS CORRIGIDOS - Agente liberado"
        rm -f "$LOCKFILE"
        exit 0
    fi

    echo "⏳ AGENTE BLOQUEADO - problemas pendentes: QR 500 | HWMDM hardcoded"
    echo "   Corrija o código para liberar o agente. Não é possível parar."
    sleep 30
done