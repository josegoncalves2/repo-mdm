# 02 — Ciclo de cada tarefa

Variáveis usadas abaixo:

```bash
CH=openspec/changes/corrigir-permissoes-e-console     # relativo a repo-mdm/
G="python3 $CH/fiscal/guarda.py"
EV=/opt/projetos/hwmdm/evidencias/corrigir-permissoes-e-console
```

## Abertura (uma vez)

1. O FISCAL lê `README.md`, `01-prompts.md`, `02-ciclo.md`, `03-jornadas.md`, `../proposal.md`, `../tasks.md` e `../fiscal/estado.md`. Se `estado.md` já tiver tarefas em andamento, retoma dali.
2. `$G verificar-ledger`. Se falhar, PARE e escale: o ledger foi adulterado.
3. `$G registrar "abertura de sessão fiscal"`.
4. A primeira tarefa é sempre a **0.1**. Nenhuma tarefa de código começa sem 0.1 `ACEITA`, porque é ela que define o que "não pode desaparecer".

## Ciclo C0–C12

| Etapa | Quem | O quê | Saída em disco |
|---|---|---|---|
| **C0** Selecionar | FISCAL | tarefa PENDENTE com dependências ACEITAS; tarefa de tablet sem tablet no DEV → BLOQUEADA | `estado.md` |
| **C1** Emitir OS | FISCAL | OS pelo modelo M1, com o texto **literal** da tarefa e da jornada, e escopo em globs | `fiscal/os/<id>-EXECUTOR-t<n>.md` |
| **C2** Snapshot | FISCAL | `$G snapshot <id> <n> --allow <globs da OS>` | `$EV/<id>/t<n>/snapshot/` |
| **C3** Executar | EXECUTOR (sub-agente novo) | investiga e corrige no escopo; relatório M2 | `$EV/<id>/t<n>/relatorio-executor.md` |
| **C4** Guarda | FISCAL | `$G diff <id> <n>`; saída ≠ 0 → devolução imediata, sem ir ao VALIDADOR | `$EV/<id>/t<n>/diff.md` |
| **C5** Aplicar no DEV | AMBIENTE (sub-agente novo) | OS M1 papel AMBIENTE; aplica, confere que o DEV subiu pela **tela de login**, registra hashes | `$EV/<id>/t<n>/relatorio-ambiente.md` |
| **C6** Jornada | USUÁRIO (sub-agente novo) | OS M1 papel USUÁRIO com a jornada literal; vídeo, capturas, diário, `inventario-pos.md` | `$EV/<id>/t<n>/jornada/` |
| **C7** Manifesto | FISCAL | `$G manifesto $EV/<id>/t<n>` | `MANIFESTO.sha256` |
| **C8** Pedido de validação | FISCAL | arquivo M3 em `fila/para-validador/`, só com caminhos, **sem opinião** | `fiscal/fila/para-validador/<id>-t<n>.md` |
| **C9** Validar | VALIDADOR (sub-agente novo) | auditoria V1–V6; parecer M4 | `fiscal/pareceres/<id>-t<n>.md` |
| **C10** Assinatura | FISCAL → HUMANO | FISCAL regera `$G manifesto $EV/<id>/t<n>` (agora com `validador/`) e abre `fiscal/escalonamentos/<id>-assinatura.md` com o vídeo principal e o sha256; o humano assiste e assina (M5) | `fiscal/assinaturas/<id>.md` |
| **C11** Aceite | FISCAL | `$G aceite --tarefa <id>` sem erro → marca `[x]`; `$G registrar "..."` | `tasks.md`, `estado.md`, ledger |
| **C12** Devolução | FISCAL | parecer REPROVADA → devolução M6 e volta a C1 com `t<n+1>` | `fiscal/devolucoes/<id>-t<n>.md` |

Tarefas **só de observação** (0.1, 0.2, 2.1, 3.1) pulam C2–C5 quando nenhum arquivo do repositório muda. A 2.1 muda `docs/PERMISSOES.md` e, por isso, passa por C2–C4. Nenhuma tarefa pula C6–C11, exceto a 3.1, que não tem jornada: o VALIDADOR confere a matriz contra o console e o humano aprova.

Depois de C11, o FISCAL atualiza `estado.md` (painel M7) e segue para a próxima tarefa. Tarefas independentes podem correr em paralelo (ex.: 1.1 e 2.1), **cada uma com seus próprios sub-agentes**. Duas tarefas que alteram o mesmo arquivo nunca ficam abertas ao mesmo tempo.

## Rótulos dos sub-agentes

Cada sub-agente recebe do FISCAL, no despacho, um rótulo único `<PAPEL>-<tarefa>-t<n>` (ex.: `EXE-1.1-t1`, `AMB-1.1-t1`, `USU-1.1-t1`, `VAL-1.1-t1`). O FISCAL registra no ledger `despacho <rótulo> <tipo de agente>` **antes** de chamar a ferramenta `Agent` e usa o rótulo como `description` da chamada. Um rótulo nunca é reaproveitado, e o `guarda.py aceite` recusa parecer cujo validador não tenha despacho próprio no ledger ou coincida com outro papel.

## Auditoria do VALIDADOR (V1–V6)

