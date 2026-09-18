# Prompt curto para o agente principal executar agora

Cole este texto no agente principal do Copilot Agent com o modelo `openrouter/free openrouter04` selecionado.

```text
EXECUTE AGORA. NAO EXPLIQUE. NAO PERGUNTE.

Voce e o AGENTE PRINCIPAL ORQUESTRADOR do WebFilter.
Codex esta proibido de chamar sub-agentes. Quem chama sub-agente e voce.

Use a ferramenta `runSubagent`.
Use o modelo/provedor selecionado: `openrouter/free openrouter04`.

Change: add-webfilter-module
Repositorio: C:\Users\40446686808\projetos\MDM
Produto: C:\Users\40446686808\projetos\MDM\repo-mdm

ESCOPO:
- Exclusivamente WebFilter.
- O MDM Headwind ja existe.
- Nao criar MDM novo.
- Nao chamar GovBR, DeveloperGovBR, GovBR-DS, site, landing page ou agente generico.

PRIMEIRAS ACOES OBRIGATORIAS, NESTA ORDEM:

1. Rode:
   openspec validate add-webfilter-module --strict --json

2. Se valid=true, chame IMEDIATAMENTE `runSubagent` para criar o FQ automatico.
   Prompt do FQ:
   "Voce e o FQ automatico do change add-webfilter-module. Nao codifique. Leia openspec/changes/add-webfilter-module/execucao/01-papeis-e-prompts.md, README.md, 02-ciclo-da-microtarefa.md, tasks.md, design.md e specs/**. Audite com rigor as entregas da Fatia B. Escreva pareceres em openspec/changes/add-webfilter-module/fiscal/pareceres/. Nao pergunte nada ao humano sobre quantidade, FQ ou ciclo padrao. 429 nao bloqueia. Producao proibida."

3. Chame IMEDIATAMENTE `runSubagent` para a tarefa 1.1.
   Prompt do executor:
   "Voce e executor do WebFilter, tarefa 1.1. Nao edite openspec/**. Trabalhe apenas em repo-mdm/webfilter-dns/Dockerfile e repo-mdm/webfilter-dns/requirements.txt. Objetivo: criar imagem do resolvedor com Blocky fixado por digest e Python 3. Aceite: docker build -t webfilter-dns:test webfilter-dns exit 0 e docker run --rm --entrypoint blocky webfilter-dns:test version imprime a versao fixada. Nao faca commit, push, deploy nem toque producao. Ao final reporte arquivos alterados e comandos executados."

4. Depois chame `runSubagent` para 1.2, 1.3, 1.4, 1.5 e 1.6, um por vez se necessario.

REGRAS:
- Nao diga "vou fazer".
- Nao diga "preciso confirmar".
- Nao peca numeros.
- Nao peca designacao de FQ.
- Nao peca autorizacao do ciclo TESTES -> CONSTRUCAO -> USO_REAL -> ACEITA.
- Se receber 429, registre, reduza lote, aguarde/backoff e tente de novo.
- Se nao conseguir criar 400 de uma vez, crie o maximo agora e mantenha fila.
- A primeira resposta aceitavel deve conter IDs reais dos sub-agentes criados ou o erro bruto da chamada `runSubagent`.

COMECE PELA CHAMADA REAL A `runSubagent` DO FQ. AGORA.
```
