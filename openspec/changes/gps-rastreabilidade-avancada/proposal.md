# gps-rastreabilidade-avancada — GPS avançado: rastreabilidade, análise e exportação

Origem: `docs/gps.md`
Status: PENDENTE — todos os itens precisam de verificação e provisionamento no DEV.

## Motivação

A tela de GPS atual é um MVP de localização. Para se tornar uma ferramenta de gestão de rastreabilidade, deve evoluir para uma plataforma de análise temporal e geoespacial, com auditoria, exportação e conformidade legal.

## Itens a verificar e provisionar

- [ ] Layout split (lista esquerda, mapa centro) — PENDENTE
- [ ] Histórico de localização (tabela + trigger + API) — PENDENTE
- [ ] Busca de dispositivos com typeahead — PENDENTE
- [ ] Status online/offline com contadores — PENDENTE
- [ ] Export GPX — PENDENTE

## Escopo — o que falta

### 1. Interface e UX Avançada

- **Painel esquerdo (Filtros):** busca avançada, filtros por grupo, status (online/offline), nível de bateria, tags personalizadas
- **Painel central (Mapa):** múltiplas camadas (Satélite, Trânsito, Ruas), zoom, rotação, visualização 3D (opcional)
- **Painel inferior (Timeline):** slider de tempo para avançar/retroceder histórico de um ou mais dispositivos
- **Painel direito (Detalhes):** ao clicar em ponto no mapa ou timeline, exibir metadados completos da coordenada

### 2. Rastreabilidade e Telemetria

Para cada ponto de localização, registrar e exibir:
- Coordenadas: latitude, longitude, altitude
- Precisão: raio de precisão GPS (metros) para saber se dado é confiável
- Timestamp: data/hora com fuso horário e milissegundos
- Origem da localização: GPS, Wi-Fi ou Torre de Celular (Cell ID)
- Telemetria de contexto: bateria, status da rede (4G/5G/Wi-Fi), velocidade instantânea
- Status do dispositivo: bloqueado, em kiosk, qual usuário logado

### 3. Histórico de Localização (Timeline)

- **Reprodução de rota (Playback):** Play, Pause, Avançar, Retroceder, controle de velocidade
- **Polyline:** rota exata percorrida entre pontos
- **Detecção de paradas (Stop Detection):** agrupar pontos onde device ficou parado por X min, gerar POI com chegada/saída
- **Heatmap:** áreas de maior frequência durante um período
- **Comparação de histórico:** sobrepor rotas de 2+ devices no mesmo período

### 4. Exportação de Dados

Módulo configurável, não botão genérico:
- **Formatos:** CSV/XLSX, PDF (relatórios com gráficos e mapas estáticos), KML/KMZ/GPX, JSON/XML
- **Escopo:** por device, por grupo, frota inteira; por intervalo de tempo; filtrar eventos (velocidade > 60km/h, saída de geocerca)
- **Agendamento:** rotinas automáticas de envio por email (diário, semanal, mensal)

### 5. Análise de Dados (Data Analytics)

- **Geocercas (Geofencing):** perímetros virtuais com alertas de Entrada, Saída, Permanência
- **Alertas baseados em localização:** tempo real se device entrar em área proibida, exceder velocidade, ficar offline
- **Relatórios de produtividade:** km rodados, tempo em movimento vs parado, otimização de rotas
- **API RESTful:** endpoints para consultar último ponto ou histórico completo
- **Integração com sistemas terceiros:** Field Service, ERPs

### 6. Segurança, Privacidade e Conformidade

- **RBAC:** quem pode ver histórico de quem (supervisor vê equipe, técnico vê apenas seu device)
- **Modo Privacidade:** desligar coleta fora do expediente (LGPD)
- **Log de auditoria:** quem visualizou/exportou dados de localização de qual device
- **Política de retenção:** configurar tempo de armazenamento (30 dias, 6 meses, 5 anos) + expurgo automático
- **Criptografia:** TLS em trânsito, banco criptografado em repouso

### 7. Elementos Visuais no Mapa

- **Marker Clustering:** agrupar dispositivos em círculos numéricos quando muitos
- **Círculo de precisão:** raio translúcido de margem de erro
- **Ícones de status:** cores para Online (verde), Offline (cinza), Em Movimento (azul), Alerta (vermelho)
- **Escala gráfica:** régua visual de distância

## Tarefas

- [ ] Verificar estado atual: quais dos itens acima já estão implementados no DEV
- [ ] Polyline com timeline/playback/animação
- [ ] Heatmap
- [ ] Detecção de paradas (Stop Detection)
- [ ] Geocercas (geofencing) com alertas
- [ ] Exportação multi-formato (CSV, XLSX, PDF, KML, JSON) — só GPX existe
- [ ] Marker clustering
- [ ] Círculo de precisão GPS
- [ ] Comparação de histórico entre devices
- [ ] Filtros avançados (grupo, bateria, tags)
- [ ] Múltiplas camadas de mapa (satélite, trânsito)
- [ ] Slider de timeline
- [ ] Relatórios de produtividade
- [ ] RBAC na visualização de GPS
- [ ] Modo privacidade (LGPD)
- [ ] Política de retenção de dados com expurgo
- [ ] Agendamento de envio de relatórios por email
