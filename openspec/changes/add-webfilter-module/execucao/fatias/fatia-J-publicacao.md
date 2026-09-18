# Fatia J - Documentacao, ensaio e publicacao

## Resultado visivel

O responsavel humano tem runbook completo para publicar e voltar atras. Agentes nao executam producao.

## Tarefas

### 10.1 - `docs/WEBFILTER.md`

**ET:** checklist documental D12 -> secoes esperadas.

**EI:** escreve documento operacional.

**FQ exige:**

- pre-requisitos DNS, certificado, porta 853 e memoria;
- backup;
- sequencia de publicacao;
- piloto;
- rollback funcional e tecnico;
- limites conhecidos;
- atribuicao de licencas.

### 10.2 - Script SQL de remocao

**ET:** teste em banco de homologacao com schema antes/depois.

**EI:** cria `remove-webfilter.sql`.

**FQ exige:**

- remove somente objetos `plugin_webfilter_*`, plugin e permissao;
- core fica identico no schema dump;
- WAR anterior inicia sem erro;
- script nao usa `DROP ... CASCADE` amplo sem escopo comprovado.

### 10.3 - Ensaio completo em homologacao

**Papel:** EA/EJ, auditado por FQ.

Executar J12 com copia do banco de producao em homologacao.

**FQ exige:**

- runbook seguido literalmente;
- tablet ja matriculado atualiza sem rematricula;
- perfil piloto funciona;
- perfis nao piloto sem mudanca;
- rollback funcional e tecnico ensaiados.

### 10.4 - Publicacao em producao

**Papel:** responsavel humano do ambiente. Agentes nao executam.

**FO/FQ podem apenas:**

- conferir autorizacao explicita;
- conferir backup legivel;
- conferir logs e capturas fornecidos;
- registrar parecer.

**FQ reprova se:**

- agente executou qualquer comando em producao;
- backup nao e legivel;
- piloto nao demonstra app e site bloqueados;
- perfis nao piloto foram alterados.

## Jornada de aceite

J12 em homologacao. Producao exige autorizacao humana separada e evidencia conferida, nunca acao direta de agentes.
