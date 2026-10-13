# Pedido de validação — Web Filter no DEV — tentativa 3

Mesmo escopo, caminhos, conta, sonda e regras dos pedidos t1 e t2 (`fila/para-validador/WEBFILTER-DEV-t1.md` e `-t2.md`). As **decisões humanas registradas no t2 continuam valendo**.
Pareceres anteriores (ambos REPROVADA): `pareceres/WEBFILTER-DEV-t1.md` e `pareceres/WEBFILTER-DEV-t2.md`. Reconfira com evidência nova cada defeito ainda aberto e confirme que os já corrigidos não regrediram.

- Pasta para a evidência do validador: `/opt/projetos/hwmdm/evidencias/webfilter/validador-t3/`
- Parecer: `openspec/changes/add-webfilter-module/fiscal/pareceres/WEBFILTER-DEV-t3.md`
- WAR publicada no DEV: `dist/hmdm.war`, sha256 `393d53806531e6ec71b0ba276feb61e784afb0264403ca114582a9f5fa76c419`. O container do MDM foi reiniciado por completo às 18:44 de 2026-09-18. A imagem do resolvedor foi reconstruída e o container recriado no mesmo horário.
- Runbook novo: `docs/WEBFILTER.md`

## Entrega desta tentativa (resposta ao parecer t2)

- **2 (causa-raiz):** `ResolverConfigWriter` passa a ter dono dos arquivos (`<dns>/.owner`). A instância mais nova assume na primeira gravação; uma instância antiga que encontra outro dono para de gravar para sempre e registra `...owned by a newer instance...`. Pode ser provado com uma recarga a quente (ex.: `touch` na `ROOT.war` dentro do container) seguida de observação pelo ciclo de 10 min. O validador pode fazer essa recarga, desde que ao final o MDM volte a responder na tela de login.
- **13:** a nova tentativa de uma configuração que falhou é ensaiada em portas alternativas, sem parar a instância em uso, com espera crescente (120 s → 1 h).
- **11:** a categoria obrigatória não usa mais `opacity`.
- **12:** a faixa e o aviso mostram o domínio **salvo**, não o digitado.
- **14:** o texto de ajuda não usa mais `<...>`.
- Runbook `docs/WEBFILTER.md` com publicação por reinício completo.
