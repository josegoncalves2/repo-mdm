## Purpose

Tela do plugin Web Filter no console existente do MDM para configurar, por perfil, o controle de acesso a sites e aplicativos: categorias, allowlist e blocklist, categorização de aplicativos do cliente e domínio DNS do filtro, com acesso controlado por permissão.

## ADDED Requirements

### Requirement: Acesso controlado por permissão

Origem: PRD-13

O plugin SHALL registrar a permissão `plugin_webfilter_access`. A entrada "Web Filter" no menu lateral do console SHALL aparecer somente para usuários com essa permissão e com o plugin ativo para o seu cliente. Os endpoints privados do plugin SHALL responder somente a usuários com essa permissão.

#### Scenario: Usuário sem permissão
- **WHEN** usuário sem `plugin_webfilter_access` acessa o console
- **THEN** o menu não exibe "Web Filter"
- **THEN** chamadas aos endpoints privados do plugin respondem HTTP 403

#### Scenario: Usuário com permissão
- **WHEN** usuário com `plugin_webfilter_access` clica em "Web Filter" no menu lateral
- **THEN** o console exibe a tela do plugin sem erro de JavaScript

#### Scenario: Plugin desativado para o cliente
- **WHEN** administrador desativa "Web Filter" na tela de Plugins do console
- **THEN** a entrada "Web Filter" deixa de aparecer no menu lateral para os usuários do cliente

### Requirement: Configurar política por perfil

Origem: PRD-02, PRD-05, PRD-06, PRD-13

A tela SHALL listar os perfis do cliente com o estado do filtro e a quantidade de categorias e de entradas de lista. Ao editar um perfil, a tela SHALL exibir:
- as 14 categorias do catálogo com nome e descrição, com `doh` marcada e não editável;
- a opção de ativar o filtro;
- allowlist e blocklist de domínios;
- allowlist e blocklist de aplicativos;
- a ação de salvar.

A tela SHALL exibir os erros de validação retornados pelo servidor junto à entrada correspondente.

#### Scenario: Salvar política completa
- **WHEN** administrador ativa o filtro do perfil "Tablets Loja", marca `adult` e `social_media`, inclui `*.empresa.com.br` na allowlist de domínios e `com.king.candycrushsaga` na blocklist de aplicativos, e salva
- **THEN** o console exibe confirmação de sucesso
- **THEN** a lista mostra o perfil como ativo, com 3 categorias e 2 entradas de lista

#### Scenario: doh não pode ser desmarcada
- **WHEN** administrador edita um perfil
- **THEN** a categoria `doh` aparece marcada e desabilitada para edição

#### Scenario: Entrada inválida
- **WHEN** administrador inclui `*.com` na blocklist de domínios e salva
- **THEN** o console exibe a mensagem de entrada inválida junto a `*.com`
- **THEN** a lista continua mostrando a política anterior do perfil

#### Scenario: Perfil sem política
- **WHEN** administrador abre a tela e existe perfil sem política
- **THEN** a lista mostra esse perfil como desativado, sem categorias e sem entradas

### Requirement: Categorizar aplicativos do cliente

Origem: PRD-04, PRD-13

A tela SHALL exibir, por categoria, os pacotes do catálogo inicial (somente leitura) e os pacotes incluídos pelo cliente. A tela SHALL permitir incluir e remover pacotes do cliente.

#### Scenario: Incluir pacote em categoria
- **WHEN** administrador inclui `br.com.empresa.jogo` na categoria `games`
- **THEN** o pacote aparece em `games` como pacote do cliente, com opção de remover

#### Scenario: Pacote do catálogo inicial
- **WHEN** administrador visualiza a categoria `social_media`
- **THEN** os pacotes do catálogo inicial aparecem sem opção de remover

#### Scenario: Pacote protegido
- **WHEN** administrador tenta incluir `com.hmdm.launcher` em uma categoria
- **THEN** o console exibe a mensagem de pacote protegido e não inclui o pacote

### Requirement: Configurar domínio DNS do filtro

Origem: PRD-09, PRD-13

A tela SHALL permitir ao administrador salvar o domínio DNS do filtro do seu cliente. O sistema SHALL aceitar somente nome de domínio válido. Enquanto o domínio estiver vazio, a tela SHALL exibir aviso de que o filtro de sites não é entregue aos dispositivos, mesmo que o bloqueio de aplicativos esteja ativo.

#### Scenario: Salvar domínio válido
- **WHEN** administrador salva o domínio `dns.empresa.com`
- **THEN** o console exibe confirmação de sucesso
- **THEN** o aviso de domínio não configurado deixa de ser exibido

#### Scenario: Domínio inválido
- **WHEN** administrador tenta salvar o domínio `dns empresa`
- **THEN** o sistema responde HTTP 400 e o console exibe a mensagem de domínio inválido
- **THEN** o domínio anterior permanece salvo

#### Scenario: Domínio vazio
- **WHEN** administrador abre a tela e o cliente não tem domínio DNS do filtro
- **THEN** a tela exibe o aviso de que o filtro de sites não chega aos dispositivos

### Requirement: Tela segue o padrão visual e de navegação do console

Origem: PRD-13

As telas do plugin SHALL usar o tema ativo do console (claro ou escuro) com contraste legível. Elas SHALL NOT gerar rolagem horizontal da página nas larguras de 360, 768 e 1280 px. O título SHALL ser igual ao da entrada do menu lateral. Voltar SHALL retornar à tela de origem dentro do plugin. Os textos SHALL aparecer em português quando o idioma do usuário for português.

#### Scenario: Tema escuro
- **WHEN** usuário com tema escuro abre cada aba do Web Filter
- **THEN** todo texto visível tem razão de contraste de pelo menos 4,5:1 com o seu fundo

#### Scenario: Larguras de tela
- **WHEN** usuário abre cada aba do Web Filter nas larguras de 360, 768 e 1280 px
- **THEN** a largura de rolagem do documento não excede a largura da janela
- **THEN** as ações de salvar, incluir e remover ficam alcançáveis

#### Scenario: Voltar da edição de perfil
- **WHEN** usuário abre a edição de um perfil a partir da lista do Web Filter e aciona voltar
- **THEN** o console mostra a lista de perfis do Web Filter, e não a tela de dispositivos

#### Scenario: Título e idioma
- **WHEN** usuário com idioma português abre o Web Filter
- **THEN** o título da tela é igual ao texto da entrada do menu lateral
- **THEN** rótulos, mensagens e erros da tela aparecem em português

### Requirement: Atribuição das fontes das listas

Origem: PRD-03, PRD-13

A tela SHALL exibir as fontes das listas de domínios com nome, link e licença: IPFire DBL (CC BY-SA 4.0), Block List Project (Unlicense) e UT1 - Université Toulouse Capitole (CC BY-SA 4.0).

#### Scenario: Consultar fontes
- **WHEN** administrador abre a tela do plugin
- **THEN** a tela exibe as três fontes com nome, link e licença
