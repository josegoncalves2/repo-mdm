# Fatia A - Ambiente e linha de base

## Resultado da fatia

Homologacao identificada, tablet piloto registrado, OpenSpec valido e J00 gravada. Nenhuma implementacao comeca antes desta fatia.

## Microtarefas documentais

### A.1 - Registrar identidade da homologacao

**Papel:** EA, auditado por FQ.

**OS-A deve pedir:**

1. registrar URL do console de homologacao;
2. registrar host/usuario que roda servidor;
3. registrar como distinguir homologacao de producao;
4. registrar conta admin de teste;
5. registrar tablet piloto: serial, Android, launcher atual, perfil, cliente;
6. registrar onde ficam evidencias temporarias.

**FQ reprova se:**

- producao nao estiver explicitamente excluida;
- tablet nao for fisico quando a jornada exigir fisico;
- conta nao conseguir login pela tela.

### A.2 - Validar planejamento

**Papel:** FO, auditado por FQ.

**Comando obrigatorio:**

```powershell
python C:/Users/40446686808/projetos/MDM/.agents/skills/openspec-fiscal/scripts/openspec_cli.py --cwd C:/Users/40446686808/projetos/MDM validate add-webfilter-module --strict --json
```

**Aceite:** `valid: true`, sem issues.

### A.3 - Executar J00

**Papel:** EJ, auditado por FQ.

**OS-J:** copiar J00 de `../03-provas-de-uso-real.md`.

**Evidencia minima:**

- video/trace do login no console;
- captura de Devices e perfil;
- screenrecord ou capturas do tablet abrindo app permitido e site permitido;
- diario da jornada.

## Saida da fatia

FO atualiza `../fiscal/estado.md` com todas as tarefas de `tasks.md` em PENDENTE e registra que a Fatia B esta liberada.
