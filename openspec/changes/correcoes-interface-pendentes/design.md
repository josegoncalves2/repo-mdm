# Design — correcoes-interface-pendentes

## Context

O painel HWMDM DEV (192.168.1.65:8080) roda em Tomcat dentro de Docker.
Alterações de frontend (HTML/CSS/JS/i18n) são editadas no volume
`volumes/webapps/ROOT/` e copiadas para o container, sem rebuild de WAR.

Auditorias anteriores (auditoria.md) já resolveram vários itens desta lista.
Este design classifica cada item e define a abordagem técnica para os restantes.

## Goals / Non-Goals

**Goals:**
- Corrigir todos os bugs de interface que podem ser resolvidos com edição de
  frontend (HTML, CSS, JS, i18n) ou SQL direto.
- Marcar explicitamente o que já foi feito e o que está bloqueado por APK/WAR rebuild.

**Non-Goals:**
- Rebuild de APK (kiosk, suporte remoto, papel de parede, tela bloqueada).
- Implementação completa de WebFilter no dispositivo (requer launcher + APK).
- Redesign M365 completo (change separada: redesign-consistencia-painel).
- Implementação de modularidade (desativar módulo sem parar tudo).
- Gerência de containers via interface web.

## Triagem dos itens da proposal

### JÁ FEITO (verificado na auditoria)

| Item | Evidência |
|------|-----------|
| F5 volta pro mesmo menu | localStorage fix em app.js — DEPLOYED |
| Informação Detalhada: busca aceita outros termos | Backend busca 15+ campos, typeahead — DEPLOYED |
| Containers nomeados hwmdm-* | docker-compose.yaml — OK |
| docker-compose.yaml único | OK |
| Persistência após baixar/subir stack | Volume montado, md5 match — OK |
| Tooltips em devices.html | 11 localized-title — DEPLOYED |

### A FAZER (frontend, factível sem rebuild)

| # | Item | Abordagem |
|---|------|-----------|
| 1 | F5/troca de aba derruba sessão remota | Não chamar stopRemote em $destroy quando é navegação interna |
| 2 | Quiosque > Política de bloqueio: botões não funcionam | Verificar bindings e handlers no controller |
| 3 | Quiosque > Apps permitidos: botões estourados | CSS fix nos botões |
| 4 | WebFilter Dashboard vazio | Verificar endpoints REST e binding no JS |
| 5 | WebFilter "Recently blocked traffic" não registra | Verificar se o plugin popula a tabela corretamente |
| 6 | WebFilter paginação/limpeza de histórico | Adicionar paginação no frontend + endpoint paginado |
| 7 | Menu Módulos: erros parsing/CSS | Verificar template e CSS |
| 8 | Menu Integrações: erros parsing/CSS | Verificar template e CSS |
| 9 | Permissões RBAC: 4 nomes em russo → pt-BR | UPDATE no banco + i18n JS |
| 10 | Permissões: combobox placebo | Verificar se o save realmente grava no backend |
| 11 | Permissões: nomes estranhos / não cumprem propósito | Auditoria nome vs. efeito real |
| 12 | Nada hardcoded: configs na interface web | Parcial — verificar o que falta |
| 13 | Página de bloqueio customizada (URL configurável) | Config na interface + backend |

### BLOQUEADO (requer APK rebuild ou WAR)

| Item | Motivo |
|------|--------|
| Tela bloqueada/desligada mata sessão remota | MediaProjection + wake lock no APK |
| Acesso remoto soberano/incondicional | Requer lógica no APK |
| APK permite desativar suporte remoto | StatusActivity LAUNCHER no APK |
| Kiosk parou de funcionar | Device Owner no APK |
| Kiosk + Proteger Config automáticos | Requer lógica no APK |
| Papel de parede paisagem | Requer rebuild launcher |
| Tela de bloqueio no device | Requer APK |
| WebFilter comportamento "macro" | Chrome redireciona no dispositivo — requer DNS/VPN no APK |
| Modularidade (desativar módulo) | Arquitetura WAR |
| Gerência de containers via interface | Arquitetura separada |

## Decisions

1. **Itens bloqueados serão documentados mas não implementados** — marcados como
   bloqueados no tasks.md com justificativa. Não expandir escopo.

2. **Ordem de trabalho**: permissões RBAC primeiro (rápido, SQL + i18n), depois
   Quiosque, depois WebFilter dashboard, depois menus Módulos/Integrações.

3. **Edições no volume**: editar em `volumes/webapps/ROOT/`, copiar para container
   com `docker cp`, sem rebuild.

## Risks / Trade-offs

- [Permissões placebo] → verificar no backend se o save realmente persiste;
  se não, o fix é no Java (WAR rebuild) e vira bloqueado.
- [WebFilter dashboard vazio] → pode ser problema de backend (plugin Java),
  não só frontend; nesse caso, escalar para bloqueado.
- [Menus Módulos/Integrações] → se os erros são de plugins Java, não de template,
  escalar para bloqueado.
