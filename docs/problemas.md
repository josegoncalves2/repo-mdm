

[ ] garanta e disponha que se o dispositivo estiver com a tela bloqueada o acesso remoto aconteça mesmo assim.

[ ] garanta e disponha que se o usuario desligar a tela do tablet o aceso remoto nao seja encerrado.

[ ] garanta e disponha que acesso remoto é livre, é soberano ao administrador/helpdesk/suporte.

[ ] garanta e disponha que tudo deve ser tratado como modular.

[ ] garanta e que nada será removido ou esquecido durante os testes, manter fielmente o projeto intacto realizando apenas as alterações, ajustes e adições solicitadas e necesárias.

[ ] menu `Acesso remoto` - fix: se trocar de dispositivo ou tela o acesso remoto é perdido - tela preta.

[ ] menu `Quiosque` - aba `Politica de bloqueio` - fix: botões não funcionam, testei um por um e não surtem resultado no dispositivo.

[ ] menu `Quiosque` - aba `Apps permitidos` - fix: botões `instalar`, `permitir no quiosque` e `app inicial` estourados css e texto.

[ ] menu `Web Filter` - aba `Dashboard` - fix:`Trafego bloqueado recentemente` não exibe nada, não está exibindo ou não lendo os dados quando o usuário tenta acessar um site ou categoria bloqueada.

[ ] menu `Informação detalhada` - fix: campo de pesquisa deve aceitar outros termos e opções de busca e não deve limitar-se a busca pelo `Número do dispositivo`.

[ ] menu `Módulos` - fix: erros de parsing, css, botões estourados.

[ ] menu `Integrações` - fix: erros de parsing, css, botões estourados.

[ ] add: todos botões em todas as sessões e menus não possuem descrição ao passar o mouse por cima, não cooperando com o a usabilidade nem com a aprendizagem.

[ ] fix: qualquer F5/recarregar de qualquer aba/sessao a tela volta para a tela `Dispositivos` e não para a mesma tela ou a tela anterior, a `Devices` está como default na visualização, completamente errado.

[ ] adicionar uma tela apresentavel de bloqueio no device e dispor da gerencia desta pagina na interface - tela de bloqueio de navegação

[ ] F5 dado no menu Webfilter ainda continua sorteando destino, nao volta pro mesmo menu/sessao

[ ] o menu `Webfilter` encontra-se totalmente inutil. Não registra nenhum acesso bloqueado. O Frontend parece totalmente e completamente desconectado do backend, não funciona, embora o backend esteja fazendo já o bloqueio no tablet. 

[ ] o menu `**Recently blocked traffic**` não registra mais nada, apenas ha alguns registros de ontem.

[ ] nome dos containers obrigatoriamente : hwmdm-webfilter, hwmdm-mdm, hwmdm-postgres, hwmdm-admin.

[ ] docker-compose.yaml um só com a stack inteira, nada separado.

[ ] o menu `Webfilter` bloqueia, resgitra, exibe tela de bloqueio do dispositivo, mas se parece com uma macro, ridiculo, não é redirecionado, algum script seleciona a url, apaga e digita a url de bloqueio, corrija

[ ] a pagina de bloqueio customizada é http://mdm.pmeto.local para (dev/homolog)  e https://mdm.olimpia.sp.gov.br  para (produção), e isso deve ser configuravel dentro da interface web, modo desenvolvimento -> aponta para o SERVIDOR-DEV (dev/homolog) e modo produção -> aponta para o SERVIDOR-PRD (producao)

[ ] o menu `**Recently blocked traffic**` registra tudo, porém está com historico infinito, vai estourar a pagina ja ja, adicione um menu de navegação entre as paginas, quantidade a ser exibido de registros por pagina e também a opção de apagar o historico, tudo e itens selecionados.

[ ] confirme e garanta que nada está temporario eu apenas em cache, que tudo esteja persistente, e que após baixar e subir a stack ou server não perderá dados, informações ou tudo.

[ ] garanta e confirme que não existe nada hardcoded, tudo é configuravel via variavel, .env etc. e tudo que for configuravel deve ser disponibilizado dentro da interface web, assim como os botoes para gerenciar os proprios containers, reinicia-los etc.

[ ] sem sub-agentes seus. Se quiser subagentes, chame o deepseek da openrouter que já tem configurado aqui no vscode. Estes voce pode usar quantos quiser, agentes claude, codex, NUNCA! 

````json
	{
		"name": "DeepSeek-v4-flash",
		"vendor": "customendpoint",
		"apiKey": "${input:chat.lm.secret.-41821777}",
		"apiType": "chat-completions",
		"models": [
			{
				"id": "deepseek-v4-flash:0423",
				"name": "deepseek-v4-flash:0423",
				"url": "https://openrouter.ai/api/v1/chat/completions",
				"toolCalling": true,
				"vision": false,
				"maxInputTokens": 96000,
				"maxOutputTokens": 8192
			}
		]
	}
	
````


[ ] viabilize que seja possivel a segregação de permissoes entre os grupos, poís como está é simplesmente impossível de trabalhar.
    
[ ] GARANTA E disponha da possibilidade de permissionar quem pode alterar as configurações do perfil do dispositivo e outras areas da plataforma, hoje nao existe e o que existe é uma merda.
    
[ ] VIABILIZE QUE as permissoes sejam segregaveis, exemplo: ver ou nao ver o _perfil de dispositivo_, nao tem meio termo atualmente, e também se seja refatorado o menu `Permissoe`

[ ] garanta que papel de parede do dispositivo se ajuste ao modo paisagem, seja responsivo, está perfeito em modo retrato.

[ ] combobox de permissoes placeholder deve ser substituido por combobox real, o atual é placebo. O usuario nao tem permissao para alterar o perfil de dispositivo, mas mesmo assim ele altera, permissoes cagadas e NUNCA testadas. Corrija.
    
[ ] verifique e corrija o apk ainda permite desativar ou desinstalar o _suporte remoto_.
    
[ ] garanta que apos a ativação das permissões, o _bloqueio do kiosk_ seja automatico assim como o _Proteger Configurações_, pois esta sendo necessário realizar isso manualmente para cada device.
    
[ ] verifique as permissoes, nomes estranhos, outros tem o nome que diz fazer uma coisa mas faz outra coisa, verifique cada uma das permissões e ver se estão de acordo com o proposito.
    
[ ] identifique e corrija as permissoes não cumprem o proposito real, nomenclaturas de permissões não condiz com o que de fato elas fazem ou se de fato fazem alguma coisa.
    
[ ] garanta e mantenha todas as características e funcionalidades presentes.

[ ] nunca altere o que não foi solicitado, apenas corrija os pontos mencionados.


[ ] GPS: listar à ESQUERDA     (hwmdm-split-layout, lista antes do mapa) (layout estilo microsfot 365, barra lateral com sub menus aninhados da esquerda para direita)

[ ] GPS historico e rastreabilidade dos devices em tempo real, como o google devices: (DB + JSP + controller + UI)     (trigger no DB, JSP endpoint, controller com history, UI com polyline/export) (layout estilo microsfot 365, barra lateral com sub menus aninhados da esquerda para direita)

[ ] garanta e persista todas alterações (nota: eu exclui o entrypoint porque ele rebuildava e destruia tudo. `/opt/projetos/hwmdm/repo-mdm/source/docker-entrypoint.sh.bak-20261002-095114`)

[ ] DOCKER ENTRYPOINT ESTÁ DESCONSTRUINDO, DESTRUINDO, DETURPANDO, QUEBRANDO, FUDENDO COM TODA A STACK! O CSS utilitário foi destruído pelo overlay de novo.