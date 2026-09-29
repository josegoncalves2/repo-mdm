# 🔴 RELATÓRIO DE AUDITORIA QA — HEADWIND MDM

**Data:** 29/09/2026 | **Versão:** HWMDM 1.0.0 build 1 · 76f2734
**Ambiente:** mdm.pmeto.local:8080 | **4 dispositivos:** SM-T225 (Samsung Galaxy Tab)
**Analista:** QA (modo cuzão, sem dó)

---

## 🚨 CRÍTICOS — Requer ação imediata

### 1. 3 dispositivos OFFLINE há 50 DIAS sem alerta/notificação
| Dispositivo | Último reporte | Dias offline |
|-------------|----------------|--------------|
| R9XT106VP1E | 10/08/2026 | ~50 |
| R9XT106Y5RP | 10/08/2026 | ~50 |
| R9XT108EM8T | 10/08/2026 | ~50 |

- O sistema mostra "No report for more than 7 days" mas **não dispara alerta, email, push ou ação corretiva**
- Existe um banner "3 device(s) have not reported for more than 7 days" mas é fácil de ignorar
- **Correção:** Implementar notificação proativa (email/push) e destacar visualmente em vermelho com badge

### 2. TODOS os dispositivos com app desatualizada — sem atualização automática
| Dispositivo | Instalado | Disponível |
|-------------|-----------|------------|
| R9XT106VP1E | 1.14 | 1.29 |
| R9XT106Y5RP | 1.14 | 1.29 |
| R9XT108EM8T | 1.14 | 1.29 |
| R9XT200AMYY | 1.28 | 1.29 |

- Botão "Update now" existe mas **depende de ação manual do admin**
- 100% dos devices com app desatualizada — falha sistêmica
- **Correção:** Política de atualização automática OTA para apps críticas (Suporte Remoto)

### 3. Launcher version 6.36 — configuração Kiosk Total é 6.37.3
- Todos os devices rodando launcher **6.36**, mas a config atribuída é "Kiosk Total (6.37.3)"
- A versão do launcher **não bate com a configuração** — ninguém percebeu
- **Correção:** Verificar pipeline de deploy do launcher — ou força atualização OTA ou corrige a configuração

---

## 🟡 MÉDIOS — Requer planejamento

### 4. Configuração "teste" em produção
- Filtro de configurações ainda mostra opção `teste` ao lado de configs reais
- Polui o seletor e pode causar confusão
- **Correção:** Remover configuração "teste"

### 5. "Group action" permanentemente desabilitado
- Botão nunca habilita, mesmo com checkboxes marcados
- Funcionalidade de ação em lote simplesmente não funciona
- **Correção:** Implementar ações em lote ou remover o botão

### 6. Kiosk mode = "no" para TODOS os dispositivos
- Coluna Kiosk mode mostra "no" em todos — se o dispositivo está em kiosk, deveria mostrar "yes"
- Indica que o report do kiosk mode pode estar quebrado
- **Correção:** Verificar se o campo kiosk mode está sendo populado corretamente

### 7. Files status e Description sempre vazios
- Colunas ocupam espaço na tabela mas **nenhum dispositivo tem dado**
- Description: vazio em 4/4 devices
- Files status: vazio em 4/4 devices
- **Correção:** Ocultar colunas por default ou preencher automaticamente

### 8. Bateria entre 44%-78% sem alerta configurável
- R9XT106VP1E com **44%** — não crítico hoje, mas não há threshold configurável
- **Melhoria:** Implementar alerta configurável (< 20%, < 10%)

### 9. Location map — mapa sem devices visíveis
- Nenhum dispositivo aparece no mapa GPS
- Pode ser que GPS não esteja sendo reportado ou módulo desabilitado
- **Correção:** Verificar se o módulo de GPS está funcionando e os devices reportando

### 10. Messages (Chat) — conteúdo não acessível via snapshot
- O Angular não expõe o conteúdo do chat no DOM acessível
- **Correção:** Garantir acessibilidade no módulo de chat

---

## 🔧 MELHORIAS — Sugestões

### 11. Botões de ação inconsistentes entre dispositivos
- Devices online mostram: Edit, QR code, Remote access, More ...
- **Sugestão:** Padronizar ações visíveis e mover ações secundárias para dentro do "More"

### 12. Tooltip "Fast search by number" existe mas nome confuso
- Tooltip explica bem, mas o nome é ambíguo
- **Sugestão:** Renomear para "Busca exata por número (final)"

### 13. Paginação 1-4/4 sem controles de navegação
- Mostra "1-4/4" e seletor "Per page" mas não tem botões de página anterior/próxima
- **Sugestão:** Implementar navegação completa de paginação

### 14. Versão do Android fixa em 14 para todos
- Todos SM-T225 com Android 14 — correto, mas sem destaque para versões diferentes
- **Sugestão:** Destacar visualmente versões desatualizadas (< 13)

### 15. Navegação com data em formato inconsistente
- Header da coluna mostra "10/08/2026 16:18" mas tooltip mostra "2 mon ago"
- **Sugestão:** Padronizar formato de data em todo o sistema

---

## 📊 RESUMO FINAL

| Gravidade | Qtd | Prioridade |
|-----------|-----|------------|
| 🔴 Crítico | 3 | Imediata |
| 🟡 Médio | 7 | Curto prazo |
| 🔧 Melhoria | 5 | Médio prazo |
| **Total** | **15** | |

**Veredito do QA:** O sistema funciona, mas está largado às traças. 75% dos dispositivos estão abandonados há 50 dias, apps desatualizadas em 100% dos devices, configuração "teste" em produção, launcher com versão errada, kiosk mode reportando "no" em kiosks. Isso não é MDM de produção — é um sistema que ninguém está monitorando. Precisa de correções urgentes antes de qualquer rollout.