## Context

O painel AngularJS tem cópias web relevantes em `server-source/server/src/main/webapp/` e em `source/volumes/webapps/ROOT/`; o plugin deviceinfo também tem fonte própria em `server-source/plugins/deviceinfo/src/main/webapp/`. A aplicação usa templates carregados por `ng-include` com query strings de cache. O template de Mensagens da fonte e o ativo do DEV divergiram: a fonte apresenta um seletor nativo, enquanto o DEV apresenta uma lista clicável e só monta o compositor após a seleção. O controller de chat envia usando `selectedDevice`, então qualquer template que apenas atualize `chat.deviceNumber` não satisfaz o contrato do controller.

Os templates de Arquivos já contêm os modos Cartões/Lista e traduções novas, mas os assets têm versões de cache anteriores às últimas mudanças. Além das versões dos recursos em `index.html` e dos `ng-include` em `content.html`, as rotas em `app.js` também reutilizam a mesma query string antiga para `content.html`; ela precisa ser atualizada junto com a versão do próprio script para que a aplicação deixe de usar o template pai em cache. No GPS, Histórico/Centralizar pertencem à barra da área do mapa. No Acesso remoto, a interface existente possui uma coluna de ações à direita e regras compartilhadas de layout que precisam preservar responsividade. Relatórios contém seis visualizações agregadas que devem se distribuir sem uma linha superior de quatro cartões e uma linha inferior com apenas dois. Informação Detalhada já consulta o endpoint de busca de dispositivos e usa typeahead, mas precisa oferecer estados visíveis e uma seleção inequívoca também no fluxo acionado pelo botão Buscar.

## Goals / Non-Goals

**Goals:**
- Tornar o compositor de Mensagens sempre visível para usuários que podem acessar a tela, sem permitir envio sem destino/permissão/texto.
- Padronizar a apresentação segura dos nomes/identificadores.
- Preservar a alternância de Arquivos e suas traduções exclusivamente nessa tela.
- Posicionar as ações do GPS na área do mapa e manter os controles do Acesso remoto legíveis.
- Organizar Relatórios em até três colunas amplas, reduzindo a quantidade de espaços vazios em telas grandes e adaptando a grade em telas menores.
- Permitir pesquisar e selecionar dispositivos em Informação Detalhada, com feedback explícito de carregamento, ausência de resultados e falha de solicitação.
- Manter fonte e cópia servida consistentes nos arquivos diretamente envolvidos e invalidar seus caches.

**Non-Goals:**
- Redesenhar a navegação ou outras telas do painel.
- Adicionar a alternância Cartões/Lista fora da tela de Arquivos.
- Alterar APIs, permissões de backend, protocolo de acesso remoto, comportamento do APK ou componentes de produção.
- Reiniciar a stack ou modificar dados persistidos quando os arquivos estáticos servidos podem ser atualizados pelo volume montado.

## Revisão UX/UI

A revisão das capturas fornecidas identificou seis problemas de usabilidade que orientam a implementação:

- **Mensagens:** o compositor não podia ser localizado sem selecionar um dispositivo. O campo deve permanecer visível, com envio desabilitado até haver dispositivo, permissão e texto.
- **Arquivos:** as chaves de tradução apareciam literalmente e o controle de Cartões/Lista deve continuar restrito a esta tela.
- **Mapa GPS:** os filtros e ações estavam visualmente separados do título e do mapa; os filtros devem alinhar-se à direita do título e o histórico deve permanecer no contexto do mapa.
- **Acesso remoto:** a coluna de ações precisa conservar rótulos e alvos clicáveis sem comprimir a visualização, reorganizando-se em telas estreitas.
- **Relatórios:** quatro cartões na mesma linha e cartões esticados criavam colunas estreitas e grandes áreas vazias; a grade deve usar até três colunas em telas amplas, duas em médias e uma em estreitas, alinhando os cartões ao conteúdo.
- **Informação Detalhada:** a busca precisa tornar sugestões selecionáveis e explicitar carregamento, ausência de resultados e falhas, sem manter detalhes antigos para uma consulta nova.

