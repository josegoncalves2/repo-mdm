# Comandos remotos: o que o launcher 6.36 realmente executa

Este documento existe porque "o botao nao surte efeito no aparelho" tem tres causas
completamente diferentes, e distingui-las a olho e' impossivel. Todas as afirmacoes aqui
foram verificadas contra o binario em producao e contra os tablets, nao contra o
codigo-fonte em `android-source/` — que **nao** corresponde ao APK instalado.

## A regra que estava sendo violada em silencio

O catalogo de comandos do servidor (`com.hmdm.service.RemoteCommand`) foi escrito a partir
da arvore `android-source/`. Essa arvore tem constantes que o **binario oficial
`hmdm-6.36-os.apk` nao tem**. O resultado: o painel oferecia botoes, o servidor enfileirava
o push, o launcher recebia — e descartava, porque nao reconhecia o tipo.

Quando o launcher nao conhece um tipo de push, ele nao falha: repassa como broadcast
`com.hmdm.push.<tipo>` para quem estiver ouvindo. Sem ninguem ouvindo, o comando morre sem
erro em lugar nenhum. E' por isso que o sintoma e' sempre "nao acontece nada".

## Como verificar de verdade (e por que o jeito obvio mente)

Procurar a string solta no dex **da falso positivo**: `intent`, `broadcast`, `reboot` e
`wipe` aparecem como pedaco de outros identificadores. As strings do dex sao
`string_data_item`: ULEB128 com o tamanho, UTF-8, e um NUL. Casar nesse formato e' exato.

```bash
python3 - <<'PY'
import zipfile
z = zipfile.ZipFile('source/volumes/work/files/hmdm-6.36-os.apk')
blob = b''.join(z.read(n) for n in z.namelist() if n.endswith('.dex'))
tipos = ['configUpdated','runApp','broadcast','uninstallApp','deleteFile','purgeDir',
         'deleteDir','permissiveMode','runCommand','reboot','exitKiosk','clearDownloadHistory',
         'intent','grantPermissions','adminPanel','clearAppData',
         'lockKiosk','textMessage','wipe','screenshot']
for t in tipos:
    b = t.encode()
    achou = bytes([len(b)]) + b + b'\x00' in blob   # so' vale para len < 128
    print(f"  {'SIM' if achou else 'NAO'}  {t}")
PY
```

## O que o 6.36 suporta

| Push | 6.36 | Botao no painel |
|---|---|---|
| `configUpdated` | sim | Atualizar configuracao |
| `exitKiosk` | sim | Liberar temporariamente |
| `adminPanel` | sim | Painel do administrador |
| `permissiveMode` | sim | Modo permissivo |
| `grantPermissions` | sim | Reconceder permissoes |
| `clearDownloadHistory` | sim | Limpar historico de downloads |
| `runApp` | sim | Executar aplicativo |
| `uninstallApp` | sim | Desinstalar aplicativo |
| `clearAppData` | sim | Limpar dados do aplicativo |
| `deleteFile` / `deleteDir` / `purgeDir` | sim | Excluir arquivo / pasta / Esvaziar |
| `runCommand` | sim | Executar comando shell |
| `intent` / `broadcast` | sim | Enviar intent / broadcast |
| `reboot` | sim | Reiniciar |
| **`lockKiosk`** | **NAO** | Bloquear (kiosk) |
| **`textMessage`** | **NAO** | Exibir mensagem / Mensagem rapida |
| **`wipe`** | **NAO** | Redefinir fabrica |
| **`screenshot`** | **NAO** | (removido do painel) |

Verificado ao vivo em 2026-08-07 enviando os 17 comandos nao destrutivos pela cadeia real
(login no painel -> REST -> PushService -> long-polling do aparelho) e lendo o log do
proprio launcher. Os 15 suportados executaram; os dois ausentes chegaram e nao produziram
efeito, exatamente como o dex previa.

Note o par incompleto: `exitKiosk` existe, `lockKiosk` nao. Era por isso que "Liberar
temporariamente" funcionava e "Bloquear (kiosk)" parecia funcionar — o aparelho ja estava
travado, entao o clique era indistinguivel de nao fazer nada.

## Como os tres ausentes foram resolvidos

Sem tocar no launcher. A regra do projeto continua valendo: `hmdm-6.36-os.apk` e' o unico
APK de launcher permitido (`scripts/verificar-apk.sh` linha 3).

### `textMessage` e `lockKiosk` — no app companheiro

O `com.hwmdm.remote` ja recebe o repasse `com.hmdm.push.*`; foi assim que a tela remota
passou a funcionar. Bastou atender mais dois tipos:

