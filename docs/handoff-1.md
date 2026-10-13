# Handoff 1: correcao do enrollment e APK

Data: 2026-10-01

## Objetivo

Corrigir o erro recorrente durante o enrollment do dispositivo, associado ao aviso do Google Play Protect, preservando os recursos existentes na versao 1.3 e usando o APK 6.36 como referencia do comportamento que ja funcionava.

## Escopo adotado

- A base de codigo utilizada foi `repo-mdm/android-source`.
- A versao 1.2 foi descartada por nao funcionar com acesso remoto.
- A versao 1.4 foi descartada por ter sido criada apenas para testar um APK sem acesso remoto.
- As versoes 1.5 a 1.7 foram tratadas como historico de testes; recursos validos ja incorporados na base atual foram preservados.
- O APK 6.36 foi usado como referencia de compatibilidade, assinatura e comportamento funcional, nao como substituto integral do codigo atual.

## Diagnostico

O fluxo de enrollment esta em:

`android-source/app/src/main/java/com/hmdm/launcher/task/GetServerConfigTask.java`

Durante o primeiro inicio, o aplicativo executa um POST de enrollment. O cliente tenta o servidor principal e, em caso de falha, tenta o servidor secundario. O problema encontrado era:

1. A tentativa contra o servidor principal podia lancar uma excecao.
2. A tentativa contra o servidor secundario tambem podia lancar uma excecao.
3. Nesse caso, `response` permanecia nulo.
4. O codigo seguinte acessava `response.isSuccessful()`, causando falha do fluxo em vez de apresentar um erro de disponibilidade compreensivel.

Esse caminho afetava tanto o enrollment normal quanto o enrollment com resposta assinada.

O Play Protect tambem foi considerado no diagnostico. O manifesto atual nao declara `READ_SMS`, uma permissao que ja estava documentada no projeto como causadora de bloqueios em alguns paises. O recurso de captura de tela e componentes ligados a acessibilidade tambem foram removidos/desativados na base atual por risco de bloqueio. O APK deve continuar sendo distribuido apenas com assinatura release confiavel e consistente.

## Correcao aplicada

No metodo `enrollPlain`:

- A chamada ao servidor secundario passou a estar protegida por `try/catch`.
- Foi adicionada uma verificacao explicita para `response == null`.
- Quando os dois servidores ficam indisponiveis, o cliente retorna a mensagem:

`Enrollment failed: primary and secondary servers are unavailable`

No metodo `enrollSecure`, foram aplicadas as mesmas protecoes para o endpoint que retorna a resposta bruta usada na verificacao de assinatura.

Arquivos alterados para esta entrega:

- `android-source/app/src/main/java/com/hmdm/launcher/task/GetServerConfigTask.java`
- `android-source/app/build.gradle` ja continha a configuracao local de release, com `versionName "1.9"`, `versionCode 15399` e assinatura v2.

Nenhuma senha, chave privada ou segredo foi incluido nesta documentacao.

## Build

O wrapper Gradle possui um manifesto incompleto e nao pode ser executado diretamente com `./gradlew`. O build foi executado chamando a classe principal do wrapper:

```bash
cd /opt/projetos/hwmdm/repo-mdm/android-source
java -cp gradle/wrapper/gradle-wrapper.jar \
  org.gradle.wrapper.GradleWrapperMain \
  :app:assembleOpensourceRelease --no-daemon
```

Resultado:

`BUILD SUCCESSFUL`

Artefato gerado:

`android-source/app/build/outputs/apk/opensource/release/app-opensource-release.apk`

SHA-256 do APK gerado:

`a82838f80dba112ec9eda362ffdc15b8145386601affcd2b9f8f28b3cf572662`

## Validacoes realizadas

- Compilacao Java concluida sem erros.
- Recursos Android processados com sucesso.
- Lint vital de release concluido.
- Empacotamento release concluido.
- `git diff --check` nao apontou problema no arquivo alterado.
- O APK foi gerado como release e utiliza o esquema de assinatura configurado no Gradle para o artefato atual.

Avisos nao bloqueantes do build:

- O `local.properties` aponta para um diretorio de SDK inexistente, mas o build conseguiu usar o SDK/cache disponivel.
- O Java 21 avisou que source/target 8 esta obsoleto; isso nao impediu a compilacao.
- O Gradle informou uso de funcionalidades depreciadas para futuras versoes do Gradle.

## Teste recomendado no dispositivo

