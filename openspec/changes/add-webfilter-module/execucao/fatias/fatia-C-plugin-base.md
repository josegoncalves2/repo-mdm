# Fatia C - Plugin, banco e catalogos

## Resultado visivel

O plugin WebFilter existe no WAR, possui schema proprio, permissao registrada e catalogos carregaveis.

## Tarefas

### 2.1 - Modulo Maven e plugin

**ET:** probe ou teste que falha se o plugin nao entra no WAR.

**EI:** cria modulo, `WebFilterPluginConfigurationImpl`, LiquibaseModule, registra em `plugins/pom.xml` e `server/pom.xml`.

**FQ exige:**

- `PLUGIN_ID = "webfilter"`;
- pacote `com.hmdm.plugins.webfilter`;
- jar dentro do WAR;
- alteracoes do MDM restritas aos arquivos permitidos.

### 2.2 - Infra de testes e changelog

**ET:** `WebFilterChangelogTest` com PostgreSQL Testcontainers.

**EI:** adiciona JUnit 5, Mockito, Testcontainers e Liquibase com tabelas de D7.

**FQ exige:**

- changelog core + plugin aplicam em banco limpo;
- cascades e unicidades provadas;
- registro em `plugins` e `permissions`;
- permissao `plugin_webfilter_access`.

### 3.1 - Catalogo de categorias

**ET:** `CategoryCatalogTest`.

**EI:** cria `webfilter-catalog.json` e carregador.

**FQ exige:**

- exatamente 14 categorias;
- somente `doh` obrigatoria;
- fontes permitidas;
- mapeamento igual a D5.

### 3.2 - Catalogo de aplicativos

**ET:** `AppCatalogTest` e `tests/verify_app_catalog_play.py`.

**EI:** cria `webfilter-app-catalog.json` e carregador.

**FQ exige:**

- nenhum pacote protegido;
- nenhum pacote em `doh`;
- pacote Android valido;
- verificacao de pagina na Google Play registrada.

### 3.3 - Categorias de apps do cliente

**ET:** `AppCategoryDAOTest` e `AppCategoryServiceTest`.

**EI:** implementa mapper, DAO e service.

**FQ exige:**

- isolamento por customerId;
- incluir/remover pacote;
- recusar pacote protegido;
- categoria invalida recusada.

## Jornada de aceite

Esta fatia e aceita tecnicamente por build e testes, mas so vira experiencia visivel em J02/J06 depois da Fatia H. O FO marca as tarefas apenas quando o FQ aceitar a construcao e a jornada correspondente.
