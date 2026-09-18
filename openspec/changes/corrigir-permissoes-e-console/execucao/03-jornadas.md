# 03 — Jornadas de uso real e política de prova

## Política de prova

**Teste automatizado é proibido** (PRD-17). A prova é a simulação real do uso comum: uma pessoa, ou um sub-agente agindo como ela, usa o console pelo navegador e o tablet físico pelo toque.

### Aceito como prova principal

- Navegador real (Chromium via Playwright), **dirigido só por gestos de pessoa**: abrir a URL do console, digitar login e senha na tela de login, clicar no menu lateral, clicar em botões, digitar em campos, rolar, redimensionar a janela. Com vídeo da sessão inteira e captura numerada por passo.
  - O roteiro do robô é **descartável**, fica na pasta de evidência daquela tentativa, não tem asserts nem runner de teste e nunca entra no repositório. Ele equivale à mão da pessoa, não a um teste.
  - Proibido no roteiro: `page.request`, `page.route`, `fetch`/`XMLHttpRequest` montados à mão, `page.evaluate` que altere estado, injeção de token/cookie, abrir URL interna que a pessoa não alcança pelo menu.
  - **Exceção única:** no roteiro "sem permissão tenta de verdade" (J04), a chamada é repetida **pelo DevTools do navegador da própria sessão do usuário sem permissão** (reenvio da requisição que a tela faria), com vídeo. Isso simula a pessoa mal-intencionada e é o que prova o bloqueio no servidor.
- Tablet físico matriculado no DEV: toques reais, ou `adb shell input` guiado por `uiautomator dump`, com `screenrecord` ligado e o relógio do tablet registrado.
- O que aparece para a pessoa: texto, menu presente/ausente, botão habilitado/desabilitado, mensagem de erro, dado salvo que reaparece ao reabrir, efeito no tablet.

### Só complemento (nunca aprova sozinho)

`curl`, status HTTP, logs, `docker compose ps`, consulta ao banco, `dumpsys`, `settings get`, "compilou", "o build passou".

### Evidência obrigatória por jornada

Na pasta `/opt/projetos/hwmdm/evidencias/corrigir-permissoes-e-console/<tarefa>/t<n>/`:

- `video/` (sessão inteira), `capturas/NN-descricao.png`, `diario.md` (passo | horário | ação | o que apareceu | captura);
- `roteiro.*` (roteiro descartável do robô, se houve);
- `inventario-pos.md` (verificação de "nada desapareceu" da parte afetada);
- `MANIFESTO.sha256`, gerado com `guarda.py manifesto`.

### Larguras e temas (toda jornada de console que envolve layout)

1920×1080, 1366×768, 1024×768, 768×1024 e 390×844; tema claro e escuro; idioma pt-BR. Em nenhuma delas pode haver scroll horizontal da página, conteúdo cortado sem acesso ou ação inalcançável.

### Usuários e papéis de teste

Criados **pela tela** pelo AMBIENTE na 0.1, com senha registrada em `evidencias/.../0.1/contas.md` (fora do repo):

| Login | Papel | Uso |
|---|---|---|
| `admin` | Admin (existente) | referência |
| `f.superadmin` | Super-Admin | referência total |
| `f.<papel>` | um por papel existente: User, Observer, Helpdesk 1, Helpdesk 2, Helpdesk 3, Guest | inventário por papel |
| `f.perfil-ver` / `f.perfil-editar` / `f.perfil-nada` | papéis novos da 3.2 (criados pela tela na 3.2/3.3) | segregação |

---

## J00 — Inventário da linha de base (console)

**Tarefa:** 0.1. **Roda antes de qualquer alteração de código.**

**Passos:** para cada login da tabela, entrar pela tela de login; percorrer **todos** os itens do menu lateral; em cada tela, registrar título, abas, colunas, botões, campos, ações de linha e menus de contexto; abrir o detalhe de um dispositivo e de um perfil de dispositivo e percorrer todas as seções/abas (1 Profile basics … 6 Home screen appearance); abrir cada diálogo acionável sem confirmar ação destrutiva; sair.

**Entrega:** `inventario.md` com uma linha por item: `papel | menu > tela > seção | item | tipo | visível? | habilitado?`, e as capturas correspondentes. Esse arquivo é a referência de "nada desaparece" para todas as tarefas.

**Constatações:** cada login entra; cada tela abre sem erro visível; o inventário cobre todo o menu de cada papel.

## J00-T — Linha de base do tablet de DEV

**Tarefa:** 0.2. **Pré-requisito:** tablet físico matriculado no DEV.

**Passos:** registrar modelo, serial e versões do launcher e do suporte remoto; foto/gravação do kiosk em retrato e em paisagem (papel de parede); abrir Configurações e tentar desativar e desinstalar o suporte remoto (registrar o que acontece hoje); no painel, abrir acesso remoto, ver a tela e tocar num ícone pelo painel; conferir o efeito no tablet.

**Constatações:** estado atual de cada item, registrado como está (inclusive o que está com defeito).

## J01 — Tabela de Dispositivos responsiva

**Tarefa:** 1.1.

**Passos:** entrar como `admin`; abrir Dispositivos; em cada largura e tema, rolar a tabela, localizar cada coluna do inventário, abrir o menu de ação de uma linha, usar busca/filtro, paginar; repetir com um papel restrito que veja Dispositivos.

**Constatações:** a tabela acompanha a largura da janela (não fica travada); em telas estreitas, rola horizontalmente só dentro da própria área ou reorganiza, sem cortar ações; nenhuma coluna, botão ou ação a menos que no J00; o tema escuro permanece legível.

