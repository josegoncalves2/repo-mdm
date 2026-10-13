# 03 - Provas de uso real

Este documento define as jornadas que aceitam fatias do WebFilter. Uma tarefa so passa quando o comportamento aparece no console ou no tablet como uma pessoa veria. Provas tecnicas podem complementar, mas nao substituem a experiencia visivel.

## Politica de prova

### Aceito como prova principal

- Playwright usando a interface: login real pela tela, clique, preenchimento, leitura do DOM visivel, video e trace.
- Tablet fisico gerenciado: toque real ou `adb shell input` guiado por tela, screenrecord, screenshots, navegador aberto, app visivel/ausente.
- Estado visivel no console: mensagem de erro, contador, lista, aba, texto, botao desabilitado, permissao no menu.
- Comportamento percebido pelo usuario: app some, app volta, pagina nao abre, DNS privado aparece bloqueado nas configuracoes.
- Para J01 apenas: consulta DNS-over-TLS com SNI, porque a fatia B ainda nao tem console/tablet integrado. J01 e gate tecnico, nao aceite de uso real do produto.

### Aceito apenas como complemento

- `adb shell settings get`, `dumpsys`, `logcat`.
- `docker compose ps`, healthcheck, logs.
- `curl`, `pytest`, JUnit, Maven, Gradle.
- `ping`, `nslookup`, `dig`, dnspython fora da J01.

### Nunca aceita sozinho

- endpoint respondeu 200;
- servico esta online;
- container esta up;
- porta responde;
- unit test passou sem jornada;
- print isolado sem roteiro;
- mock confirmou chamada;
- banco contem linha.

## Evidencias obrigatorias

Toda jornada deve gerar video/trace, capturas numeradas, diario com horario, hashes sha256, identificacao do ambiente/tablet e falhas registradas. Evidencias pesadas ficam fora do repo:

```text
<tmp>/fiscal-probes/add-webfilter-module/<jornada>/<timestamp>/
```

Quando o FQ for sub-agente criado pelo FO, cada jornada de console/tablet tambem exige J13: assinatura humana dizendo que assistiu a evidencia principal e que ela corresponde ao ambiente informado.

## J00 - Linha de base do MDM existente

**Objetivo:** provar que o MDM atual funciona antes do WebFilter.

**Pessoa:** administrador MDM e usuario do tablet.

**Passos:** login na homologacao pela tela; abrir Devices; abrir dispositivo piloto; abrir perfil; confirmar launcher ativo no tablet; abrir app permitido; abrir navegador e carregar `wikipedia.org`; acionar ou aguardar sync normal.

**Constatacoes:** console autentica; dispositivo e perfil aparecem; tablet continua gerenciado; app permitido abre; site permitido carrega; nenhum erro novo aparece na tela.

## J01 - Gate tecnico do resolvedor DNS isolado

**Cobre:** fatia B, tarefas 1.1 a 1.6.

**Natureza:** J01 nao prova uso real por pessoa; e um gate tecnico obrigatorio antes de integrar o resolvedor ao MDM. A Fatia B nao pode comecar sem J00 aceita.

**Passos:** subir resolvedor isolado com fixtures de `blocky.yml`, listas e certificado de teste; consultar por DNS-over-TLS com SNI de perfil; consultar dominio de categoria bloqueada, subdominio listado, dominio em allowlist e dominio permitido; alterar blocklist e repetir sem reinicio; trocar categoria em `blocky.yml`; reiniciar e confirmar que nao responde sem filtro antes de carregar listas validas.

**Constatacoes:** handshake TLS usa hostname esperado; bloqueados retornam NXDOMAIN; allowlist vence categoria; allowlist nao vira modo exclusivo; perfil sem politica resolve; allow/blocklist muda sem reinicio; categoria muda dentro do limite; nao ha janela observada com resposta sem filtro.

## J02 - Acesso ao Web Filter no console

**Cobre:** 8.1 e parte de 6.1.

**Passos:** entrar sem permissao e procurar menu; entrar com permissao e abrir Web Filter; desativar plugin na tela de Plugins e voltar ao menu.

**Constatacoes:** sem permissao nao ha menu; com permissao a tela abre sem erro visivel; plugin desativado remove a entrada.

## J03 - Politica por perfil no console

**Cobre:** 8.2, 4.1, 4.2, 6.1.

**Passos:** abrir Web Filter; editar perfil "Tablets Loja"; ativar filtro; marcar `adult` e `social_media`; confirmar `doh` marcada e desabilitada; adicionar `*.empresa.com.br` na allowlist de dominios e `com.king.candycrushsaga` na blocklist de apps; salvar; reabrir; tentar salvar `*.com` na blocklist.

**Constatacoes:** sucesso visivel; lista mostra perfil ativo, 3 categorias e 2 entradas; valores persistem; erro de `*.com` aparece junto da entrada; politica anterior permanece apos erro.

## J04 - Sites pelo console e tablet

**Cobre:** 5.1, 5.2, 8.2, 9.3.

**Passos:** configurar dominio DNS do filtro; ativar politica bloqueando `social_media`; colocar `linkedin.com` na allowlist e `noticias.example.com` na blocklist; salvar; no tablet com launcher novo, sincronizar; abrir Chrome para `facebook.com`, `noticias.example.com`, `www.linkedin.com`, `wikipedia.org` e dominio do MDM; abrir Configuracoes > DNS privado e tentar alterar.

**Constatacoes:** facebook e noticias nao abrem; linkedin, wikipedia e MDM abrem; DNS privado aparece em modo hostname; opcao de DNS privado esta bloqueada.

