# Desenho — Auditoria de hardcodes e centralização de versões

## Princípios

1. **Inventariar antes de editar.** Registrar os valores encontrados, onde são
   definidos, onde são consumidos e como afetam build, execução ou publicação.
2. **Uma fonte oficial para versões próprias.** A localização e o formato serão
   escolhidos após identificar as ferramentas e os fluxos reais do repositório.
   Os consumidores devem ler essa fonte, em vez de manter cópias independentes.
3. **Não confundir valores diferentes.** Versão do produto, tag de imagem,
   versão de pacote e versão de protocolo podem ter ciclos distintos. O
   inventário deve modelar essas diferenças sem forçá-las a um único número.
4. **Migrar sem atualizar.** A mudança centraliza os valores existentes; não
   altera tags publicadas nem atualiza dependências por conveniência.
5. **Configuração sem segredos.** Valores por ambiente devem seguir os
   mecanismos seguros já usados pelo projeto, com defaults documentados quando
   aplicável e sem credenciais em arquivos versionados.

## Estratégia

### 1. Inventário e classificação

Examinar os arquivos mantidos pelo projeto, excluindo conteúdo gerado,
dependências vendorizadas e artefatos de build quando isso não esconder uma
fonte de configuração real. Para cada ocorrência relevante, registrar:

- arquivo e localização;
- valor e categoria (tag/versão, ambiente/infraestrutura, endpoint, limite,
  regra funcional ou constante intencional);
- consumidores e fluxo afetado;
- decisão, responsável pela fonte central e forma de validação.

Buscar em particular tags de imagens e versões de artefatos em código, manifests,
builds, scripts, publicação e documentação de operação. Uma busca textual inicial
não substitui a confirmação de todos os consumidores.

### 2. Fonte central de versões

Definir um catálogo versionado para os artefatos mantidos pelo HWMDM. A escolha
entre propriedades de build, manifest compartilhado ou outro formato deve
considerar quais ferramentas precisam consumi-lo e evitar geração manual de
cópias divergentes. Cada artefato pode ter versão própria quando seu ciclo de
release for independente.

Migrar todas as referências de tags desse catálogo para os fluxos pertinentes.
Se uma ferramenta exigir um arquivo derivado, ele deve ser gerado ou validado
automaticamente a partir da fonte oficial.

### 3. Demais hardcodes elegíveis

Externalizar valores operacionais que precisem variar entre ambientes ou
instalações usando o padrão de configuração já existente. Manter como constantes
os valores cuja fixidez seja parte do contrato técnico ou da regra funcional e
registrar a justificativa no inventário.

### 4. Verificação e documentação

Adicionar uma checagem que detecte referências duplicadas ou divergentes às
versões próprias, além de validar a resolução do catálogo nos fluxos de build,
publicação e deploy aplicáveis. Documentar como alterar uma versão uma única vez
e confirmar quais artefatos serão afetados.

## Critérios de aceite

- O inventário cobre os arquivos de código e operação mantidos pelo projeto e
  classifica cada ocorrência relevante, inclusive as mantidas por exceção.
- As tags/versões dos artefatos próprios não são definidas independentemente em
  múltiplas fontes editáveis.
- Alterar a versão de um artefato na fonte oficial atualiza todas as referências
  aplicáveis no build/publicação/deploy, sem editar cada consumidor manualmente.
- Os valores e artefatos atuais permanecem iguais após a migração.
- Os valores operacionais externalizados continuam com comportamento compatível
  nos ambientes existentes; segredos permanecem fora do controle de versão.
