## Regras de execucao fiscal

Cada item e uma microtarefa para Ordem de Servico. Todo item traz: escopo de arquivos permitido, requisitos cobertos, comando de aceite e evidencia obrigatoria. O fiscal so marca `[x]` depois de reexecutar o aceite. Criterios de reprovacao: os do checklist do fiscal (arquivo fora do escopo, teste que nao falha antes da implementacao, placeholder, valor chumbado, teste pulado, regressao, relato divergente da reexecucao).

- Nenhum executor edita `openspec/**`.
- O MDM existente so pode ser alterado nos 3 pontos de integracao do `proposal.md` (Impact): `server-source/plugins/pom.xml`, `server-source/server/pom.xml` e launcher (`ServerConfig.java`, `ConfigUpdater.java`). Qualquer outro arquivo do MDM alterado = reprovacao.
- Caminhos relativos a `repo-mdm/`. Comandos Maven em `repo-mdm/server-source`; Gradle em `repo-mdm/android-source` com JDK 21 (ver `PROVISIONAMENTO.md`); pytest em `repo-mdm/webfilter-dns`.
- Toda OS deve passar pelo `fiscal_guard.py` real em `.agents/skills/openspec-fiscal/scripts/fiscal_guard.py` para registrar ledger encadeado, estado derivado, escopo permitido, testes protegidos, citacoes `arquivo:linha` exigidas e manifestos de evidencia. Registro fiscal antigo sem esse guard, sem FQ independente ou sem sub-agente real e invalido.
- Requisitos citados em OS devem ter `Origem: PRD-nn` nas specs. Requisito sem origem bloqueia a OS.
- Aceite de uso real nunca e feito por `curl`, status HTTP, porta aberta, container `up`, healthcheck, `ping`, `nslookup`, `dig`, `dumpsys`, `settings get`, log, teste unitario ou consulta direta ao banco como prova principal. Esses sinais sao complementares a video/trace/capturas da tela ou do tablet fisico.
- O grupo 1 e gate de arquitetura: se 1.5 ou 1.6 falhar, a execucao para e `design.md` (D3/D8) e revisto antes de qualquer tarefa dos grupos 2 a 10.
- Grupos 1 a 9 rodam somente em homologacao. Nenhum agente executa acao em producao. A tarefa 10.4 e executada pelo responsavel do ambiente, com autorizacao explicita registrada; o fiscal apenas verifica a evidencia.

## 1. Resolvedor webfilter-dns (gate de arquitetura)

- [x] 1.1 Criar imagem do resolvedor com Blocky fixado por digest e Python 3; aceite: `docker build -t webfilter-dns:test webfilter-dns` exit 0 e `docker run --rm --entrypoint blocky webfilter-dns:test version` imprime a versao fixada.
  - Escopo: `webfilter-dns/Dockerfile`, `webfilter-dns/requirements.txt`
  - Cobre: design D3
  - Evidencia: saida do build com o digest e saida de `blocky version`
  - **Executor-1 APROVADO:** Blocky v0.34.0 (digest 17b03f89...), docker build EXIT 0
- [x] 1.2 Implementar updater de listas (le `sources.json`, baixa, concatena partes, normaliza cada entrada para `*.<dominio>`, ignora comentarios, rejeita conteudo sem dominios, troca atomica, intervalo minimo de 1 h por URL, maximo de 24 h, chama `POST /api/lists/refresh`); aceite: `python -m pytest tests/test_updater.py` com servidor HTTP local cobrindo sucesso, HTTP 404, timeout, HTTP 200 vazio, lista em 3 partes, normalizacao com e sem `*.` e comentarios, intervalo minimo e chamada de refresh.
  - Escopo: `webfilter-dns/app/updater.py`, `webfilter-dns/tests/test_updater.py`, `webfilter-dns/tests/conftest.py`, `webfilter-dns/requirements-dev.txt`
  - Cobre: classification "Listas de dominios atualizadas sem perder a ultima versao valida" (4 cenarios); design D3 (normalizacao)
  - Evidencia: saida vermelha antes da implementacao e verde depois
  - **Executor-1 APROVADO:** 10/10 testes passando
