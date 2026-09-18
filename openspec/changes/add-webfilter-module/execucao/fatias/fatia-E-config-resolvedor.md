# Fatia E - Configuracao gerada para o resolvedor

## Resultado visivel

Salvar politica no MDM gera arquivos que o resolvedor usa sem intervencao manual.

## Tarefas

### 5.1 - Gerador de `blocky.yml` e listas de perfil

**ET:** `ResolverConfigWriterTest` com fixtures para 0, 1 e 2 perfis ativos.

**EI:** implementa writer em `webfilter/resolver/**`.

**FQ exige:**

- escrita atomica;
- perfil desativado ausente de `clientGroupsBlock`;
- dominio de `base.url` sempre em allowlist;
- `*.<dominio DNS do filtro>` protegido;
- saida sobe no container da Fatia B.

### 5.2 - Gatilhos de regeneracao

**ET:** `ResolverConfigTriggerTest`.

**EI:** implementa task module e chamadas ao salvar politica, listas, dominio, inicializacao e periodicidade de 10 minutos.

**FQ exige:**

- padrao de task module igual aos plugins existentes;
- regeneracao idempotente;
- sem acesso do servidor ao socket Docker;
- falha de escrita registrada sem derrubar servidor.

## Jornada de aceite

J04 valida a aplicacao no tablet. Antes dela, FQ aceita apenas a construcao.