1. Instalar o APK release em um dispositivo de teste autorizado.
2. Limpar o estado anterior do aplicativo ou usar um dispositivo ainda nao matriculado.
3. Executar o enrollment pela configuracao/QR code correspondente ao servidor.
4. Confirmar no logcat que a chamada principal ou secundaria retorna configuracao valida.
5. Repetir com o endpoint principal indisponivel e confirmar que o aplicativo mostra erro de servidor indisponivel, sem encerramento inesperado.
6. Confirmar que o Google Play Protect nao bloqueia a instalacao ou a abertura do APK.
7. Validar pos-enrollment: atualizacao de configuracao, instalacao de aplicativos gerenciados, notificacoes e acesso remoto autorizado.

## Limitacoes conhecidas

- Nao houve teste fisico de enrollment nesta sessao; a validacao realizada foi de compilacao, empacotamento e revisao do fluxo.
- O alerta do Play Protect depende do dispositivo, pais, reputacao do certificado e politicas atuais do Google. A remocao de permissoes/componentes de alto risco reduz a superficie, mas nao substitui um teste no aparelho afetado.
- O `gradlew` deve ser corrigido posteriormente para conter um launcher executavel com manifesto `Main-Class`, ou o ambiente deve fornecer uma instalacao Gradle oficial.
- A identidade de assinatura deve permanecer a mesma para permitir atualizacao sobre dispositivos ja matriculados. Nunca trocar o keystore sem planejar reinstalacao/factory reset.

## Correcao aplicada no ambiente ativo

Depois do primeiro handoff, foi confirmado que o perfil usado no teste nao estava usando a base corrigida:

- Perfil: `modelo-kiosk`
- ID do perfil: `57`
- APK anterior: `hmdm-1.2-olimpia.apk`
- Versao anterior: `1.2`
- ID anterior: `10128`

Isso explicava por que o teste continuava apresentando o mesmo comportamento. O perfil estava configurado para instalar exatamente a versao que havia sido descartada.

Foi realizada a seguinte atualizacao no banco ativo:

- Novo APK: `hmdm-1.9-olimpia.apk`
- Nova versao: `1.9`
- Novo ID de versao: `10134`
- `mainappid` do perfil 57 atualizado para `10134`
- Vinculo do aplicativo `com.hmdm.launcher` atualizado para `10134`
- Hash publicado: `qCg4-A26ES7J7aNi_9wVuBRThmAa_80rn48os89XJmI`

O arquivo foi publicado em:

`source/volumes/work/files/hmdm-1.9-olimpia.apk`

## Ajuste posterior: bootstrap Play Protect

O teste real mostrou que o Play Protect bloqueava o APK `1.9` antes do enrollment. O perfil foi temporariamente revertido para o artefato original `6.36`, que foi confirmado como instalavel no mesmo ambiente e usa o certificado release conhecido:

- Perfil: `modelo-kiosk` (ID 57)
- APK de bootstrap atual: `hmdm-6.36-os.apk`
- Versao ativa: `6.36` (ID 10045)
- Backup antes do ajuste: `source/volumes/backups/pre-modelo-kiosk-bootstrap-636-20261001.dump`

O APK `1.9` possui um manifesto com permissoes de alto risco para o Play Protect, incluindo instalacao de pacotes, consulta ampla de aplicativos, gerenciamento de armazenamento e injecao de eventos. Alem disso, o APK original `6.36` possui assinatura JAR legada junto da assinatura Android, enquanto o `1.9` foi gerado apenas com V2. Por isso o `1.9` nao deve ser usado como bootstrap ate ser reconstruido com manifesto reduzido e assinatura compatível com o artefato aceito.

## Variante 1.10 publicada

Foi gerada uma nova variante para corrigir o bloqueio no bootstrap:

- Versao: `1.10`
- ID no banco: `10135`
- APK: `source/volumes/work/files/hmdm-1.10-olimpia.apk`
- Perfil `modelo-kiosk` atualizado para usar essa versao
- Assinatura release V1+V2
- Removidas do manifesto as permissoes de alto risco que nao sao necessarias para o enrollment inicial: gerenciamento amplo de armazenamento, leitura de telefone privilegiada/numeros, consulta ampla de pacotes, injecao de eventos e instalacao/exclusao direta de pacotes.
- SHA-256: `5c75f2c6e85b3b4fd78a1993964e6de7b67f0afd4f844af6c1205e4bf2473357`

O APK foi confirmado por HTTP no servidor antes da troca do perfil.

## Estado final do perfil

Como as variantes recompiladas `1.9` e `1.10` continuaram sendo bloqueadas pelo Play Protect, o perfil `modelo-kiosk` foi deixado no ultimo artefato comprovadamente aceito no dispositivo:

- Versao ativa: `6.36`
- ID: `10045`
- URL: `http://192.168.1.65:8080/files/hmdm-6.36-os.apk`
- Backup: `source/volumes/backups/pre-modelo-kiosk-final-636-20261001.dump`

