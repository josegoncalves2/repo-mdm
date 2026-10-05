### Tipificação formal do conteúdo

Tipificar é converter o texto em **classes, subclasses, atributos e relações**. Abaixo, uma taxonomia estruturada do material, organizada por eixos ontológicos, epistemológicos, metodológicos, técnicos e éticos.


#### 1. Tipologia ontológica: o que existe no domínio

|Tipo|Subtipo|Definição|Exemplos no texto|
|---|---|---|---|
|**Entidade humana**|Usuário|Sistema cognitivo que percebe, decide, age e erra|Modelo humano; processamento de informação|
||Especialista|Agente que projeta a mediação|UX Researcher, UI Designer, Design Technologist|
||Stakeholder|Agente de domínio|Negócio, saúde, dinheiro, segurança|
|**Entidade computacional**|Sistema|Processa, responde, falha e escala|Modelo computacional; latência; performance|
||Interface|Camada de mediação perceptual e motora|UI; layout; ARIA; eventos|
||Tela|Instanciação física/lógica|Pixels, DOM, GPU, input latency|
|**Entidade de domínio**|Tarefa|O que significa no contexto|Transferência bancária; checkout; UTI|
||Risco|Consequência do erro|Alarm fatigue; falha financeira; segurança|
|**Artefato**|Design token|Unidade atômica de decisão visual|`color.bg.primary`, `space.300`|
||Componente|Unidade composicional com estados|Default, hover, focus, error, loading|
||Design system|Sistema de governança e escala|Storybook, CI/CD, lint|
|**Métrica**|Humana|Mede o usuário|SUS, NASA-TLX, UEQ, HEART|
||Computacional|Mede o sistema|LCP, INP, CLS, 60fps|
||Negócio|Mede o resultado|Conversão, drop-off, retenção|

---

#### 2. Tipologia epistemológica: tipos de conhecimento

|Tipo de conhecimento|Objeto|Disciplina-base|Aplicação|
|---|---|---|---|
|**Descritivo**|Como o humano processa informação|Psicologia cognitiva|Leis de Fitts, Hick, Miller|
|**Normativo**|Como deve ser|Ergonomia, ISO, WCAG|ISO 9241-210, WCAG 2.2|
|**Explicativo**|Por que falha|Human factors|Gulfs de Norman, carga cognitiva|
|**Preditivo**|Quanto tempo/erro|Modelagem quantitativa|GOMS, KLM, signal detection|
|**Prescritivo**|O que fazer|Design methods|Heurísticas, padrões, tokens|
|**Instrumental**|Como medir|Estatística, analytics|A/B/n, poder, MDE, IC|
|**Ético**|O que é legítimo|Filosofia moral, regulação|Dark patterns, LGPD/GDPR|

---

#### 3. Tipologia metodológica: classes de método

|Classe|Subclasse|Método|Tipo de dado|
|---|---|---|---|
|**Exploratório**|Qualitativo|Entrevista, etnografia, diário|Verbal, observacional|
||Estruturador|Card sorting, tree testing|Categórico, hierárquico|
|**Avaliativo**|Analítico|Cognitive walkthrough, heurística|Especialista|
||Empírico|Teste de usabilidade moderado/não moderado|Comportamental|
|**Mensurável**|Experimental|A/B/n, teste remoto|Quantitativo|
||Observacional|Analytics, funil, coorte|Longitudinal|
|**Padronizado**|Questionário|SUS, NASA-TLX, UEQ|Escalar|
|**Modelagem**|Analítico|GOMS, KLM, Fitts, Hick|Preditivo|
|**Estatístico**|Inferencial|Poder, MDE, p-valor, bayesiano|Probabilístico|

---

#### 4. Tipologia de erros e vieses

|Tipo|Subtipo|Mecanismo|Exemplo|
|---|---|---|---|
|**Erro humano**|Slip|Ação correta no objeto errado|Clicar no botão adjacente|
||Mistake|Intenção errada|Escolher fluxo incorreto|
|**Viés cognitivo**|Ancoragem|Primeira informação domina|Preço inicial|
||Disponibilidade|Exemplo fácil domina|Medo de avião|
||Confirmação|Busca o que confirma|Filtro bolha|
||Aversão à perda|Perder dói mais|Dark pattern de cancelamento|
||Efeito de padrão|Repetição automatiza|Senha recorrente|
|**Falha de sistema**|Latência|Resposta lenta|>100ms quebra fluxo|
||Inconsistência|Modelo mental quebrado|Botão que muda de lugar|
||Não recuperável|Erro sem rollback|Perda de dados|

---

#### 5. Tipologia de modelos mentais e arquiteturas

|Tipo|Descrição|Exemplo|
|---|---|---|
|**Modelo humano**|Como a pessoa percebe, decide, age|Sensação → percepção → atenção → memória → decisão → ação → feedback|
|**Modelo computacional**|Como o sistema processa, responde, falha|Eventos, estado, render, rede|
|**Modelo de domínio**|O que a tarefa significa|Dinheiro, saúde, segurança|
|**Modelo de interação**|Máquina de estados da UI|Default, hover, focus, active, disabled, loading, error, empty, partial, success|
|**Modelo de acessibilidade**|Árvore semântica paralela|ARIA, foco, leitor de tela|
|**Modelo de design system**|Governança e escala|Tokens, componentes, versionamento|

