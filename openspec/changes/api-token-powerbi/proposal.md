# api-token-powerbi — Integração Power BI e gerador de API token

Origem: `docs/api-rest.md`
Status: A VERIFICAR — pode já estar parcialmente implementado.

## Motivação

O painel HWMDM precisa expor dados para ferramentas de BI (Power BI, Grafana, etc) e permitir integrações via API com tokens de longa duração. A infraestrutura JWT já existe no servidor.

## Escopo

### 1. Integração para extração de dados (Power BI)

O servidor já dispõe de API JAX-RS. O Power BI pode consumir via conector "Web" com Power Query (M).

**Endpoints relevantes existentes:**
- `/rest/private/devices/search` — lista detalhada de dispositivos
- `/rest/private/summary/devices` — estatísticas consolidadas de inventário
- `/rest/plugins/deviceinfo/private/search/dynamic` — dados técnicos avançados (plugin)

**Alternativa:** acesso direto ao PostgreSQL para dashboards complexos de grande volume.

### 2. Gerador de API Token

**Infraestrutura atual:**
- JWT (JSON Web Tokens) para autenticação (`server-source/jwt/`)
- `TokenProvider` gerencia criação e validação
- Token gerado no login (`/public/jwt/login`) com validade padrão (24h ou 30 dias)
- Campo `auth_token` na tabela de usuários serve como "secret" para validar JWT — revogar = alterar esse valor

**O que implementar:**
- Funcionalidade para gerar tokens com **expiração longa** (ex: 1 ano) e escopo limitado (apenas `GET`)
- Interface no console administrativo para gerar e copiar o token
- Perfil de leitura (`UserRole` específico com permissões somente de visualização)

### 3. Documentação Swagger

O projeto já usa anotações Swagger (`@Api`, `@ApiOperation`). Mapear campos para facilitar integração.

## Tarefas

- [ ] Verificar se endpoints REST existentes retornam dados suficientes para Power BI
- [ ] Implementar endpoint ou tela para gerar token de longa duração com escopo read-only
- [ ] Criar `UserRole` de leitura (API reader)
- [ ] Tela no console: gerar token, copiar, revogar
- [ ] Documentar endpoints Swagger acessíveis via token de API
- [ ] Testar integração Power BI com token gerado no DEV
