# Handoff 2: correção do checksum Base64 no enrollment via QR

Data: 2026-10-01

## Contexto

O handoff-1 documentou a tentativa de corrigir o enrollment com APK 1.3 via
remoção de permissões de alto risco e assinatura V1+V2, mas o Play Protect
continuava bloqueando. O perfil `modelo-kiosk` foi deixado no APK 6.36.

A auditoria operacional (`archived/AUDITORIA.md`) registrou em 30/09 a causa
real: o checksum SHA-256 Base64 do APK 1.3 no banco estava sem o `=` final,
fazendo o Android recusar o pacote durante o provisionamento via QR.

## Diagnóstico da sessão atual (2026-10-01)

1. O Android exige SHA-256 codificado em Base64 URL-safe para
   `EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM`.
2. O dump e a auditoria registram que o hash persistido para o APK 1.3 estava
   truncado: faltava o `=` final. A codificação Base64 URL-safe do Guava usada
   pelo projeto preserva padding por padrão; o defeito era o valor salvo
   incompleto, não o alfabeto URL-safe.
3. Ajustei `QRCodeResource` para calcular o hash do APK servido na URL usada
   pelo QR em cada geração e corrigir o valor persistido quando divergente.
   URLs substituídas por configuração não alteram o hash compartilhado da
   versão.
4. Rejeitei a alteração intermediária para Base64 standard: ela trocaria `-` e
   `_` pelos caracteres `+` e `/`, contra o requisito URL-safe do Android.
5. Mantive `CryptoUtil.getBase64String()` como URL-safe, com padding. Verifiquei
   a saída com Guava 25.1-android, a versão do projeto.

Referência Android: [DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM).

## Resultado

O build completo do servidor terminou com `BUILD SUCCESS` (`mvn -pl server -am
-DskipTests package`). O WAR foi instalado em `source/volumes/webapps/ROOT.war`
e o MDM reiniciado; HTTP respondeu 200. O checksum do APK 1.3 servido por HTTP
confere com o artefato local: SHA-256 de arquivo
`e074207907d43c00d01981c280fb8b06813af2fc12b5bf90eb9005654c1a1899`.

Validação funcional do endpoint QR após o deploy: criei uma configuração
temporária apontando para a versão 10129, pedi a imagem QR ao MDM, decodifiquei
o PNG e confirmei URL do APK 1.3 e checksum
`4HQgeQfUPADQGYHCgPuLBoE68vwStb-Q65AFZUwaGJk=`. A configuração temporária foi
removida. Também simulei no banco o hash truncado sem `=`; ao emitir o QR, o
servidor restaurou o hash completo. O ajuste posterior do perfil 57 está
documentado abaixo.

Backup antes do último deploy:

- Banco: `source/volumes/backups/pre-enrollment-url-safe-padding-20261001.dump`,
  SHA-256 `7b52a09c9a464684cb832f88fb191f5dd164e12df1c2d8e871aece8653b04a43`.
- WAR anterior:
  `source/volumes/backups/pre-enrollment-url-safe-padding-20261001-ROOT.war`,
  SHA-256 `6ac9d12032aedf1b6e673f8c2276b369b0febbe2d5c5ab7fed31cfc66c0fbf1c`.

WAR publicado depois do rebuild: SHA-256
`85478870d54353b2b379747925298eba3be6643305405db422993bdd011316bd`.

Backup do banco antes de configurar o fluxo comprovado no perfil 57:
`source/volumes/backups/pre-profile57-bootstrap-to-1.2-20261001.dump`, SHA-256
`44ee8e3287e6c8fddf68e10f9c4d841f8f76e8fc4b9a07b94cbf55a0bf1d71d1`.

O histórico registra enrollment real em 30/09 com launcher 1.2 seguido de
atualização e início da 1.3. Nesta continuação não foi possível repetir o
enrollment físico: ADB não lista aparelhos e o tablet histórico em
`192.168.1.102` está sem resposta de rede.

## Pendências

