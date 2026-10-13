## Why

## PRD IDs

- **PRD-01:** criar plugin WebFilter isolado no MDM existente.
- **PRD-02:** manter catalogo fixo de 14 categorias para sites e aplicativos.
- **PRD-03:** combinar fontes abertas de dominios com atribuicao de licenca.
- **PRD-04:** manter catalogo inicial de aplicativos e extensoes por cliente.
- **PRD-05:** bloquear `doh` sempre que o filtro estiver ativo.
- **PRD-06:** aplicar allowlist > blocklist > categoria > permitido para dominios e aplicativos.
- **PRD-07:** proteger os dominios e pacotes essenciais do MDM.
- **PRD-08:** proteger tambem o app principal de quiosque do perfil.
- **PRD-09:** usar resolvedor DNS-over-TLS isolado por perfil.
- **PRD-10:** entregar hostname DNS e pacotes pelo sync existente.
- **PRD-11:** alterar o launcher somente para DNS privado do WebFilter.
- **PRD-12:** enviar push `configUpdated` ao salvar dominio, politica, listas ou categorizacao que afetem perfis.
- **PRD-13:** prover tela Web Filter no console com permissao propria.
- **PRD-14:** exigir validacao em homologacao com tablet real antes de producao.
- **PRD-15:** publicar em producao somente pelo responsavel humano com backup e rollback.

O Headwind MDM ja existe, esta provisionado e em producao, e gerencia os tablets Android como Device Owner, mas nao controla o acesso a sites nem a aplicativos por categoria. Esta mudanca adiciona ao MDM em producao um modulo WebFilter: por perfil, o administrador bloqueia categorias (que valem para sites e para aplicativos) e mantem allowlist e blocklist de dominios e de aplicativos. O MDM nao e reconstruido nem alterado alem dos pontos de integracao listados em Impact.

## What Changes

- Novo plugin Maven `webfilter` em `repo-mdm/server-source/plugins/webfilter/`, no padrao real dos plugins existentes (`plugins/deviceinfo`): Liquibase, MyBatis, Guice, rotas `private`/`public`, permissao `plugin_webfilter_access` e isolamento por `customerId`.
- **Categorizacao:** catalogo fixo de 14 categorias, a mesma lista para sites e aplicativos.
  - Sites: cada categoria e a uniao das listas de dominios de tres fontes abertas, IPFire DBL (CC BY-SA 4.0), Block List Project (Unlicense) e UT1 (CC BY-SA 4.0).
  - Aplicativos: catalogo de pacotes Android por categoria, versionado no plugin, que cada cliente pode ampliar com seus proprios pacotes.
  - A categoria `doh` e sempre bloqueada quando o filtro esta ativo, para impedir contorno por DNS-over-HTTPS.
- **Basico de webfilter:** allowlist e blocklist por perfil, tanto de dominios quanto de aplicativos. Ordem de decisao unica: allowlist > blocklist > categoria bloqueada > permitido. Dominios e pacotes do proprio MDM nunca sao bloqueados.
- **Sites:** novo resolvedor `webfilter-dns` em `repo-mdm/webfilter-dns/`, com `docker-compose` proprio. E um resolvedor DNS-over-TLS que aplica categorias, allowlist e blocklist do perfil que consulta. O launcher recebe `webfilterDnsHost` no sync e aplica o DNS privado do Android. **[REVISAO 2026-09-16]** O filtro de sites exige uma nova versao do launcher publicada pelo mecanismo existente.
- **Aplicativos:** o plugin entrega no sync os pacotes bloqueados do perfil pelas configuracoes `locked_packages`/`unlocked_packages` do launcher, que o launcher em producao ja aplica (pacote suspenso e oculto). **[REVISAO 2026-09-16]** Para esta parte, o launcher atual continua suficiente; a mudanca de launcher e apenas para DNS privado.
- Entrega ao dispositivo pelo `SyncResponseHook` que o core ja oferece; nenhuma classe do core do servidor e alterada.
- Console: tela "Web Filter" do plugin para dominio DNS, categorias, allowlist/blocklist de dominios e aplicativos e categorizacao de aplicativos do cliente, com atribuicao das fontes.
- **Publicacao em producao:** runbook, ensaio completo em homologacao com copia do banco de producao, e execucao em producao somente com autorizacao explicita do responsavel, com evidencia verificada pelo fiscal.
- **[REVISAO 2026-09-16] Contrato fiscal:** registros existentes em `fiscal/` que tenham sido produzidos sem sub-agente real, sem FQ independente, sem vermelho confirmado ou por FO julgando qualidade sao invalidos e nao podem ser usados como base de aceite.

