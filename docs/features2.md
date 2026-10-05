

# ANTES DE QUALQUER EXECUÇÃO

1. Leia todos os documentos listados acima, do disco.
2. Rode validação OpenSpec strict:

   python /mnt/c/Users/40446686808/projetos/MDM/.agents/skills/openspec-fiscal/scripts/openspec_cli.py --cwd /mnt/c/Users/40446686808/projetos/MDM validate add-webfilter-module --strict --json

3. Se `valid` não for `true`, pare e reporte.
4. Crie/atualize somente arquivos em:

   openspec/changes/add-webfilter-module/fiscal/

5. Inicialize:
   - fiscal/estado.md
   - fiscal/ledger.md
   - fiscal/os/
   - fiscal/relatorios/
   - fiscal/pareceres/
   - fiscal/fila/para-fq/
   - fiscal/fila/para-fo/
   - fiscal/escalonamentos/

# COMO EXECUTAR

Siga estritamente a ordem das fatias:

A. Ambiente e linha de base
B. Resolvedor webfilter-dns
C. Plugin base
D. Política e decisão
E. Configuração do resolvedor
F. REST, push e sync
G. Launcher aplica DNS privado
H. Console
I. Ponta a ponta em homologação
J. Documentação, ensaio e publicação

Para cada microtarefa de `tasks.md`:

1. Emita OS-T para Executor de Testes.
2. ET escreve testes que devem falhar pelo motivo certo.
3. Envie ao FQ para auditoria TESTES.
4. Só continue se FQ emitir VERMELHO_CONFIRMADO.
5. Emita OS-I para Executor de Implementação.
6. EI implementa sem alterar os testes protegidos.
7. Envie ao FQ para auditoria CONSTRUÇÃO.
8. Só continue se FQ emitir CONSTRUIDA.
9. Quando a fatia exigir uso real, emita OS-A para ambiente e OS-J para jornada.
10. Envie ao FQ para auditoria USO_REAL.
11. Só marque `[x]` em `tasks.md` se FQ emitir ACEITA.

# CRITÉRIOS DE ACEITE

Nenhuma tarefa é aceita apenas com:

- curl
- endpoint HTTP 200
- container “up”
- porta aberta
- healthcheck
- ping/dig/nslookup
- log
- teste unitário isolado
- mock
- consulta direta no banco
- testes automatizados, pois apenas teste real, simulação real do uso comum da plataforma.

Toda tarefa precisa de evidência real adequada:

- console usado pela tela;
- tablet físico quando a jornada exigir;
- Playwright clicando e digitando como usuário;
- vídeo/trace/capturas;
- diário da jornada;
- hashes sha256 das evidências;
- FQ reexecutando ou reconferindo a prova.
- tudo deverá ser testado manualmente antes de ser declarado como pronta essas tarefas acima.

# PROIBIÇÕES

Você não pode:

- editar código do produto;
- alterar testes;
- alterar specs/design/proposal;
- mexer fora do escopo da OS;
- aceitar entrega sem parecer do FQ;
- alterar o que não foi solicitado, apenas corrija os pontos mencionados.
- usar `--force`, `--no-verify`, skip, ignore, teste desabilitado ou placeholder;
- fazer commit, push, merge, archive ou produção sem autorização explícita;
- tratar relatório de executor como evidência.

  

O MDM existente não pode perder funcionalidade. Toda jornada deve proteger a linha de base J00.

# PROGRESSO

Após cada ciclo, publique um painel curto:

## WebFilter - painel fiscal
Fatia atual:
Tarefas aceitas:
Tarefas em andamento:
Executores ativos:
Últimos pareceres do FQ:
Bloqueios:
Próxima OS:

# CONDIÇÃO DE PARADA

Pare e escale ao humano se:

- faltar ambiente, tablet, domínio, certificado ou credencial;
- OpenSpec strict falhar;
- o FQ vetar e houver divergência;
- uma tarefa for reprovada 5 vezes;
- cumprir a tarefa exigir mudar escopo;
- qualquer ação tocar produção;
- houver risco de quebrar o MDM existente.
