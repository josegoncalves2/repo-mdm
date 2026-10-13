# Fatia B - Resolvedor `webfilter-dns`

## Resultado visivel

Um resolvedor DNS-over-TLS isolado, em homologacao, aplica categorias, allowlist e blocklist por perfil antes de qualquer integracao com o MDM.

Pre-condicao: J00 da Fatia A aceita. Sem J00, nenhuma OS da Fatia B e valida.

## Tarefas

### 1.1 - Imagem do resolvedor

**ET:** teste/probe que falha se Dockerfile nao fixa Blocky por digest ou se `blocky version` nao roda.

**EI:** cria `webfilter-dns/Dockerfile` e `requirements.txt`.

**FQ exige:**

- digest fixado;
- sem `latest`;
- build reproduzivel;
- `blocky version` mostra versao fixada.

### 1.2 - Updater de listas

**ET:** `tests/test_updater.py` com servidor HTTP local cobrindo sucesso, 404, timeout, 200 vazio, lista em partes, normalizacao, comentarios, intervalo minimo e refresh.

**EI:** implementa `app/updater.py`.

**FQ exige:**

- falha vermelha antes por comportamento ausente;
- nao baixar a mesma URL antes de 1 hora;
- rejeitar conteudo sem dominios;
- manter ultima lista valida;
- nunca depender de rede real no teste.

### 1.3 - Supervisor

**ET:** `tests/test_supervisor.py` cobrindo inicio, restart por `blocky.yml`, rollback de config invalida e refresh sem restart em mudanca de lista de perfil.

**EI:** implementa `app/supervisor.py`.

**FQ exige:**

- log claro de config invalida;
- PID preservado quando muda so lista;
- rollback atomico para ultima config valida.

### 1.4 - Compose isolado

**ET:** probe de compose validando TCP 853 publicado, 53 nao publicado, volumes e healthcheck.

**EI:** cria `webfilter-dns/docker-compose.yaml` e `entrypoint.sh`.

**FQ exige:**

- `source/docker-compose.yaml` sem diff;
- certificado montado somente leitura;
- dados do plugin montados somente leitura;
- restart policy declarada.

### 1.5 - Integracao real do resolvedor

**ET:** `tests/test_resolver_integration.py` com fixtures de perfis, listas e CA de teste.

**EI:** ajusta fixtures/codigo ate consultas DoT passarem.

**FQ exige:**

- SNI identifica perfil;
- NXDOMAIN para categoria e blocklist;
- allowlist vence categoria;
- allowlist nao vira modo exclusivo;
- isolamento entre perfis;
- refresh sem restart em ate 5 minutos.

### 1.6 - Medicao com catalogo completo

**ET/EI:** script `tests/measure_full_catalog.py`.

**FQ exige:**

- JSON com RSS, tempo ate primeira resposta bloqueada e contagem por categoria;
- memoria do host de homologacao registrada e limite de memoria de producao informado pelo responsavel humano, sem leitura direta de host de producao por agente;
- se tempo > 60 s ou memoria inviavel, bloquear execucao e exigir revisao de design.

## Jornada de aceite

Executar J01. A fatia so passa quando J01 provar comportamento por DNS-over-TLS, nao apenas container healthy.
