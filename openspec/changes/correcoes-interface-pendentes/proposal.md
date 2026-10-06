# correcoes-interface-pendentes — Correções diversas de interface e comportamento

Origem: `docs/problemas.md`
Status: A VERIFICAR — vários itens podem já ter sido corrigidos por outros agentes/sessões.

## Motivação

Lista de bugs e correções pontuais identificadas pelo responsável do produto. Cada item é um problema específico de interface, comportamento ou configuração.

## Itens

### Acesso Remoto

- [ ] Tela bloqueada do tablet: garantir que acesso remoto funcione mesmo com tela bloqueada
- [ ] Tela desligada pelo usuário: acesso remoto NÃO deve ser encerrado
- [ ] Acesso remoto é soberano ao admin/helpdesk/suporte — livre e incondicional
- [ ] Trocar de dispositivo ou tela: acesso remoto perdido (tela preta) — corrigir controller
- [ ] F5 no interface: continua sorteando destino, não volta pro mesmo menu/sessão

### Quiosque

- [ ] Aba "Política de bloqueio": botões não funcionam, testei um por um e não surtem resultado no dispositivo
- [ ] Aba "Apps permitidos": botões `instalar`, `permitir no quiosque` e `app inicial` estourados (CSS e texto)

### Web Filter

- [ ] Dashboard > "Tráfego bloqueado recentemente": não exibe nada, não lê os dados quando usuário tenta acessar site bloqueado
- [ ] Frontend totalmente desconectado do backend — não funciona embora backend faça bloqueio no tablet
- [ ] "Recently blocked traffic" não registra mais nada (só registros antigos)
- [ ] Comportamento "macro" (seleciona URL, apaga, digita URL de bloqueio) — ridículo, não redireciona
- [ ] Página de bloqueio customizada: `http://mdm.pmeto.local` (DEV) e `https://mdm.olimpia.sp.gov.br` (PROD) — configurável na interface
- [ ] Histórico infinito vai estourar a página: adicionar paginação, registros por página, opção de limpar histórico (tudo ou selecionados)

### Informação Detalhada

- [ ] Campo de pesquisa deve aceitar outros termos e opções de busca, não apenas "Número do dispositivo"

### Módulos e Integrações

- [ ] Menu Módulos: erros de parsing, CSS, botões estourados
- [ ] Menu Integrações: erros de parsing, CSS, botões estourados

### UX geral

- [ ] Todos os botões em todas as sessões/menus não possuem descrição (tooltip) ao passar o mouse — prejudica usabilidade e aprendizagem
- [ ] Tela de bloqueio no device: tela apresentável de bloqueio + gerência dessa página na interface

### Permissões RBAC

- [ ] Segregação de permissões entre grupos (hoje impossível de trabalhar)
- [ ] Permissionar quem pode alterar configurações do perfil de dispositivo e outras áreas
- [ ] Permissões segregáveis: ver OU editar (hoje é tudo ou nada)
- [ ] Combobox de permissões placeholder → combobox real (atual é placebo — usuário sem permissão ainda altera)
- [ ] Verificar permissões: nomes estranhos, nome diz fazer uma coisa mas faz outra
- [ ] Identificar e corrigir permissões que não cumprem propósito real

### Tablet / APK

- [ ] APK ainda permite desativar/desinstalar suporte remoto — corrigir
- [ ] Após ativação de permissões, bloqueio do kiosk + Proteger Config devem ser automáticos (hoje manual por device)
- [ ] Papel de parede responsivo em modo paisagem (perfeito em retrato, quebrado em paisagem)
- [ ] Kisok parou de funcionar.

### Infra / Docker

- [ ] Containers nomeados: hwmdm-webfilter, hwmdm-mdm, hwmdm-postgres, hwmdm-admin
- [ ] docker-compose.yaml único com a stack inteira
- [ ] Nada hardcoded: tudo configurável via variável/.env E disponível na interface web
- [ ] Botões para gerenciar containers (reiniciar etc) na interface web
- [ ] Persistência: garantir que nada é temporário/cache, tudo persiste após baixar/subir stack

### Modularidade

- [ ] Tudo modular: desativar 1 item sem parar software todo (hoje tudo amarrado)

### Princípios

- [ ] Nunca alterar o que não foi solicitado
- [ ] Manter todas as características e funcionalidades presentes
- [ ] Garantir que nada foi removido ou esquecido durante testes
