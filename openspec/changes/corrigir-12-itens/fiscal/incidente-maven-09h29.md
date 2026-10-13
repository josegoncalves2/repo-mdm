# Incidente 2026-09-23 09:29 — recompilação do núcleo do WAR, interrompida

## O que aconteceu

O executor da tarefa C (versão/build) rodou, em `server-source/`:

```
mvn -o -q -pl server -am clean package -DskipTests
```

Isso é a recompilação do núcleo do WAR — proibida neste repositório, porque o fonte em
`server-source/server` não corresponde ao build de 2026-08-11 que está no ar. Já quebrou o MDM
antes. **A culpa é do enunciado que a sessão principal escreveu**: o prompt da tarefa C pedia um
alvo de build "console" sem proibir explicitamente o javac do núcleo, proibição que estava no
prompt da tarefa B e faltou no da C.

## Consequência

Nada foi publicado. O DEV em execução não foi tocado: o `mvn` só mexeu na árvore de trabalho.

O `clean` apagou 610 arquivos versionados sob `server-source/**/target/` e o `package` depois
recriou a maior parte deles — com JDK 21, ou seja, **bytecode diferente do versionado**. Estado
resultante na árvore, fora o trabalho legítimo dos executores:

```
84 arquivos M  (modificados, sob **/target/)
 5 arquivos D  (apagados,   sob **/target/)
 2 arquivos ?? (novos:      RemoteSessionHub$EscritaEspectador.class em
                server/target/classes e server/target/launcher/WEB-INF/classes)
```

## O que eu fiz

1. Mandei parar e corrigi o escopo da tarefa C por mensagem: nada de `clean`/`package`/`install`
   em `server-source/`; alvos legítimos passam a ser `agente-remoto` (gradle), `console` (carimbo
   e manifesto, sem javac) e `plugin <nome>` (um plugin isolado, `-pl plugins/<nome>`, sem `-am`
   e sem `clean` — foi assim que o Web Filter foi construído).
2. Tentei restaurar os artefatos com `git checkout -- <caminhos sob target/>`.

## O que está travado e precisa de você

A trava de escopo recusa:

```
TRAVA DE ESCOPO: BLOQUEADO. 'git checkout' altera o repositorio ou o historico;
versionamento e decisao do humano.
```

Não contornei, e não vou contornar. **A restauração é sua.** Os caminhos afetados são todos sob
`server-source/**/target/` — nenhum arquivo de fonte foi tocado pelo maven (conferido: as únicas
três alterações fora de `target/` são o trabalho legítimo do executor do acesso remoto, em
`RemoteSessionHub.java`, `RemoteSupportResource.java` e `remote.controller.js`).

```bash
cd /opt/projetos/hwmdm/repo-mdm
git status --short server-source | grep "/target/" | awk '{print $2}' | xargs git checkout --
rm -f "server-source/server/target/classes/com/hmdm/remote/RemoteSessionHub\$EscritaEspectador.class" \
      "server-source/server/target/launcher/WEB-INF/classes/com/hmdm/remote/RemoteSessionHub\$EscritaEspectador.class"
git status --short server-source | grep -c "/target/"    # deve imprimir 0
```

Confira antes de rodar que a lista é só de `target/`:

```bash
git status --short server-source | grep -v "/target/"
```

Se aparecer qualquer coisa além dos três arquivos de fonte citados acima, **não rode** — me avise.