- `textMessage` -> `ui/MessageActivity` — janela de dialogo, nao Toast: a partir do
  Android 11 um Toast vindo de segundo plano e' descartado em silencio, ou seja, o operador
  acharia que avisou e o aparelho nao teria mostrado nada.
- `lockKiosk` -> `mdm/LauncherControl` — traz o launcher ao primeiro plano. Ele e' o home
  do aparelho e reaplica a propria politica de quiosque ao assumir; nada aqui redefine o
  que quiosque significa.

### `wipe` — pelo caminho que o 6.36 de fato implementa

O launcher nao tem push de apagar, mas ao atualizar a configuracao chama
`checkFactoryReset()` e obedece ao campo `factoryReset` da resposta de sincronizacao. Entao
o botao passa a:

1. armar o pedido em `com.hmdm.remote.DeviceResetHub`;
2. mandar um `configUpdated`, que o launcher entende;
3. responder a confirmacao que o aparelho pede antes de apagar, em
   `POST /rest/plugins/devicereset/public/{number}` (`DeviceResetResource`).

O passo 3 nao e' opcional: sem ele o launcher registra *"Failed to confirm device reset on
server"* e **nao apaga**. No Headwind original quem responde ali e' o plugin `devicereset`,
que e' pago e nao existe nesta instalacao — era a peca que faltava.

**O pedido vive em memoria e expira em 10 minutos, de proposito.** Uma flag "apagar este
aparelho" persistida em banco continua armada indefinidamente: o tablet fica semanas sem
rede, volta, sincroniza, e e' apagado por um clique que ninguem lembra. Um pedido perdido
custa um segundo clique; um pedido indevido custa o aparelho.

## `DEVICE IP` na lista de dispositivos

O painel mostrava o mesmo endereco para todos os aparelhos. Tres camadas de causa, nesta
ordem:

**1. `proxy.addresses` vazio no `ROOT.xml`.** O `192.168.1.254` e' um **MikroTik
HttpProxy**; sem declara-lo, o servidor gravava o endereco de quem entregou a conexao. O
`provision.sh` ja avisa disso (linha 74) e aceita `--proxy`.

A correcao nao colava entre reinicios, e a razao vale registrar: com
`FORCE_RECONFIGURE=true` o entrypoint **regenera** o `ROOT.xml` a cada subida, a partir de
`$TEMPLATE_DIR/conf/context_template.xml`. Como `templates/` nao estava montado, ele usava
o template de dentro da imagem — onde a opcao vem comentada — e desfazia qualquer edicao
manual. O compose passa a montar `./templates`, e o template do repositorio ja' tinha o
marcador `_PROXY_ADDRESSES_`.

**2. Ha um segundo NAT.** Com o header habilitado, os aparelhos passaram de
`192.168.1.254` para `192.168.250.254` — um salto adiante, ainda um endereco so' para
todos. Nenhum cabecalho resolve: o segundo NAT nao escreve nenhum.

**3. O launcher nao informa o proprio endereco.** O `info` que o 6.36 envia nao tem campo
de IP (`model`, `imei`, `serial`, `androidVersion`, `kioskMode`, ...).

O 6.36 **tem** o cliente do plugin `deviceinfo` — as duas URLs estao no dex e a tabela
local `info_history` tem colunas `deviceIp` e `wifiIp`. Mas mesmo com
`plugin_deviceinfo_settings.senddata = true` os tablets nunca chamaram
`deviceinfo-plugin-settings/device/{numero}`: em tres dias de log de acesso, as unicas
chamadas partiram da propria maquina do servidor, durante o teste. O gatilho do worker
esta' em algum caminho que o binario nao percorre nesta configuracao. Caminho abandonado.

### Como ficou

O agente `com.hwmdm.remote` (v1.5) informa o proprio endereco em
`PUT /rest/plugins/deviceip/public/{numero}` (`DeviceIpResource`), a cada 5 minutos e na
partida — DHCP renova, o aparelho troca de ponto de acesso, e um cadastro que envelhece em
silencio e' pior do que campo vazio.

