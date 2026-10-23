# TESTE DE ACEITAÇÃO - DARK/LIGHT MODE & SERVER ADMIN

**Tempo Estimado**: 2 minutos  
**Pré-requisito**: Navegador Chrome/Firefox  

---

## 🧪 TESTE 1: Dark/Light Mode Toggle

### Passos:
1. Abra o navegador e acesse: **192.168.250.249:8080**
2. Faça login com: `admin` / `admin123`
3. Procure o botão **🌙 (lua)** na barra superior (canto direito antes do menu usuário)

### Validações:
- [ ] Botão 🌙 está visível na topbar
- [ ] Ao clicar, a interface muda para tema escuro
- [ ] Botão muda para ☀️ (sol) em tema escuro
- [ ] Ao clicar novamente, volta para tema claro
- [ ] Ao recarregar a página (F5), o tema é mantido
- [ ] Ícone é clicável e responsivo

**Resultado Esperado**: Interface com fundo escuro (#1a1d23) e texto claro (#e0e3e8)

---

## 🌐 TESTE 2: Server Admin - Múltiplos IPs

### Teste 2A: Via IP de Gerenciamento
1. Acesse: **192.168.250.249:8080**
2. Login: `admin` / `admin123`
3. No menu lateral, clique em **⚙️ Servidor**

### Teste 2B: Via IP de Dispositivos
1. Acesse: **172.16.24.70:8080**
2. Login: `admin` / `admin123`
3. No menu lateral, clique em **⚙️ Servidor**

### Validações:
- [ ] Em ambas URLs, a aba Servidor carrega **sem erro**
- [ ] A aba mostra o painel de administração do servidor
- [ ] Sem mensagem "Módulo Servidor não configurado"
- [ ] Sem erro de timeout ou conexão recusada

**Resultado Esperado**: Servidor admin acessível de AMBOS os IPs

---

## 📋 Checklist de Conclusão

| Teste | Status | Observações |
|-------|--------|-------------|
| Dark mode toggle | ✅/❌ | Botão 🌙 funciona? |
| Persistência tema | ✅/❌ | Tema mantém após F5? |
| Admin IP 192.168 | ✅/❌ | Carrega sem erro? |
| Admin IP 172.16 | ✅/❌ | Carrega sem erro? |

---

## 🐛 Se algo não funcionar:

### Dark mode não aparece
- Abra DevTools (F12) → Console
- Se houver erros em vermelho, anote-os
- Tente acessar de novo incógnito (Ctrl+Shift+N)

### Server Admin não carrega
- Verifique que está digitando **correto**: `192.168.250.249:8080` ou `172.16.24.70:8080`
- Confirme que consegue fazer login (prova que servidores estão online)
- Tente F5 para recarregar

---

**Gerado**: 2026-10-23  
**Versão**: HWMDM 1.0.0
