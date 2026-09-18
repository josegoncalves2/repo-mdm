# 02 - Ciclo da microtarefa

Este documento define o rito obrigatorio para executar cada item de `../tasks.md`. Ele nao inicia implementacao; serve para o FO, FQ e executores conduzirem o desenvolvimento quando o humano autorizar.

## Principio central

Uma microtarefa so e aceita quando existem tres provas independentes:

1. **Construcao correta:** testes de construcao falharam antes e passaram depois.
2. **Conformidade fiscal:** FQ reexecutou gates, leu diff, escopo e atalhos.
3. **Uso real:** uma pessoa consegue perceber o resultado em console/tablet/resolvedor, conforme `03-provas-de-uso-real.md`.

`curl`, endpoint online, container "up", porta aberta, healthcheck, log ou teste unitario isolado podem apoiar uma evidencia, mas nunca aceitam sozinhos uma microtarefa.

## Baseline B0-B3

Antes da primeira microtarefa, o FO deve emitir OS de ambiente para registrar a linha de base.

### B0 - Identidade do alvo

Registrar em `../fiscal/estado.md`:

- change: `add-webfilter-module`;
- raiz do planejamento: `C:/Users/40446686808/projetos/MDM`;
- raiz do produto: `C:/Users/40446686808/projetos/MDM/repo-mdm`;
- URL de homologacao;
- conta administrativa de teste;
- tablet fisico piloto, serial ADB, Android, versao do launcher;
- regra explicita: producao nao e alvo.

### B1 - Gates de planejamento

FO roda:

```powershell
python C:/Users/40446686808/projetos/MDM/.agents/skills/openspec-fiscal/scripts/openspec_cli.py --cwd C:/Users/40446686808/projetos/MDM validate add-webfilter-module --strict --json
```

Aceite: `valid: true` e `issues: []`. Falha bloqueia qualquer OS.

### B2 - Linha de base de uso diario

EJ executa J00 em `03-provas-de-uso-real.md`. FQ confere que o MDM atual ainda funciona antes do WebFilter:

- login no console;
- lista de dispositivos abre;
- perfil existente abre;
- tablet sincroniza;
- app comum abre;
- navegador resolve dominio permitido;
- acesso remoto/GPS/kiosk nao sofreram regressao visivel no fluxo escolhido.

### B3 - Snapshot fiscal

FO registra snapshot antes de despachar qualquer executor:

```powershell
python C:/Users/40446686808/projetos/MDM/.agents/skills/openspec-fiscal/scripts/fiscal_guard.py snapshot --root C:/Users/40446686808/projetos/MDM/repo-mdm --out $env:TEMP/fiscal-probes/add-webfilter-module/baseline.json
```

O caminho de saida deve ser um caminho real do sistema. Caminho de exemplo no ledger invalida o registro.

## Ciclo E0-E14

### E0 - Selecionar microtarefa

FO escolhe uma tarefa PENDENTE cujas dependencias estejam ACEITAS.

Para fan-out, nao ha limite documental de quantidade de executores. O FO pode convocar centenas de executores para trabalhos de teste, implementacao isolada, probes, revisao, documentacao, preparacao de ambiente ou analise.

Dependencias e escopos sobrepostos nao bloqueiam a convocacao. Eles apenas bloqueiam a integracao e o aceite ate que o FQ valide a sequencia correta. Quando houver risco de colisao, a OS deve exigir worktree, branch, pasta isolada ou entrega em patch separado.

Pedido explicito de quantidade pelo humano (ex.: 400 executores) vale como confirmacao operacional. O FO nao pergunta novamente quantos executores criar, nao pede nova autorizacao para o ciclo padrao e, havendo sub-agente disponivel, cria FQ automatico como sub-agente real.

429/rate limit durante o despacho e tratado como transitorio: registrar no ledger, reduzir lote, aguardar/backoff quando possivel e continuar a fila ate atingir a quantidade pedida ou ate a ferramenta retornar falha terminal.

