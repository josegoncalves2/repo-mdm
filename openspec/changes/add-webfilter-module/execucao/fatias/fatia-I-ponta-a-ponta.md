# Fatia I - Ponta a ponta em homologacao

## Resultado visivel

O WebFilter funciona no dia a dia em tablet real: apps e sites bloqueiam/liberam, desativacao devolve o normal e queda do resolvedor e compreendida.

## Tarefas

### 9.1 - Preparar homologacao DNS/TLS

**Papel:** EA, auditado por FQ.

**FQ exige:**

- registro wildcard `*.<dominio>`;
- certificado publico valido por DNS-01;
- DoT externo com SNI funcionando;
- producao intocada.

### 9.2 - Jornada de aplicativos

**Papel:** EJ, auditado por FQ.

Executar J05B para apps.

**FQ exige:**

- app bloqueado some/inacessivel;
- app allowlist continua acessivel;
- liberacao preserva dados;
- pacote nao instalado nao quebra os demais.

### 9.3 - Jornada de sites

**Papel:** EJ, auditado por FQ.

Executar J04 para sites.

**FQ exige:**

- DNS privado hostname aplicado;
- configuracao bloqueada ao usuario;
- Chrome nao abre dominio bloqueado;
- dominio permitido e dominio do MDM abrem.

### 9.4 - Desativacao

**Papel:** EJ, auditado por FQ.

Executar J10.

**FQ exige:**

- apps voltam;
- DNS privado volta ao automatico;
- site antes bloqueado resolve;
- restricao some quando o perfil permite.

### 9.5 - Resolvedor fora do ar

**Papel:** EJ/EA, auditado por FQ.

Executar J11.

**FQ exige:**

- queda afeta resolucao como esperado;
- restart recupera sem tocar no tablet;
- apps bloqueados continuam bloqueados;
- comportamento documentado como risco operacional.

## Saida da fatia

Todas as jornadas devem ter video, capturas, diario, hashes e parecer ACEITA. Sem tablet real, a fatia fica BLOQUEADA.
