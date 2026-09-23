# Item 1 — por que o Web Filter não filtra, e qual é o único caminho que filtra de verdade

Levantamento feito na sessão principal em 2026-09-23, com comando, antes de despachar a tarefa D.

## O que JÁ funciona (não refazer)

O resolvedor do DEV bloqueia de verdade. Container `webfilter-dns` (`hwmdm/webfilter-dns:0.34.0`,
Blocky supervisionado por `webfilter-dns/app/resolver.py`), ouvindo em `192.168.1.65:53` e DoT em
`:853`:

```
$ dig +short @192.168.1.65 facebook.com     (vazio — NXDOMAIN)
$ dig +short @192.168.1.65 instagram.com    (vazio — NXDOMAIN)
$ dig +short @192.168.1.65 pornhub.com      (vazio — NXDOMAIN)
$ dig +short @192.168.1.65 google.com       142.251.133.78
```

A política também está no banco, e para o perfil certo — o tablet R9XT200AMYY está na
configuração 11, e existe `plugin_webfilter_policies (id=2, configurationid=11, enabled=t)` com 14
categorias bloqueadas e `facebook.com` na lista de bloqueio explícito.

O servidor calcula tudo isso corretamente e devolve no sync, em
`plugins/webfilter/.../sync/WebFilterSyncResponseHook.java`, o campo `webfilterDnsHost` mais os
`locked_packages`/`unlocked_packages`.

## O que NÃO existe — a causa-raiz

**Nada no aparelho lê `webfilterDnsHost`.**

```
$ grep -ril "webfilter\|web_filter" android-source/
(nenhum resultado)
```

O launcher `com.hmdm.launcher` é o oficial, intocado, e não conhece esse campo. O APK
`com.hwmdm.remote` (nosso) também não. Ou seja: o servidor decide, escreve a decisão na resposta
do sync, e **ninguém do outro lado obedece**. O tablet continua usando o DNS que o Wi-Fi entregar.

Foi por isso que o responsável ativou o perfil, ativou tudo, pôs o DNS e mesmo assim abriu
facebook, instagram e pornografia. Do lado dele estava tudo certo.

## Por que "é só configurar o DNS no tablet" não resolve

Mesmo pondo o DNS do filtro na mão, na conexão Wi-Fi:

1. vale só naquela rede — sai do Wi-Fi, entra no 4G, acabou o filtro;
2. o usuário desfaz em dois toques, e o MDM não fica sabendo;
3. `192.168.1.65` não é alcançável fora da LAN.

Filtro que o usuário desliga não é filtro. O pedido é imposição.

## O caminho que estava disponível e NÃO serve

`DevicePolicyManager.setGlobalPrivateDnsModeSpecifiedHost()` (API 29+) é a imposição ideal:
Private DNS obrigatório, à prova de usuário, válido em qualquer rede. **Exige ser device owner.**

O device owner é o launcher oficial, que não podemos alterar. E a API de plugin que ele expõe ao
nosso APK é só leitura — conferido em `remote-agent/app/src/main/java/com/hwmdm/remote/mdm/MdmLink.java`,
a única chamada é `queryConfig()`, devolvendo `serverHost`, `serverPath`, `deviceId` e `managed`.
Nenhuma delegação de política. Logo, **este caminho está fechado** sem mexer no launcher — e mexer
no launcher obrigaria a rematricular o tablet.

## O caminho que serve: VpnService de DNS no APK do agente

Um `VpnService` no `com.hwmdm.remote`, no modo "só DNS": a interface TUN é levantada com um
servidor DNS próprio e **rota apenas para o endereço desse resolvedor**, não para `0.0.0.0/0`.
Assim só os pacotes de DNS entram no túnel; todo o resto do tráfego segue o caminho normal do
sistema, sem pilha TCP/IP em espaço de usuário e sem penalizar a banda.

Por que resolve o que o DNS da rede não resolve:

- vale em **qualquer** rede — Wi-Fi, 4G, roaming — porque é do aparelho, não da rede;
- o Android dá precedência ao DNS da VPN sobre o Private DNS e sobre o DHCP;
- o consentimento é **uma vez** (diálogo de VPN do sistema); depois fica, e o app reergue a VPN
  no boot;
- funciona sem device owner.

### Decisões de projeto que a tarefa D precisa tomar (e justificar)

1. **Onde mora a lista.** Recomendado: o agente responde localmente, com a política que baixou do
   MDM, em vez de encaminhar tudo para `192.168.1.65`. Encaminhar quebra fora da LAN. Local
   funciona inclusive sem rede. Para consultas não bloqueadas, encaminhe ao DNS que o sistema já
   usava (obtenha-o via `ConnectivityManager`/`LinkProperties`, não cravado).
2. **Como a política chega.** O `webfilterDnsHost` sozinho não basta — é preciso a lista de
   domínios. Ou o plugin do servidor ganha um endpoint que devolve a política resolvida do perfil
   (categorias já expandidas em domínios, mais allow/block explícitos), ou o sync passa a carregar
   a lista. O endpoint é mais limpo e não mexe no núcleo. As categorias grandes (adulto, malware)
   têm centenas de milhares de domínios: **decida e justifique** um limite, um formato compacto e
   um cache com validade, ou mantenha o encaminhamento ao resolvedor filtrante como caminho
   preferencial quando ele for alcançável, com a lista local como garantia mínima.
3. **Fail-closed ou fail-open.** O `resolver.py` do servidor é explicitamente fail-closed. Diga
   qual foi a sua escolha no aparelho e por quê. Um filtro que abre tudo quando falha é o defeito
   que estamos consertando.
4. **O que o painel mostra.** Hoje a tela do Web Filter mostra a política. Precisa passar a
   mostrar o **estado real no aparelho**: VPN ativa, política em vigor, quando foi aplicada.
   Enquanto o painel disser "filtro ativo" sem saber o que o aparelho está fazendo, ele mente.

### O que NÃO prova nada

`dig` contra o servidor. `curl` no endpoint. "200 OK". A prova é o tablet na mão, com o filtro
ligado, tentando abrir facebook.com e **não conseguindo** — e tentando abrir um site permitido e
conseguindo. Sem esse par, não há entrega.

## Limite honesto, a declarar ao responsável

DNS bloqueia domínio, não conteúdo. Fica de fora: app que fala com IP fixo sem consultar DNS, e
navegador com DoH próprio ligado à força (o Chrome não faz auto-upgrade quando o DNS é um endereço
privado, mas o usuário pode ligar na mão). Se isso precisar ser coberto, aí sim é VPN com
inspeção de SNI — outra ordem de grandeza de trabalho, e assunto para outra tarefa.
