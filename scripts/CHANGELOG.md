# Changelog do hwmdm (repo-mdm)

Fica em `scripts/` porque a raiz do repositorio esta fora do escopo de escrita
autorizado para quem criou este arquivo (ver `output/relatorio-C.md` da tarefa C
em `openspec/changes/corrigir-12-itens/`). **Recomendacao para o responsavel:**
mover este arquivo para a raiz do repositorio como `CHANGELOG.md` assim que
`ESCOPO-AUTORIZADO.txt` permitir.

## Regra de como manter

1. Toda vez que `scripts/build.sh <alvo>` produzir um artefato que vai ser
   **publicado de verdade** (via `scripts/publicar-apk.sh` ou copiando
   `dist/hmdm.war` para o lugar certo), adicione uma linha aqui, na secao
   `## Nao lancado` no topo, com: data (UTC), alvo, versao, commit curto e uma
   frase do que mudou. Os quatro primeiros dados saem prontos do manifesto que
   `scripts/build.sh` grava em `scripts/builds/<data-hora>-<alvo>.json` --
   copie de la, nao digite de cabeca.
2. Quando o conteudo de `## Nao lancado` corresponder a uma entrega real para o
   cliente, renomeie a secao para a data da entrega e abra uma nova
   `## Nao lancado` vazia.
3. Isto e' HISTORICO HUMANO, legivel em uma tela. O rastro tecnico exato
   (sha256, ferramentas usadas, arvore suja ou nao) mora em
   `scripts/builds/*.json` e e' consultado por `scripts/versao-publicada.sh` --
   nao duplique esses detalhes aqui, so' referencie o arquivo do manifesto se
   for relevante.
4. Nunca apague uma entrada. Se algo publicado foi revertido, adicione uma
   entrada nova dizendo isso -- nao edite a entrada antiga.

## Nao lancado

- 2026-09-23T13:25:13Z -- agente-remoto 1.19 -- `7edfabcb+sujo` -- corrige digitacao remota em campo focado, busca janelas de acessibilidade e publica `dist/hwmdm-remote-1.19.apk` (`scripts/builds/20260923-132522-agente-remoto.json`).
- 2026-09-23T13:33:15Z -- launcher 1.1 -- `7edfabcb` -- aplica WebFilter como politica real de DNS privado, politica de keyguard e publica `dist/hmdm-v1.1.apk` (`scripts/builds/20260923-133328-launcher.json`).
- 2026-09-23T13:35:17Z -- plugin-webfilter -- `7edfabcb+sujo` -- recompila o hook de sincronizacao do WebFilter que entrega/limpa `webfilterDnsHost` para o launcher (`scripts/builds/20260923-133517-plugin-webfilter.json`).
- 2026-09-23T13:35:26Z -- console 5.39.2 -- `7edfabcb+sujo` -- carimba as mudancas de UI do acesso remoto e da tela Modulos sem recompilar o nucleo do WAR (`scripts/builds/20260923-133529-console.json`).

## Historico reconstruido do `git log` (antes deste changelog existir)

Este bloco cobre os 68 commits do repositorio ate 2026-09-22, agrupados por dia,
como evidencia de que da' para reconstruir o passado sem o changelog -- e como
prova de que, dai em diante, so' o changelog conta.

- **2026-09-21/22 -- primeiro Web Filter.** `8f04bafe` "primeiro webfilter
  21-09-26", `7273b9e9`, `fe41f221` "dia 22-09-26", `7edfabcb` "fim do dia
  22-09-26". Commits com mensagem fraca (".", "dia X") -- exatamente o tipo de
  rastro que este changelog deveria ter evitado.
- **2026-09-17/18 -- correcoes criticas e webfilter inicial.** `b7ba9474`
  responsividade/permissoes granulares/seguranca de APK; `9852d24b` fix erros
  gerais; `c1433e71` primeiro webfilter.
- **2026-09-11 -- hotfix de permissoes.** `73f04bb8`.
- **2026-08-11 -- ultimo commit antes do hiato de um mes; e' o build que esta
  em producao no DEV hoje (ver aviso em `scripts/build.sh` sobre o alvo
  `nucleo-perigoso`).** `c6c6b1c4` manual de matricula em PDF (exclui do git 3
  APKs grandes adicionados por engano); `7713cf30` registra o incidente de
  10/08 (settings zerado por PATCH parcial); `b747f6b2`.
- **2026-08-10 -- dia de maior volume: acesso remoto, branding, DEVICE IP.**
  16 commits, entre eles: DEVICE IP passa a ser o endereco do proprio
  aparelho (`07474f02`), agente 1.6/1.7/1.12/1.15 (consentimento, tela acesa,
  diagnostico de acessibilidade, servico junto com acessibilidade),
  logotipo e cores do painel finalmente salvando (`a30167d6`, `390df125`),
  protecao das Configuracoes (`12f2ae30`, `519189ae`), publicacao do
  companion 1.17 (`f641998a`).
- **2026-08-05/07 -- acesso remoto ao vivo, backup/restauracao, GPS.**
  `5425ebf4` acesso remoto ao vivo (video, clique, digitacao); `e33c3fd0`
  backend de backup/restauracao; `73a34829` GPS e atividade recente no
  dashboard/mapa; `dd362a47` fixa nome do projeto Compose (evita clone novo
  derrubar producao -- historico do mesmo tipo de incidente que motivou a
  tarefa C); `f2997f6d` versiona producao por completo (volumes, .env, APKs,
  dump do banco).
- **2026-08-06 -- release do launcher.** `748b3057` "Release APK v6.37.6".
- **2026-08-03/04 -- reforma do painel.** Titulos alinhados ao menu,
  historico de navegacao, tema escuro, kiosk/mensagens/relatorios/perfil.
- **2026-07-24/26 -- inicio do fork versionado.** Remocao de IPs/segredos
  hardcoded, provisionamento portatil (painel versionado, APK assinado, setup
  por IP/DNS), QR code.

## Por que nao existia isto antes

`openspec/changes/corrigir-12-itens/` (item 10) registra a reclamacao literal
do responsavel: sete scripts de build soltos na raiz, nenhum caminho oficial,
nenhum artefato em `dist/` com identificacao de build, e um incidente real em
2026-09-21 em que `dist/hmdm.war` foi substituido por um backup antigo sem
ninguem perceber ate o filtro sumir. `scripts/versao-artefato.py`,
`scripts/build.sh` e `scripts/versao-publicada.sh` (todos desta mesma tarefa)
resolvem a causa raiz; este arquivo resolve o sintoma de "ninguem sabe o que
mudou entre uma versao e outra".