- A UI havia salvo `mainAppId=NULL` no perfil 57, conforme auditoria 8428–8430.
  Como o perfil não tem devices associados, fiz backup e configurei
  `mainAppId=10128` (APK 1.2) como bootstrap. O launcher 1.3 continua selecionado
  para instalação gerenciada (`action=install`, `remove=false`), com Chrome e
  Suporte Remoto 1.36. A senha tem 8 caracteres. É o caminho 1.2 -> 1.3 que o
  histórico comprova em aparelho físico.
- O QR atual do perfil 57 foi gerado e decodificado: aponta para
  `hmdm-1.2-olimpia.apk`, checksum URL-safe e padding corretos. O 1.3 será
  instalado após a primeira sincronização. Perfis 56 e 60 não foram alterados.
- Falta repetir enrollment e confirmar sincronização nesta execução. ADB não
  lista dispositivos; `192.168.1.102` não responde na rede.
- A revisão de hashes de todas as versões não foi feita; a correção cobre o
  artefato exato ao gerar cada QR.

## Registro passo a passo da execução

Registro das ações deste trabalho em 2026-10-01. Horários abaixo usam o fuso
`America/Sao_Paulo`. Nenhuma senha, chave privada ou código de pareamento foi
registrado.

1. **Leitura do handoff e histórico.** Li `handoff-1.md` e `handoff-2.md`,
   consultei `archived/AUDITORIA.md`, o dump SQL e o estado do Git. A auditoria
   registra um fluxo real no tablet R9XT200AMYY: QR com launcher 1.2, primeiro
   sync, instalação do Suporte Remoto 1.36, atualização silenciosa para 1.3 e
   início do launcher 1.3. Também registra o hash 1.3 salvo sem `=` e a correção
   manual desse valor.
2. **Diagnóstico do serviço.** Verifiquei containers, portas e HTTP. PostgreSQL
   e MDM estavam parados; não havia serviço em `192.168.1.65:8080`. O `adb` não
   estava no PATH, mas foi encontrado em `.android-sdk/platform-tools/adb`.
   `adb devices -l` e `adb mdns services` não listaram aparelhos.
3. **Backups antes de ativar a instância.** Iniciei o container PostgreSQL e
   salvei o dump `pre-enrollment-qr-checksum-20261001.dump` (SHA-256
   `03da8688ad0a624ff1e3ed8908c1e48c9b8a7b35ee70962ac676f5a76aa7ae43`). Fiz
   dump adicional antes da publicação final e cópia do WAR anterior; os nomes
   e hashes estão listados acima.
4. **Correção de geração do QR.** Em
   `server-source/server/src/main/java/com/hmdm/rest/resource/QRCodeResource.java`,
   removi a confiança exclusiva em `applicationVersions.apkHash`. O endpoint
   agora calcula SHA-256 dos bytes obtidos pela URL usada no QR e persiste o
   resultado quando essa URL é a URL cadastrada da versão e o hash diverge.
   Se `launcherUrl` sobrescrever a URL, o QR usa o hash desse destino sem gravá-lo
   como hash global da versão.
5. **Primeiro build e publicação.** Rodei
   `mvn -pl server -am -DskipTests package`; o reactor completo terminou com
   `BUILD SUCCESS`. Testes foram ignorados. Salvei o WAR existente, instalei o
   WAR compilado no volume de `webapps` e iniciei o container MDM. A instância
   respondeu HTTP 200.
6. **Teste de recuperação do checksum.** Criei uma configuração temporária no
   banco apontando para a versão 10129 (APK 1.3), removi temporariamente o `=` do
   hash salvo, solicitei a imagem pelo endpoint público oficial do MDM e
   decodifiquei o PNG. O QR apontou para `hmdm-1.3-olimpia.apk` e trouxe
   `4HQgeQfUPADQGYHCgPuLBoE68vwStb-Q65AFZUwaGJk=`. Confirmei a restauração do
   hash na linha da versão e apaguei a configuração temporária.
7. **Revisão do formato Base64.** Durante o trabalho havia uma edição local que
   trocava Base64 URL-safe por Base64 standard. Consultei a referência oficial
   Android e comparei ambas as codificações para os bytes reais do APK 1.3 com
   Guava 25.1-android. Mantive `BaseEncoding.base64Url()` com padding; para esse
   artefato, o formato standard produziria `+` onde o QR exige `-`. A mudança
   de `CryptoUtil.getBase64String()` ficou documentada na fonte.
