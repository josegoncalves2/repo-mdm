# Itens 11 e 12 — a trava de entrega está instalada mas NÃO está impondo nada nesta sessão

## O que eu verifiquei, com comando

```
$ cd /opt/projetos/hwmdm/repo-mdm && ./trava status
  IMPOSIÇÃO: ATIVA
  PRODUTO:   sha256:a3301ad2d8e9e2b6a1ffb64df1310abb  (1188 arquivos)
  BANCADA:   nunca aberta — nenhum humano usou isto ainda
  AUDITORIA: cadeia íntegra: 26 registro(s)
```

O `./trava status` diz ATIVA porque lê o contrato em `.trava/contrato.json`. Ele **não**
verifica se o runtime que está rodando carregou os hooks. E não carregou.

```
$ test -f /opt/projetos/hwmdm/.claude/hooks/trava_gate.py && echo SIM || echo NAO
NAO

$ ls /opt/projetos/hwmdm/.claude/hooks/
trava-escopo.py        <- só este

$ ls /opt/projetos/hwmdm/repo-mdm/.claude/hooks/
trava_gate.py  trava_entrega.py  trava_sessao.py  trava_prompt.py
trava_pos.py   trava_stop.py     lib_trava.py     verificar_bancada.py  ...
```

Os hooks do TRAVA-KIT estão registrados em `repo-mdm/.claude/settings.json` apontando para
`$CLAUDE_PROJECT_DIR/.claude/hooks/…`. A **raiz de projeto desta sessão é
`/opt/projetos/hwmdm`**, não `repo-mdm`. Então `$CLAUDE_PROJECT_DIR/.claude/hooks/trava_gate.py`
resolve para um arquivo que não existe, e nenhum hook do kit dispara.

Prova indireta no próprio registro: os 26 eventos de `AUDITORIA.md` são todos de
`2026-09-23T12:13:44Z` — o autoteste da instalação, às 09:13 locais. Nada depois.
Esta sessão fez dezenas de chamadas de ferramenta e **nenhuma** aparece na auditoria.

## O que isso significa para o item 12

O pedido foi: "Deve ser registrado com token e não é passivel de ser burlado pelo agente."

Hoje não é burlável **por desenho** — é inerte **por instalação**. A diferença importa: o
mecanismo está certo, o ponto de montagem está errado.

## A correção — é sua, não minha

O Artigo 11 do `CONTRATO-DE-ENTREGA.md` proíbe ao agente escrever em `.claude/hooks/`,
`.claude/settings.json` e `.trava/`, e a trava de escopo também bloqueia `repo-mdm/.claude/`.
Eu não devo consertar isto, e não tentei. Um agente que ativa a própria trava não provou nada.

**Opção 1 (recomendada — uma ação, nenhum arquivo editado):** abra a sessão com o repositório
como raiz de projeto, em vez da pasta acima dele:

```bash
cd /opt/projetos/hwmdm/repo-mdm && claude
```

Isso basta porque `repo-mdm/.claude/settings.json` **já** registra as duas coisas:
os hooks do TRAVA-KIT (por `$CLAUDE_PROJECT_DIR`, que passa a resolver certo) e a trava de
escopo e a de produção (por caminho absoluto, que já funcionava). O `trava-escopo.py` tem
`PROJETO = "/opt/projetos/hwmdm"` cravado, então continua valendo com a raiz nova.

**Opção 2:** manter a raiz atual e montar o kit lá — copiar `repo-mdm/.claude/hooks/*.py` para
`/opt/projetos/hwmdm/.claude/hooks/` e fundir os blocos de hooks dos dois `settings.json`.
Mais passos, mais chance de errar, duas cópias para manter.

## Como você confere que passou a valer

Depois de reabrir, numa sessão nova:

```bash
cd /opt/projetos/hwmdm/repo-mdm
./trava doutor                  # deve listar os hooks registrados
./trava testar                  # prova que as travas travam de verdade
./trava auditoria -n 20         # devem aparecer eventos com a data de HOJE, não só 12:13:44Z
./trava auditoria --verificar   # cadeia íntegra
```

Se `./trava auditoria -n 20` continuar mostrando só os registros de 12:13:44Z depois de você
mandar o agente mexer em qualquer arquivo, o kit continua inerte.

## O que ainda falta, e não depende de instalação

`BANCADA: nunca aberta — nenhum humano usou isto ainda`. Enquanto isso for verdade, pelo
Artigo 3 nenhuma entrega deste change pode ser declarada ENTREGUE — incluindo tudo o que
está sendo corrigido agora nos itens 1 a 10.
