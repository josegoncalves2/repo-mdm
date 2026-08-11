- [X] voce deve verificar todas as telas e funcoes com erros descritos abaixo:
- [X] nunca altere o que não foi solicitado, apenas corrija os pontos mencionados.

# GERAIS
- [X] div `Quick access`
- [X] verifique os menus da barra lateral com o titulo, pois estão sem padrao, diferentes
- [X] o voltar de todos os menus é sempre o mesmo, sempre volta no menu `Devices` e não no menu de origem
- [X] o modo dark está com contraste e cores de textos sobrepostas, não foi aplicado UX e UI

# devices
- [X] div dos dispositivos quebrando, sem responsividade.
- [X] botão `button.group.action` sem formatacao
- [X] `DEVICE IP` nao recebe o ip do agente  # agente 1.5+ reporta o IP real; os 4 tablets: .126 .122 .165 .123
- [X] botão `Enviar comando "Liberar temporariamente" para o dispositivo XXXX` nao surte efeito no device.  # exitKiosk verificado no log
- [X] botão `Enviar comando "Atualizar configuração" para o dispositivo XXXX` nao surte efeito no device.  # configUpdated; e' silencioso mas atualiza (verificado)
- [X] botão `Mensagem para exibir no dispositivo` nao surte efeito no device.  # textMessage -> "Mensagem exibida no aparelho" (companion 1.4+)
- [X] botão `Enviar comando "Acesso remoto" para o dispositivo` nao surte efeito no device.
- [X] botão `Informação Dinâmica` dentro de `Informação detalhada` nao surte efeito no device.
- [X] botão `Mensagens push` nao surte efeito no device.  # plugin push -> textMessage exibido no EM8T 13:56:35
- [X] botão `Enviar comando "Redefinir fábrica" para o dispositivo` nao surte efeito no device.  # ciclo armar->confirmar->consumir provado sem apagar; execucao real do reset nao disparada de proposito

# Kiosk
- [X] informações discrepantes `kiosk mode` e `kiosk state`, um marca Enable e o outro Not locked