Se nenhuma sub-agente executor ou FQ foi criado de fato, o FO nao pode marcar tarefa como EM_TESTES, EM_IMPLEMENTACAO, EM_JORNADA, CONSTRUIDA ou ACEITA. Registros que afirmem progresso sem agente real sao invalidos.

### E1 - Montar OS-T

FO escreve Ordem de Servico de teste em `../fiscal/os/<tarefa>-ET-t<n>.md`.

A OS-T deve conter:

- texto literal da tarefa de `tasks.md`;
- requisitos e cenarios literais de `specs/**` que a tarefa cobre;
- decisoes do `design.md` aplicaveis;
- escopo permitido;
- escopo protegido;
- fronteiras externas que podem ser mockadas;
- comando vermelho esperado;
- proibicoes;
- formato de relatorio.

### E2 - Despachar ET

FO cria um Executor de Testes novo. O ET recebe o prompt da secao 6.3 de `01-papeis-e-prompts.md` e a OS-T.

ET escreve somente testes. Se tocar producao, codigo de produto fora de teste ou `openspec/**`, a tentativa e reprovada.

### E3 - Pedido ao FQ: TESTES

FO cria `../fiscal/fila/para-fq/<tarefa>-TESTES-t<n>.md`, apontando:

- OS-T;
- relatorio do ET;
- snapshot anterior;
- arquivos de teste alterados;
- comando que deve falhar.

### E4 - Auditoria A1: testes de construcao

FQ valida:

- teste esta no escopo;
- teste cobre cada WHEN/THEN com assert que morde;
- teste falha pelo motivo certo;
- nao usa skip, mock do comportamento, sleep cego, fixture que ja implementa a resposta ou rota interceptada;
- falha nao e import quebrado, ambiente ausente ou erro de sintaxe.

Veredito possivel:

- `VERMELHO_CONFIRMADO`;
- `REPROVADA`;
- `BLOQUEADA`.

### E5 - Montar OS-I

Com `VERMELHO_CONFIRMADO`, FO escreve OS-I para um Executor de Implementacao novo.

A OS-I deve incluir:

- OS-T resumida por referencia de arquivo, sem alterar criterios;
- lista de testes protegidos que EI nao pode editar;
- escopo permitido de producao;
- arquivos de referencia do projeto real;
- comando verde esperado;
- gates de regressao da fatia;
- riscos de uso real que a implementacao deve respeitar.

### E6 - Despachar EI

EI implementa no escopo. Se precisar mudar teste de aceite, spec, design ou escopo, deve parar e relatar bloqueio.

### E7 - Pedido ao FQ: CONSTRUCAO

FO cria `../fiscal/fila/para-fq/<tarefa>-CONSTRUCAO-t<n>.md`, com:

- OS-I;
- relatorio do EI;
- snapshot antes do EI;
- lista de arquivos alterados;
- comando verde alegado.

### E8 - Auditoria A2: construcao

FQ executa, no minimo:

```powershell
python <skill>/scripts/fiscal_guard.py diff --snapshot <snapshot> --allow <globs da OS> --protect "openspec/**"
python <skill>/scripts/fiscal_guard.py scan --snapshot <snapshot>
```

Depois reexecuta comandos da OS e gates de regressao aplicaveis.

FQ tambem le o diff e procura:

- placeholder, TODO/FIXME, stub, codigo morto;
- segredo ou URL/IP/porta chumbados;
- catch vazio;
- teste afrouxado;
- alteracao fora do escopo;
- regressao no MDM existente;
- divergencia contra `proposal.md`, `design.md` ou specs.

Veredito possivel:

- `CONSTRUIDA`;
- `REPROVADA`;
- `BLOQUEADA`.

### E9 - Preparar jornada da fatia

Quando todas as tarefas de construcao necessarias para uma jornada estiverem `CONSTRUIDA`, FO emite OS-A para EA preparar homologacao.

EA nao codifica. Ele so publica em homologacao quando a OS permitir e registra versoes, hashes e dados criados pela tela.

### E10 - Auditoria do ambiente

FQ confirma:

- alvo e homologacao;
- versoes publicadas correspondem ao build esperado;
- dados de teste foram criados como usuario quando a tela existir;
- nenhum passo tocou producao.

