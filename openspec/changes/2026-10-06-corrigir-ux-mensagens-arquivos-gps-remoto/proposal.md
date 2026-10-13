## Why

As telas de Mensagens, Arquivos, GPS, Acesso remoto, Relatórios e Informação Detalhada não apresentam de forma confiável os controles e dados esperados: o compositor pode ficar oculto, identificadores aparecem mal formatados, o modo de visualização de arquivos não é evidente, ações do mapa/acesso remoto ficam deslocadas ou comprimidas, os gráficos de Relatórios deixam grandes áreas vazias e a busca de Informação Detalhada não orienta a seleção de um dispositivo. O painel servido e a fonte também divergem, fazendo uma correção não necessariamente chegar à interface ativa.

## What Changes

- Manter o compositor de mensagens visível para usuários autorizados, com envio condicionado à seleção de um dispositivo e à permissão adequada.
- Exibir identificadores de dispositivos de forma consistente, sem valores ausentes ou objetos brutos.
- Disponibilizar e localizar a alternância entre cartões e lista na tela de Arquivos.
- Manter Histórico/Centralizar na área do mapa, à direita da lista de dispositivos.
- Corrigir o dimensionamento e a posição das ações do Acesso remoto sem cortar rótulos.
- Organizar os seis painéis de Relatórios em uma grade responsiva equilibrada.
- Oferecer em Informação Detalhada uma busca digitável com sugestões de dispositivos selecionáveis, carregamento, resultado vazio e erros explícitos.
- Manter Cartões/Lista exclusivamente na tela de Arquivos.
- Sincronizar as mudanças correspondentes entre a fonte web e a cópia servida no DEV, invalidando o cache dos assets alterados.

## Capabilities

### New Capabilities
- `panel-interface-ux`: Comportamento e apresentação consistentes nas telas de Mensagens, Arquivos, GPS, Acesso remoto, Relatórios e Informação Detalhada.

### Modified Capabilities

## Impact

- Templates, controllers, estilos e traduções do painel AngularJS e do plugin deviceinfo.
- Cópia implantada em `source/volumes/webapps/ROOT/`, fontes web em `server-source/server/src/main/webapp/` e `server-source/plugins/deviceinfo/src/main/webapp/`.
- Apenas o ambiente DEV em `192.168.1.65:8080`; sem alterações na produção, no backend/API ou no escopo funcional dos módulos.