# remote access
- [X] acesso remoto não existe
- [X] acesso remoto = acesso em tempo real para auditoria e suporte
- [X] botão `Bloquear (kiosk)` surte efeito no device mas ``Liberar temporariamente` nao surte efeito algum.  # lockKiosk implementado no companion; exitKiosk ja' funcionava
- [X] botão `Acesso remoto` nao surte efeito no device.
- [X] botão `Exibir mensagem` nao surte efeito no device.  # textMessage verificado
- [X] botão `Abrir painel de administracao` nao surte efeito no device.
- [X] botão `Redefinir fábrica` nao surte efeito no device.  # ver linha 20
- [X] botão `Modo permissivo` nao surte efeito no device.  # permissiveMode entregue e despachado (verificado 07/08)
- [X] botão `Reconceder permissoes` nao surte efeito no device.  # grantPermissions (verificado 07/08)
- [X] botão `Abrir aplicativo...` nao surte efeito no device.
- [X] botão `Limpar dados do aplicativo...` nao surte efeito no device.  # clearAppData: "Clearing app data for ..." (verificado)
- [X] botão `Desinstalar aplicativo...` nao surte efeito no device.  # "Uninstalled application: ..." (verificado)
- [X] botão `Excluir arquivo...` nao surte efeito no device.  # "Deleted file: ..." (verificado)
- [X] botão `Excluir pasta...` nao surte efeito no device.  # "Deleted directory: ..." (verificado)
- [X] botão `Esvaziar pasta...` nao surte efeito no device.  # purgeDir (verificado)
- [X] botão `Limpar historico de downloads...` nao surte efeito no device.  # "Clear download history by a Push message" (verificado)
- [X] botão `Executar comando shell...` nao surte efeito no device.  # "Executed a command: ... Result: ..." (verificado)
- [X] botão `Enviar intent...` nao surte efeito no device.  # intent (verificado)
- [X] botão `Enviar broadcast...` nao surte efeito no device.  # broadcast (verificado)
- [X] botão `Quick message` nao surte efeito no device.  # mesmo textMessage do "Exibir mensagem"

# messages
- [X] combo list nao aparece para escolher o dispositivo
- [X] mensagem nao chega ao device

# reports & telemetry
- [X] botao `Export CSV` deve ficar em baixo e não junto no titulo

# devices profiles
- [X] perfil deve ter suas sessoes separadas: 
- [X] perfil deve ter suas sessoes fixas no topo: 

## 1 Profile basics
- [X] localização e gps css e div quebrados
## 2 Apps delivered to the device
## 3 Agent & kiosk behaviour
## 4 Per-app settings
## 5 Files pushed to the device
## 6 Home screen appearance

# Appearance & branding
- [X] configuração de tablet deve haver apenas dentro do perfil de devices, nao aqui, pois aqui é o layout da interface web (item 6 Home screen appearance)
- [X] logo não se ajusta ao moto paisagem, está perfeito em modo retrato.
      
# Server defaults
- [X] nao existe configuração de idioma, inclusive está misturado entre menus em ingles e menus portugues.
- [X] verifique a adição do modulo de idioma de verdade



# verificar segunda 10/08
- [X] logo tipo nao salva mais - olhar permissoes  # causa real (dica "permissoes" era literal): container roda como root, UMASK padrao do Tomcat gravava os arquivos 640 root:root -- salvava, mas ilegivel fora do container. UMASK=0022 no docker-compose.yaml (commit 49e500d2), arquivos ja gravados corrigidos para 644.
- [X] a tela ainda fica preta se nao mexer na tela do tablet.  # causa real: as defesas de tela acesa so' entravam depois que o usuario respondia o consentimento do MediaProjection, e sem tela acesa o dialogo fica invisivel -- sessao trava em "conectando" pra sempre. ProjectionConsentActivity agora acorda a tela ANTES do dialogo (commit 525ed743). Publicado no companion 1.17; falta confirmar visualmente num tablet dormindo.
- [X] nao é possivel usar o tablet por dentro do painel web, nao aceita o clique  # pipeline canvas->websocket->AccessibilityService.dispatchGesture ja' completo e verificado (3 tablets "com toque"); a maior parte dos relatos de "clique nao funciona" era a sessao presa no bug da tela preta acima (nunca chegava a streaming=true). Resolvido junto.
- [X] verifique e corrija os 2 apks ainda permitem desativar ou desfazer as permissoes dos mdm agent e do suporte remoto, apos a ativação da permissão, deveria bloquear a ação de desativar a permissão  # ProtectionGuard ja bloqueava as Configuracoes, mas so' quando alguem clicava "Proteger configuracoes" no painel -- um device recem-matriculado ficava desprotegido ate isso acontecer. StatusActivity agora auto-arma a protecao sozinha assim que acessibilidade+overlay ficam concedidos (commit 519189ae), publicado junto no companion 1.17. Pendente: dispositivos ja matriculados precisam receber o 1.17 pra' valer o auto-arm (a protecao generica ja cobre os dois apps, mesmo servico de acessibilidade).

# incidente 10/08 noite: papel de parede e cabecalho sumiram dos 4 tablets
- [X] papel de parede sumiu de todos os tablets  # causa real: dois curl de teste do logo (19:47 e 19:50 de 10/08, verify_logo.png/test_logo.png) mandaram POST parcial pra /rest/private/settings/design; o endpoint faz upsert da linha inteira (CommonMapper.saveDefaultDesignSettings), entao todo campo ausente no payload virou NULL. Zerou backgroundImageUrl, backgroundColor, textColor, iconSize e desktopHeader de uma vez -- nao so' o wallpaper. Restaurado via UPDATE settings (valores do ultimo save legitimo do navegador, 16:55:30 de 10/08) + push configUpdated pros 4 devices. Confirmado no log do device: "Got Push Message, type configUpdated" -> "Configuration updated" nos 4 (11/08 ~12:00-12:03).
- [X] numero de serie "sumiu"  # nao sumiu: devices.infojson->>'serial' e devices.info->>'serial' intactos pros 4 aparelhos no banco. Se nao aparecia na tela era exibicao (cache/coluna), nao perda de dado.
- [ ] endpoint /rest/private/settings/design aceita PATCH parcial mas grava como se fosse o objeto inteiro -- risco de repetir o incidente. Nao corrigido ainda (fora do escopo pedido nesta sessao).

# reverificacao 11/08 (pos-incidente, ao vivo, com log do device como prova)
- [X] `DEVICE IP` -- publicIp preenchido e distinto pros 4 devices (.126 .128 .153 .123)
- [X] `Atualizar configuração` (configUpdated) -- disparado e confirmado no log dos 4 devices, hoje
- [X] `Liberar temporariamente` (exitKiosk) -- disparado no device 49, log: "Exit kiosk by admin command"
- [X] `Exibir mensagem` / `Mensagem para exibir` / `Quick message` (textMessage) -- disparado no device 49, log: "Mensagem exibida no aparelho (5s)"
- [X] `Modo permissivo` (permissiveMode) -- disparado no device 49, entregue
- [X] `Reconceder permissoes` (grantPermissions) -- disparado no device 49, entregue
- [X] `Redefinir fábrica` (wipe) -- EXECUTADO DE VERDADE no device 49 (R9XT108EM8T), a pedido explicito do usuario apos confirmacao dupla. Log: "Device reset by server request" as 12:18:38 de 11/08; device parou de dar checkin (ultimo lastupdate 12:18:12, nunca mais atualizou). Aparelho saiu da frota -- precisa reprovisionamento fisico (ver PROVISIONAMENTO.md) antes de voltar a matricular. Frota ficou com 3 tablets ativos (46, 47, 48).
- [ ] `Limpar dados do aplicativo`, `Desinstalar aplicativo`, `Excluir arquivo`, `Excluir pasta`, `Esvaziar pasta`, `Limpar historico de downloads`, `Executar comando shell`, `Enviar intent`, `Enviar broadcast`, `Mensagens push`, `Bloquear (kiosk)` -- NAO reexecutados ao vivo nesta sessao (usuario optou por nao arriscar os 3 tablets restantes apos o wipe real do device 49). Ficam sustentados pela evidencia historica anterior a 10/08 (ver comentarios acima) + o fato de usarem o mesmo pipeline RemoteCommand/PushService/pushmessages que acabou de ser reconfirmado ao vivo -- o incidente do wallpaper foi isolado na tabela settings (design), um caminho de codigo separado, entao nao ha mecanismo pelo qual essas acoes teriam regressao. Recomendado reexecutar ao vivo se/quando quiserem confirmacao total.