## Capabilities

### New Capabilities

- `webfilter/policy-management`: politica por perfil com categorias, allowlist e blocklist de dominios e aplicativos, ordem de decisao e protecao do MDM.
- `webfilter/classification`: catalogos de categorias de sites e de aplicativos, fontes abertas, atualizacao das listas e decisao de bloqueio no resolvedor DNS.
- `webfilter/distribution`: entrega do hostname DNS e dos pacotes bloqueados do perfil pelo sync existente, e notificacao de mudanca.
- `webfilter/android-enforcement`: dispositivo aplica o DNS privado do perfil e bloqueia/libera aplicativos.
- `webfilter/admin-console`: tela do plugin no console, com permissao propria.

### Modified Capabilities

Nenhuma. Nao ha specs existentes em `openspec/specs/`.

## Non-Goals

- Reconstruir, refatorar ou alterar funcionalidades do MDM existente (enrollment, kiosk, GPS, acesso remoto, lista de apps do perfil, compose e configuracao do servidor).
- Excecoes por grupo, dispositivo ou usuario; agendamento por horario.
- Eventos, relatorios e log de navegacao ou de uso de aplicativos.
- VPN/`VpnService`, APK companion, proxy, inspecao TLS, filtro por URL/caminho ou por endereco IP.
- Classificacao automatica de aplicativos pela Google Play (sem API oficial) e provedor comercial de classificacao.
- Deteccao em tempo real de app nao listado no perfil (recurso da versao Pro do launcher, ausente na versao open source em uso).
- Filtro de sites em Android anterior ao 10 (API 29), que nao tem API de DNS privado para Device Owner. O bloqueio de aplicativos funciona em todos os dispositivos suportados pelo launcher (Android 8+, `minSdkVersion 26`).
- Alta disponibilidade do resolvedor.

## Impact

- **Pontos de integracao com o MDM existente (lista fechada de arquivos do MDM que podem ser alterados):**
  1. `server-source/plugins/pom.xml`: linha `<module>webfilter</module>`.
  2. `server-source/server/pom.xml`: linha de dependencia do plugin, como os demais plugins, para entrar no WAR.
  3. Launcher `com.hmdm.launcher`: campo `webfilterDnsHost` em `ServerConfig`, classe auxiliar em pacote novo `com.hmdm.launcher.webfilter` e chamada de aplicacao do DNS privado em `ConfigUpdater`. So o Device Owner pode definir o DNS privado. **Ha APK novo para o launcher quando o filtro de sites for usado.**
- **Novo codigo do WebFilter:** `server-source/plugins/webfilter/**`, `webfilter-dns/**`, testes e documentacao do modulo.
- **Banco de producao:** o changelog Liquibase do plugin cria tabelas `plugin_webfilter_*` e registra plugin e permissao. Hoje o `rollback-para-producao.sh` assume WAR sem mudanca de schema; por isso o backup do banco e obrigatorio antes da publicacao.
- **Dispositivos ja matriculados:** nao precisam de nova matricula. O bloqueio de aplicativos funciona com o launcher atual. O filtro de sites exige a versao nova do launcher, publicada pelo mecanismo existente (`scripts/publicar-apk.sh`, que aponta a nova versao no perfil).
- **Insumos humanos obrigatorios antes de uso real:** URL e credenciais de homologacao, tablet fisico piloto, dominio DNS do filtro, registro wildcard, certificado TLS wildcard publico emitido por DNS-01, limites de memoria da homologacao, autorizacao formal para qualquer leitura de copia de banco de producao em homologacao, e autorizacao separada para 10.4 em producao.
- **Infra:** container `webfilter-dns` com compose proprio; porta 853/TCP exposta; registro DNS curinga `*.<dominio DNS do filtro>`; certificado TLS curinga valido publicamente para esse dominio.
- **Operacao:** com o filtro de sites ativo, resolvedor fora do ar = tablets do perfil sem resolucao de nomes (o modo estrito do Android nao faz fallback). Remover o resolvedor exige antes desativar as politicas e esperar os dispositivos sincronizarem.
- **Licencas:** listas CC BY-SA 4.0 exigem atribuicao das fontes, exibida no console.
- **Validacao:** execucao por fiscal + executores; cada tarefa traz escopo de arquivos, cenario coberto, comando de aceite e evidencia.