Sem isso, nenhuma OS-J e despachada.

### E11 - Executar jornada de uso real

FO emite OS-J para EJ, copiando a jornada aplicavel de `03-provas-de-uso-real.md`.

EJ deve agir como pessoa real:

- console por tela;
- tablet por toques/entrada normal;
- Playwright apenas como robo usando tela;
- ADB apenas para interacao fisica guiada e coleta complementar.

Proibido para aceite de uso real:

- `curl` como prova principal;
- `page.request`, `page.route`, token injetado;
- update direto em banco;
- abrir tela interna por intent que usuario nao acessa;
- emulador substituindo tablet fisico quando a jornada pede tablet.

### E12 - Auditoria A3: uso real

FQ revisa video, capturas e diario da jornada. Quando necessario, reexecuta partes no console/tablet.

FQ aprova somente se cada constatacao exigida estiver visivel para uma pessoa, nao apenas inferida de log.

Quando o FQ for sub-agente do FO, a auditoria A3 de console/tablet exige J13 assinado por humano antes de `ACEITA`.

### E13 - Aceite e checkbox

Com parecer `ACEITA`, FO:

- atualiza `estado.md`;
- registra evidencia no `ledger.md`;
- marca somente a linha correspondente em `../tasks.md`;
- tira novo snapshot.

### E14 - Devolucao e loop

Se FQ reprovar, FO emite Devolucao ao executor adequado.

Devolucao deve conter:

- defeito numerado;
- evidencia;
- comportamento esperado;
- criterio de reaprova;
- se volta ao mesmo executor ou a executor novo.

A mesma falha tres vezes exige executor novo. Cinco reprovas na mesma microtarefa exigem escalonamento ao humano.

Nao sao motivos de escalonamento: quantidade 400 ja solicitada; ausencia de FQ humano quando a ferramenta permite criar sub-agente FQ; necessidade de autorizar TESTES -> CONSTRUCAO -> USO_REAL -> ACEITA; 429/rate limit transitorio.

## Modelos

### M1 - Ordem de Servico

```markdown
# OS <tarefa> - <papel> - tentativa <n>

## Identidade
- Change: add-webfilter-module
- Tarefa: <id e texto literal>
- Papel: ET | EI | EA | EJ
- Estado de origem:
- Snapshot:

## Requisitos e cenarios literais
<copiar de specs>

## Decisoes de design aplicaveis
<copiar trechos exatos ou referencias linha/arquivo>

## Escopo PERMITIDO
<globs>

## Escopo PROTEGIDO
- openspec/**
- producao
- arquivos fora do escopo

## Comandos de aceite
<comandos>

## Prova de uso real relacionada
<Jxx e constatacoes>

## Definicao de pronto
<itens verificaveis>

## Proibicoes
<lista especifica>

## Relatorio obrigatorio
<modelo do papel>
```

### M5 - Devolucao

```markdown
# Devolucao <tarefa> - tentativa <n>

**Veredito:** REPROVADA | BLOQUEADA

| # | Defeito | Evidencia | Esperado | Criterio de reaprova |
|---|---|---|---|---|

## Instrucao ao proximo executor
<manter escopo; destacar defeitos recorrentes>
```

### M6 - Parecer do FQ

```markdown
# Parecer FQ - <tarefa> - <tipo> - tentativa <n>

## Evidencias verificadas
| Item | Evidencia | Resultado |
|---|---|---|

## Analise
- Escopo:
- Atalhos:
- Testes:
- Regressao:
- Uso real:
- Conformidade:

## Veredito
VERMELHO_CONFIRMADO | CONSTRUIDA | ACEITA | REPROVADA | BLOQUEADA

## Devolucao
<preencher se reprovada/bloqueada>
```

### M8 - Painel de progresso

```markdown
## WebFilter - painel fiscal
**Fatia:** <A-J>
**Aprovadas:** <n>/<total>
**Executores ativos:** <n>
**Bloqueios:** <n>

| Tarefa | Estado | Tentativa | Executor | Ultimo parecer |
|---|---|---|---|---|
```
