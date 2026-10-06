# Tasks — redesign-consistencia-painel

## Fase 1 — Acesso Remoto (terminar) + Mensagens

### 1.1 Acesso Remoto — contadores no controller
- [ ] `chown` nos controllers (pré-requisito do usuário)
- [ ] Editar `remote.controller.js`: adicionar `$scope.onlineCount` e `$scope.offlineCount` após carregar devices
- [ ] Copiar alterações do server-source para volumes/webapps (ou vice-versa, manter sincronizado)
- [ ] Testar no DEV: lista mostra dot + serial + modelo + hora, contadores corretos

### 1.2 Mensagens — lista lateral de devices
- [ ] Ler `chat.html` e `chat.controller.js` atuais
- [ ] Reescrever `chat.html`: trocar dropdown por `hwmdm-split-layout` com lista lateral padrão (reutilizar classes `remote-device-item`, `gps-device-search`, etc)
- [ ] Adaptar `chat.controller.js`: carregar lista de devices (provavelmente já carrega para o dropdown), adicionar `selectDevice()`, contadores
- [ ] Área de chat à direita: formulário de mensagem + histórico filtrado pelo device selecionado
- [ ] Testar no DEV

## Fase 2 — Botões laterais no Remoto + Relatórios por device

### 2.1 Acesso Remoto — botões de ação na coluna lateral
- [x] Ler layout atual dos botões de ação em `remote.html`
- [x] Reestruturar: tela do device à esquerda, coluna de ações à direita (grid `remote-body-columns` com 2 colunas)
- [x] Categorias de botões em grupos verticais (Tela e Sessão, Ciclo de Vida, Aplicativos, etc) — já existiam como `remote-command-group`
- [x] CSS: coluna lateral fixa 300px (`remote-body-right`), botões empilhados verticalmente (`flex-direction: column`), scrollável
- [ ] Testar no DEV: botões acessíveis, não cobrem a tela, responsivo

### 2.2 Relatórios — lista lateral + relatório por device
- [ ] Ler `reports.html` e `reports.controller.js` atuais
- [ ] Adicionar lista lateral de devices (reutilizar padrão)
- [ ] Quando nenhum device selecionado: mostrar relatório geral (comportamento atual)
- [ ] Quando device selecionado: filtrar dados para aquele device específico
- [ ] Verificar se a API REST já suporta filtro por device ou se precisa de endpoint novo
- [ ] Testar no DEV

## Fase 3 — Perfis de dispositivo inline

### 3.1 Eliminar página separada de edição
- [ ] Ler `configuration.html`, `configEditor.html`, `configEditor.controller.js`
- [ ] Mapear todas as abas/seções do editor (Básico, Apps, Quiosque, Config por app, Arquivos, Aparência)
- [ ] Criar layout split: lista lateral de perfis + editor inline à direita
- [ ] Migrar lógica do `ConfigEditorController` para funcionar sem rota separada (ou usar sub-view inline)
- [ ] Remover `beforeunload` handler
- [ ] Remover rota `#/config/editor/:id` (ou redirecionar para a nova view)
- [ ] Testar no DEV: criar, editar, salvar perfil sem sair da página

## Fase 4 — Aplicativos, Arquivos, Ícones, Usuários, Permissões

### 4.1 Padrão repetitivo — cada tela recebe split layout
Para cada tela (Aplicativos, Arquivos, Ícones do launcher, Usuários, Permissões):
- [ ] Ler HTML e controller atuais
- [ ] Reescrever HTML com `hwmdm-split-layout`: lista lateral com busca + detalhe/edição inline
- [ ] Item da lista: campos relevantes (nome + descrição para apps/arquivos, login + nome + papel para usuários, etc)
- [ ] Seleção abre detalhe/formulário no painel direito
- [ ] Botão "Adicionar" abre formulário vazio no painel direito (não modal, não página nova)
- [ ] Testar cada tela no DEV

### 4.2 Telas específicas:
- [ ] **Aplicativos**: lista com nome + versão, detalhe com ícone, configurações, atribuição a perfis
- [ ] **Arquivos**: lista com nome + tamanho + data, detalhe com descrição, caminho, upload
- [ ] **Ícones do launcher**: lista com preview + nome, detalhe com upload/edição
- [ ] **Usuários**: lista com login + nome + papel, detalhe com formulário de edição
- [ ] **Permissões**: lista com nome do papel, detalhe com checkboxes de permissões

## Fase 5 — Aparência e marca

### 5.1 Expandir opções de personalização
- [ ] Ler `branding.html` e controller/service atuais
- [ ] Verificar o que o backend já suporta vs o que precisa de endpoint novo
- [ ] Adicionar campos: nome do produto (título da navbar), favicon (upload), cores adicionais
- [ ] Tela inicial do tablet: redirecionar para edição no perfil de dispositivo (passo 6 do perfil)
- [ ] Preview ao vivo das alterações de marca
- [ ] Testar no DEV

## Pré-requisitos gerais

- [ ] `sudo chown -R sahw:sahw` nos diretórios com arquivos do root
- [ ] Manter sincronia entre server-source e volumes/webapps após cada edição
- [ ] Cada fase validada no DEV (.65) antes de avançar para a próxima