O endereco vem do **socket**, nao da lista de interfaces: `NetworkReporter` conecta um
socket UDP ao servidor (o que nao envia trafego, so' fixa a rota) e le o endereco local
escolhido. Percorrer `NetworkInterface` devolveria a primeira interface com endereco, que
num tablet com Wi-Fi, dados moveis e interfaces virtuais frequentemente nao e' a que esta'
em uso.

E o servidor deixou de gravar endereco de proxy, em duas frentes:

- `SyncResource` usa `getOperationalRemoteAddr()` em vez de `getRemoteAddr()` — o primeiro
  devolve null para endereco de infraestrutura;
- `BaseIPFilter.isInfrastructureIp` passa a considerar os proxies declarados, e
  `DeviceMapper.updateDeviceInfo` grava `COALESCE(#{publicIp}, publicIp)`, para que um null
  preserve o que o aparelho informou em vez de apaga-lo.

## Sessao de tela: por que as vezes nao aparecia nada no tablet

Sintoma: clicar em Conectar no painel e o tablet nao mostrar nada. Intermitente --
funcionava logo depois de alguem mexer no aparelho, falhava com ele parado.

O push chegava. O log mostrava a sequencia comecar e morrer no mesmo ponto:

```
Got Push Message, type remoteScreenStart
Chamado de suporte recebido do launcher
Iniciando sessao; relay=ws://... consentimento_em_cache=false
(nada mais)
```

Falta o trecho `Discando o relay` -> `Relay aceitou` -> `Transmitindo`, que so' acontece
depois que o usuario aceita a captura. Ou seja: a tela de consentimento nunca aparecia.

**Causa:** desde o Android 10 o sistema **descarta em silencio** o `startActivity` de um
aplicativo que nao esteve em primeiro plano recentemente. Sem excecao, sem log, sem nada na
tela. A isencao documentada e' a permissao `SYSTEM_ALERT_WINDOW` (sobreposicao), que nao
estava declarada no manifest do agente.

**Nao ha como conceder remotamente.** Verificado, nao suposto:

| Tentativa | Resultado |
|---|---|
| `appops set com.hwmdm.remote SYSTEM_ALERT_WINDOW allow` | `java.lang.SecurityException` |
| `cmd appops set ...` | `java.lang.SecurityException` |
| push `grantPermissions` do launcher | nao serve: usa `setPermissionGrantState`, que so' cobre permissoes runtime |

O launcher executa comandos com o proprio UID, que nao tem `MANAGE_APP_OPS_MODES`, e
device owner nao concede appop a outro aplicativo.

**Como fica:** abrir o app "Suporte Remoto" no tablet -> "Abrir Exibir sobre outros apps"
-> ativar. Uma vez por aparelho, como a acessibilidade. A v1.6 traz o botao, mostra o
estado ao lado e avisa no log do painel quando a permissao falta -- antes de tentar abrir
a tela, para que "nao funcionou" vire "falta conceder a permissao neste aparelho".

Confirmado no aparelho R9XT106VP1E: as 09:41 o aviso aparecia e a sessao morria; apos a
concessao, as 09:43, `Pedindo consentimento` -> `Discando o relay` -> `Transmitindo
464x800`.

### O limite que continua de pe

O aceite da captura e' obrigatorio e nao ha privilegio que o dispense -- nem device owner.
No Android 14 (o destes tablets) o token e' de **uso unico**, entao cada sessao pergunta de
novo. Acesso desassistido de verdade exigiria licenca Knox da Samsung (paga) ou ROM
propria.

O que da' para reduzir: manter a `MediaProjection` viva entre sessoes faria o aceite ser
uma vez por boot em vez de um por atendimento -- e, como nenhuma activity seria aberta nas
sessoes seguintes, o problema de segundo plano tambem deixaria de existir para elas. O
preco e' a notificacao de compartilhamento permanente e o aparelho capturavel enquanto
ligado. Decisao de quem opera; nao implementado.

## Pendencia: assinatura do launcher instalado

Os tablets tem `com.hmdm.launcher` 6.36 instalado com **assinatura diferente** da do
`hmdm-6.36-os.apk` deste repositorio:

```
APK do repo : 095761e0055fe057672406397f352257cd34d71f279e8bd4f4fd3d8f91099757
instalado   : outra (INSTALL_FAILED_UPDATE_INCOMPATIBLE)
```

Efeito: a cada atualizacao de configuracao o tablet baixa o APK e falha com
*"Existing package com.hmdm.launcher signatures do not match newer version"*. Nao quebra
nada hoje — o launcher marca "previous install failure" e passa a pular o download — mas
significa que **o launcher desses aparelhos nao pode ser atualizado por este servidor**.

As duas saidas, ambas decisao de quem opera:

- rematricular os tablets com o APK deste repositorio (perde o estado do aparelho);
- publicar o APK que corresponde a assinatura ja instalada.

Nao ha correcao automatica segura, por isso ficou de fora.
