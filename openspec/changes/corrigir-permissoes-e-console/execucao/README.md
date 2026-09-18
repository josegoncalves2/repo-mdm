# Manual de execução — `corrigir-permissoes-e-console`

Este manual define **como** as tarefas de `../tasks.md` são executadas e aprovadas. Ele não executa nada sozinho. Nada roda até o responsável humano iniciar o FISCAL (comando `/fiscal-permissoes`).

| Arquivo | Conteúdo |
|---|---|
| `README.md` | papéis, estados, ambiente, regras de ouro |
| `01-prompts.md` | prompt de cada papel (colado sem alteração no sub-agente) |
| `02-ciclo.md` | ciclo de cada tarefa, modelos de OS, parecer, devolução e painel |
| `03-jornadas.md` | jornadas de uso real (J00–J10) e política de prova |
| `../fiscal/guarda.py` | guarda mecânica: snapshot, escopo, sumiços, testes proibidos, ledger encadeado e checagem de aceite |

## Ambiente (identidade fixa)

| Item | Valor |
|---|---|
| Alvo único | **DEV** — `http://localhost:8080` = `http://192.168.1.65:8080`, host `pmohw` |
| Stack | `repo-mdm/source/docker-compose.yaml`, projeto Compose `hwmdm` (já online, não reprovisionar) |
| Proibido | produção `192.168.1.75`, `mdm.olimpia.sp.gov.br`, e os 4 tablets matriculados nela (R9XT200AMYY, R9XT106Y5RP, R9XT108EM8T, R9XT106VP1E) |
| Evidências | `/opt/projetos/hwmdm/evidencias/corrigir-permissoes-e-console/<tarefa>/t<n>/` (fora do repositório) |
| Ferramentas de navegador | `/opt/projetos/hwmdm/ferramentas/` (fora do repositório) |

O hook `.claude/hooks/bloquear-producao.sh` recusa qualquer comando que cite o IP ou o domínio de produção. Ele é uma rede de proteção e não substitui a regra.

## Papéis

Todos, menos o FISCAL e o HUMANO, são **sub-agentes reais** criados pela ferramenta `Agent`, um **novo** por OS. O mesmo sub-agente nunca ocupa dois papéis na mesma tarefa.

| Papel | Tipo de agente | Faz | Nunca faz |
|---|---|---|---|
| **FISCAL** | sessão principal | escolhe a tarefa, escreve a OS, convoca os sub-agentes, mantém `estado.md` e o ledger, roda `guarda.py`, marca `[x]` | codificar, aplicar no DEV, executar jornada, julgar qualidade, aprovar sem VALIDADOR e sem assinatura humana |
| **EXECUTOR** | `hwmdm-executor` | investiga a causa no código e corrige **dentro do escopo da OS** | criar teste automatizado, sair do escopo, aplicar no DEV, declarar pronto |
| **AMBIENTE** | `hwmdm-ambiente` | aplica a entrega no DEV (reinício do container, build da WAR, publicação do APK no DEV), cria usuários e papéis de teste **pela tela**, registra hashes | alterar código, tocar produção, criar dado de verificação direto no banco |
| **USUÁRIO** | `hwmdm-usuario` | executa a jornada como a pessoa real faria (cliques e digitação no navegador; toques no tablet), grava vídeo, capturas e diário | alterar código, banco ou configuração; chamar API; repetir até "dar certo" sem registrar |
| **VALIDADOR** | `hwmdm-validador` | reexecuta a guarda, lê o diff, confere a evidência quadro a quadro, **refaz ele mesmo** os passos críticos no navegador, emite parecer | editar código, aceitar relatório como prova, aprovar com ressalva |
| **HUMANO** | responsável | assiste à evidência principal, assina o aceite, aprova a matriz da 3.1, resolve bloqueios | — |

## Estados

```text
PENDENTE → EM_EXECUCAO → IMPLEMENTADA → APLICADA_DEV → EM_JORNADA → EM_VALIDACAO → VALIDADA → ACEITA
```

- `IMPLEMENTADA`: o EXECUTOR entregou e a `guarda.py diff` passou (escopo, sem sumiço injustificado, sem teste automatizado).
- `APLICADA_DEV`: o AMBIENTE aplicou no DEV e registrou os hashes.
- `VALIDADA`: parecer `ACEITA` do VALIDADOR.
- `ACEITA`: assinatura humana registrada e `guarda.py aceite` sem erro. Só então o `[x]` é marcado.
- Desvios: `REPROVADA`, que volta com devolução numerada; `BLOQUEADA`, que exige decisão humana.

## Regras de ouro

1. **Sem sub-agente, sem progresso.** Estado só avança com OS despachada a um sub-agente real e relatório dele em disco. O FISCAL não faz trabalho de executor "para adiantar".
2. **Quem faz não valida.** VALIDADOR é sempre um sub-agente novo, que nunca foi EXECUTOR, AMBIENTE ou USUÁRIO da mesma tarefa.
3. **Relatório é alegação.** Prova é o que o VALIDADOR vê no vídeo e nas capturas e o que ele mesmo refaz no navegador.
4. **Proibido teste automatizado.** A `guarda.py diff` reprova arquivo de teste criado ou alterado. Suíte verde não conta como prova.
5. **Nada desaparece.** Toda jornada termina com a verificação de inventário da parte afetada contra J00. Qualquer menu, tela, aba, botão, coluna, campo ou ação a menos = `REPROVADA`, mesmo que a tarefa em si funcione.
6. **Nada além do pedido.** Só os arquivos da OS mudam. Linha removida de código existente precisa de justificativa no relatório do EXECUTOR, e o VALIDADOR confere cada uma.
7. **Permissão se prova dos dois lados.** Toda prova de permissão tem um usuário **com** e um **sem** a permissão, e o "sem" tenta de verdade a ação (inclusive pela sessão do próprio navegador), não só "não vê o botão".
8. **Somente DEV.** Produção é intocável. Falta de tablet no DEV bloqueia as tarefas 0.2 e 4.x, que não são substituídas por emulador nem pelos tablets de produção.
9. **Estado em disco.** `fiscal/estado.md` e `fiscal/ledger.md` permitem retomar depois de troca de sessão ou compactação de contexto.
10. **Cinco reprovações** na mesma tarefa, ou três pelo mesmo defeito com executores diferentes → `BLOQUEADA` e escalonamento.

## Quando chamar o humano (`fiscal/escalonamentos/`)

- aprovação da matriz da 3.1 (obrigatória);
- assinatura de aceite de cada tarefa (obrigatória);
- falta de tablet no DEV, de credencial ou de ferramenta;
- correção que exija mudar algo não pedido ou remover algo existente;
- divergência entre VALIDADOR e FISCAL;
- qualquer passo que tocaria produção.