A revisão não autoriza mudanças de enrollment: preservar o APK aprovado 6.36 e os três perfis existentes é requisito de escopo. Nenhum APK ou configuração de perfil deve ser alterado nesta mudança.

## Decisions

1. **Manter AngularJS e os controllers atuais.** A correção ficará em templates, formatação local de identificadores e CSS/i18n já usados. Isso evita introduzir dependências ou alterar endpoints. Um seletor de dispositivo só atualiza `selectedDevice` via `selectDevice`; o envio continuará usando esse estado como fonte única.

2. **Exibir o compositor independentemente da seleção.** O campo de texto ficará no painel principal mesmo quando nenhum dispositivo estiver selecionado; a ação de enviar será desabilitada até haver seleção, permissão e conteúdo. Isso resolve a ausência do controle sem enfraquecer as verificações existentes.

3. **Tratar identificadores como texto somente quando forem primitivos.** O template/controlador escolherá campos conhecidos de identificador/modelo e ignorará objetos ou valores ausentes, evitando `undefined` e representações de objeto bruto.

4. **Preservar a estrutura visual existente.** O seletor Cartões/Lista permanece na barra de Arquivos. Histórico/Centralizar ficam no toolbar do painel do mapa. A coluna de ações remotas mantém largura mínima em telas amplas e empilha abaixo do preview em telas estreitas.

5. **Atualizar as duas cópias de forma cirúrgica.** Aplicar as alterações equivalentes tanto à fonte quanto a `ROOT`, sem substituir templates GPS legados inteiros. Incrementar as query strings do script `app.js`, do template pai `content.html`, dos templates filhos, controllers, traduções e CSS alterados para forçar o navegador a buscar o conteúdo atualizado. O DEV lê a cópia ROOT pelo volume Docker; não é necessário rebuild nem alteração da produção.
6. **Manter a busca de Informação Detalhada orientada a seleção.** Reutilizar `rest/private/devices/search` e a resposta já usada pelo typeahead; apresentar cada resultado como dispositivo selecionável e carregar detalhes apenas depois da seleção. Não inventar endpoint nem formato de resposta.
7. **Distribuir Relatórios em uma grade limitada e responsiva.** Organizar as seis visualizações agregadas em até três colunas em telas amplas, sem alterar seus dados nem criar controles de Cartões/Lista nessa ou em outras telas.

Alternativas consideradas: depender de limpeza manual do cache (rejeitada porque deixa usuários com arquivos antigos); fazer o compositor depender de clique em um dispositivo (rejeitada porque foi o comportamento reportado como ausente); substituir toda a fonte GPS pela cópia ROOT (rejeitada por poder eliminar diferenças não relacionadas).

## Risks / Trade-offs

- [As cópias podem continuar divergindo em trechos não relacionados] → comparar e sincronizar apenas o markup/regra correspondente, e verificar ambos os caminhos.
- [Campos de identificador podem chegar com tipos inesperados] → limitar a renderização a strings/números primitivos e cobrir valores ausentes no helper.
- [O layout remoto pode mudar de posição em telas médias] → manter o breakpoint existente, validar o CSS e conferir visualmente em viewport amplo e estreito.
- [A página ativa pode manter estado/cache de sessão] → invalidar URLs de recursos e validar diretamente as respostas do serviço e a tela após recarregar.

## Migration Plan

1. Atualizar templates, controller/helper, estilos e traduções em fonte e ROOT.
2. Incrementar as versões de cache dos scripts, CSS e views alterados.
3. Validar sintaxe, OpenSpec e respostas servidas pelo DEV; confirmar conteúdo novo em cada recurso.
4. Fazer smoke test nas seis telas no DEV e conferir console/requests quando o navegador estiver acessível.
5. Em caso de regressão, reverter somente os arquivos e versões de cache desta alteração; não reconstruir nem reiniciar serviços.

## Open Questions

- A validação visual autenticada depende do navegador integrado conseguir acessar o host DEV; se essa conexão falhar, registrar a limitação e não afirmar validação visual concluída.
