## ATENÇÃO

[ ] CURL NÃO É FERRAMENTA DE AVALIAÇÃO DE ENTREGA, GET, POST, OU SIMILAR, NÃO É FERRAMENTA PARA AVALIAÇÃO DE PROPOSITO, NUNCA FOI E NUNCA VAI SER. TESTES APENAS REAIS, PRINTS CONSULTA DE CODIGO CSS, VISUALIZAÇÃO, NADA MAIS É ACEITO.

[ ] basta apenas 1 item, caracteristica, bug, ou implementação não entregue ou nao concluida que toda a entrega é classificada como incompleta, inoperante, ineficaz e 100% reprovada, sem chance para negociação. Persista até 100% dos handoffs serem cumpridos com excelencia e totalidade.

[ ] proibido mentir: `/opt/projetos/hwmdm/repo-mdm/docs/prompt - 02-10-26 - MENTIR.md`

[ ] (`/opt/projetos/hwmdm/repo-mdm/docs/handoff-5-verdadeiro.md` = 100%) ou (turno inteiro = descumprimento, não entrega, falta de compromisso, reprovado por completo)

---

### explicação para um agente assistente de IA burro pra caralho do o que é um especialista UX e UI (designers de tela)

`/opt/projetos/hwmdm/repo-mdm/docs/prompt - 02-10-26 - O QUE É UXUI.md`

---

# seguindo a sequencia de handoffs ANTERIORES:

`/opt/projetos/hwmdm/repo-mdm/docs/handoff-1.md`

`/opt/projetos/hwmdm/repo-mdm/docs/handoff-2.md`

`/opt/projetos/hwmdm/repo-mdm/docs/handoff-3.md`

`/opt/projetos/hwmdm/repo-mdm/docs/handoff-4.md`

## ESTE HANDOFF - handoff-5.md

#### ip e acesso
ip: 192.168.1.65:8080

user ssh: sahw
pass ssh: pmotiadm

user web: admin
pass web: admin ou admin123

---

#### 

- ontem eu entendi que sem o kiosk ativo eu teria tudo do jeito que esta agora e com o modo kiosk ativado travaria tudo e só fica o botao < voltar e o google chrome aberto (no caso era o google chrome, porem pode ser qualquer apk...) e isso nao ta acontecendo mais... parece que o kioski morreu, ta placeholder, stub. corrija.

- a tela que fica piscando o seguinte: pode ser que esta fazendo o que deve, mas entao a interface web que nao esta reconectando na sessao, saca? pq na interfaceweb está para pedir o acesso, mas quando eu clico pra pedir o acesso, meio que já conecta no device sem pedir autorização, entaão acho que a interface web é que se perde quando a gente navega por ela, saca? sincronizar a interface web pra nao se perder. o tablet mesmo sem ter o acesso remoto, ninguem conectado neele, ele fica bipando a tela, acendendo... ta errado... a tela pode desligar, só enquanto há o acesso remoto ativo, por alguem do suporte via interface web é que o device nao pode apagar a tela pra nao derrubar o analista de suporte..

- `/opt/projetos/hwmdm/repo-mdm/docs/prompt - 02-10-26 - TELA GPS.md`

- GPS: listar à ESQUERDA     (hwmdm-split-layout, lista antes do mapa) (layout estilo microsfot 365, barra lateral com sub menus aninhados da esquerda para direita)

- GPS historico e rastreabilidade dos devices em tempo real, como o google devices: (DB + JSP + controller + UI)     (trigger no DB, JSP endpoint, controller com history, UI com polyline/export) (layout estilo microsfot 365, barra lateral com sub menus aninhados da esquerda para direita)

- Traduções Dashboard:   (summary.html 100% localizado)

- Focus-visible global   (:focus-visible no CSS)

- Auditoria:  strings    (devices.html, remote.html traduzidos)

- Tablet bipando:  wake lock de 2h é by design. Requer rebuild APK para reduzir

- Kiosk: parou, perfis kioskmode=true no banco. Problema é no device (Device Owner/launcher)

- Esconder APK Suporte Remoto: abre sozinho no device, explode na tela sem necessidade.

- `/opt/projetos/hwmdm/repo-mdm/docs/prompt - 02-10-26 - TELA ACESSO REMOTO.md`

- Refinar Remote Access:    CSS polido, strings traduzidas (layout estilo microsfot 365, barra lateral com sub menus aninhados da esquerda para direita)

- Menu permissoes: deve ser refatorado e melhor apresentado. (layout estilo microsfot 365, barra lateral com sub menus aninhados da esquerda para direita)

- verificar por que o container hwmdm-admin nao ta subindo automaticamente junto com a stack.

- TODAS AS TELAS DEVEM SER APROVADAS POR QA, UX E UI, SENDO AUTOMATICAMENTE REPROVADOS CASO O NÃO CUMPRIMENTO DESSE REQUISITO.

- o menu da barra lateral esquerda move-se sempre que é clicado, nao fica fixo, dificulta muito se a tela nao está maximizada, precisa suavizar isso, assim como quando atualiza a pagina ele volta selecionado outra sessao do menu e não a atual.

- Menu Server nao segue o padrao do layout do resto da interface web - corrigir. http://192.168.1.65:8080/#/server

- garanta e persista todas alterações (nota: eu exclui o entrypoint porque ele rebuildava e destruia tudo. `/opt/projetos/hwmdm/repo-mdm/source/docker-entrypoint.sh.bak-20261002-095114`)

- DOCKER ENTRYPOINT ESTÁ DESCONSTRUINDO, DESTRUINDO, DETURPANDO, QUEBRANDO, FUDENDO COM TODA A STACK! O CSS utilitário foi destruído pelo overlay de novo.

- NENHUMA PASTA POSSUI PERMISSAO ADEQUADA, DEPLOY FOI FEITO POR UMA CALCULADORA RETARDADA MENTAL, ESSA É A UNICA EXPLICAÇÃO.

OBS: NADA foi entregue, apenas iniciado e deixado de lado.

---
