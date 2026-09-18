## Regras de execução fiscal

Cada item é uma tarefa que o FISCAL conduz por Ordem de Serviço (OS), com sub-agentes reais, conforme `execucao/`. `[x]` só é marcado depois de três coisas:
- parecer `ACEITA` do VALIDADOR (sub-agente diferente do executor);
- `python3 fiscal/guarda.py aceite` sem erro;
- assinatura humana em `fiscal/assinaturas/<tarefa>.md`.

- **Proibido teste automatizado**: nenhum arquivo de teste (`*Test.java`, `test_*.py`, `*.spec.*`, `*.test.*`, `__tests__/`, `src/test/**`, `androidTest/**`) é criado ou alterado, e nenhuma suíte é usada como prova.
- **Prova = uso real**: pessoa (ou sub-agente agindo como pessoa) usando o console pelo navegador ou o tablet físico, com vídeo, capturas numeradas e diário da jornada. `curl`, status HTTP, log, consulta ao banco e "compilou" são no máximo complemento.
- **Nada desaparece**: toda tarefa roda a comparação contra o inventário da linha de base (J00). Item do inventário ausente = `REPROVADA`.
- **Escopo fechado**: arquivo alterado fora do escopo da OS = `REPROVADA`. Nenhum executor edita `openspec/**`.
- **Somente DEV** (`localhost:8080` = `192.168.1.65:8080`). Qualquer ação no 192.168.1.75 ou em `mdm.olimpia.sp.gov.br` = tarefa `BLOQUEADA` e escalonamento.
- Caminhos relativos a `repo-mdm/`. As jornadas estão em `execucao/03-jornadas.md`.

## 0. Linha de base (antes de qualquer alteração)

- [ ] 0.1 Inventário visual completo do console DEV: todos os menus, telas, abas, botões, colunas, campos e ações, com `admin` e com cada papel existente (Super-Admin, Admin, User, Observer, Helpdesk 1/2/3, Guest). Os usuários de teste são criados pela tela.
  - Jornada: J00
  - Entrega: `evidencias/.../0.1/inventario.md` (lista navegável) + capturas + vídeo; nenhum arquivo do repositório muda
  - Cobre: PRD-02, PRD-04
- [ ] 0.2 Linha de base do tablet de DEV: kiosk, papel de parede em retrato e paisagem, suporte remoto instalado, acesso remoto com toque pelo painel.
  - Jornada: J00-T
  - Pré-requisito humano: tablet físico matriculado no DEV (.65) e acessível. Sem ele, fica `BLOQUEADA`.
  - Cobre: PRD-02, PRD-04, PRD-11

## 1. Dispositivos (prioridade imediata)

- [ ] 1.1 Tabela de dispositivos responsiva: remover o travamento de largura do `.table-responsive` na tela Dispositivos, sem perder coluna, botão ou ação.
  - Escopo provável: `server-source/server/src/main/webapp/app/components/main/view/devices.html`, CSS do console. O executor confirma o arquivo exato e a OS fixa o escopo.
  - Jornada: J01
  - Cobre: PRD-06, PRD-02, PRD-04

## 2. Auditoria e correção das permissões existentes

- [ ] 2.1 Matriz de permissões: para cada uma das permissões da tabela `permissions` (hoje 41), registrar nome, descrição atual, propósito declarado, onde é verificada (servidor e tela, `arquivo:linha`) e o efeito **observado em uso real** (papel com a permissão × papel sem ela). Classificar cada uma: CORRETA | NOME_ERRADO | NAO_FAZ_NADA | FAZ_OUTRA_COISA | SO_NA_TELA (servidor não verifica).
  - Entrega: `docs/PERMISSOES.md`
  - Jornada: J02
  - Cobre: PRD-14
- [ ] 2.2 Corrigir nome e descrição (pt-BR, e demais idiomas existentes) de toda permissão classificada NOME_ERRADO ou FAZ_OUTRA_COISA. Os ids e o identificador técnico são preservados, a menos que a OS justifique a troca com migração. Nenhuma permissão é apagada.
  - Jornada: J03
  - Depende de: 2.1 ACEITA
  - Cobre: PRD-14, PRD-15, PRD-04
- [ ] 2.3 Fazer cada permissão classificada NAO_FAZ_NADA ou SO_NA_TELA cumprir o propósito: verificação no servidor (recusa a chamada REST) e na tela (oculta ou somente leitura).
  - Jornada: J02 (reexecução completa) + J04
  - Depende de: 2.1 ACEITA
  - Cobre: PRD-15, PRD-03

## 3. Segregação ver/editar e seletor real

- [ ] 3.1 Proposta de matriz de áreas × níveis (sem acesso | ver | editar) para as áreas do console, começando pelo perfil de dispositivo. Entrega: seção em `docs/PERMISSOES.md`. **Gate humano:** a matriz precisa de aprovação do responsável, registrada em `fiscal/assinaturas/3.1-matriz.md`, antes de qualquer implementação da 3.2.
  - Jornada: nenhuma (documento). Parecer do VALIDADOR sobre a coerência com o que existe no console.
  - Depende de: 2.1 ACEITA
  - Cobre: PRD-07, PRD-08
- [ ] 3.2 Implementar a matriz aprovada: permissões novas via Liquibase, sem apagar existentes; verificação no servidor e na tela para cada área.
  - Jornada: J04, J05
  - Depende de: 3.1 ACEITA com aprovação humana
  - Cobre: PRD-03, PRD-07, PRD-08
- [ ] 3.3 Substituir o combobox placeholder de permissões na tela de papéis por um seletor real: lista as permissões existentes com nome legível, grava, recarrega mostrando o que foi salvo, e o que foi salvo vale no próximo login do usuário.
  - Jornada: J05
  - Depende de: 3.2 CONSTRUIDA
  - Cobre: PRD-10
- [ ] 3.4 Caso reportado: usuário sem permissão de editar o perfil de dispositivo não consegue alterá-lo, nem pela tela nem por chamada direta feita pelo próprio navegador da sessão dele.
  - Jornada: J04 (roteiro "sem permissão tenta salvar")
  - Depende de: 3.2 CONSTRUIDA
  - Cobre: PRD-10

## 4. Tablet (bloqueadas até existir tablet no DEV — ver 0.2)

- [ ] 4.1 Papel de parede do dispositivo responsivo em paisagem, sem piorar o retrato.
  - Jornada: J06
  - Cobre: PRD-09
- [ ] 4.2 Suporte remoto: a pessoa no tablet não consegue desativar (acessibilidade, admin, forçar parada, desativar app) nem desinstalar o app de suporte remoto.
  - Jornada: J07
  - Cobre: PRD-12
- [ ] 4.3 Kiosk bloqueado e "Proteger Configurações" armados automaticamente logo que as permissões do tablet ficam concedidas, sem clique no painel.
  - Jornada: J08
  - Cobre: PRD-13
- [ ] 4.4 Não regressão do acesso remoto com interação real (ver a tela e tocar pelo painel), repetida depois de 4.1, 4.2 e 4.3.
  - Jornada: J09
  - Cobre: PRD-11, PRD-02

## 5. Fechamento

- [ ] 5.1 Reexecução completa de J00 (e J00-T, se houver tablet) comparada item a item com a linha de base. Tudo que existia continua existindo e funcionando para o papel que tinha acesso.
  - Jornada: J10
  - Depende de: todas as anteriores ACEITAS ou BLOQUEADAS com decisão humana registrada
  - Cobre: PRD-02, PRD-04, PRD-16
