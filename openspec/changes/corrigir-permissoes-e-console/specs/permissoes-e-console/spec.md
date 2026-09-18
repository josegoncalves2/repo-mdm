## ADDED Requirements

### Requirement: Tabela de dispositivos responsiva
A tela Dispositivos SHALL exibir a tabela de dispositivos acompanhando a largura da janela, sem largura travada, mantendo todas as colunas, botões e ações existentes. Origem: PRD-06, PRD-02, PRD-04.

#### Scenario: Janela estreita
- **WHEN** o administrador abre Dispositivos numa janela de 390 px de largura
- **THEN** a página não tem scroll horizontal e toda coluna e ação da linha continua alcançável

#### Scenario: Janela larga
- **WHEN** o administrador abre Dispositivos numa janela de 1920 px
- **THEN** a tabela ocupa a largura disponível em vez de ficar num tamanho fixo

### Requirement: Permissões fazem o que o nome diz
Cada permissão SHALL ter nome e descrição, no idioma da interface, que correspondam ao efeito observado, e SHALL ser verificada no servidor e na tela. Nenhuma permissão existente SHALL ser apagada. Origem: PRD-14, PRD-15, PRD-04.

#### Scenario: Usuário sem a permissão tenta a ação
- **WHEN** um usuário cujo papel não tem a permissão tenta a ação que ela controla, pela tela ou reenviando a requisição pela sessão do próprio navegador
- **THEN** a ação não está disponível na tela, o servidor recusa a requisição e o dado não muda

#### Scenario: Usuário com a permissão executa a ação
- **WHEN** um usuário cujo papel tem a permissão executa a ação
- **THEN** a ação conclui e o resultado aparece ao reabrir a tela

### Requirement: Segregação ver e editar por área
O console SHALL permitir conceder, por papel e por área, os níveis sem acesso, ver e editar, a começar pelo perfil de dispositivo, conforme a matriz aprovada pelo responsável. Origem: PRD-03, PRD-07, PRD-08.

#### Scenario: Papel só com "ver perfil de dispositivo"
- **WHEN** um usuário desse papel abre um perfil de dispositivo e tenta salvar uma alteração
- **THEN** ele vê todas as seções em modo somente leitura, não consegue salvar e o perfil permanece inalterado

#### Scenario: Papel sem acesso ao perfil de dispositivo
- **WHEN** um usuário desse papel procura o menu ou cola na barra o endereço da tela de perfis
- **THEN** o menu não aparece e os dados do perfil não são exibidos

### Requirement: Seletor real de permissões
A tela de papéis SHALL oferecer um seletor que liste as permissões reais, grave a escolha, mostre ao reabrir o que foi salvo e tenha efeito no login seguinte do usuário. Nenhum controle da tela SHALL ser apenas decorativo. Origem: PRD-10.

#### Scenario: Alterar permissões de um papel
- **WHEN** o super-administrador marca uma permissão num papel, salva e o usuário desse papel entra de novo
- **THEN** a tela de papéis mostra a permissão marcada e o usuário passa a ter o efeito correspondente

### Requirement: Papel de parede em paisagem
O papel de parede do dispositivo SHALL cobrir a tela em paisagem sem faixas, distorção ou corte do conteúdo relevante, mantendo o comportamento atual em retrato. Origem: PRD-09.

#### Scenario: Girar o tablet
- **WHEN** a pessoa gira o tablet de retrato para paisagem na tela do kiosk
- **THEN** o papel de parede se ajusta à nova orientação

### Requirement: Suporte remoto protegido
O usuário do tablet SHALL NOT conseguir desativar, forçar a parada nem desinstalar o app de suporte remoto. Origem: PRD-12.

#### Scenario: Tentativa de desinstalar pelas Configurações
- **WHEN** a pessoa abre Configurações > Apps > suporte remoto e toca em Desinstalar, Desativar ou Forçar parada
- **THEN** a ação não se conclui e o acesso remoto pelo painel continua funcionando

### Requirement: Proteções automáticas após as permissões
Quando as permissões do agente ficarem concedidas no tablet, o bloqueio do kiosk e a proteção das Configurações SHALL ser ativados sem ação no painel. Origem: PRD-13.

#### Scenario: Tablet recém-configurado
- **WHEN** a pessoa concede no tablet as permissões pedidas pelo agente
- **THEN** o kiosk fica bloqueado e as Configurações ficam protegidas sem que ninguém clique no painel

### Requirement: Acesso remoto interativo preservado
O acesso remoto pelo painel SHALL continuar mostrando a tela do tablet ao vivo e aceitando toques e digitação. Origem: PRD-11, PRD-02.

#### Scenario: Tocar pelo painel
- **WHEN** o operador abre o acesso remoto de um tablet e toca num ícone pelo painel
- **THEN** o app correspondente abre no tablet

### Requirement: Aceite somente por uso real fiscalizado
Nenhuma tarefa deste change SHALL ser marcada como concluída sem: execução por sub-agente convocado pelo FISCAL; validação por outro sub-agente; evidência de uso real (vídeo, capturas, diário) no ambiente DEV; conferência de que nada do inventário da linha de base desapareceu; assinatura humana. Testes automatizados SHALL NOT ser criados nem aceitos como prova. Origem: PRD-16, PRD-17, PRD-18, PRD-04, PRD-05.

#### Scenario: Tentativa com arquivo de teste automatizado
- **WHEN** a entrega de um EXECUTOR cria ou altera um arquivo de teste
- **THEN** `guarda.py diff` aponta TESTE_AUTOMATIZADO e a tentativa é reprovada

#### Scenario: Checkbox sem prova
- **WHEN** uma tarefa está marcada `[x]` sem parecer ACEITA de validador despachado, sem evidência íntegra ou sem assinatura humana
- **THEN** `guarda.py aceite` falha e aponta o que falta