## J05A - Aplicativos com launcher atual

**Cobre:** 6.4 e parte de aplicativos de 4.3/6.3.

**Passos:** no tablet com launcher atualmente em producao, abrir app alvo e criar/verificar dado local; bloquear categoria ou pacote pelo console; salvar; sincronizar; procurar e tentar abrir o app; remover bloqueio; sincronizar; abrir app e verificar dado; abrir Configuracoes > DNS privado.

**Constatacoes:** app bloqueado some ou fica inacessivel; sync segue funcionando; app liberado volta com dados; app protegido do MDM nunca some; DNS privado nao muda no launcher atual.

## J05B - Aplicativos com launcher novo

**Cobre:** 4.3, 6.2, 6.3, 7.2 e 9.2.

**Passos:** no tablet com launcher novo, criar/verificar dado no app alvo; bloquear `social_media` com app catalogado instalado; colocar outro app da categoria na allowlist; colocar app sem categoria na blocklist; salvar; sincronizar; procurar e tentar abrir cada app; remover app sem categoria da blocklist; sincronizar; abrir app liberado e verificar dado.

**Constatacoes:** app da categoria bloqueada some ou fica inacessivel; app da allowlist fica acessivel; app da blocklist some ou fica inacessivel; app liberado volta com dados; pacote nao instalado na blocklist nao quebra os demais; app protegido do MDM e app principal de quiosque nunca somem.

## J06 - Categorizar aplicativo do cliente

**Cobre:** 3.3 e 8.3.

**Passos:** abrir Web Filter > Aplicativos; adicionar `br.com.empresa.jogo` em `games`; salvar; abrir politica que bloqueia `games`; sincronizar tablet com esse app; remover o pacote do catalogo do cliente; sincronizar novamente.

**Constatacoes:** pacote aparece como item do cliente; perfil do mesmo cliente bloqueia o pacote; outro cliente nao herda; apos remover, pacote deixa de ser bloqueado por categoria; pacote do catalogo inicial nao oferece remover; pacote protegido gera erro visivel.

## J07 - Launcher aplica DNS privado sozinho

**Cobre:** 7.1 e 7.2.

**Passos:** publicar launcher novo em homologacao pelo procedimento autorizado; confirmar versao antes/depois no tablet; ativar politica com dominio DNS; sincronizar sem abrir configuracoes manualmente; abrir Configuracoes > DNS privado; navegar para dominio bloqueado e permitido; desativar politica; sincronizar e voltar as configuracoes.

**Constatacoes:** tablet atualiza sem rematricula; DNS privado muda para hostname do perfil sem acao manual; configuracao fica bloqueada; dominio bloqueado falha e permitido abre; ao desativar, DNS volta ao automatico quando permitido.

## J08 - Padrao visual do console

**Cobre:** 8.4.

**Passos:** abrir Web Filter no tema claro em 1280, 768 e 360 px; alternar para tema escuro; abrir as tres abas; entrar e sair da edicao de perfil; trocar idioma para portugues.

**Constatacoes:** nao ha scroll horizontal; botoes principais sao alcancaveis; contraste e legivel; voltar retorna a lista do Web Filter; titulo bate com menu; textos e erros aparecem em portugues.

## J10 - Desativacao

**Cobre:** 9.4.

**Passos:** com filtro ativo, confirmar app e site bloqueados; desativar politica no console; sincronizar tablet; abrir app antes bloqueado; acessar site antes bloqueado; abrir Configuracoes > DNS privado.

**Constatacoes:** app volta com dados; site volta a carregar; DNS privado volta ao automatico quando o perfil permite; restricao some quando nao esta definida pelo perfil; MDM continua sincronizando.

## J11 - Resolvedor fora do ar

**Cobre:** 9.5.

**Passos:** com filtro ativo, confirmar `wikipedia.org` abrindo; parar `webfilter-dns` em homologacao; tentar abrir `wikipedia.org`; confirmar app bloqueado continua bloqueado; reiniciar container; sem tocar no tablet, repetir `wikipedia.org`.

**Constatacoes:** com resolvedor parado, paginas deixam de abrir por falha de resolucao; comportamento e compreensivel e documentado; apps bloqueados continuam bloqueados; ao voltar o resolvedor, navegacao permitida retorna sem acao no tablet.

## J12 - Ensaio de publicacao e rollback

**Cobre:** 10.1 a 10.4, sem agentes publicarem em producao.

**Passos:** restaurar homologacao com copia autorizada do banco de producao; seguir `docs/WEBFILTER.md`; fazer backup; publicar WAR em homologacao; subir resolvedor; publicar launcher novo em homologacao; ativar perfil piloto; executar J04, J05B, J10 e J11; confirmar perfis nao piloto sem alteracao; executar rollback funcional e tecnico.

**Constatacoes:** runbook e suficiente; backup existe e e legivel; Liquibase aplica plugin; piloto funciona; nao piloto nao muda; rollback funcional libera tablet; rollback tecnico remove somente WebFilter; producao permanece intocada por agentes.

## J13 - Assinatura humana de jornada com FQ sub-agente

**Cobre:** qualquer jornada de console/tablet auditada por FQ sub-agente do FO.

**Passos:** abrir manifesto de evidencias; assistir ao video/trace principal; conferir ambiente, tablet, conta e horario; registrar assinatura em `fiscal/pareceres/<jornada>-assinatura-humana.md`.

**Constatacoes:** assinatura identifica pessoa, data, jornada e hash do manifesto; a pessoa declara ter visto a evidencia principal; sem assinatura, FQ sub-agente nao pode emitir `ACEITA` para a jornada.
