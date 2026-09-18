## Why

A segregação de permissões entre grupos não funciona. Um usuário sem permissão para alterar o perfil de dispositivo consegue alterá-lo. O combobox de permissões da tela de papéis é um placeholder: parece configurar algo, mas nada do que ele mostra é aplicado. Várias permissões têm nome ou descrição que não corresponde ao que fazem, algumas não fazem nada, e não existe meio-termo como "ver sem editar" o perfil de dispositivo. Somam-se defeitos visíveis no console (tabela de Dispositivos travada, sem responsividade) e no tablet (papel de parede em paisagem, suporte remoto desinstalável, bloqueio do kiosk e "Proteger Configurações" feitos à mão device a device).

Nada disso foi testado de verdade antes de ser declarado pronto. Esta mudança corrige os pontos listados abaixo **e** impõe um controle fiscal em que nenhuma tarefa é aprovada sem prova de uso real.

## PRD IDs (texto do responsável, 2026-09-18)

- **PRD-01:** a stack de desenvolvimento já existe e está online em `localhost:8080` (servidor DEV 192.168.1.65).
- **PRD-02:** garantir e manter todas as características e funcionalidades presentes.
- **PRD-03:** viabilizar a segregação de permissões entre os grupos.
- **PRD-04:** garantir que nada desapareça durante os ajustes.
- **PRD-05:** nunca alterar o que não foi solicitado; corrigir apenas os pontos mencionados.
- **PRD-06:** identificar e corrigir na tela `Dispositivos` o div que quebra sem responsividade; tabela de dispositivos com tamanho travado (`class="table-responsive ng-scope"`). **Prioridade imediata.**
- **PRD-07:** dispor da possibilidade de permissionar quem pode alterar as configurações do perfil do dispositivo e outras áreas da plataforma.
- **PRD-08:** permissões segregáveis, por exemplo ver ou não ver o perfil de dispositivo, com meio-termo.
- **PRD-09:** papel de parede do dispositivo se ajusta ao modo paisagem (hoje só está correto em retrato).
- **PRD-10:** combobox de permissões placeholder substituído por combobox real; usuário sem permissão para alterar o perfil de dispositivo não pode alterá-lo.
- **PRD-11:** manter a possibilidade de usar o tablet por dentro do painel web (interação real no acesso remoto/suporte remoto).
- **PRD-12:** o APK não pode permitir desativar ou desinstalar o suporte remoto.
- **PRD-13:** após a ativação das permissões no tablet, o bloqueio do kiosk e o "Proteger Configurações" são automáticos.
- **PRD-14:** verificar cada permissão: nomes estranhos, nome que diz fazer uma coisa e faz outra, se de fato faz algo.
- **PRD-15:** corrigir as permissões que não cumprem o propósito real e as nomenclaturas que não condizem com o que fazem.
- **PRD-16:** tudo testado manualmente antes de ser declarado pronto.
- **PRD-17:** não são permitidos testes automatizados; só teste real, simulação real do uso comum da plataforma.
- **PRD-18:** um FISCAL convoca obrigatoriamente sub-agentes para cada tarefa e para a validação de cada tarefa; nenhuma tarefa é aprovada sem a solução comprovada.

## What Changes

- Tela Dispositivos: tabela ocupa a largura disponível e se adapta a qualquer largura de tela, sem perder coluna, botão ou ação.
- Permissões: auditoria de todas as permissões existentes (nome × descrição × onde é verificada × efeito real); correção de nome/descrição (pt-BR) e do comportamento das que não cumprem o propósito. **Nenhuma permissão é apagada** e os ids existentes são preservados.
- Segregação: permissões separadas de **ver** e **editar** por área do console, começando pelo perfil de dispositivo. A verificação acontece no servidor (REST) e na tela (oculto ou somente leitura).
- Tela de papéis: o combobox de permissões placeholder é substituído por um seletor real, que grava e é respeitado.
- Tablet: papel de parede responsivo em paisagem; suporte remoto não desativável nem desinstalável pelo usuário do tablet; kiosk e "Proteger Configurações" armados sozinhos ao fim da concessão de permissões.
- Não regressão: o acesso remoto com interação real pelo painel continua funcionando, e todo item do inventário da linha de base continua presente.
- Controle fiscal próprio deste change em `execucao/` e `fiscal/`.

## Não faz parte

- Qualquer ação no servidor de produção 192.168.1.75 / `mdm.olimpia.sp.gov.br`, nem nos tablets matriculados nele.
- Remover, esconder ou renomear telas, menus, botões, abas ou funções não citados acima.
- Testes automatizados (unitários, integração, suítes de regressão) no repositório.
- Refatorações, trocas de biblioteca ou "melhorias" não pedidas.

## Impact

- Console web (`server-source/server/src/main/webapp/**`), servidor Java (recursos REST e verificação de permissão), banco (Liquibase para permissões novas ou renomeadas, sem apagar linhas).
- APKs do launcher/agente e do suporte remoto (`android-source/**`), publicados **somente no DEV**.
- Ambiente: somente DEV `localhost:8080` / `192.168.1.65:8080`.