- [x] 1.3 Implementar supervisor: inicia Blocky; reinicia quando o sha256 de `blocky.yml` muda; volta para a ultima configuracao valida se o Blocky nao ficar saudavel; chama `POST /api/lists/refresh`, sem reiniciar, quando so arquivos de `profiles/` mudam; aceite: `python -m pytest tests/test_supervisor.py` cobrindo inicio, troca de configuracao, configuracao invalida com retorno a anterior e registro no log, e mudanca de lista de perfil sem reinicio.
  - Escopo: `webfilter-dns/app/supervisor.py`, `webfilter-dns/tests/test_supervisor.py`
  - Cobre: classification "Configuracao invalida nao derruba o resolvedor"; design D8
  - Evidencia: saida vermelha e verde; trecho de log da configuracao invalida
  - **Executor-1 APROVADO:** 4/4 testes passando
- [x] 1.4 Criar `webfilter-dns/docker-compose.yaml` proprio, sem alterar o compose do MDM: TCP 853 publicada, 53 nao publicada, volume somente leitura de `../source/volumes/work/plugins/webfilter/dns`, volume de listas, certificado somente leitura, healthcheck, `restart: unless-stopped`, e caminho de certificado declarado por variavel/volume. Aceite:
  - com fixtures de `blocky.yml`, listas locais e certificado de teste nos caminhos documentados, `docker compose -f webfilter-dns/docker-compose.yaml config` exit 0 e servico `healthy`;
  - `docker compose -f webfilter-dns/docker-compose.yaml port webfilter-dns 853` retorna porta, e o mesmo comando para 53 nao retorna;
  - `source/docker-compose.yaml` sem diff.
  - Escopo: `webfilter-dns/docker-compose.yaml`, `webfilter-dns/entrypoint.sh`, `webfilter-dns/Dockerfile`
  - Cobre: classification "Resolvedor exposto somente por DNS-over-TLS" (cenario sem TLS)
  - Evidencia: saidas dos comandos
  - **Executor-1 APROVADO:** docker compose config OK, portas corretas, volumes read-only
- [ ] 1.5 Teste de integracao do resolvedor com `blocky.yml` de fixture no formato de D8 (dois perfis, grupos de categoria, grupos de perfil com allowlist/blocklist), listas de fixture e CA de teste, consultando por DoT com SNI via dnspython; aceite: `python -m pytest tests/test_resolver_integration.py` cobrindo:
  - NXDOMAIN de dominio listado e de subdominio;
  - allowlist do perfil vencendo categoria de outro grupo;
  - blocklist do perfil;
  - allowlist sem modo exclusivo (`wikipedia.org` resolve);
  - dominio fora das regras resolvido, isolamento entre perfis e perfil sem politica resolvido;
  - handshake TLS com o certificado do hostname;
  - mudanca de blocklist aplicada sem reinicio e troca de categorias em `blocky.yml` aplicada, ambas em ate 5 minutos.
  - Escopo: `webfilter-dns/tests/test_resolver_integration.py`, `webfilter-dns/tests/fixtures/**`, `webfilter-dns/requirements-dev.txt`
  - Cobre: classification "Resolvedor aplica a decisao de sites do perfil" (8 cenarios), "Politica salva e aplicada pelo resolvedor sem intervencao manual" (categoria adicionada, dominio adicionado a blocklist)
  - Evidencia: saida do pytest com tempos medidos e PID do Blocky antes e depois da mudanca de blocklist
- [ ] 1.6 Medir o resolvedor com o catalogo completo real (todas as fontes de D5) em homologacao ou ambiente de medicao isolado; aceite: `python tests/measure_full_catalog.py` gera JSON com RSS apos carga, tempo entre reinicio e primeira resposta bloqueada e contagem de dominios por categoria; gate: tempo <= 60 s e RSS dentro do limite de memoria declarado para homologacao/producao pelo responsavel humano, sem ler memoria de host de producao por agente.
  - Escopo: `webfilter-dns/tests/measure_full_catalog.py`, `webfilter-dns/docker-compose.yaml` (somente `mem_limit` do servico, com base na medicao)
  - Cobre: design D3/D8 e riscos de reinicio e memoria
  - Evidencia: JSON da medicao, limite de memoria declarado para producao pelo responsavel humano, memoria do host de homologacao (`free -m`) e decisao sobre `mem_limit`

## 2. Plugin: estrutura e banco

