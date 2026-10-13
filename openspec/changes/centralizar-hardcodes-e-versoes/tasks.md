## 1. Inventário e critérios

- [ ] 1.1 Mapear as fontes mantidas pelo projeto e os fluxos de build, publicação
  e deploy que definem ou consomem tags/versões.
- [ ] 1.2 Inventariar hardcodes relevantes em código, manifests, configurações,
  scripts e documentação operacional; excluir conteúdo gerado e dependências
  vendorizadas, registrando as exclusões.
- [ ] 1.3 Classificar cada ocorrência como versão/tag, configuração operacional,
  constante intencional ou fora de escopo; registrar consumidor, decisão e
  justificativa para as constantes mantidas.
- [ ] 1.4 Registrar a linha de base dos valores efetivamente usados pelos
  artefatos e ambientes existentes para assegurar que a migração não os altere.

## 2. Fonte central de versões

- [ ] 2.1 Definir o catálogo oficial de versões após confirmar compatibilidade
  com todas as ferramentas consumidoras e ciclos de release dos artefatos.
- [ ] 2.2 Migrar tags e versões dos artefatos próprios para o catálogo e adaptar
  build, publicação, deploy e scripts consumidores.
- [ ] 2.3 Gerar ou validar automaticamente arquivos derivados que precisem
  repetir uma versão por exigência de ferramenta.
- [ ] 2.4 Confirmar que nenhuma versão ou tag mudou apenas como efeito da
  centralização.

## 3. Outros hardcodes elegíveis

- [ ] 3.1 Externalizar configurações operacionais identificadas como variáveis
  por ambiente/instalação, preservando defaults e comportamento existentes.
- [ ] 3.2 Manter constantes técnicas e regras funcionais fixas; documentar no
  inventário a justificativa de cada exceção relevante.
- [ ] 3.3 Verificar que a mudança não introduz segredos em arquivos versionados.

## 4. Validação e documentação

- [ ] 4.1 Adicionar validação de divergências entre catálogo e referências
  editáveis nos fluxos aplicáveis.
- [ ] 4.2 Validar build, publicação e deploy dos componentes afetados e comparar
  os identificadores/artefatos com a linha de base.
- [ ] 4.3 Documentar a fonte oficial, como alterar uma versão globalmente, os
  valores configuráveis por ambiente e os comandos de validação.
- [ ] 4.4 Revisar o inventário final e confirmar que cada ocorrência relevante
  foi corrigida ou tem exceção justificada.