As variantes `1.9` e `1.10` permanecem publicadas no diretorio de arquivos, mas nao sao usadas pelo perfil. O trabalho de incorporar os recursos da 1.3 sem disparar o bloqueio exige o artefato original aceito como base binaria ou autorizacao para trocar a politica/reputacao do Play Protect no dispositivo; nao e seguro continuar alterando o APK sem esse controle.

Foi confirmado por HTTP que o MDM serve o mesmo arquivo publicado, com SHA-256:

`a82838f80dba112ec9eda362ffdc15b8145386601affcd2b9f8f28b3cf572662`

Backup realizado antes da alteracao:

`source/volumes/backups/pre-modelo-kiosk-fix-20261001.dump`

## Proximo teste

O dispositivo deve ser reenrolado usando o QR code do perfil `modelo-kiosk`. Para evitar estado antigo, o teste deve ser feito com o aparelho limpo ou com o aplicativo anterior removido conforme o procedimento de provisionamento autorizado. No painel/banco, a resposta do perfil deve indicar a versao `1.9` e a URL `hmdm-1.9-olimpia.apk`; se aparecer `1.2`, o dispositivo esta usando uma resposta/cache antigo.

## Arquivos de referencia

- Codigo de enrollment: `android-source/app/src/main/java/com/hmdm/launcher/task/GetServerConfigTask.java`
- Manifesto e permissoes: `android-source/app/src/main/AndroidManifest.xml`
- Configuracao de build e assinatura: `android-source/app/build.gradle`
- APK release: `android-source/app/build/outputs/apk/opensource/release/app-opensource-release.apk`

## Complemento de verificacao: APK 1.3 e assinatura

Em 2026-10-01, foi verificado o artefato 1.3 que ja estava no repositorio. Os caminhos `source/volumes/work/files/hmdm-1.3-olimpia.apk`, `dist/hmdm-1.3-olimpia.apk` e `dist/hmdm-v1.3.apk` apontam para conteudo identico, SHA-256 `e074207907d43c00d01981c280fb8b06813af2fc12b5bf90eb9005654c1a1899`. O APK e `com.hmdm.launcher`, `versionName 1.3`, `versionCode 15382`, e inclui permissoes sensiveis como `MANAGE_EXTERNAL_STORAGE`, `INJECT_EVENTS`, `REQUEST_INSTALL_PACKAGES`, `REQUEST_DELETE_PACKAGES` e `QUERY_ALL_PACKAGES`.

O APK 1.3 esta assinado com o certificado municipal cujo SHA-256 e `44372f140d1d64c6b5136524f5ccac0391172ca3348d09218598486e3c7ad510`. O keystore local `android-source/keystore/hwmdm-release.jks` tem o mesmo certificado, portanto permite produzir atualizacoes compativeis com o 1.3. O APK 6.36 aceito no teste anterior tem outro certificado (`095761e0055fe057672406397f352257cd34d71f279e8bd4f4fd3d8f91099757`). Android nao permite atualizar uma instalacao 6.36 por cima com o APK 1.3 assinado pelo certificado municipal; requer provisionamento limpo/reinstalacao autorizada. Essa incompatibilidade nao deve ser contornada trocando ou removendo o launcher de um dispositivo matriculado sem procedimento aprovado.

O APK 1.3 disponivel declara permissoes que foram removidas nas variantes reduzidas por suspeita de disparar o Play Protect. Nao existe evidencia nesta sessao de que 1.3 passe pelo Play Protect nem de que complete enrollment em dispositivo real. O ambiente nao tem `adb`, portanto nao foi possivel fazer o teste fisico. A existencia do arquivo, sua assinatura valida, o hash publicado ou o sucesso de build nao provam instalacao nem enrollment. Por isso, o estado comprovado continua sendo apenas o bootstrap 6.36 documentado acima. Nao alterar o perfil ativo para 1.3 como se isso fosse uma correcao comprovada.

### Condicao para fechar o enrollment com 1.3

Usar aparelho de teste autorizado e limpo (ou seguir o processo aprovado para reinstalacao), confirmar que Play Protect permite instalar o APK 1.3, executar enrollment pelo QR do perfil correto, e validar no servidor o registro do dispositivo e a resposta de configuracao. Registrar modelo/Android, resultado do Play Protect, hash instalado, logs do cliente/servidor e resultado de sincronizacao. Se o Play Protect bloquear, interromper: o ajuste de manifesto/assinatura exige novo APK e novo teste real; nao se deve afirmar compatibilidade sem essa evidencia.
