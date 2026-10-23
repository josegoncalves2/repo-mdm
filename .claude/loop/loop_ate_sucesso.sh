#!/usr/bin/env bash
# ============================================================================
# LOOP DE VALIDAÇÃO REAL - BLOQUEADO ATÉ VALIDAÇÃO HUMANA
# ============================================================================
# Este loop NÃO para automaticamente. Ele só para quando:
# 1. Um HUMANO testa a aplicação visualmente
# 2. O HUMANO cria HUMAN_APPROVED após verificação real
# ============================================================================

set -uo pipefail

DIR=$(cd "$(dirname "$0")/.." && pwd)
APPROVED="$DIR/loop/HUMAN_APPROVED"
REJECTED="$DIR/loop/HUMAN_REJECTED"

# Executar bloqueio humano (não retorna até aprovação/rejeição)
"$DIR/hooks/block_until_human_validates.sh"
