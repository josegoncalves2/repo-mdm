#!/usr/bin/env bash
# ============================================================================
# VERIFICADOR REAL - APENAS VALIDADO POR HUMANO
# ============================================================================
# Este script VERIFICA se a aplicação está rodando, mas NÃO decide.
# A decisão final é SEMPRE humana.
# ============================================================================

set -uo pipefail

DIR=$(cd "$(dirname "$0")/.." && pwd)
COMPOSE=(docker compose -f "$DIR/source/docker-compose.yaml" --env-file "$DIR/source/.env")

echo "══════════════════════════════════════════════════════════════════════"
echo "🔍 VERIFICAÇÃO TÉCNICA (apenas informativa)"
echo "══════════════════════════════════════════════════════════════════════"
echo ""

# Verificar se os containers estão rodando
echo "Verificando containers..."
for svc in postgresql admin hmdm webfilter-dns; do
    cid=$("${COMPOSE[@]}" ps -q "$svc" 2>/dev/null)
    if [ -n "$cid" ]; then
        st=$(docker inspect -f '{{.State.Status}}' "$cid" 2>/dev/null)
        echo "  ✓ $svc: $st"
    else
        echo "  ✗ $svc: NÃO ESTÁ RODANDO"
    fi
done

echo ""
echo "══════════════════════════════════════════════════════════════════════"
echo "📱 VALIDAÇÃO HUMANA NECESSÁRIA"
echo "══════════════════════════════════════════════════════════════════════"
echo ""
echo "AGORA UM HUMANO DEVE:"
echo ""
echo "  1. Abrir navegador em: http://localhost:8080"
echo "  2. Testar a aplicação MANUALMENTE:"
echo "     - Tema dark/light toggle"
echo "     - Funcionalidades do painel"
echo "     - Interação com dispositivos"
echo "  3. Verificar que NADA está quebrado"
echo ""
echo "  4. SE OK:   touch $DIR/loop/HUMAN_APPROVED"
echo "  5. SE ERRO:  touch $DIR/loop/HUMAN_REJECTED"
echo ""
echo "══════════════════════════════════════════════════════════════════════"