## ADDED Requirements

### Requirement: Valores hardcoded são inventariados e classificados

Todas as ocorrências de valores fixos em arquivos mantidos pelo projeto SHALL
ser consideradas na auditoria, incluindo código, configuração, manifests,
scripts e documentação operacional. Conteúdo gerado, dependências vendorizadas
e artefatos de build podem ser excluídos quando identificados no inventário.
Cada ocorrência relevante SHALL ser classificada como configuração
externalizável, versão/tag centralizável, constante intencional ou fora de
escopo, com consumidor e justificativa registrados.

#### Scenario: Configuração operacional varia por ambiente

- **WHEN** a auditoria identifica um valor que precisa variar entre ambientes ou
  instalações
- **THEN** o valor é obtido pelo mecanismo de configuração adotado pelo projeto
  e mantém um default compatível documentado quando aplicável

#### Scenario: Valor fixo não deve virar configuração

- **WHEN** uma ocorrência é texto de interface, regra funcional ou constante de
  protocolo que precisa permanecer fixa
- **THEN** ela não é externalizada sem necessidade demonstrada e sua decisão é
  registrada no inventário

#### Scenario: Segredo encontrado em configuração

- **WHEN** a auditoria encontra credencial, token, chave ou outro segredo em
  arquivo versionado
- **THEN** a ocorrência é registrada para correção e o segredo não é replicado
  para outra fonte versionada

### Requirement: Externalização não expõe segredos

Configurações externalizadas SHALL NOT introduzir credenciais, tokens, chaves ou
outros segredos em arquivos rastreados pelo repositório.

#### Scenario: Configuração exige credencial

- **WHEN** um valor externalizado contém informação secreta
- **THEN** ele é fornecido pelo mecanismo seguro apropriado ao ambiente e não
  aparece em um arquivo versionado
