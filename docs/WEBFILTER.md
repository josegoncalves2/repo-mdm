# Web Filter — publicação, operação e rollback

O Web Filter bloqueia **sites** (DNS privado do Android apontando para o resolvedor `webfilter-dns`) e **aplicativos** (pacotes ocultos pelo launcher via `locked_packages`) por perfil de dispositivo. Design: `openspec/changes/add-webfilter-module/design.md`.

| Peça | Onde | Como chega ao ambiente |
|---|---|---|
| Plugin (banco, API, tela, sync) | `server-source/plugins/webfilter/` | dentro da WAR (`dist/hmdm.war`) |
| Item "Web Filter" no menu + F5 em página de plugin | `server-source/server/src/main/webapp/` | dentro da WAR |
| Resolvedor DNS-over-TLS | `webfilter-dns/` | `docker compose -f webfilter-dns/docker-compose.yaml up -d --build` |
| Launcher com DNS privado | `android-source/` → `dist/hmdm-v1.1-webfilter.apk` | `scripts/publicar-apk.sh` |

## Pré-requisitos do filtro de sites

1. Um domínio só para o filtro, por exemplo `filtro.empresa.com.br`.
2. Um registro DNS **curinga** `*.filtro.empresa.com.br` apontando para a máquina do resolvedor, com a porta **TCP 853** liberada para os tablets.
3. Um certificado **curinga válido publicamente** para `*.filtro.empresa.com.br`. O Android valida a cadeia no modo DNS privado estrito. Emissão Let's Encrypt curinga exige desafio DNS-01; o `server-source/letsencrypt-ssl.sh` usa HTTP-01 e não serve para isso.
4. `cert.pem` e `key.pem` numa pasta fora do git, informada em `WEBFILTER_CERTS_DIR`. O padrão é `webfilter-dns/certs/`, que é ignorada pelo git.

Sem os itens 1 a 3, o bloqueio de **aplicativos** funciona normalmente. O de sites não: o launcher registra `private DNS ... refused` no log do aparelho e tenta de novo a cada sync.

## Publicação

1. **Backup:** `pg_dump` do banco, a WAR em uso e o APK do launcher em uso.
2. **WAR:** coloque a WAR nova em `dist/hmdm.war` e em `webapps/ROOT.war` do container. Em seguida **reinicie o container do MDM** (`docker restart <container hmdm>`). Não faça só a troca a quente da WAR: a recarga a quente deixa a instância antiga viva. O plugin se protege disso (só a instância mais nova grava a configuração do resolvedor), mas a instância antiga continua ocupando memória até o reinício.
3. **Conferir no log do MDM:** o changelog `webfilter.changelog.xml` aplicado e a linha `Web filter resolver configuration written`.
4. **Resolvedor:** `docker compose -f webfilter-dns/docker-compose.yaml up -d --build`. Espere `Blocky pronto` em `docker logs webfilter-dns`. Enquanto as listas não foram baixadas, a porta 853 fica fechada (fail-closed).
5. **Launcher:** publique primeiro num perfil piloto: `scripts/publicar-apk.sh --apk dist/hmdm-v1.1-webfilter.apk --perfil <id do perfil piloto>`. Ele só atualiza aparelhos cujo launcher foi assinado com o mesmo certificado (SHA-256 `44372f14…`). O `hmdm-6.36-os.apk` oficial da Headwind tem outra assinatura e não é atualizado por este APK.
6. **Console:** conceda `plugin_webfilter_access` ao papel que administra o filtro. Na aba Configuração, grave o domínio do filtro. Ative a política só no perfil piloto, valide sites e aplicativos num tablet real, e só então amplie para os demais perfis.

## Operação

- Alteração de allowlist/blocklist de sites: vale em segundos, sem reiniciar o resolvedor. O log mostra `listas recarregadas sem reinicio`.
- Mudança de categorias: o resolvedor baixa a lista da categoria, se ainda não tiver, e reinicia o Blocky. A porta 853 fica fechada durante a carga, de poucos segundos a cerca de 25 s com a categoria `adult`.
- Configuração que não sobe: o resolvedor volta para a última configuração boa em poucos segundos e agenda uma nova tentativa com espera crescente (2 min até 1 h). A nova tentativa é ensaiada em portas alternativas, sem derrubar o DNS em uso. Durante essa espera:
  - as mudanças de allowlist/blocklist de perfis que já existem na configuração boa continuam valendo em segundos;
  - uma queda do Blocky em uso é recuperada em segundos;
  - só o que depende da configuração nova (perfil recém-ativado, categoria nova) aguarda a tentativa bem-sucedida.
- Listas de origem: baixadas no máximo a cada 24 h, nunca mais de uma vez por hora. Falha de download mantém a lista anterior.

## Limites conhecidos

- O filtro de sites é por nome (DNS). Acesso por IP literal não é filtrado.
- Um app que fala DNS-over-HTTPS com um IP fixo, ou uma VPN de terceiros, contorna o filtro de sites. A categoria `doh` fica sempre bloqueada, e a categoria `vpn_proxy` bloqueia apps e domínios de VPN conhecidos. Para impedir a configuração de VPN, use a restrição de perfil `no_config_vpn`.
- Portal cativo de Wi-Fi público (hotel, aeroporto) não abre com o DNS privado estrito. É preciso desativar o filtro do perfil ou usar outra rede.
- O Android só aceita o DNS privado com um certificado válido publicamente para o hostname do perfil. Com certificado autoassinado, que é o caso do DEV, o launcher registra `refused` e os sites não são filtrados no aparelho.
- Memória medida no DEV: cerca de 47 MiB com as categorias `doh`, `games`, `social_media` e `streaming` (~95 mil domínios). A categoria `adult` é a maior (3 partes da UT1) e sobe o tempo de carga para cerca de 25 s. Meça no ambiente real antes de liberá-la para muitos perfis. O limite do container é 1,5 GiB.
- Não foi feito ensaio em homologação com cópia do banco de produção (tarefa 10.3), nem teste em tablet real, porque não há tablet matriculado no DEV.

## Rollback

**Funcional:** desative as políticas pelo console. No sync seguinte, os tablets recebem `unlocked_packages` e os apps voltam com os dados, e o launcher devolve o DNS privado ao modo automático.

**Técnico:** faça-o só depois do rollback funcional confirmado nos aparelhos.
1. `docker compose -f webfilter-dns/docker-compose.yaml down`
2. Restaure a WAR anterior e reinicie o container do MDM. As tabelas `plugin_webfilter_*` ficam inertes.
3. Para remover as tabelas e o registro do plugin, faça um `pg_dump` novo e rode o rollback do changelog, depois de ensaiado em homologação.
4. O launcher novo pode permanecer: sem o campo `webfilterDnsHost` no sync, ele não mexe no DNS.