## J02 — Cada permissão, com e sem

**Tarefas:** 2.1 (observação) e 2.3 (reexecução após correção).

**Passos:** para **cada** permissão, criar pela tela (ou reutilizar) um papel que a tenha e um idêntico sem ela; entrar com cada um; executar a ação que a permissão promete (ver a tela, criar, editar, salvar, excluir, enviar); registrar o que aconteceu.

**Constatações por permissão:** com permissão, a ação funciona até o fim (dado salvo e visível ao reabrir); sem permissão, a ação não está disponível **e**, na 2.3, a tentativa pela sessão do navegador é recusada pelo servidor, com mensagem compreensível na tela; o nome e a descrição exibidos descrevem o que aconteceu.

## J03 — Nomes e descrições

**Tarefa:** 2.2.

**Passos:** abrir a tela de papéis/permissões em pt-BR e nos demais idiomas disponíveis; ler cada permissão; comparar com a matriz da 2.1.

**Constatações:** nenhuma descrição em idioma errado (ex.: russo em tela pt-BR); todo nome ou descrição corrigido corresponde ao efeito observado no J02; nenhuma permissão sumiu da lista; papéis existentes mantêm as permissões que tinham.

## J04 — Perfil de dispositivo: ver × editar × nada

**Tarefas:** 2.3, 3.2 e 3.4.

**Passos:**
1. `f.perfil-nada`: procurar o menu de perfis de dispositivo e abrir a URL da tela colando o endereço na barra (como uma pessoa curiosa faria).
2. `f.perfil-ver`: abrir um perfil, percorrer as seções 1–6, tentar alterar um campo e salvar.
3. **Sem permissão tenta de verdade:** na sessão do `f.perfil-ver`, pelo DevTools, reenviar a requisição de salvar o perfil com um campo alterado.
4. `f.perfil-editar`: alterar um campo inócuo combinado na OS, salvar, sair, entrar como `admin` e ver o valor novo; desfazer a alteração.

**Constatações:** (1) não vê o menu, e a URL direta não mostra os dados; (2) vê tudo em modo somente leitura, sem salvar; (3) o servidor recusa, e ao reabrir como `admin` o perfil **não mudou**; (4) a alteração persiste e é visível para o `admin`; nada do inventário J00 do `admin` sumiu.

## J05 — Seletor real de permissões nos papéis

**Tarefas:** 3.2 e 3.3.

**Passos:** como `f.superadmin`, abrir a tela de papéis; criar o papel `F Perfil Ver` marcando pelo seletor apenas "ver perfil de dispositivo" (e o mínimo para entrar); salvar; reabrir e conferir o que ficou marcado; atribuir ao `f.perfil-ver`; entrar como ele e conferir o efeito; voltar, trocar para "editar", salvar; entrar de novo e conferir o novo efeito.

**Constatações:** o seletor lista as permissões reais com nome legível; o que foi marcado persiste ao reabrir; a mudança vale no login seguinte do usuário; não há controle "decorativo" na tela (todo controle visível tem efeito).

## J06 — Papel de parede em paisagem

**Tarefa:** 4.1. **Tablet de DEV.**

**Passos:** no painel, conferir a imagem do perfil (6 Home screen appearance); no tablet, com a tela do kiosk em retrato, fotografar/gravar; girar para paisagem; aguardar; gravar; voltar para retrato.

**Constatações:** em paisagem, a imagem cobre a tela sem faixas, esticão ou corte do conteúdo relevante; o retrato continua como na J00-T.

## J07 — Suporte remoto não desativável

**Tarefa:** 4.2. **Tablet de DEV.**

**Passos:** como pessoa no tablet: Configurações > Apps > suporte remoto > Desinstalar / Forçar parada / Desativar; Configurações > Acessibilidade > desligar o serviço; Configurações > Segurança > Apps de administração do dispositivo > desativar; pressionar e segurar o ícone (se visível) > Desinstalar.

**Constatações:** nenhuma das tentativas remove ou desativa o suporte remoto; depois de todas, o acesso remoto pelo painel continua funcionando (J09 curto).

## J08 — Kiosk e "Proteger Configurações" automáticos

**Tarefa:** 4.3. **Tablet de DEV.**

**Passos:** matricular ou reinstalar o agente no tablet de DEV; conceder as permissões pedidas na tela do tablet; **sem tocar no painel**, aguardar; tentar sair do kiosk e abrir Configurações.

**Constatações:** o kiosk fica bloqueado sozinho; as Configurações ficam protegidas sozinhas; o painel mostra os dois estados como ativos sem clique manual.

## J09 — Acesso remoto com interação real

**Tarefa:** 4.4 (e trecho curto em J07/J08).

**Passos:** no painel, abrir o acesso remoto do tablet de DEV; ver a tela ao vivo; tocar num ícone pelo painel; digitar num campo; voltar à tela inicial pelo painel.

**Constatações:** a imagem aparece e atualiza; o toque pelo painel abre o app no tablet (gravado no próprio tablet); a digitação chega; a sessão encerra limpa.

## J10 — Fechamento: nada desapareceu

**Tarefa:** 5.1.

**Passos:** repetir J00 inteiro (e J00-T, se houver tablet) e gerar o `inventario.md` novo; comparar linha a linha com o da 0.1.

**Constatações:** todo item da linha de base continua presente e habilitado para quem o tinha. Diferenças aceitas: só as previstas nas tarefas ACEITAS (ex.: um papel restrito deixa de ver algo que a matriz aprovada tirou dele), cada uma citando a tarefa.
