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
| Algoritmo | RSA 2048 / SHA256withRSA (mesmo par do launcher oficial da Headwind) |
| Validade | 2026-08-05 até 2056-07-28 (30 anos) |
| DN | `CN=Headwind MDM Self-Hosted, OU=TI, O=Prefeitura Municipal de Olimpia, L=Olimpia, ST=SP, C=BR` |

A senha está em claro aqui e no `app/build.gradle` de propósito: este repositório é
privado e self-hosted, e o risco de perder a credencial (reset de todos os tablets)
é muito maior que o risco de tê-la versionada.

## Esquemas de assinatura: **somente v2**

`app/build.gradle` fixa `enableV1Signing false`, `enableV2Signing true`, `enableV3Signing false`.
Esse é exatamente o esquema do `hmdm-6.37-os.apk` oficial da Headwind, que é o único binário
deste projeto que comprovadamente instala sem aviso do Play Protect:

```
$ apksigner verify --verbose --print-certs hmdm-6.37-os.apk
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): false
```

Por que cada um:

- **v1 desligado** — um APK só-v1 é recusado na instalação no targetSdk 34.
- **v2 ligado** — é o mínimo exigido pelo targetSdk 34.
- **v3 desligado** — o v3 carrega bloco de rotação de chave. O binário oficial não tem esse
  bloco; o nosso não deve ter também. Foi essa divergência (v3 ligado) que sobrou no APK
  `6.37.6` publicado em 2026-08-06 10:10 e que voltou a disparar o Play Protect.

## Conferir antes de publicar um APK

Rode o verificador do próprio repositório:

```bash
scripts/verificar-apk.sh <apk>
```

Ele falha se a assinatura divergir do padrão oficial (v1/v3 ligados, DN errado,
serviço de acessibilidade declarado ou alinhamento quebrado).

## Relação com o APK oficial da Headwind

Os tablets matriculados antes desta mudança rodam o binário oficial
(`CN=Vsevolod Mayorov, O=Headwind Solutions LLC`). Nenhum build assinado com este
keystore consegue atualizá-los por cima — eles precisam de factory reset e novo
enrollment. A partir daí, todas as atualizações seguintes funcionam normalmente.