- [ ] 2.1 Criar modulo Maven do plugin com `WebFilterPluginConfigurationImpl` (`PLUGIN_ID = "webfilter"`, pacote `com.hmdm.plugins.webfilter`) e modulo Liquibase; registrar em `plugins/pom.xml` e na linha unica de dependencia de `server/pom.xml`; aceite: `mvn -q -pl server -am package -DskipTests` exit 0 e `unzip -l server/target/*.war | grep webfilter-0.1.0.jar` encontra o jar.
  - Escopo: `server-source/plugins/webfilter/pom.xml`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/WebFilterPluginConfigurationImpl.java`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/guice/module/WebFilterLiquibaseModule.java`, `server-source/plugins/pom.xml`, `server-source/server/pom.xml`
  - Cobre: padrao de plugin do Context
  - Evidencia: saida do build e listagem do WAR
- [ ] 2.2 Adicionar infraestrutura de teste ao plugin (JUnit 5, Mockito, Testcontainers PostgreSQL 12, surefire) e changelog Liquibase com as 6 tabelas de D7, registro em `plugins` e permissao `plugin_webfilter_access`; aceite: `mvn -q -pl plugins/webfilter -am test -Dtest=WebFilterChangelogTest` aplica o changelog do core e o do plugin em banco limpo e prova:
  - `configurationId` unico;
  - cascade ao excluir perfil em politica, categorias, entradas e historico;
  - unicidade de categoria, de entrada por (`kind`, `value`), de pacote categorizado por cliente e de settings por cliente;
  - registro do plugin em `plugins` com `identifier`, `javascriptModuleFile`, `functionsViewTemplate`, `namelocalizationkey` e `functionsPermission = plugin_webfilter_access` (D13), e da permissao.
  - Escopo: `server-source/plugins/webfilter/pom.xml`, `server-source/plugins/webfilter/src/main/resources/liquibase/webfilter.changelog.xml`, `server-source/plugins/webfilter/src/test/**`
  - Cobre: policy-management "Exclusao do perfil remove a politica", "Entradas de allowlist e blocklist sao validadas" (mesma entrada nas duas listas, no banco); admin-console "Acesso controlado por permissao" (registro)
  - Evidencia: saida vermelha e verde; log do Testcontainers

## 3. Plugin: catalogos

