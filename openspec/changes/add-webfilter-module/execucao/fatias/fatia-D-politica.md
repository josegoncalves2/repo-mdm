# Fatia D - Politica e decisao

## Resultado visivel

O modulo sabe salvar politica por perfil, validar entradas, proteger o MDM e calcular bloqueios de apps sem tocar no launcher.

## Tarefas

### 4.1 - DAOs de politica/settings

**ET:** `WebFilterPolicyDAOTest` e `WebFilterSettingsDAOTest`.

**EI:** implementa mappers/DAOs.

**FQ exige:**

- todo acesso filtra `customerId`;
- politica por `configurationId` e unica;
- historico registrado;
- `dnsDomain` salvo e lido;
- dados de outro cliente retornam vazio.

### 4.2 - Service de politica

**ET:** `WebFilterPolicyServiceTest`.

**EI:** implementa validacao de categoria, dominio, curinga, pacote, conflito, pacote protegido, `doh` obrigatoria.

**FQ exige:**

- politica anterior preservada em erro;
- `*.com` recusado;
- mesma entrada em allow/block recusada;
- `com.hmdm.launcher` e `com.hwmdm.remote` protegidos;
- `doh` sempre bloqueada quando `enabled=true`.

### 4.3 - Decisao de aplicativos

**ET:** `AppDecisionTest` e `LockedPackagesCalculatorTest`.

**EI:** implementa ordem allowlist > blocklist > categoria > permitido e calculo de locked/unlocked.

**FQ exige:**

- valores do administrador preservados;
- historico permite liberar dispositivo offline;
- app principal de quiosque protegido;
- dados do app nao sao removidos pelo bloqueio.

## Jornada de aceite

J05A/J05B aceitam a parte de aplicativos. Sem a jornada aplicavel, a fatia fica CONSTRUIDA, nao ACEITA.