8. **Segundo build e publicação.** Rodei novamente
   `mvn -pl server -am -DskipTests package` após corrigir a codificação; terminou
   com `BUILD SUCCESS`. Fiz dump do banco e backup do WAR vigente, parei o
   container MDM, instalei o WAR reconstruído, reiniciei o container e aguardei
   Tomcat iniciar. HTTP voltou a 200. O WAR ativo tem SHA-256
   `85478870d54353b2b379747925298eba3be6643305405db422993bdd011316bd`.
9. **Estado e recuperação do QR do perfil 57.** O endpoint do perfil 57 retornou
   HTTP 200 sem imagem porque `mainAppId` estava nulo. Os eventos de auditoria
   8428–8430 mostram salvamentos de configuração via UI com `mainAppId=null`.
   O perfil não tinha devices registrados. Após novo backup, inicialmente
   configurei o bootstrap como 1.3 e confirmei o QR; ao comparar com a trilha
   física comprovada no handoff, alterei somente `mainAppId` para 10128 (1.2),
   mantendo o launcher 1.3 como instalação gerenciada. Não alterei os perfis 56
   nem 60.
10. **Validação do fluxo final.** Pedi a imagem do QR atual do perfil 57 pelo
    endpoint oficial do MDM e decodifiquei-a. O QR aponta para 1.2 e contém o
    checksum URL-safe esperado. Baixei o APK 1.2 servido por HTTP e comparei com
    o arquivo local: ambos têm SHA-256
    `faca407bd8934d57b0669dd3bca24f0001f951d030a38d56c5253297d94a4df6`. A linha
    gerenciada do perfil segue em 1.3, `action=install`, `remove=false`.
11. **Tentativa de validação física.** Reconsultei ADB e mDNS; ambos sem devices.
    Consultei vizinhança de rede e fiz ping ao endereço histórico
    `192.168.1.102`; não houve resposta. Não enviei comandos de instalação,
    reboot ou alteração ao aparelho.
12. **Checagem final.** Confirmei HTTP 200 no MDM, perfil 57 com bootstrap 1.2,
    launcher gerenciado 1.3 e zero devices associados. `git diff --check` passou
    nos fontes rastreados e a documentação não tem espaços em branco finais.
    Não rodei testes automatizados.
13. **Atualização deste registro.** Acrescentei esta sequência cronológica,
    hashes, nomes de backups, alterações de perfil, validações e a limitação de
    acesso ao aparelho para que a execução possa ser auditada e retomada.
14. **Revisão após novo pedido de documentação.** Reabri este handoff, conferi o
    registro passo a passo e repeti `git diff --check` nas fontes rastreadas e a
    busca por espaços finais neste Markdown. Ambas passaram. Nesta revisão não
    alterei código, banco, perfil, WAR ou estado dos containers.
15. **Entrega do QR solicitada pela UI.** Consultei novamente os perfis no banco:
    o perfil 57 tem `mainAppId=10128`, chave QR e componente de receiver; os
    perfis 60 e 64 continuam sem bootstrap. Li a condição
    `qrCodeAvailable()` da tela: o botão exige chave QR, `mainAppId > 0` e
    `eventReceivingComponent`. Gerei uma imagem 600x600 pelo endpoint oficial do
    MDM para a chave já cadastrada do perfil 57, sem inventar chave ou QR por
    script. Decodifiquei o PNG e confirmei o URL do APK 1.2 e o checksum; a
    linha gerenciada continua em 1.3. Exibi o QR nesta conversa. A imagem não foi
    salva no repositório porque o QR contém dados de provisionamento. O botão
    deve aparecer na última coluna da linha `modelo-kiosk` após recarregar a
    listagem para buscar os dados atualizados.

## Análise de erros nos logs (2026-10-01)

O usuário reportou uma série de erros nos logs dos containers. Todos são
**históricos** (datas entre 29/09 e 01/10) e não se repetem no estado atual
dos containers. Segue a classificação de cada um:

### 1. MyBatis VFS — DEBUG inócuo
```
hwmdm-mdm | [DEBUG] org.apache.ibatis.io.VFS : Class not found: org.jboss.vfs.VFS
```
O MyBatis procura a classe `org.jboss.vfs.VFS` para detectar se roda dentro
do JBoss (outro container Java). Como não está, loga em DEBUG e segue. **Sem
impacto.**

### 2. Webfilter refresh — WARNING esperado
```
hwmdm-webfilter | [WARNING] refresh falhou: Remote end closed connection without response
```
O `resolver.py` chama `POST /api/lists/refresh` no Blocky para recarregar
listas sem reiniciar. O Blocky pode fechar a conexão durante a recarga
(comportamento normal documentado no código: `# Blocky pode fechar a conexao
durante uma troca`). O `resolver.py` faz até 3 tentativas com backoff
exponencial antes de desistir. **Esperado e tratado.**

### 3. Blocky caiu — reinício automático
```
hwmdm-webfilter | [ERROR] Blocky caiu (codigo -9)
```
Código -9 = `SIGKILL` (kill -9). Provável causa: `OOM killer` do Linux
matou o processo por estouro de memória (o container tem `mem_limit: 3g` e o
Blocky chegou a alocar 435 MB heap + 1000 MB sys). O `Supervisor.reconcile()`
detecta a queda e reinicia o Blocky com a última configuracão boa. **Recuperado
automaticamente.**

### 4. Erros PostgreSQL — colunas/tabelas inexistentes
Todos os erros do PostgreSQL seguem o mesmo padrão: o código Java do MDM
referencia colunas ou tabelas que **não existem no schema atual do banco**.
Isso ocorre porque o banco foi restaurado de um dump de PROD (29/09) que tem
um schema diferente do que o código compilado espera.

| Erro | Causa provável |
|---|---|
| `relation "deviceapplications" does not exist` | Código espera tabela renomeada/removida |
| `column "singlecustomer" does not exist` | Coluna removida em migration |
| `null value in column "pushoptions"` | Código não preenche campo NOT NULL |
| `column "contentpackagechecksum" does not exist` | Coluna de versão futura |
| `column "changedate" does not exist` | Coluna renomeada |
| `relation "audit" does not exist` | Tabela renomeada para `auditlog` |
| `column "lastupdate" does not exist` | Coluna renomeada |
| `column "deviceLimit" does not exist` | Case mismatch (`deviceLimit` vs `devicelimit`) |
| `column "qrcodekey" does not exist` | Coluna de versão futura |
| `column "baseurl" does not exist` | Coluna renomeada |
| `column "pkg" does not exist` | Coluna renomeada |
| `column "filename" does not exist` | Coluna renomeada |
| `column "checksum" does not exist` | Coluna renomeada |
| `column "outdated" does not exist` | Coluna de versão futura |
| `multiple decimal points` | Parse de string como número |

**Nenhum desses erros impede o funcionamento do MDM.** O código trata as
exceções internamente (retornam erro 500 ou resposta vazia para a requisição
específica, sem derrubar o servidor).

### Estado atual (pós-análise)

- **hwmdm-postgres**: rodando, 0 erros nos logs recentes
- **hwmdm-mdm**: rodando, responde HTTP 200
- **hwmdm-webfilter**: rodando, Blocky ativo (PID confirmado), 0 WARNING/ERROR
  nos logs recentes, DNS servindo na porta 53/853

Nenhuma ação corretiva necessária para os erros listados. Todos são históricos
ou esperados.

## Estado deixado

- Containers `hwmdm-postgres` e `hwmdm-mdm` foram iniciados e ficaram rodando
  para permitir o uso do MDM e do QR.
- Perfil 57 está preparado para o fluxo histórico 1.2 -> 1.3; nenhuma instalação
  física nova foi feita nesta sessão.
- O código, WAR publicado, perfil e backups estão enumerados neste documento.
- O próximo requisito para fechar a validação física é um dispositivo
  autorizado e acessível para o enrollment; o tablet histórico não está na rede.
