# Pedido de validação — Web Filter no DEV — tentativa 4

Mesmo escopo, caminhos, conta, sonda e regras dos pedidos t1 a t3 (`fila/para-validador/`). As **decisões humanas registradas no t2 continuam valendo**.
Pareceres anteriores: `pareceres/WEBFILTER-DEV-t1.md`, `-t2.md` e `-t3.md`. No t3, A, C e E foram aceitos e B reprovado pelos defeitos 15 e 16.

- Pasta para a evidência do validador: `/opt/projetos/hwmdm/evidencias/webfilter/validador-t4/`
- Parecer: `openspec/changes/add-webfilter-module/fiscal/pareceres/WEBFILTER-DEV-t4.md`
- A WAR não mudou desde o t3 (`dist/hmdm.war`, sha256 `393d53806531e6ec71b0ba276feb61e784afb0264403ca114582a9f5fa76c419`; o container a reempacota ao subir, como o t3 registrou). O container do MDM foi reiniciado por completo às 19:57, para descartar a instância antiga deixada pela recarga a quente do t3.
- Mudaram só `webfilter-dns/app/resolver.py` (imagem reconstruída e container recriado às 19:53) e `docs/WEBFILTER.md`.
- Sonda: `consulta_dot.py` agora imprime "SEM RESPOSTA" em vez de traceback quando a porta não responde.

## Entrega desta tentativa (resposta ao parecer t3)

- **15 e 16:** o supervisor foi reorganizado em três passos a cada ciclo:
  1. manter viva a instância em uso: sem instância viva, a última configuração boa sobe de novo em até ~10 s, inclusive durante a espera de nova tentativa e se a própria configuração boa também falhar;
  2. trocar de configuração quando houver uma nova e ela estiver na hora (ensaio em portas alternativas nas novas tentativas);
  3. recarregar listas na instância em uso, inclusive durante a espera.
- **Runbook:** comando `--apk` no passo do launcher; comportamento durante as novas tentativas; seção "Limites conhecidos" (IP literal, DoH por IP, VPN, portal cativo, certificado, memória medida, ensaio em homologação e tablet não realizados).

Com o pedido do t3, o validador pode matar o processo `blocky` dentro do container (`/proc/*/cmdline` começando por `/usr/local/bin/blocky`) para provocar a falha de carga e a queda.
