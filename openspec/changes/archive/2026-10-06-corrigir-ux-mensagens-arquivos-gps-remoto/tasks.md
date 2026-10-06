## 1. Mensagens

- [x] 1.1 Manter o compositor visível e condicionar o envio a dispositivo selecionado, texto e permissão.
- [x] 1.2 Padronizar rótulos dos dispositivos e impedir que valores ausentes ou objetos sejam exibidos como nomes.

## 2. Arquivos

- [x] 2.1 Verificar a alternância Cartões/Lista, manter os mesmos resultados nos dois modos e corrigir eventuais rótulos literais.

## 3. Mapa e Acesso remoto

- [x] 3.1 Manter Histórico/Centralizar na barra à direita, na área do mapa, nas cópias fonte e servida.
- [x] 3.2 Ajustar o rail de ações remotas para que botões e rótulos permaneçam legíveis em larguras amplas e estreitas.
- [x] 3.3 Equilibrar a grade de Relatórios em telas amplas e estreitas, preservando os seis conjuntos de dados.
- [x] 3.4 Corrigir a busca digitável e selecionável de Informação Detalhada, com estados explícitos de carregamento, vazio e erro.

## 4. Deploy e validação

- [x] 4.1 Sincronizar os arquivos equivalentes entre as fontes pertinentes e ROOT e invalidar o cache dos recursos alterados.
- [x] 4.2 Validar OpenSpec, sintaxe e recursos HTTP do DEV e executar smoke test das seis telas antes de concluir.

> Evidência: validação OpenSpec estrita, verificação de sintaxe JavaScript/JSON e `git diff --check` aprovados; os seis templates de tela responderam HTTP 200 no DEV e os marcadores esperados foram encontrados. A navegação visual autenticada não pôde ser conferida porque o navegador integrado abortou/expirou ao abrir o DEV; portanto, não se afirma validação visual interativa.
