# Pedido de validação — Web Filter no DEV — tentativa 2

Mesmo escopo, caminhos, conta, sonda e regras da tentativa 1: `fila/para-validador/WEBFILTER-DEV-t1.md`.
Parecer anterior (REPROVADA): `pareceres/WEBFILTER-DEV-t1.md`. Cada defeito dele deve ser reconferido com evidência nova.

- Pasta para a evidência do validador: `/opt/projetos/hwmdm/evidencias/webfilter/validador-t2/`
- Parecer: `openspec/changes/add-webfilter-module/fiscal/pareceres/WEBFILTER-DEV-t2.md`
- WAR publicada no DEV: `dist/hmdm.war`, sha256 `3eb58b074d25058f88e2cab4cbb836891d81ea631cdd1f20983e0bc2fe9137a5`. O container do MDM foi reiniciado por completo (não houve troca a quente), às 18:00 de 2026-09-18.

## Decisões humanas registradas (fora do julgamento do validador)

1. **Item próprio "Web Filter" no menu lateral e correção do F5 em `app.js`.** Ordem explícita do responsável em 2026-09-18, que prevalece sobre o design D13 ("inserir e fazer o módulo aparecer na interface", "cadê o módulo exclusivo do Web Filter", "se atualizar a página morre tudo"). O validador confere que essas alterações funcionam e não quebram outras telas, mas não reprova por elas existirem.
2. **Commit e versionamento** são decisão do responsável; os agentes não fazem commit. O commit `c1433e71` foi feito pelo responsável, não pelo executor. O validador confere que o que roda no DEV é o build da árvore de trabalho atual: sha256 da WAR acima = WAR servida, e APK = `dist/hmdm-v1.1-webfilter.apk`.
3. **Contraste dos botões primários (`.btn-primary`) do console** é estilo do núcleo, anterior a esta entrega e usado em todas as telas. Mudá-lo não foi pedido. Registre-o como observação, não como defeito desta entrega. Contraste de elementos **desta** tela (erros, selos, textos) continua sendo defeito se estiver abaixo de 4,5:1.
4. **Alterações de terceiros em paralelo.** Outra ferramenta ou pessoa altera o repositório ao mesmo tempo: `localization/hwmdm_modules.js`, `remoteSupport.service.js` e o commit `c1433e71`. Arquivos que não constam nesta lista de entrega não são do executor.

## Entrega desta tentativa (resposta ao parecer t1)

Defeitos 1–10 do t1: permissão no menu e na tela; HTTP 400/403/404 reais; contraste dos erros e dos selos; app de quiosque recusado; domínio pai de protegido aceito; descrição das categorias; aviso de domínio DNS vazio; nova tentativa do supervisor; chave TLS fora do git; carga dupla do módulo removida do `index.html`; reinício completo contra escritores antigos.