- [ ] 3.1 Criar `webfilter-catalog.json` com as 14 categorias e o mapeamento exato de D5, e o carregador; aceite: `mvn -q -pl plugins/webfilter test -Dtest=CategoryCatalogTest` prova 14 ids unicos, somente `doh` obrigatoria, cada categoria com pelo menos uma fonte, URLs somente dos hosts `dbl.ipfire.org`, `blocklistproject.github.io` e `raw.githubusercontent.com`, e mapeamento igual a tabela D5.
  - Escopo: `server-source/plugins/webfilter/src/main/resources/webfilter-catalog.json`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/catalog/**`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/catalog/**`
  - Cobre: classification "Catalogo fixo de categorias"
  - Evidencia: saida vermelha e verde
- [ ] 3.2 Criar `webfilter-app-catalog.json` com pacotes por categoria e lista de pacotes protegidos (`com.hmdm.launcher`, `com.hwmdm.remote`), e o carregador; aceite: `mvn -q -pl plugins/webfilter test -Dtest=AppCatalogTest` prova categorias validas, nenhum pacote em `doh`, nenhum pacote protegido no catalogo e nomes de pacote validos; `python tests/verify_app_catalog_play.py` confirma HTTP 200 em `https://play.google.com/store/apps/details?id=<pacote>` para todo pacote.
  - Escopo: `server-source/plugins/webfilter/src/main/resources/webfilter-app-catalog.json`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/catalog/**`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/catalog/**`, `tests/verify_app_catalog_play.py`
  - Cobre: classification "Catalogo de aplicativos por categoria" (catalogo inicial valido)
  - Evidencia: saida vermelha e verde; saida da verificacao na Google Play com contagem de pacotes
- [ ] 3.3 Implementar categorizacao de aplicativos do cliente (mapper, DAO e servico): incluir, remover, recusar pacote protegido e isolar por `customerId`; aceite: `mvn -q -pl plugins/webfilter -am test -Dtest=AppCategoryDAOTest,AppCategoryServiceTest` cobre os cenarios.
  - Escopo: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/persistence/**`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/service/AppCategoryService.java`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/guice/module/WebFilterPersistenceModule.java`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/persistence/**`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/service/AppCategoryServiceTest.java`
  - Cobre: classification "Catalogo de aplicativos por categoria" (cliente categoriza, remover, pacote protegido)
  - Evidencia: saida vermelha e verde

## 4. Plugin: politica e decisao

- [ ] 4.1 Criar mappers e DAOs de politica, categorias, entradas, historico e settings, sempre filtrando por `customerId`; aceite: `mvn -q -pl plugins/webfilter -am test -Dtest=WebFilterPolicyDAOTest,WebFilterSettingsDAOTest` (Testcontainers) prova salvar, substituir, ler categorias e entradas, gravar historico, salvar e ler `dnsDomain`, e retorno vazio para dados de outro cliente.
  - Escopo: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/persistence/**`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/guice/module/WebFilterPersistenceModule.java`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/persistence/**`
  - Cobre: policy-management "Politica de filtro por perfil" (substituicao), "Isolamento de politicas por cliente"
  - Evidencia: saida vermelha e verde
- [ ] 4.2 Implementar servico de politica com validacao (categoria, dominio, curinga amplo, pacote, conflito entre listas, pacote protegido), inclusao de `doh`, substituicao e isolamento por cliente; aceite: `mvn -q -pl plugins/webfilter test -Dtest=WebFilterPolicyServiceTest` cobre os cenarios de "Politica de filtro por perfil", "Categoria doh sempre bloqueada com filtro ativo", "Entradas de allowlist e blocklist sao validadas", "Isolamento" e o cenario "Agente do MDM na blocklist de aplicativos".
  - Escopo: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/service/WebFilterPolicyService.java`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/service/validation/**`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/service/WebFilterPolicyServiceTest.java`
  - Cobre: policy-management (requisitos citados no aceite)
  - Evidencia: saida vermelha e verde; tabela cenario -> teste -> linha do assert
- [ ] 4.3 Implementar decisao de aplicativos (allowlist > blocklist > categoria > permitido; pacotes protegidos e app de quiosque nunca bloqueados) e calculo de `locked_packages`/`unlocked_packages` com historico e mesclagem dos valores do administrador; antes da OS-I, citar `arquivo:linha` que prova o formato real esperado pelo launcher para `locked_packages` e `unlocked_packages` e a origem real do app de quiosque; aceite: `mvn -q -pl plugins/webfilter test -Dtest=AppDecisionTest,LockedPackagesCalculatorTest` cobre:
  - cenarios de aplicativos de "Ordem de decisao unica";
  - "App de quiosque em categoria bloqueada";
  - todos os cenarios de distribution "Sync inclui aplicativos bloqueados e liberados do perfil".
  - Escopo: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/decision/**`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/decision/**`
  - Cobre: policy-management "Ordem de decisao unica para sites e aplicativos" (aplicativos), "O filtro nunca bloqueia o proprio MDM" (aplicativos); distribution (aplicativos)
  - Evidencia: saida vermelha e verde; tabela cenario -> teste -> linha do assert

## 5. Plugin: configuracao do resolvedor

- [ ] 5.1 Implementar gerador (D8): `blocky.yml`, `profiles/c<customerId>-p<configurationId>-deny.txt`, `profiles/c<customerId>-p<configurationId>-allow.txt` (normalizados, com dominios protegidos do MDM na allowlist) e `sources.json`, com escrita atomica em `<plugins.files.directory>/webfilter/dns/`. Aceite:
  - `mvn -q -pl plugins/webfilter test -Dtest=ResolverConfigWriterTest` compara com fixtures para 0, 1 e 2 perfis ativos (perfil desativado ausente de `clientGroupsBlock`);
  - prova que o dominio de `base.url` entra na allowlist de todo perfil e que o arquivo final nunca fica parcial;
  - a saida gerada para 2 perfis sobe no container de 1.1 com healthcheck `healthy`.
  - Escopo: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/resolver/**`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/resolver/**`, `server-source/plugins/webfilter/src/test/resources/resolver/**`
  - Cobre: policy-management "O filtro nunca bloqueia o proprio MDM" (dominios); classification "Resolvedor aplica a decisao de sites do perfil" (perfil sem filtro ativo); design D3/D4/D8
  - Evidencia: saida vermelha e verde; status do container com a configuracao gerada
- [ ] 5.2 Regenerar a configuracao ao salvar politica, listas ou dominio, na inicializacao do plugin e a cada 10 minutos (modulo de tarefa no padrao `DeviceInfoTaskModule`); aceite: `mvn -q -pl plugins/webfilter test -Dtest=ResolverConfigTriggerTest` prova a regeneracao nos gatilhos.
  - Escopo: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/guice/module/WebFilterTaskModule.java`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/WebFilterPluginConfigurationImpl.java`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/service/**`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/resolver/ResolverConfigTriggerTest.java`
  - Cobre: classification "Politica salva e aplicada pelo resolvedor sem intervencao manual"
  - Evidencia: saida vermelha e verde

## 6. Plugin: REST, push e sync

- [ ] 6.1 Criar `WebFilterRestModule` e resources em `/rest/plugins/webfilter/webfilter/private/*` com `JWTFilter`, `AuthFilter`, `PluginAccessFilter` e `PrivateIPFilter`. Endpoints: catalogo, perfis com politica, PUT politica, categorizacao de aplicativos do cliente (listar, incluir, remover), GET/PUT dominio DNS. Aceite:
  - `mvn -q -pl plugins/webfilter test -Dtest=WebFilterResourceTest` cobre 403 sem permissao, 404 perfil de outro cliente, 400 para categoria, dominio, curinga, pacote, conflito, pacote protegido e dominio DNS invalidos, e 200 nos fluxos validos;
  - em servidor rodando, `curl` autenticado repete um caso de cada status.
  - Escopo: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/rest/**`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/guice/module/WebFilterRestModule.java`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/WebFilterPluginConfigurationImpl.java`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/rest/**`
  - Cobre: classification "Catalogo fixo de categorias" (consulta), "Catalogo de aplicativos por categoria" (HTTP); admin-console "Acesso controlado por permissao" (endpoints), "Configurar dominio DNS do filtro" (validacao); policy-management (respostas HTTP)
  - Evidencia: saida vermelha e verde; comandos `curl` com status e corpo
- [ ] 6.2 Enviar `configUpdated` aos dispositivos dos perfis afetados ao salvar dominio DNS, politica, listas e categorizacao de aplicativos; aceite: `mvn -q -pl plugins/webfilter test -Dtest=WebFilterPushTest` prova envio a cada dispositivo afetado e a nenhum de perfil nao afetado.
  - Escopo: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/service/**`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/service/WebFilterPushTest.java`
  - Cobre: distribution "Dispositivos sao avisados quando a politica muda" (2 cenarios)
  - Evidencia: saida vermelha e verde
- [ ] 6.3 Implementar `WebFilterSyncResponseHook` (D9): arvore JSON com `@JsonValue`, `webfilterDnsHost` a partir do `dnsDomain` do cliente, mesclagem de `locked_packages`/`unlocked_packages` do launcher com o resultado de 4.3. Aceite:
  - `mvn -q -pl plugins/webfilter test -Dtest=WebFilterSyncResponseHookTest` prova os 4 cenarios de hostname;
  - prova tambem a mesclagem com valores do administrador, JSON igual campo a campo fora dos campos do filtro, e assinatura de `CryptoUtil.getDataSignature` igual a SHA1(segredo + JSON sem espacos);
  - em servidor rodando, `python tests/test_webfilter_sync.py` faz o sync real com `X-Request-Signature`, valida a assinatura pela regra do launcher e confere os campos.
  - Escopo: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/sync/**`, `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/guice/module/**`, `server-source/plugins/webfilter/src/test/java/com/hmdm/plugins/webfilter/sync/**`, `tests/test_webfilter_sync.py`
  - Cobre: distribution "Sync inclui o hostname DNS do perfil com filtro ativo" (4 cenarios), "Sync inclui aplicativos bloqueados e liberados do perfil" (entrega no sync), "Resposta do sync permanece compativel e assinada" (demais campos, assinatura)
  - Evidencia: saida vermelha e verde; saida do teste contra o servidor rodando
- [ ] 6.4 Provar compatibilidade com o launcher atualmente em producao (`dist/hmdm-v1.0.apk`, sem suporte ao DNS do filtro) em tablet de homologacao; aceite: com politica ativa bloqueando um aplicativo instalado, o sync seguinte conclui sem erro visivel, o aplicativo desaparece/inacessivel para o usuario, volta ao liberar, e o DNS privado nao muda. `logcat`, `dumpsys` e `settings get` sao evidencia complementar, nao prova principal.
  - Escopo: somente sondas em pasta temporaria do fiscal
  - Cobre: distribution "Launcher em producao sem suporte ao DNS do filtro"; android-enforcement "Dispositivo bloqueia e libera aplicativos do perfil" (aplicativo bloqueado, com o launcher atual)
  - Evidencia: video/screenrecord e capturas do tablet, diario da jornada, mais trecho do `adb logcat`, `dumpsys` e `settings get global private_dns_mode` como complemento

## 7. Launcher (somente DNS privado)

- [ ] 7.1 Adicionar `webfilterDnsHost` a `ServerConfig` e criar a classe de decisao pura de D10; aceite: `./gradlew :app:testDebugUnitTest --tests "*WebFilterDnsDecisionTest*"` cobre aplicar, trocar hostname, remover com e sem `no_config_private_dns` no perfil, nunca aplicado, Android 9, sem Device Owner e registro unico por hostname.
  - Escopo: `android-source/app/src/main/java/com/hmdm/launcher/json/ServerConfig.java`, `android-source/app/src/main/java/com/hmdm/launcher/webfilter/**`, `android-source/app/src/test/java/com/hmdm/launcher/webfilter/**`
  - Cobre: android-enforcement "Dispositivo aplica o DNS privado do perfil" (aplicar, hostname alterado), "Dispositivo remove o DNS do filtro quando o campo deixa de vir" (3 cenarios), "Dispositivo sem suporte ao filtro de sites e sinalizado"
  - Evidencia: saida vermelha e verde; tabela cenario -> teste -> linha do assert
- [ ] 7.2 Executar a decisao no `ConfigUpdater` depois do passo de restricoes do perfil, em thread de fundo, com chamadas ao DPM atras de interface, persistencia do ultimo hostname e registro no log remoto. Aceite:
  - `./gradlew :app:testDebugUnitTest --tests "*WebFilterDnsApplierTest*"` com DPM simulado cobre `PRIVATE_DNS_SET_NO_ERROR`, erro sem persistir host recusado e com nova tentativa no proximo sync, e ordem apos restricoes e `locked_packages`;
  - `./gradlew :app:assembleRelease` exit 0;
  - o bloqueio de aplicativos existente continua coberto (cenario de 6.4 repetido com o launcher novo).
  - Escopo: `android-source/app/src/main/java/com/hmdm/launcher/helper/ConfigUpdater.java`, `android-source/app/src/main/java/com/hmdm/launcher/webfilter/**`, `android-source/app/src/test/java/com/hmdm/launcher/webfilter/**`
  - Cobre: android-enforcement "Dispositivo aplica o DNS privado do perfil" (resolvedor inacessivel), "Mudanca nas restricoes do perfil nao desfaz o filtro"
  - Evidencia: saida vermelha e verde; saida do `assembleRelease`; screenrecord/capturas do aplicativo bloqueado com o launcher novo, com `dumpsys` apenas como complemento

## 8. Console

- [ ] 8.1 Criar modulo JavaScript, tela principal com as tres abas de D13 e pacotes `i18n/en_US.json` e `i18n/pt_PT.json`; aceite: `npx playwright test tests/playwright/hwmdm-webfilter-access.spec.js` prova menu lateral sem "Web Filter" e API 403 sem permissao, entrada visivel e tela sem erro de JavaScript com permissao, e entrada ausente apos desativar o plugin na tela de Plugins.
  - Escopo: `server-source/plugins/webfilter/src/main/webapp/**`, `tests/playwright/hwmdm-webfilter-access.spec.js`
  - Cobre: admin-console "Acesso controlado por permissao" (3 cenarios); design D13
  - Evidencia: relatorio do Playwright com vermelho antes e verde depois
- [ ] 8.2 Implementar a tela de politica por perfil (categorias, allowlist/blocklist de dominios e aplicativos, erros por entrada), dominio DNS e atribuicao das fontes; aceite: `npx playwright test tests/playwright/hwmdm-webfilter-policy.spec.js` cobre todos os cenarios de "Configurar politica por perfil", "Configurar dominio DNS do filtro" e "Atribuicao das fontes das listas".
  - Escopo: `server-source/plugins/webfilter/src/main/webapp/**`, `tests/playwright/hwmdm-webfilter-policy.spec.js`
  - Cobre: admin-console "Configurar politica por perfil", "Configurar dominio DNS do filtro", "Atribuicao das fontes das listas"
  - Evidencia: relatorio do Playwright e capturas de tela de cada estado
- [ ] 8.3 Implementar a tela de categorizacao de aplicativos do cliente; aceite: `npx playwright test tests/playwright/hwmdm-webfilter-apps.spec.js` cobre incluir pacote, pacote do catalogo inicial sem remocao e pacote protegido recusado.
  - Escopo: `server-source/plugins/webfilter/src/main/webapp/**`, `tests/playwright/hwmdm-webfilter-apps.spec.js`
  - Cobre: admin-console "Categorizar aplicativos do cliente"
  - Evidencia: relatorio do Playwright e capturas de tela
- [ ] 8.4 Garantir o padrao visual e de navegacao do console nas tres abas; aceite: `npx playwright test tests/playwright/hwmdm-webfilter-ui-padrao.spec.js`, no padrao de medicao de `hwmdm-ui-overhaul.spec.js`, prova:
  - contraste >= 4,5:1 (formula WCAG sobre cores computadas) de todo texto visivel no tema escuro;
  - `document.documentElement.scrollWidth <= innerWidth + 1` e acoes alcancaveis em 360, 768 e 1280 px;
  - voltar da edicao de perfil retorna a lista do Web Filter;
  - titulo igual a entrada do menu lateral;
  - textos em portugues com usuario em portugues.
  - Escopo: `server-source/plugins/webfilter/src/main/webapp/**`, `tests/playwright/hwmdm-webfilter-ui-padrao.spec.js`
  - Cobre: admin-console "Tela segue o padrao visual e de navegacao do console" (4 cenarios)
  - Evidencia: relatorio do Playwright com vermelho antes e verde depois; capturas nos temas claro e escuro em 360 e 1280 px

## 9. Ponta a ponta em homologacao

- [ ] 9.1 Preparar homologacao: registro `*.<dominio DNS do filtro>`, certificado curinga emitido por DNS-01 e montado no `webfilter-dns`, dominio salvo na tela do plugin; aceite: a partir de rede externa, consulta DoT via dnspython com SNI `id-c1-p1.<dominio>` completa o handshake com cadeia publica valida.
  - Escopo: somente configuracao de ambiente e dados de homologacao; nenhum arquivo versionado
  - Cobre: classification "Resolvedor exposto somente por DNS-over-TLS" (handshake); design D4
  - Evidencia: saida da consulta com emissor do certificado; sem ambiente, o fiscal registra bloqueio e nao aprova 9.2 a 9.5
- [ ] 9.2 Jornada de aplicativos no tablet real com o launcher novo, pelo console: bloquear `social_media` com um app catalogado instalado, incluir outro app na allowlist e um app sem categoria na blocklist, depois remover da blocklist. Aceite pela tela/tablet:
  - app da categoria desaparece ou fica inacessivel para o usuario; app da allowlist acessivel; app da blocklist desaparece ou fica inacessivel;
  - apos remover da blocklist, o app volta com os dados preservados (arquivo criado antes do bloqueio continua presente);
  - um pacote nao instalado incluido na blocklist nao impede o bloqueio dos demais;
  - tudo em ate 5 minutos.
  - Escopo: somente sondas em pasta temporaria do fiscal
  - Cobre: android-enforcement "Dispositivo bloqueia e libera aplicativos do perfil"; policy-management "Ordem de decisao unica" (aplicativos); distribution (aplicativos)
  - Evidencia: video/screenrecord, capturas numeradas e diario; `adb dumpsys` com horario apenas como complemento
- [ ] 9.3 Jornada de sites no mesmo tablet: `social_media` bloqueada, `linkedin.com` na allowlist, `noticias.example.com` na blocklist. Aceite, em ate 5 minutos:
  - tela de configuracao do Android mostra DNS privado em modo hostname com o perfil e a opcao bloqueada para o usuario;
  - Chrome nao abre `facebook.com` nem `noticias.example.com`, mas abre `www.linkedin.com`, `wikipedia.org` e o dominio do MDM;
  - `private_dns_mode`, `private_dns_specifier`, `dumpsys device_policy` e consultas tecnicas podem ser anexados como complemento.
  - Escopo: somente sondas em pasta temporaria do fiscal
  - Cobre: android-enforcement "Dispositivo aplica o DNS privado do perfil", "Filtro de sites vale para todos os apps"; policy-management "Ordem de decisao unica" (sites), "O filtro nunca bloqueia o proprio MDM"
  - Evidencia: video/screenrecord, capturas de tela do Chrome e das configuracoes, diario; saidas tecnicas somente como complemento
- [ ] 9.4 Jornada de desativacao no mesmo tablet; aceite: apos desativar a politica, os aplicativos bloqueados voltam com dados preservados, a tela de DNS privado volta ao modo automatico quando o perfil permite, a opcao volta a ser editavel e `facebook.com` abre no navegador.
  - Escopo: somente sondas em pasta temporaria do fiscal
  - Cobre: android-enforcement "Dispositivo remove o DNS do filtro quando o campo deixa de vir"; distribution "Politica desativada libera aplicativos", "Politica desativada"
  - Evidencia: video/screenrecord, capturas numeradas e diario; saidas `adb` apenas como complemento
- [ ] 9.5 Jornada de resolvedor fora do ar; aceite: com o filtro ativo, `docker compose -f webfilter-dns/docker-compose.yaml stop webfilter-dns` faz `wikipedia.org` deixar de abrir no navegador do tablet e o `start` correspondente restabelece a navegacao sem acao no tablet; os aplicativos bloqueados continuam bloqueados durante a queda.
  - Escopo: somente sondas em pasta temporaria do fiscal
  - Cobre: design, risco "Resolvedor fora do ar"
  - Evidencia: video/screenrecord, capturas antes/durante/depois e diario; comandos do host apenas como complemento

## 10. Publicacao em producao

- [ ] 10.1 Escrever `docs/WEBFILTER.md`: pre-requisitos (DNS curinga, certificado DNS-01, porta 853, memoria medida em 1.6), backup, sequencia de publicacao e piloto de D12, rollback funcional e tecnico, limites (IP literal, portal cativo, Android anterior ao 10, app nao listado sem deteccao em tempo real) e fontes com licencas; aceite: o fiscal confere o documento item a item contra D12 e contra os riscos de `design.md`.
  - Escopo: `docs/WEBFILTER.md`
  - Cobre: design D12 e riscos documentados
  - Evidencia: checklist D12 -> secao do documento
- [ ] 10.2 Criar script SQL de remocao do schema do plugin para o rollback tecnico (tabelas `plugin_webfilter_*`, registro em `plugins`, permissao e vinculos de permissao); aceite: em banco de homologacao com o plugin aplicado, o script remove somente objetos do plugin e o schema das tabelas do core fica identico ao de antes do deploy (`pg_dump --schema-only` comparado, excluindo `plugin_webfilter_*`), e o servidor com a WAR anterior inicia sem erro.
  - Escopo: `server-source/plugins/webfilter/src/main/resources/sql/remove-webfilter.sql`
  - Cobre: design D12 (rollback tecnico)
  - Evidencia: diff dos `pg_dump --schema-only`, log de inicio do servidor
- [ ] 10.3 Ensaiar a publicacao completa em homologacao restaurada com copia do banco de producao, seguindo apenas `docs/WEBFILTER.md`. Aceite:
  - backup, deploy, resolvedor, publicacao do launcher por `scripts/publicar-apk.sh` e atualizacao de um tablet ja matriculado sem nova matricula;
  - piloto de sites e aplicativos em um perfil com os demais perfis sem mudanca de comportamento;
  - rollback funcional e rollback tecnico (10.2) executados.
  - Escopo: somente ambiente de homologacao e sondas em pasta temporaria do fiscal
  - Cobre: design D12; proposal Impact "Dispositivos ja matriculados" e "Banco de producao"
  - Evidencia: registro passo a passo com horarios, versao do launcher antes e depois no tablet, log Liquibase, comparacao de configuracao dos perfis nao piloto
- [ ] 10.4 Publicar em producao, executada pelo responsavel do ambiente com autorizacao explicita registrada, seguindo `docs/WEBFILTER.md`. Aceite, verificado pelo fiscal sem executar acoes em producao:
  - backup existente e legivel (`pg_restore --list` ou cabecalho do dump);
  - log Liquibase com o changelog `webfilter` aplicado e plugin "Web Filter" listado no console de producao;
  - tablet piloto de producao com launcher novo, aplicativo bloqueado e site bloqueado conforme o perfil piloto;
  - demais perfis sem politica.
  - Escopo: ambiente de producao, somente pelo responsavel; nenhum arquivo versionado
  - Cobre: proposal Why (modulo adicional no MDM em producao)
  - Evidencia: registro da autorizacao, saidas e capturas fornecidas pelo responsavel e conferidas pelo fiscal
