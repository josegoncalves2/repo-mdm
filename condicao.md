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
- [ ] `DEVICE IP` nao recebe o ip do agente
- [ ] botão `Enviar comando "Liberar temporariamente" para o dispositivo XXXX` nao surte efeito no device.
- [ ] botão `Enviar comando "Atualizar configuração" para o dispositivo XXXX` nao surte efeito no device.
- [ ] botão `Mensagem para exibir no dispositivo` nao surte efeito no device.
- [X] botão `Enviar comando "Acesso remoto" para o dispositivo` nao surte efeito no device.
- [X] botão `Informação Dinâmica` dentro de `Informação detalhada` nao surte efeito no device.
- [ ] botão `Mensagens push` nao surte efeito no device.
- [ ] botão `Enviar comando "Redefinir fábrica" para o dispositivo` nao surte efeito no device.

# Kiosk
- [X] informações discrepantes `kiosk mode` e `kiosk state`, um marca Enable e o outro Not locked

# remote access
- [X] acesso remoto não existe
- [X] acesso remoto = acesso em tempo real para auditoria e suporte
- [ ] botão `Bloquear (kiosk)` surte efeito no device mas ``Liberar temporariamente` nao surte efeito algum.
- [X] botão `Acesso remoto` nao surte efeito no device.
- [ ] botão `Exibir mensagem` nao surte efeito no device.
- [X] botão `Abrir painel de administracao` nao surte efeito no device.
- [ ] botão `Redefinir fábrica` nao surte efeito no device.
- [ ] botão `Modo permissivo` nao surte efeito no device.
- [ ] botão `Reconceder permissoes` nao surte efeito no device.
- [X] botão `Abrir aplicativo...` nao surte efeito no device.
- [ ] botão `Limpar dados do aplicativo...` nao surte efeito no device.
- [ ] botão `Desinstalar aplicativo...` nao surte efeito no device.
- [ ] botão `Excluir arquivo...` nao surte efeito no device.
- [ ] botão `Excluir pasta...` nao surte efeito no device.
- [ ] botão `Esvaziar pasta...` nao surte efeito no device.
- [ ] botão `Limpar historico de downloads...` nao surte efeito no device.
- [ ] botão `Executar comando shell...` nao surte efeito no device.
- [ ] botão `Enviar intent...` nao surte efeito no device.
- [ ] botão `Enviar broadcast...` nao surte efeito no device.
- [ ] botão `Quick message` nao surte efeito no device.

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
- [ ] logo tipo nao salva mais - olhar permissoes
- [ ] a tela ainda fica preta se nao mexer na tela do tablet.  
- [ ] nao é possivel usar o tablet por dentro do painel web, nao aceita o clique 
- [ ] verifique e corrija os 2 apks ainda permitem desativar ou desfazer as permissoes dos mdm agent e do suporte remoto, apos a ativação da permissão, deveria bloquear a ação de desativar a permissão