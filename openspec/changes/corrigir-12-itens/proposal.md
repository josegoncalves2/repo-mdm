# corrigir-12-itens — reclamação do responsável em 2026-09-23

Texto do pedido, copiado literalmente (não resumir, não reinterpretar):

1) webfilter nao funciona, ativei o perfil do kiosk, ativei tudo, coloquei dns, testei no tablet, acessou tudo, facebook, instagram, porno, tudo. nao funciona entao, é só placeholder. Corrija para ser real.
2) o acesso remoto está quase bom, mas teclado nao reconhece o que é digitado.
3) acesso remoto se perde se o dispositivo nao estiver conectado, as requisições se perdem no caminho.
4) qualquer F5 OU mudar de item na tela o acesso remoto é perdido. corrija.
5) nao há tratativa pra tela travada do tablet, se estiver bloqueada o acesso remoto nao acontece e se o usuario desligar a tela do tablet o aceso remoto é encerrado. COrrigir, nao pode, acesso remoto é livre, é soberano.
6) eu sinto que os botoes da interface não estão padronizados, não estao refatorados, cada um integrante do seu modulo, nao, está uma gambiarra, mostrando botoes de um jeito, outros botoes parece que só chama depois que carrega a pagina, está um remeendo.
7) tudo deve ser tratado como modular, se eu quiser desativar 1 item da plataforma para dar manutenção eu sou obrigado a parar o sofware todo, pq está tudo amarrado nas coxas, porco demais.
8) nao existe logica nos menus, há uma salada mista de opções.
9) o menu Plugins é um completo exemplo de salada de frutas, bonito mas sem proposito, sem logica de uso nenhum na interface, um monte de opção que se repete, que nao faz nada, ou que é inutil, irrelevante. O que é More plugins? é um local onde eu posso adicionar um modulo que eu construi? NAO! ´´E LIXO, FUNÇÃO??? DESCONHECIDA, SEM PROPOSITO... é só 1 dos infinitos exemplos. Corrija.
10) nao existe nenhum controle de versao, build, compilação, é simplesmente impossivel trabalhar num projeto sem nexo nenhum como este. Corrija.
11) mecanismo de trava de entrega de resultados = ATIVADA.
12) Deve ser registrado com token e não é passivel de ser burlado pelo agente. Obrigatoriamente deve ser registrado e claro, nao basta testes como curl, 200 ok, funcionando, os unicos testes permitidos e que de fato atestam a eficacia do que está sendo entregue é teste real, teste como um usuario sentado na cadeira, clicando e digitando, olhando para a tela e se impressiionando com o resultado, caso contrário é falha, nao tem exceção.

## Causas-raiz já confirmadas no disco (2026-09-23, sessão principal)

- **Item 1 — webfilter é placeholder no aparelho.** O resolvedor DNS do DEV BLOQUEIA de
  verdade (`dig @192.168.1.65 facebook.com` → NXDOMAIN; `google.com` resolve). As políticas
  estão no banco para o perfil 11 (o do tablet). O `WebFilterSyncResponseHook` devolve um campo
  `webfilterDnsHost` no sync. **Ninguém no aparelho consome esse campo**: `grep -ril webfilter
  android-source/` não retorna nada e o launcher oficial `com.hmdm.launcher` não conhece o campo.
  Não existe nenhuma imposição no dispositivo — nem Private DNS forçado, nem VPN local. Por isso
  o tablet abre tudo.
- **Item 2 — teclado.** `InputInjectionService.type()` chama `getRootInActiveWindow()`, mas
  `res/xml/input_injection_config.xml` NÃO declara `canRetrieveWindowContent`. Sem essa
  capacidade o Android devolve `null`, `type()` cai no ramo "nenhum campo em foco" e retorna
  `false` sempre. A digitação nunca chegou a ser tentada.
- **Item 4 — F5 / troca de tela.** `remote.controller.js` não persiste nada: `$scope.remote`
  nasce zerado a cada carga do controlador e `$scope.$on('$destroy')` chama `stopRemote()`, que
  manda `stop` ao servidor. Qualquer F5 ou troca de aba encerra a sessão nas duas pontas.
- **Item 3 — aparelho offline.** `startRemote()` desiste no cliente quando `!device.online`; o
  push de início não é enfileirado para quando o aparelho voltar.
- **Itens 6/7/8/9 — console.** `app/components/main/view/content.html` é uma lista escrita à mão
  de botões com expressões `ng-if` de permissão diferentes entre si; o Web Filter está cravado no
  menu enquanto os demais plugins vivem numa página "EXTENSIONS"; não existe registro de módulos,
  logo não há como desligar um módulo para manutenção.
- **Item 11 — trava.** O TRAVA-KIT está instalado em `repo-mdm/.claude/` e `./trava status` diz
  IMPOSIÇÃO: ATIVA, mas os hooks apontam para `$CLAUDE_PROJECT_DIR/.claude/hooks/…` e a raiz de
  projeto desta sessão é `/opt/projetos/hwmdm`, onde esses hooks NÃO existem. Na prática a trava
  não intercepta esta sessão. Ativar exige editar hooks/settings — proibido ao agente pelo
  Artigo 11 do próprio contrato. **Ação do humano.**
