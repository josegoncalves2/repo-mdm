# Spec: correcoes-interface

## ADDED Requirements

### Requirement: Permissões RBAC com descrições em pt-BR
O sistema SHALL exibir todas as 45 permissões com nomes e descrições em português brasileiro. As 4 permissões atualmente em russo (edit_device_app_settings, plugin_audit_access, plugin_deviceinfo_access, plugins_customer_access_management) SHALL ter descrições corrigidas.

#### Scenario: Permissões exibidas em pt-BR
- **WHEN** o administrador abre a tela de Permissões
- **THEN** todas as 45 permissões exibem nome e descrição em pt-BR, sem texto em russo

### Requirement: Combobox de permissões funcional
O combobox de atribuição de permissões a papéis SHALL gravar efetivamente no backend. Se o usuário sem permissão tenta alterar, o sistema SHALL negar.

#### Scenario: Gravar permissão em papel
- **WHEN** o admin seleciona uma permissão no combobox e salva
- **THEN** a permissão é persistida no banco e efetiva no próximo login do usuário afetado

#### Scenario: Verificar se combobox é placebo
- **WHEN** o admin altera permissão e recarrega a página
- **THEN** a alteração permanece salva

### Requirement: Quiosque Política de bloqueio funcional
Os botões da aba "Política de bloqueio" do Quiosque SHALL executar a ação correspondente no dispositivo.

#### Scenario: Botão de política de bloqueio
- **WHEN** o admin clica em um botão de política de bloqueio
- **THEN** o comando é enviado ao dispositivo e o status atualiza

### Requirement: Quiosque Apps permitidos sem CSS estourado
Os botões "instalar", "permitir no quiosque" e "app inicial" SHALL estar visualmente corretos e clicáveis.

#### Scenario: Botões de apps permitidos renderizados
- **WHEN** o admin abre a aba "Apps permitidos" do Quiosque
- **THEN** os botões aparecem com tamanho adequado, texto legível e sem sobreposição

### Requirement: WebFilter Dashboard exibe tráfego bloqueado
O dashboard do WebFilter SHALL exibir os registros de tráfego bloqueado do banco de dados.

#### Scenario: Dashboard com dados
- **WHEN** o admin abre WebFilter > Dashboard
- **THEN** os registros de "Recently blocked traffic" são exibidos a partir do banco

### Requirement: WebFilter histórico com paginação
O histórico de bloqueios SHALL ter paginação e opção de limpeza.

#### Scenario: Paginação do histórico
- **WHEN** o admin abre o histórico de bloqueios com mais de 50 registros
- **THEN** os registros são exibidos em páginas de 50, com navegação

#### Scenario: Limpeza de histórico
- **WHEN** o admin clica em "Limpar histórico"
- **THEN** os registros selecionados ou todos são removidos do banco

### Requirement: Menu Módulos sem erros
O menu Módulos SHALL renderizar sem erros de parsing, CSS correto, botões funcionais.

#### Scenario: Menu Módulos abre sem erro
- **WHEN** o admin clica em "Módulos"
- **THEN** a tela carrega sem erros de console e com layout correto

### Requirement: Menu Integrações sem erros
O menu Integrações SHALL renderizar sem erros de parsing, CSS correto, botões funcionais.

#### Scenario: Menu Integrações abre sem erro
- **WHEN** o admin clica em "Integrações"
- **THEN** a tela carrega sem erros de console e com layout correto

### Requirement: Página de bloqueio WebFilter configurável
A URL da página de bloqueio SHALL ser configurável via interface (DEV: mdm.pmeto.local, PROD: mdm.olimpia.sp.gov.br).

#### Scenario: Configurar URL de bloqueio
- **WHEN** o admin define a URL da página de bloqueio na interface
- **THEN** o Blocky redireciona para essa URL ao bloquear um site

### Requirement: Sessão remota sobrevive F5/troca de aba
O controller de acesso remoto SHALL NÃO encerrar a sessão quando o usuário navega entre abas ou pressiona F5.

#### Scenario: F5 durante sessão remota
- **WHEN** o admin pressiona F5 durante uma sessão de acesso remoto
- **THEN** a sessão é reconectada automaticamente, sem encerrar no dispositivo
