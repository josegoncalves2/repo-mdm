## ADDED Requirements

### Requirement: Versões dos artefatos próprios têm uma fonte oficial

Tags e versões de artefatos mantidos pelo projeto SHALL ser declaradas em uma
fonte oficial centralizada. Build, publicação e deploy SHALL consumir essa
fonte, diretamente ou por arquivos derivados gerados ou validados
automaticamente. Artefatos com ciclos de release independentes SHALL poder ter
valores independentes no catálogo.

#### Scenario: Atualizar a versão de um artefato

- **WHEN** a pessoa responsável altera a versão de um artefato na fonte oficial
- **THEN** todos os fluxos aplicáveis usam essa versão sem exigir atualização
  manual de tags duplicadas nos consumidores

#### Scenario: Versões de artefatos independentes

- **WHEN** dois artefatos têm ciclos de release distintos
- **THEN** cada um pode manter sua própria versão central, sem obrigar a
  atualização do outro

### Requirement: Centralização não altera versões publicadas

A migração para a fonte oficial SHALL preservar as versões e tags atualmente
usadas por build, publicação e deploy. A change SHALL NOT atualizar dependências
ou implantar uma nova versão implicitamente.

#### Scenario: Build após a migração

- **WHEN** os fluxos existentes são executados sem uma alteração explícita de
  versão
- **THEN** eles resolvem as mesmas tags e versões usadas antes da migração

### Requirement: Divergências de versão são verificáveis

O repositório SHALL possuir uma validação adequada aos fluxos existentes que
detecte referências editáveis divergentes das versões oficiais e SHALL
documentar o procedimento para alterar e verificar versões.

#### Scenario: Referência de versão divergente

- **WHEN** uma tag editável de artefato próprio diverge da fonte oficial
- **THEN** a validação sinaliza a divergência antes de concluir com sucesso o
  fluxo de build, publicação ou deploy aplicável

#### Scenario: Alteração global de versão documentada

- **WHEN** uma pessoa responsável segue a documentação para mudar a versão de
  um artefato
- **THEN** consegue identificar a fonte a editar, os consumidores afetados e a
  validação a executar
