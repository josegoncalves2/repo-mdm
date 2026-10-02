# Changelog

Todas as mudanças notáveis da plataforma HWMDM são registradas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e o versionamento segue [Semantic Versioning 2.0.0](https://semver.org/lang/pt-BR/)
— explicado em detalhe, aplicado a este projeto, em `docs/VERSIONAMENTO.md`.

## [Unreleased]

### Added
- Controle de versão/build/compilação da plataforma, visível na interface web
  (rodapé do menu lateral e diálogo "About"): `VERSION`, `CHANGELOG.md`,
  `docs/VERSIONAMENTO.md`, ferramenta `docs/versao/gerar-build-info.py`,
  diretiva `hwmdmVersionFooter` e bloco de versão no About. — pacote de
  trabalho "versao".

<!-- ORQUESTRADOR: complete aqui, antes do lançamento de 1.0.0, os itens dos
     demais pacotes de trabalho (outer loop, acesso remoto, todos os módulos,
     webfilter) que ainda não tiverem entrada própria abaixo. -->

## [1.0.0] - 2026-09-24

Primeiro lançamento numerado da plataforma HWMDM como guarda-chuva única,
reunindo o console de administração (baseado no núcleo Headwind MDM 5.39.2),
o agente de acesso remoto Android (`com.hwmdm.remote`) e o plugin de
Web Filter com resolvedor DNS dedicado.

### Added
- Plugin de Web Filter (`server-source/plugins/webfilter`, versão `0.1.0`):
  catálogo de sites e aplicativos (`webfilter-catalog.json`,
  `webfilter-app-catalog.json`) e sincronização com o agente
  (`WebFilterSyncResponseHook`, `WebFilterCatalog`).
- Plugin de registro de módulos (`server-source/plugins/moduleregistry`,
  versão `0.1.0`).
- Resolvedor DNS de filtro (`webfilter-dns`, imagem
  `hwmdm/webfilter-dns:0.34.0`) para impor o filtro no servidor/DNS, já que
  nada no tablet obedece a imposição local.
- Agente Android de acesso remoto `com.hwmdm.remote`
  (`remote-agent/`, `versionName 1.20` / `versionCode 21`), com injeção de
  toque via `InputInjectionService` (Accessibility) e decodificação de vídeo
  via MSE+JMuxer no console, servida em `remote.html` /
  `remoteSupport.service.js` / `remote.controller.js`.

### Changed
- Console web (`server-source/server/src/main/webapp`) sobre o núcleo
  Headwind MDM `5.39.2` (`REPO/dist/hmdm.war`), com telas próprias de acesso
  remoto e do menu lateral de módulos.

<!-- ORQUESTRADOR: liste aqui os demais itens confirmados no disco pelos
     outros pacotes de trabalho (outer loop / todos os módulos / visual) que
     compõem este primeiro lançamento 1.0.0, com a mesma exigência: só o que
     for comprovável no repositório. -->

### Fixed
<!-- ORQUESTRADOR: registre aqui as correções (ex.: teclado IP/MSE do acesso
     remoto, corrigido em 24-09 segundo a memória do projeto) com a
     referência de onde estão no disco. -->