- **V1 Guarda:** roda de novo `$G diff <id> <n>` e `$G conferir-manifesto $EV/<id>/t<n>`. Qualquer erro → REPROVADA.
- **V2 Escopo e sumiços:** lê o `diff.md` inteiro. Cada linha removida de código existente tem justificativa no relatório do EXECUTOR e não tira funcionalidade. Nenhum arquivo fora do escopo mudou.
- **V3 Causa:** a correção ataca a causa descrita, não esconde o sintoma (ex.: esconder o botão sem bloquear o servidor não resolve permissão).
- **V4 Evidência:** assiste ao vídeo e confere cada constatação da jornada numa captura ou num trecho do vídeo com horário. Constatação sem imagem = não comprovada.
- **V5 Refazer:** o VALIDADOR **repete ele mesmo**, no navegador, no DEV, os passos críticos da jornada (no mínimo: o caso "sem permissão tenta de verdade", quando houver, e a checagem de layout na menor largura), com o próprio vídeo em `$EV/<id>/t<n>/validador/`.
- **V6 Nada desapareceu:** compara `inventario-pos.md` com o `inventario.md` da 0.1 para a parte afetada.

Veredito: `ACEITA` somente se V1–V6 passarem todos, sem ressalva. Caso contrário, `REPROVADA` (com devolução) ou `BLOQUEADA` (precisa de humano).

## Modelos

### M1 — Ordem de Serviço

```markdown
# OS <id> · <PAPEL> · tentativa <n>

## Identidade
- Change: corrigir-permissoes-e-console
- Tarefa: <id> — <texto literal da tarefa em tasks.md>
- PRD cobertos: <PRD-nn literais do proposal.md>
- Ambiente: DEV http://localhost:8080 (192.168.1.65). Produção 192.168.1.75 PROIBIDA.
- Pasta de evidência: /opt/projetos/hwmdm/evidencias/corrigir-permissoes-e-console/<id>/t<n>/

## Problema relatado (literal)
<texto do responsável>

## Jornada de aceite (literal de 03-jornadas.md)
<Jxx inteira>

## Escopo PERMITIDO (globs relativos a repo-mdm/)
<lista>

## Escopo PROTEGIDO
- tudo fora do permitido; openspec/**; arquivos de teste de qualquer tipo; produção

## Itens do inventário J00 que não podem sumir
<recorte do inventario.md da 0.1 para a parte afetada>

## Definição de pronto
<itens verificáveis, cada um ligado a uma constatação da jornada>

## Devoluções anteriores
<M6 anteriores, se houver>
```

### M2 — Relatório do EXECUTOR

```markdown
### Causa encontrada            (arquivo:linha, trecho, por que produz o defeito)
### Arquivos alterados          (arquivo | mudança | constatação da jornada que atende)
### Linhas removidas            (arquivo:linha | conteúdo | por que não tira funcionalidade)
### Como aplicar no DEV         (o que o AMBIENTE precisa fazer: reiniciar container | build WAR | publicar APK)
### Riscos para o uso real      (o que o USUÁRIO/VALIDADOR deve olhar com atenção)
### Bloqueios / dúvidas
```

### M3 — Pedido de validação

```markdown
# Validação <id> · tentativa <n>
- OS: fiscal/os/<id>-*-t<n>.md
- Evidência: /opt/projetos/hwmdm/evidencias/corrigir-permissoes-e-console/<id>/t<n>/
- Sub-agentes desta tentativa: EXECUTOR <rótulo> · AMBIENTE <rótulo> · USUÁRIO <rótulo>
- Validador designado: <rótulo novo, ex.: VAL-<id>-t<n>>
```

### M4 — Parecer do VALIDADOR

```markdown
# Parecer <id> · tentativa <n>
Executor: <rótulo>
Ambiente: <rótulo>
Usuario: <rótulo>
Validador: <rótulo designado a este validador>
Evidencia: /opt/projetos/hwmdm/evidencias/corrigir-permissoes-e-console/<id>/t<n>/

## Evidências verificadas (preencher ANTES do veredito)
| Constatação da jornada | Onde vi (captura / vídeo mm:ss) | Refiz? | Resultado |
|---|---|---|---|

## V1–V6
- V1 Guarda:
- V2 Escopo e sumiços:
- V3 Causa:
- V4 Evidência:
- V5 Refazer (pasta validador/):
- V6 Nada desapareceu:

Veredito: ACEITA | REPROVADA | BLOQUEADA

## Devolução (se não ACEITA)
| # | Defeito | Evidência | Esperado | Critério de reaprovação |
|---|---|---|---|---|
```

### M5 — Assinatura humana

```markdown
# Assinatura <id>
Assinado por: <nome>
Data: <AAAA-MM-DD HH:MM>
Tentativa: t<n>
Manifesto sha256: <sha256 do arquivo MANIFESTO.sha256 — `sha256sum` dele>
Declaro que assisti à evidência principal, que ela foi gravada no DEV (192.168.1.65) e que o resultado é o que pedi.
```

### M6 — Devolução

```markdown
# Devolução <id> · tentativa <n>
| # | Defeito | Evidência | Esperado | Critério de reaprovação |
|---|---|---|---|---|
Próximo executor: mesmo | novo (3ª reprovação pelo mesmo defeito = novo obrigatório)
```

### M7 — Painel (topo de `fiscal/estado.md`)

```markdown
| Tarefa | Estado | Tentativa | Sub-agentes (papel:rótulo) | Último parecer | Bloqueio |
|---|---|---|---|---|---|
```
