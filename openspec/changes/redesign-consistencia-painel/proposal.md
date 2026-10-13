# redesign-consistencia-painel — Padronização visual e de interação do painel HWMDM

## Motivação

O painel web do HWMDM cresceu organicamente e cada tela adotou um layout próprio:
tabelas full-width, dropdowns, páginas separadas para edição, listas com estilos
diferentes. O responsável pelo produto identificou a inconsistência em todas as telas
e pediu padronização completa.

## Padrão visual alvo

A referência é a tela **Mapa de Localização**, que já implementa o layout desejado:

- **Lista lateral** com campo de busca inline no topo
- **Contadores de status** (bolinhas verde/vermelha + contagem online/offline + total) — para telas que listam devices
- **Item da lista**: dot de status + nome/serial + modelo + hora do último contato — para devices; para outros itens (apps, arquivos, usuários) adaptar com os campos relevantes
- **Item selecionado** com destaque (borda esquerda colorida + background sutil)
- **Detalhe/edição no painel direito** (inline, nunca página separada)
- **Sem confirmação de saída** desnecessária (beforeunload)

## Itens levantados

### Grupo A — Telas com lista de devices

#### A1. Acesso Remoto — lista padronizada
- **Hoje**: lista com CSS quebrado (grid antigo conflitando com layout novo), nomes sobrepostos
- **Parcialmente corrigido**: HTML reescrito para padrão do Mapa (dot + serial + modelo + hora), CSS limpo, busca movida para inline
- **Falta**: contadores online/offline no controller (bloqueado por permissão de arquivo)

#### A2. Acesso Remoto — tela remota "estourada"
- **Hoje**: canvas e overlay "Chamando o aparelho" lado a lado em vez de sobrepostos
- **Causa**: `.remote-preview-screen` usava `display: flex`
- **Já corrigido**: removido flex do CSS

#### A3. Acesso Remoto — botões de ação na lateral
- **Hoje**: botões de comando (Bloquear, Reiniciar, Redefinir fábrica, etc) esticados horizontalmente abaixo da tela
- **Desejado**: coluna lateral à direita do frame da tela do device, layout de painel de controle

#### A4. Mensagens — trocar dropdown por lista lateral
- **Hoje**: dropdown de seleção de dispositivo + textarea + histórico lado a lado
- **Desejado**: lista lateral padrão (igual Mapa) com dispositivos + área de chat à direita

#### A5. Relatórios — lista lateral + relatório por device
- **Hoje**: só relatório geral (agregado de toda a frota)
- **Desejado**: lista lateral de devices + relatório individual por device selecionado + visão unificada/geral

### Grupo B — Telas com lista de itens próprios (não devices)

#### B1. Perfis de dispositivo — edição inline
- **Hoje**: tabela de perfis, clique abre página separada (`#/config/editor/`) sem sidebar, confirmação de saída toda hora
- **Desejado**: lista lateral de perfis + edição inline (abas/seções no painel direito), sem página separada, sem beforeunload

#### B2. Aplicativos — lista lateral + detalhe inline
- **Hoje**: tabela full-width com paginação
- **Desejado**: lista lateral de apps com busca + detalhe/edição inline à direita

#### B3. Arquivos — lista lateral + detalhe inline
- **Hoje**: tabela full-width
- **Desejado**: lista lateral de arquivos + detalhe/edição inline à direita

#### B4. Ícones do launcher — lista lateral + detalhe inline
- **Hoje**: tabela full-width
- **Desejado**: lista lateral de ícones + detalhe/edição inline à direita

#### B5. Usuários do painel — lista lateral + edição inline
- **Hoje**: tabela full-width
- **Desejado**: lista lateral de usuários + edição inline à direita

#### B6. Permissões — lista lateral + edição inline
- **Hoje**: tabela full-width
- **Desejado**: lista lateral de papéis + edição inline à direita

### Grupo C — Personalização

#### C1. Aparência e marca — personalização completa
- **Hoje**: permite apenas cor principal, cor lateral, cor texto, logo do console
- **Desejado**: personalizar nome do produto, logo, favicon, tela inicial do tablet, e mais opções visuais

## O que já foi feito nesta sessão

1. ✅ CSS `.remote-preview-screen` — removido `display: flex` (canvas não "estoura" mais)
2. ✅ CSS `.remote-device-item` antigo — removido grid de 3 colunas que conflitava
3. ✅ CSS `.remote-device-item` novo — reescrito com grid 3 colunas correto (dot + main + meta)
4. ✅ CSS `.remote-status-dot` — restauradas classes de bolinha (removidas acidentalmente)
5. ✅ HTML `remote.html` — lista reescrita com padrão do Mapa (dot + serial + modelo + hora)
6. ✅ HTML `remote.html` — busca movida de toolbar para inline no painel
7. ⬜ JS `remote.controller.js` — adicionar `onlineCount`/`offlineCount` (bloqueado: arquivo do root)

## Notas técnicas

- **Deploy**: `/opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/` — arquivos do root, precisam de `chown`
- **Fonte editável**: `/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/webapp/`
- **Framework**: AngularJS 1.x — controllers, services, HTML templates
- **CSS compartilhado**: classes já existentes que serão reutilizadas:
  - `remote-device-item`, `remote-status-dot`, `remote-device-main`
  - `gps-device-search`, `gps-device-stats`, `gps-stat`, `gps-device-meta`
  - `hwmdm-split-layout`, `hwmdm-section-card`
- **Perfis**: hoje usa rota separada `#/config/editor/:id` com controller `ConfigEditorController` — precisa ser refatorado para inline

## Fases sugeridas

| Fase | Escopo | Complexidade |
|------|--------|-------------|
| 1 | A1 (terminar Remoto) + A2 (já feito) + A4 (Mensagens) | Baixa |
| 2 | A3 (botões lateral) + A5 (Relatórios por device) | Média |
| 3 | B1 (Perfis inline) | Alta — refatoração de rota e controller |
| 4 | B2-B6 (Aplicativos, Arquivos, Ícones, Usuários, Permissões) | Média — padrão repetitivo |
| 5 | C1 (Aparência e marca completa) | Média |

## Pré-requisitos

1. Rodar `sudo chown -R sahw:sahw /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/controller/` para desbloquear edição dos controllers
2. Cada fase deve ser testada no DEV (.65) antes de avançar
