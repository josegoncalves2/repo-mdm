# Versionamento do HWMDM

Este documento explica, em português e de forma didática, como o HWMDM 1.0 numera
suas entregas. A base é o [Semantic Versioning 2.0.0](https://semver.org/lang/pt-BR/)
(SemVer), com as adaptações que este projeto precisa para lidar com branches de
feature, build automatizado e vários componentes internos com vida própria.

Se você só quer saber "que número eu escrevo aqui", vá direto à seção
**"Passo a passo de um lançamento"** no fim. O resto é o porquê.

---

## 1. A forma geral

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
```

Exemplo completo, de um build real de desenvolvimento:

```
1.0.0-beta.2+build.7.20260924.76f2734.sujo
```

- `1.0.0` — versão (MAJOR.MINOR.PATCH)
- `-beta.2` — pré-lançamento (opcional)
- `+build.7.20260924.76f2734.sujo` — metadados de build (opcional)

O HWMDM usa **um único número guarda-chuva** para a plataforma inteira — hoje
`1.0.0`, guardado em `VERSION` na raiz do repositório. É esse número que o
responsável quer ver na tela. Só ele sobe conforme as regras de MAJOR/MINOR/PATCH
abaixo. Os componentes internos (núcleo Headwind, plugins, agente Android,
resolvedor DNS) têm cada um sua própria versão — ver seção 7 — e não mexem no
número da plataforma quando mudam sozinhos.

---

## 2. MAJOR, MINOR e PATCH neste projeto

### MAJOR (o primeiro número) — quando algo quebra para quem já usa o HWMDM

Sobe quando uma mudança **quebra compatibilidade** com o que já existe: um
endpoint que os agentes Android chamam muda de contrato, um plugin deixa de
funcionar sem migração manual, uma tela deixa de existir e não há substituto,
um formato de configuração (ex.: o catálogo do webfilter) muda de estrutura e
os arquivos antigos param de carregar.

Exemplos reais possíveis neste projeto:
- Trocar o protocolo do acesso remoto (o `remoteSupport.service.js` e o
  `remote.controller.js` usam um esquema de sinalização hoje; se ele mudar de
  forma incompatível com agentes já instalados em campo) → `1.0.0` → `2.0.0`.
- Mudar a estrutura do `webfilter-catalog.json` de forma que catálogos antigos
  parem de carregar sem conversão → MAJOR.
- Remover um módulo inteiro do menu lateral sem substituto equivalente → MAJOR.

Enquanto o projeto estiver em `1.x.y`, a promessa é: um administrador que
aprendeu a operar o HWMDM 1.0 continua operando qualquer 1.y.z sem re-treino
nem migração manual.

### MINOR (o segundo número) — quando algo novo aparece sem quebrar nada

Sobe quando se adiciona funcionalidade **compatível com o que já existe**: uma
tela nova, um filtro novo no webfilter, um campo novo em um formulário que
antes não existia, o rodapé de versão que este próprio pacote de trabalho
está construindo.

Exemplos reais deste projeto:
- Este pacote "versao" — adicionar controle de versão/build visível na
  interface, sem tirar nada que já funcionava → `1.0.0` → `1.1.0`.
- Adicionar um painel de administração para o webfilter (pedido futuro
  plausível, dado que o filtro já existe no servidor) → MINOR.
- Adicionar suporte a um novo tipo de dispositivo no MDM sem alterar o
  comportamento dos existentes → MINOR.

Ao entrar em desenvolvimento, o MINOR seguinte é reservado desde o primeiro
commit da feature, como pré-lançamento (seção 3).

### PATCH (o terceiro número) — quando é conserto, sem funcionalidade nova

Sobe para correções de bugs, ajustes de segurança, correções de texto/tradução,
performance, sem adicionar nem remover comportamento visível.

Exemplos reais deste projeto (achados literalmente na base de código hoje):
- O "bug de hint no APK" mencionado na memória do projeto (pendente) — quando
  corrigido, é PATCH: `1.0.0` → `1.0.1`.
- Corrigir o teclado do IP/MSE no acesso remoto (já corrigido em 24-09,
  segundo a memória do projeto) teria sido PATCH.
- Corrigir um texto errado no diálogo "About" → PATCH.

### Resumo em uma frase por número

| Muda | Quando |
|---|---|
| MAJOR | quebra algo que já funcionava para quem já usa o HWMDM |
| MINOR | adiciona algo novo sem quebrar o que já existia |
| PATCH | conserta algo, sem adicionar nem remover comportamento |

---

## 3. Pré-lançamento: `-alpha.N`, `-beta.N`, `-rc.N`

Um pré-lançamento marca uma versão que **ainda não está pronta para produção**.
Ele vem depois do PATCH, com um hífen, e o SemVer garante que ele conta como
**anterior** à versão final equivalente (ver a tabela de precedência na
seção 5).

Estágios usados neste projeto, do mais cru ao mais maduro:

1. **`-alpha.N`** — em construção ativa. Pode faltar pedaço, pode estar
   quebrado. Usado durante o desenvolvimento inicial de uma feature.
   Exemplo: `1.1.0-alpha.1` (primeiro commit do painel do webfilter).
2. **`-beta.N`** — funcionalidade completa, em teste. É o estágio em que este
   próprio pacote de trabalho normalmente entrega para a bancada humana
   (ver `CONTRATO DE EXECUÇÃO`: a entrega final depende de teste humano).
   Exemplo: `1.1.0-beta.1` (painel do webfilter pronto, aguardando quem senta
   e usa).
3. **`-rc.N`** ("release candidate") — candidato a lançamento, sem mudanças de
   comportamento esperadas, só validação final antes de virar `1.1.0` de
   verdade. Exemplo: `1.1.0-rc.1`.

`N` é um contador que começa em `1` e sobe a cada novo pré-lançamento do mesmo
estágio (`-beta.1`, `-beta.2`, ...). Nunca reaproveite `N` depois de descartado.

---

## 4. Trabalho de feature: branch e pré-lançamento com identificador da feature

Cada feature nova nasce em uma branch `feature/<slug>`, onde `<slug>` é um nome
curto, em minúsculas, com hífen — por exemplo `feature/webfilter-painel` para
um painel de administração do webfilter, ou `feature/versao-rodape` para este
próprio pacote de trabalho.

O SemVer exige que cada identificador de pré-lançamento (cada pedaço entre
pontos, depois do hífen) contenha **só** `[0-9A-Za-z-]` — sem underscore, sem
espaço, sem ponto dentro do identificador, sem barra. Por isso o slug da
branch, que pode ter `/`, não entra direto na versão: ele vira um identificador
próprio dentro do pré-lançamento, e a barra do nome da branch simplesmente não
aparece na versão (ela mora só no nome da branch git).

Formato usado neste projeto para pré-lançamento de feature:

```
<MINOR-alvo>-feature.<slug>.<N>
```

Exemplo real: uma branch `feature/webfilter-painel` que mira a versão `1.1.0`
tem seus commits de desenvolvimento marcados como:

```
1.1.0-feature.webfilter-painel.1
1.1.0-feature.webfilter-painel.2
1.1.0-feature.webfilter-painel.3
```

Cada identificador (`feature`, `webfilter-painel`, `3`) é validado pela regra
do SemVer — `webfilter-painel` só usa letras, números e hífen, então é válido.
Um slug como `webfilter_painel` (com underscore) **não seria** SemVer válido e
não deve ser usado.

Quando a feature amadurece, ela passa a usar os estágios da seção 3
(`-alpha`, `-beta`, `-rc`) até virar a versão final sem sufixo.

---

## 5. Metadados de build: `+build.<N>.<AAAAMMDD>.<commit7>[.sujo]`

Depois da versão (e do pré-lançamento, se houver), um `+` introduz os
metadados de build — informação **sobre como aquele artefato específico foi
gerado**, sem afetar qual versão ele é.

Formato:

```
+build.<N>.<AAAAMMDD>.<commit7>[.sujo]
```

- `<N>` — o número de compilação, sequencial, gerado por
  `docs/versao/gerar-build-info.py` (ver seção 6). Nunca reinicia, nunca some.
- `<AAAAMMDD>` — data da geração do build, UTC, sem separadores.
- `<commit7>` — os 7 primeiros caracteres do hash do commit git no momento do
  build (`git rev-parse --short=7 HEAD`).
- `.sujo` — presente **somente** se a árvore de trabalho tinha alterações não
  commitadas no momento do build (`git status --porcelain` não vazio). Ajuda a
  distinguir "isto é exatamente o commit X" de "isto é o commit X mais
  coisas soltas por cima" — crítico neste projeto, onde vários agentes mexem
  no DEV ao mesmo tempo (ver memória "Agentes concorrentes").

Exemplo real gerado por este pacote de trabalho:

```
1.0.0+build.1.20260924.76f2734.sujo
```

Significa: versão `1.0.0`, primeiro build já contado pelo contador do
projeto, gerado em 24/09/2026, a partir do commit `76f2734`, com a árvore de
trabalho suja (havia alterações não commitadas quando o build foi gerado —
o que é o caso normal em DEV).

### "Versão", "compilação" e "build": três coisas diferentes

É fácil confundir os três termos. Aqui eles significam coisas específicas:

- **Versão** — `1.0.0` (ou `1.1.0-beta.2`). O que o SemVer numera. Muda com
  intenção: alguém decide que a próxima entrega é MAJOR, MINOR ou PATCH.
- **Compilação** (`compilacao` no `build-info.json`) — uma string
  `AAAAMMDD.N`, por exemplo `20260924.1`. É a "ficha do dia": em que data e
  em qual posição do contador aquele artefato foi gerado. Não é SemVer, é só
  um identificador de auditoria local.
- **Build** — o **número** `N` sozinho (campo `build` no `build-info.json`),
  monotônico, guardado em `docs/versao/contador-build.txt`, e que nunca zera
  nem decresce, mesmo trocando de versão ou de branch. É o "build 7", ponto.
  O **"semver_completo"** é a string inteira, versão + metadados de build,
  que carrega esse `N` dentro de si (`+build.7...`).

Ou seja: uma mesma **versão** (`1.0.0`) pode ter vários **builds**
(`build.1`, `build.2`, `build.3`, ...) gerados enquanto ninguém decide lançar
uma versão nova — cada `docker cp` de teste no DEV pode virar um build novo.

---

## 6. Precedência: metadados de build NÃO contam

O SemVer é explícito: ao comparar duas versões para saber qual é "maior"
(mais nova), **os metadados de build depois do `+` são ignorados**. Só
versão e pré-lançamento contam.

Exemplos de precedência, do menor para o maior:

```
1.0.0-alpha.1
  <
1.0.0-alpha.2
  <
1.0.0-beta.1
  <
1.0.0-rc.1
  <
1.0.0                          (o lançamento "de verdade")
  <
1.1.0-feature.webfilter-painel.1
  <
1.1.0-beta.1
  <
1.1.0
```

E o ponto que costuma confundir:

```
1.0.0+build.1.20260924.76f2734        ==  1.0.0+build.9.20260930.ab12cd3
```

**São a mesma versão**, para efeito de precedência SemVer — dois builds
diferentes do mesmo `1.0.0`. Isso é esperado e correto: o metadado de build
não é usado para decidir "qual é mais novo" entre lançamentos; ele só registra
como aquele artefato específico foi produzido. Quem precisa saber "qual dos
dois builds é mais recente" olha `<N>` e a data dentro do `+build...`, não a
precedência SemVer.

Isso também significa: **nunca** use os metadados de build para decidir se
um deploy é seguro ou compatível — use a versão e, se houver, o
pré-lançamento.

---

## 7. Tags git

Uma tag anotada `vMAJOR.MINOR.PATCH` (por exemplo `v1.0.0`) marca um
**lançamento**, isto é, um commit específico que corresponde exatamente
àquela versão. Regras:

- Só se cria tag para uma versão sem metadados de build (nunca
  `v1.0.0+build.7...` — a tag já *é* um ponto fixo no histórico, não precisa
  do build para se identificar).
- Pré-lançamentos podem ganhar tag também (`v1.1.0-beta.1`), se o time quiser
  marcar um ponto de teste formal — mas isso é opcional; builds de
  desenvolvimento no DEV normalmente não geram tag nenhuma.
- A tag é sempre **anotada** (`git tag -a`, com mensagem), nunca leve
  (`git tag` sem `-a`), para carregar autor e data do lançamento.
- Criar e enviar tags está fora do alcance de agentes neste projeto — é
  comando que **o humano roda** (ver "Ambiente" no pacote de trabalho e
  `CONTRATO-DE-ENTREGA.md`).

---

## 8. Tabela de componentes e como cada um se relaciona com a plataforma

O HWMDM 1.0 é uma plataforma que empacota um núcleo de terceiros mais vários
componentes próprios. Cada um versiona de forma independente; a versão da
**plataforma** (`VERSION`, hoje `1.0.0`) é a que aparece para o usuário final
no rodapé e no About — ela é a que importa para "o que o responsável quer
ver". As versões de componente aparecem como detalhe técnico, dentro do
painel de build (clique na versão no rodapé) e no `build-info.json`.

| Componente | Versão hoje | Onde mora o número | Relação com a plataforma |
|---|---|---|---|
| **Plataforma HWMDM** | `1.0.0` | `VERSION` (raiz do repo) | É a guarda-chuva. Sobe conforme MAJOR/MINOR/PATCH definidos na seção 2, considerando o conjunto de tudo que o HWMDM entrega (console, agente remoto, webfilter, etc.), não um componente isolado. |
| Núcleo Headwind MDM (upstream) | `5.39.2` | `REPO/dist/hmdm.war` (`hmdm-5.39.2-os.war`); `APP_VERSION` em `app/app.js` | Base de terceiros sobre a qual o HWMDM é construído. Sua versão só muda quando se importa uma nova versão do upstream — não acompanha o número da plataforma. |
| Plugin `webfilter` | `0.1.0` | `server-source/plugins/webfilter/pom.xml` | Módulo próprio do HWMDM. Ainda em `0.x`, ou seja, API interna pode mudar sem aviso formal (regra do próprio SemVer para `0.x`). Quando o webfilter estabilizar sua API, vira `1.0.0` por conta própria — independente da plataforma já estar em `1.x`. |
| Plugin `moduleregistry` | `0.1.0` | `server-source/plugins/moduleregistry/pom.xml` | Mesma lógica do webfilter: módulo próprio, ainda `0.x`. |
| Agente Android `com.hwmdm.remote` | `versionName 1.20` / `versionCode 21` | `remote-agent/app/build.gradle` | Versionamento próprio do Android (`versionName` livre, `versionCode` inteiro sempre crescente exigido pelo Android para permitir upgrade). Não segue o SemVer da plataforma porque responde a uma esteira de publicação diferente (instalação manual/QR nos tablets, não deploy web). |
| Resolvedor DNS (`webfilter-dns`) | `0.34.0` | tag da imagem Docker `hwmdm/webfilter-dns:0.34.0` | Serviço auxiliar do webfilter, com ciclo de release próprio (imagem Docker). Ainda `0.x`. |

Quando a plataforma sobe de versão (por exemplo `1.0.0` → `1.1.0`), isso **não**
força nenhum componente da tabela a mudar de número — só força se aquele
componente específico teve mudança de comportamento na entrega. O
`CHANGELOG.md` é onde se registra, a cada versão da plataforma, quais
componentes mudaram e para qual número.

---

## 9. Passo a passo de um lançamento

Comandos que **o humano roda** (agentes neste projeto não têm permissão de
commit, tag nem push — ver `CONTRATO-DE-ENTREGA.md`):

1. Decidir o próximo número (MAJOR, MINOR ou PATCH) usando a seção 2 e
   conferir que tudo que for MAJOR está documentado no `CHANGELOG.md`.

2. Atualizar `VERSION` na raiz do repositório com o novo número, sem `v` na
   frente e sem espaço/linha extra:
   ```bash
   printf '%s' '1.1.0' > VERSION
   ```

3. Mover as entradas de `[Unreleased]` do `CHANGELOG.md` para uma nova seção
   `## [1.1.0] - AAAA-MM-DD`, seguindo o formato Keep a Changelog já usado no
   arquivo.

4. Gerar um build final não sujo (commitar tudo antes, senão o `.sujo` fica
   gravado no build de lançamento):
   ```bash
   git add -A
   git commit -m "release: 1.1.0"
   python3 docs/versao/gerar-build-info.py --carimbar-index
   ```
   Isso grava `build-info.json`, incrementa o contador de build e reescreve
   os `?v=...` do `index.html` para invalidar cache de navegador.

5. Conferir visualmente no DEV (rodapé + About, claro e escuro) antes de
   assinar.

6. Commitar o `build-info.json` e o `index.html` carimbados:
   ```bash
   git add server-source/server/src/main/webapp/build-info.json \
           server-source/server/src/main/webapp/index.html \
           docs/versao/contador-build.txt
   git commit -m "build: carimbar release 1.1.0"
   ```

7. Criar a tag anotada, sem metadados de build:
   ```bash
   git tag -a v1.1.0 -m "HWMDM 1.1.0"
   ```

8. Publicar (push da branch e da tag) — só depois de revisão humana:
   ```bash
   git push origin <branch>
   git push origin v1.1.0
   ```

9. Publicar os arquivos estáticos atualizados no DEV/PROD conforme o processo
   de deploy do projeto (`docker cp`, ver "Ambiente" no pacote de trabalho
   deste README/CLAUDE.md) — nunca reiniciar o container, nunca recompilar o
   WAR como parte deste passo a passo de versionamento.

Regeração de build **sem** consumir número (por exemplo, para só reler
`build-info.json` depois de trocar algo manualmente, sem gerar um build
novo):
```bash
python3 docs/versao/gerar-build-info.py --sem-incremento
```
