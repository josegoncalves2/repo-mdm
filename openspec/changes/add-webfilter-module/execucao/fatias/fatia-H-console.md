# Fatia H - Console Web Filter

## Resultado visivel

Administrador usa o Web Filter pelo menu lateral, configura politicas, categoriza apps, ve fontes/licencas e usa a tela em tema claro/escuro sem quebra visual.

## Tarefas

### 8.1 - Modulo JS, entrada e permissao

**ET:** `hwmdm-webfilter-access.spec.js`.

**EI:** cria webapp do plugin e i18n.

**FQ exige:**

- menu ausente sem permissao;
- menu presente com permissao;
- tela sem erro JS;
- plugin desativado remove entrada;
- textos em `en_US` e `pt_PT`.

### 8.2 - Politica por perfil

**ET:** `hwmdm-webfilter-policy.spec.js`.

**EI:** implementa tela de politica, dominio DNS e fontes.

**FQ exige:**

- usuario salva politica completa pela tela;
- erro aparece junto da entrada invalida;
- `doh` marcada e travada;
- aviso quando dominio DNS vazio;
- fontes mostram nome, link e licenca.

### 8.3 - Categorizacao de apps

**ET:** `hwmdm-webfilter-apps.spec.js`.

**EI:** implementa aba de apps.

**FQ exige:**

- pacote do cliente inclui/remove;
- pacote do catalogo inicial e somente leitura;
- pacote protegido recusado com mensagem visivel.

### 8.4 - Padrao visual

**ET:** `hwmdm-webfilter-ui-padrao.spec.js`.

**EI:** ajusta CSS/templates.

**FQ exige:**

- contraste >= 4,5:1;
- sem scroll horizontal em 360/768/1280;
- voltar retorna ao Web Filter;
- titulo igual ao menu;
- portugues quando usuario esta em portugues.

## Jornadas de aceite

J02, J03, J06 e J08. Playwright deve usar tela de login e cliques reais, sem `page.request` como prova principal.
