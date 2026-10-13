# Pedido de validação — Web Filter no DEV — tentativa 1

- Change: `openspec/changes/add-webfilter-module` (proposal.md, design.md, specs/**, tasks.md)
- Ambiente: DEV `http://localhost:8080` (192.168.1.65). Produção: PROIBIDA (o hook `.claude/hooks/bloquear-producao.sh` recusa).
- Executor desta tentativa: sessão principal do Claude Code, que não valida a si mesma.
- Código entregue (leia do disco, não confie em relatório):
  - `server-source/plugins/webfilter/**` (plugin: banco, API, hook do sync, gerador do resolvedor, tela)
  - `server-source/server/src/main/webapp/app/app.js` (reabrir página de plugin após F5)
  - `server-source/server/src/main/webapp/app/components/main/view/content.html` (item "Web Filter" no menu)
  - `webfilter-dns/**` (resolvedor: Dockerfile, docker-compose.yaml, app/resolver.py, certificado de DEV)
  - `android-source/app/src/main/java/com/hmdm/launcher/helper/PrivateDns*.java`, `json/ServerConfig.java`,
    `helper/ConfigUpdater.java`, `app/build.gradle` (versionCode 15380) → `dist/hmdm-v1.1-webfilter.apk`
- Conta de teste do console: `/opt/projetos/hwmdm/evidencias/webfilter/conta-teste.txt`
- Sonda DNS-over-TLS (SNI + certificado): `/opt/projetos/hwmdm/evidencias/webfilter/dns/consulta_dot.py`
- Segredo para simular o sync do aparelho: `SHARED_SECRET` em `repo-mdm/source/.env`
- Navegador: `require('/opt/projetos/hwmdm/ferramentas/node_modules/playwright')`
- Evidências do executor (alegação, não prova): `/opt/projetos/hwmdm/evidencias/webfilter/`
- Pasta para a evidência do validador: `/opt/projetos/hwmdm/evidencias/webfilter/validador-t1/`
- Parecer: `openspec/changes/add-webfilter-module/fiscal/pareceres/WEBFILTER-DEV-t1.md`
