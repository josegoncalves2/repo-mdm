# centralizar-hardcodes-e-versoes — Auditoria de hardcodes e centralização de versões

## Por quê

Valores de configuração e tags de versão repetidos em código, scripts e arquivos
de build/deploy tornam alterações globais frágeis: uma versão pode ser atualizada
em um ponto e permanecer antiga em outro. Isso aumenta o risco de publicar
componentes incompatíveis e dificulta saber qual valor é a fonte oficial.

## O que muda

- Fazer inventário abrangente dos valores hardcoded no repositório, registrando
  ocorrência, finalidade, consumidores e decisão (centralizar, externalizar,
  manter como constante intencional ou justificar exceção).
- Priorizar tags/versões dos artefatos mantidos pelo projeto, incluindo
  componentes, imagens e pacotes distribuíveis, e estabelecer uma única fonte
  oficial de versões que possa ser consumida pelos fluxos de build, publicação
  e deploy existentes.
- Migrar os consumidores para a fonte oficial sem alterar os valores atualmente
  publicados nem introduzir atualização implícita de componentes.
- Externalizar configurações operacionais que hoje estejam fixadas no código
  quando devam variar por ambiente ou instalação, preservando defaults
  compatíveis e sem incluir segredos no repositório.
- Adicionar validação e documentação para impedir divergência entre as versões
  declaradas e os artefatos efetivamente produzidos/publicados.

## Capacidades

### Novas capacidades

- `version-management`: fonte única para tags e versões dos artefatos do HWMDM,
  consumida consistentemente pelos fluxos de build, publicação e deploy.
- `hardcoded-configuration-audit`: inventário rastreável de valores fixos,
  correção dos que devem ser configuração e justificativa explícita dos que
  devem permanecer constantes.

### Capacidades modificadas

Nenhuma especificação existente em `openspec/specs/`.

## Fora de escopo

- Trocar ou atualizar versões de componentes como parte da centralização.
- Converter indiscriminadamente toda ocorrência literal: constantes de
  protocolo, regras de negócio, textos da interface e valores que precisam ser
  fixos não se tornam configuração sem justificativa.
- Alterar comportamento funcional, compatibilidade, política de release ou
  infraestrutura de produção além do necessário para consumir a fonte central.
- Colocar credenciais, tokens, chaves ou outros segredos em arquivo versionado.

## Impacto

- Inventário e alterações podem envolver código-fonte, manifests, arquivos de
  build, configuração de containers, scripts de publicação/deploy e
  documentação operacional.
- Os caminhos e arquivos exatos serão determinados pelo inventário inicial; a
  change não presume uma ferramenta ou formato único antes de verificar os
  consumidores existentes.
- Build, publicação e deploy devem continuar produzindo os mesmos identificadores
  de versão e artefatos antes e depois da migração, até que uma alteração de
  versão seja solicitada separadamente.
