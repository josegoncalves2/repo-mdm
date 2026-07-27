# Release keystore — NÃO SUBSTITUIR

`hwmdm-release.jks` é a identidade de assinatura permanente do APK `com.hmdm.launcher`
deste servidor.

## Por que ele não pode mudar

O Android recusa atualizar um app quando o certificado de assinatura muda. Não existe
flag, `versionCode` ou opção de MDM que contorne isso — é garantia da plataforma.

Trocar este keystore significa: **factory reset + re-enrollment em todos os tablets já
matriculados.** Perder este arquivo tem exatamente a mesma consequência, e é irreversível.

Faça backup dele fora desta máquina.

## Dados

| | |
|---|---|
| Arquivo | `android-source/keystore/hwmdm-release.jks` |
| Alias | `hwmdm` |
| Senha (store e key) | `HwMdm!Release2026` |
| Algoritmo | RSA 4096 / SHA384withRSA |
| Validade | 10950 dias (30 anos, a partir de 2026-07-26) |
| DN | `CN=Headwind MDM Self-Hosted, OU=TI, O=Puzzlepunker, L=Olimpia, ST=SP, C=BR` |

A senha está em claro aqui e no `app/build.gradle` de propósito: este repositório é
privado e self-hosted, e o risco de perder a credencial (reset de todos os tablets)
é muito maior que o risco de tê-la versionada.

## Esquemas de assinatura

`app/build.gradle` habilita **v1 + v2 + v3**. Isso não é opcional:

```
ERROR: Target SDK version 34 requires a minimum of signature scheme v2;
       the APK is not signed with this or a later signature scheme
```

Um APK assinado só com v1 é recusado na instalação pelo Android 14 e sinalizado pelo
Google Play Protect. Foi essa a causa do erro de Play Protect em 2026-07-26.

## Conferir antes de publicar um APK

```bash
.android-sdk/build-tools/35.0.0/apksigner verify --print-certs <apk>
```

Tem de imprimir o DN acima e **não** pode dizer `DOES NOT VERIFY`.

## Relação com o APK oficial da Headwind

Os tablets matriculados antes desta mudança rodam o binário oficial
(`CN=Vsevolod Mayorov, O=Headwind Solutions LLC`). Nenhum build assinado com este
keystore consegue atualizá-los por cima — eles precisam de factory reset e novo
enrollment. A partir daí, todas as atualizações seguintes funcionam normalmente.