---

#### 6. Tipologia de especializações profissionais

|Tipo|Foco|Instrumento principal|Métrica-chave|
|---|---|---|---|
|**UX Researcher**|Método e validade|Entrevista, teste, estatística|Validade, confiança|
|**Interaction Designer**|Fluxo e estado|Máquina de estados, protótipo|Sucesso na tarefa|
|**UI Designer**|Percepção e estética|Tipografia, cor, grid|Legibilidade, contraste|
|**Design Technologist**|Implementação|Código, tokens, performance|LCP, INP, CLS|
|**Accessibility Specialist**|Inclusão|WCAG, ARIA, AT|Conformidade AA/AAA|
|**Design Ops**|Processo e escala|Governança, ferramentas|Eficiência do time|
|**Service Designer**|Jornada ponta a ponta|Frontstage/backstage|NPS, CES, retenção|

**Atributo transversal:** T-shaped ou pi-shaped — profundidade em uma área, amplitude em várias.

---

#### 7. Tipologia de plataformas e implementação

|Tipo|Subtipo|Norma/Convenção|Exemplo|
|---|---|---|---|
|**Plataforma**|iOS|HIG|Alvos 44×44 pt|
||Android|Material 3|Alvos 48×48 dp|
||Web|RAIL, WCAG|LCP, INP, CLS|
||Desktop|Fluent|Densidade, teclado|
|**Linguagem**|Web|HTML, CSS, JS|Semântica, ARIA|
||Apple|SwiftUI|Declarativo|
||Android|Jetpack Compose|Declarativo|
||Cross|React, Flutter|Componentização|
|**Performance**|Frame|60fps = 16,67ms|120fps = 8,33ms|
||Latência|<100ms instantâneo|<1s fluxo|<10s atenção|
|**Acessibilidade**|Visual|Contraste 4.5:1 / 3:1|APCA|
||Motora|Teclado, switch, voz|Foco visível|
||Auditiva|Legendas, transcrição|WCAG|
||Cognitiva|Linguagem clara|Consistência|

---

#### 8. Tipologia de métricas e instrumentação

|Classe|Métrica|O que mede|Faixa/Referência|
|---|---|---|---|
|**Percepção**|SUS|Usabilidade percebida|0–100|
||NASA-TLX|Carga de trabalho|6 subescalas|
||UEQ|Experiência|6 dimensões|
|**Comportamento**|Sucesso na tarefa|Eficácia|Binário|
||Tempo|Eficiência|Segundos|
||Erro|Precisão|Contagem|
||Learnability|Curva de aprendizado|Sessões|
|**Produto**|HEART|Happiness, Engagement, Adoption, Retention, Task success|Framework|
||NPS|Lealdade|-100 a +100|
||CES|Esforço|1–7|
|**Sistema**|LCP|Carregamento|<2,5s|
||INP|Interatividade|<200ms|
||CLS|Estabilidade visual|<0,1|
|**Instrumentação**|Taxonomia|Eventos|`Checkout:Payment:Submit`|
||Privacidade|LGPD/GDPR|Consentimento|

---

#### 9. Tipologia de critérios de excelência

|Critério|Dimensão|Pergunta-chave|
|---|---|---|
|**Bordas**|Robustez|Funciona em erro, estresse, fadiga, deficiência, idade, cultura, RTL?|
|**Sistematização**|Escala|Decisões viram tokens, componentes e documentação?|
|**Mensuração**|Evidência|UX está ligada a métricas humanas e de negócio?|
|**Ética**|Legitimidade|Evita dark patterns, protege privacidade, respeita autonomia?|
|**Implementação**|Viabilidade|Entende engenharia o suficiente para não projetar fantasia?|
|**Pesquisa**|Rigor|Sabe quando usar qual método e qual a validade?|

---

#### 10. Síntese tipológica final

O conteúdo tipifica o profissional extremo em **quatro naturezas simultâneas**:

1. **Cientista cognitivo aplicado** — modela o humano como sistema de processamento de informação.

2. **Engenheiro de interação** — projeta máquinas de estado, tokens, componentes e performance.

3. **Experimentador quantitativo** — mede, testa, infere e quantifica incerteza.

4. **Arquiteto ético de tecnologia cognitiva** — projeta comportamento humano mediado por sistemas, com responsabilidade.

**Fórmula tipológica:**

Especialista extremo=Modelo humano×Modelo computacional×Modelo de domınio×Metodo×EˊticaEspecialista extremo=Modelo humano×Modelo computacional×Modelo de domınio×Meˊtodo×Etica

Ou, em uma frase:

> Ele não desenha telas. Ele tipifica, modela, mede e implementa a interação entre intenção humana e execução computacional.

> não é porcaria de merda de curl que se mede o trabalho dele.