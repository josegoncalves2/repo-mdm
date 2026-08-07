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

Duas causas independentes:

1. `proxy.addresses` vazio no `ROOT.xml`. O `192.168.1.254` e' um **MikroTik HttpProxy**, e
   sem declara-lo o servidor grava o endereco de quem entregou a conexao — o proxy — para
   todo aparelho. O `provision.sh` ja avisa disso (linha 74) e aceita `--proxy`.
2. O `info` que o 6.36 envia **nao tem campo de IP nenhum** (`model`, `imei`, `serial`,
   `androidVersion`, `kioskMode`, ...). A aba de acesso remoto procura
   `deviceIp`/`ip`/`localIp` e por isso mostra "nao informado" — o launcher nunca informou.

O item 1 e' configuracao. O item 2 exige o app companheiro reportar o proprio IP.

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
