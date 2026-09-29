package com.hwmdm.remote.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

import com.hwmdm.remote.mdm.BlockOverlay;
import com.hwmdm.remote.mdm.BlockedPageReporter;
import com.hwmdm.remote.mdm.RemoteLog;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * <p>Reproduz no aparelho o toque e a digitacao que o tecnico faz no painel.</p>
 *
 * <p>Um servico de acessibilidade e' o unico caminho disponivel. {@code INJECT_EVENTS} e'
 * uma permissao de assinatura de sistema, e ser device owner nao a concede -- por isso
 * nenhuma ferramenta de suporte remoto que nao venha embutida na ROM consegue clicar sem
 * passar por aqui. Foi por nao existir esse caminho que a versao anterior deste recurso
 * era so' visualizacao.</p>
 *
 * <p><b>Sobre ler a janela.</b> O servico declara {@code canRetrieveWindowContent} (ver
 * {@code res/xml/input_injection_config.xml}) porque, sem isso, {@code getRootInActiveWindow()}
 * sempre devolve {@code null} e a digitacao nunca encontra onde escrever -- era exatamente
 * isso que quebrava a digitacao antes desta correcao. A capacidade nao vira vigilancia: o
 * servico nao registra, loga nem envia a lugar nenhum o conteudo das janelas; a arvore de
 * acessibilidade e' lida so' para achar o campo em foco na hora de digitar, e descartada
 * (via {@code recycle()}) logo em seguida. A imagem da tela em si vem da MediaProjection,
 * que o usuario autoriza explicitamente e que mostra um indicador enquanto dura -- esse
 * servico de acessibilidade nao participa disso.</p>
 */
public class InputInjectionService extends AccessibilityService {

    private static final String TAG = "hwmdm-remote";

    /** Duracao de um toque simples. Curto o bastante para nao virar toque longo. */
    private static final long TAP_DURATION_MS = 60L;
    private static final long LONG_PRESS_DURATION_MS = 700L;

    private static volatile InputInjectionService instance;

    /**
     * @return o servico, ou {@code null} quando a acessibilidade nao foi habilitada nas
     *         configuracoes do aparelho. Nesse caso a sessao continua valida -- a tela e'
     *         transmitida -- apenas sem toque e sem digitacao.
     */
    public static InputInjectionService get() {
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    /**
     * <p>Se ESTE servico esta marcado nas Configuracoes do aparelho.</p>
     *
     * <p>Nao e' a mesma pergunta que {@link #isAvailable()}, e a diferenca entre as duas e'
     * justamente o que vinha faltando para diagnosticar. {@code isAvailable()} responde se o
     * Android ligou o servico; esta responde se alguem o marcou. Combinadas, separam tres
     * situacoes que ate agora produziam identicamente "sem controle remoto":</p>
     *
     * <ul>
     *   <li>nao marcado: ninguem ativou -- ou ativou <b>outro</b> servico. O launcher
     *       Headwind tem um servico de acessibilidade proprio
     *       de acessibilidade do launcher, entao a lista do aparelho tem
     *       duas entradas parecidas e marcar a errada nao produz nenhum aviso;</li>
     *   <li>marcado mas nao ligado: o Android nao vinculou o servico -- acontece depois de
     *       atualizar o aplicativo, e se resolve desmarcando e marcando de novo;</li>
     *   <li>marcado e ligado: funcionando.</li>
     * </ul>
     */
    public static boolean isEnabledInSettings(android.content.Context context) {
        try {
            String ativos = android.provider.Settings.Secure.getString(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (ativos == null || ativos.isEmpty()) {
                return false;
            }
            String alvo = context.getPackageName() + "/" + InputInjectionService.class.getName();
            String alvoCurto = context.getPackageName() + "/."
                    + InputInjectionService.class.getSimpleName();
            for (String entrada : ativos.split(":")) {
                String e = entrada.trim();
                if (e.equalsIgnoreCase(alvo) || e.equalsIgnoreCase(alvoCurto)
                        || e.startsWith(context.getPackageName() + "/")) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /*
     * Estes dois eventos vao para o log do servidor, e nao so' para o logcat.
     *
     * Sem isso nao ha como distinguir, do painel, "o usuario nao ativou a acessibilidade"
     * de "ativou e o Android desativou de novo" -- e o Android desativa sozinho em varias
     * situacoes, entre elas a atualizacao do proprio aplicativo. Os dois casos produzem
     * exatamente a mesma sessao sem controle remoto, e sem saber qual e' deles nao ha o
     * que corrigir.
     */
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "Injecao de toque disponivel");
        RemoteLog.i(this, "Acessibilidade CONECTADA: toque e digitacao remotos disponiveis");

        // Garante que o servico que recebe os pushes (protectionOn, textMessage, lockKiosk,
        // remoteScreenStart) esteja no ar.
        //
        // Por que aqui: num aparelho recem-matriculado o usuario faz os ajustes todos pelas
        // Configuracoes do Android -- sobreposicao, acessibilidade -- e nunca chega a abrir
        // este aplicativo. Sem abrir, o RemoteAgentService nao subia, o receiver dos pushes
        // nao ficava registrado, e comandos como "Proteger configuracoes" chegavam ao
        // launcher e morriam sem ninguem ouvindo. Habilitar a acessibilidade e' um passo que
        // o usuario SEMPRE faz; amarrar a partida do agente a ele fecha essa lacuna.
        try {
            RemoteAgentService.start(this);
        } catch (Throwable t) {
            Log.w(TAG, "Falha ao subir o agente a partir da acessibilidade", t);
        }
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        instance = null;
        RemoteLog.w(this, "Acessibilidade DESCONECTADA: a partir de agora a sessao fica sem controle remoto");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    /**
     * <p>A unica coisa que este servico escuta: qual aplicativo assumiu a tela.</p>
     *
     * <p>Nao ha leitura de conteudo -- {@code getPackageName()} vem no proprio evento, sem
     * {@code canRetrieveWindowContent}. Serve a {@link ProtectionGuard}, que devolve o
     * aparelho a tela inicial quando as Configuracoes sobem com a protecao ativa.</p>
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        // Unica excecao a "nao ha leitura de conteudo": a pagina de site bloqueado do Chrome.
        if (BlockedPageReporter.isBrowser(event.getPackageName())) {
            BlockedPageReporter.onBrowserEvent(this);
        }
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        // A propria janela de bloqueio e a barra do sistema tambem geram este evento; so' uma
        // troca de tela de verdade (outro app, outra atividade do Chrome) abre nova tentativa.
        CharSequence statePkg = event.getPackageName();
        if (statePkg != null && !getPackageName().contentEquals(statePkg)
                && !"com.android.systemui".contentEquals(statePkg)) {
            BlockedPageReporter.newNavigation();
        }
        // Outro aplicativo assumiu a tela: a pagina de bloqueio pertence ao Chrome e sai junto.
        // A propria janela de bloqueio dispara este evento com o pacote deste app ao abrir; sem
        // excluir o proprio pacote ela se fechava no mesmo instante e sobrava a tela do Chrome.
        CharSequence ownerPkg = event.getPackageName() == null ? "" : event.getPackageName();
        if (!BlockedPageReporter.isBrowser(ownerPkg) && BlockOverlay.isShown()
                && !getPackageName().contentEquals(ownerPkg)
                && !"com.android.systemui".contentEquals(ownerPkg)) {
            BlockOverlay.hide(this);
        }
        if (ProtectionGuard.shouldBlock(this, event.getPackageName())) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            ProtectionGuard.reportBlock(this, event.getPackageName());
        }
    }

    @Override
    public void onInterrupt() {
    }

    // =================================================================================================================

    /** Toque simples nas coordenadas em pixels da tela real. */
    public boolean tap(float x, float y) {
        return stroke(x, y, x, y, TAP_DURATION_MS);
    }

    public boolean longPress(float x, float y) {
        return stroke(x, y, x, y, LONG_PRESS_DURATION_MS);
    }

    /** Arrasto/rolagem: um unico traco continuo entre dois pontos. */
    public boolean swipe(float x1, float y1, float x2, float y2, long durationMs) {
        return stroke(x1, y1, x2, y2, Math.max(20L, Math.min(durationMs, 10_000L)));
    }

    private boolean stroke(float x1, float y1, float x2, float y2, long durationMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        try {
            Path path = new Path();
            path.moveTo(x1, y1);
            if (x1 != x2 || y1 != y2) {
                path.lineTo(x2, y2);
            }
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0L, durationMs))
                    .build();
            return dispatchGesture(gesture, null, null);
        } catch (Throwable t) {
            Log.w(TAG, "Gesto recusado", t);
            return false;
        }
    }

    /**
     * <p>Motivo da ultima falha de {@link #type(String)} ou {@link #key(String)}, quando
     * distinguivel. {@code null} quando a ultima chamada teve sucesso ou quando falhou por
     * um motivo generico (gesto recusado pelo sistema, por exemplo).</p>
     *
     * <p>Existe para separar, do lado do painel, "nao ha campo de texto em foco" de
     * "a acessibilidade esta desligada" -- ate esta correcao os dois casos chegavam ao
     * operador como o mesmo "nao aconteceu nada", e so' um dos dois se resolve pedindo para
     * tocar num campo antes de digitar.</p>
     *
     * <p>Nao e' {@code ThreadLocal} de proposito: {@link ScreenStreamService} chama estes
     * metodos sempre a partir da mesma {@code HandlerThread} de entrada, entao um campo
     * simples basta e evita a complicacao de limpar um ThreadLocal.</p>
     */
    private volatile String lastFailureReason;

    /**
     * Consome (le e limpa) o motivo da ultima falha. Chamar depois de {@code type}/{@code key}
     * terem devolvido {@code false}; antes disso ou depois de um sucesso o valor e' sempre
     * {@code null}.
     */
    public String consumeLastFailureReason() {
        String reason = lastFailureReason;
        lastFailureReason = null;
        return reason;
    }

    /**
     * <p>Insere um texto na posicao do cursor do campo que estiver em foco.</p>
     *
     * <p>Vai pela acao {@code ACTION_SET_TEXT} do no em foco, e nao por eventos de tecla:
     * um aplicativo sem privilegio de sistema nao consegue injetar KeyEvent, e emular
     * teclado por gestos no teclado virtual dependeria do layout do teclado instalado --
     * o que quebra em cada aparelho diferente.</p>
     *
     * <p>Ate esta correcao este metodo substituia o campo inteiro pelo texto novo -- o
     * proprio javadoc chamava isso de "limite conhecido da API". Nao era: {@code ACTION_SET_TEXT}
     * aceita qualquer {@code CharSequence}, entao nada impede montar o valor novo a partir
     * do valor atual e da selecao antes de aplicar a acao. E' o que {@link #insertAtCursor}
     * faz.</p>
     */
    public boolean type(String text) {
        lastFailureReason = null;
        if (text == null) {
            return false;
        }
        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused == null) {
            lastFailureReason = "no_focused_field";
            Log.w(TAG, "Digitacao ignorada: nenhum campo de texto em foco");
            return false;
        }
        try {
            return insertAtCursor(focused, text);
        } finally {
            focused.recycle();
        }
    }

    /**
     * Localiza o campo com foco de entrada na janela ativa. Devolve {@code null} tanto
     * quando nao ha janela ativa legivel quanto quando nenhum campo esta em foco nela; os
     * chamadores tratam os dois casos do mesmo jeito (nao ha onde digitar).
     */
    private AccessibilityNodeInfo findFocusedEditable() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            AccessibilityNodeInfo focused = findFocusedEditable(root);
            if (root != null) {
                root.recycle();
            }
            if (focused != null) {
                return focused;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                List<AccessibilityWindowInfo> windows = getWindows();
                if (windows != null) {
                    for (AccessibilityWindowInfo window : windows) {
                        AccessibilityNodeInfo windowRoot = null;
                        try {
                            windowRoot = window == null ? null : window.getRoot();
                            focused = findFocusedEditable(windowRoot);
                            if (focused != null) {
                                return focused;
                            }
                        } finally {
                            if (windowRoot != null) {
                                windowRoot.recycle();
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Falha ao localizar o campo em foco", t);
        }
        return null;
    }

    private AccessibilityNodeInfo findFocusedEditable(AccessibilityNodeInfo root) {
        if (root == null) {
            return null;
        }
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (isEditable(focused)) {
            return focused;
        }
        if (focused != null) {
            focused.recycle();
        }
        return findEditableInTree(root);
    }

    private AccessibilityNodeInfo findEditableInTree(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }
        if (isEditable(node) && (node.isFocused() || node.isAccessibilityFocused())) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = node.getChild(i);
                AccessibilityNodeInfo found = findEditableInTree(child);
                if (found != null) {
                    return found;
                }
            } finally {
                if (child != null) {
                    child.recycle();
                }
            }
        }
        return null;
    }

    private boolean isEditable(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        return node.isEditable()
                || "android.widget.EditText".contentEquals(node.getClassName());
    }

    /**
     * <p>Compoe o valor novo do campo inserindo {@code insert} na posicao do cursor --
     * substituindo a selecao quando ela nao estiver colapsada, exatamente como digitar por
     * cima de um trecho selecionado se comporta em qualquer editor -- e aplica o resultado
     * inteiro via {@code ACTION_SET_TEXT}, unica acao que a API oferece para mudar o texto.
     * O cursor e' reposicionado depois com {@code ACTION_SET_SELECTION} para o fim do que
     * acabou de ser inserido, para a proxima tecla continuar dali.</p>
     */
    private boolean insertAtCursor(AccessibilityNodeInfo node, String insert) {
        try {
            String value = fieldText(node);
            int start = node.getTextSelectionStart();
            int end = node.getTextSelectionEnd();
            if (start < 0 || end < 0 || start > value.length() || end > value.length()) {
                // Selecao invalida, ou aparelho/campo que nao a relata: cair no fim do
                // campo e' melhor do que recusar a digitacao inteira.
                start = value.length();
                end = value.length();
            }
            if (start > end) {
                int tmp = start;
                start = end;
                end = tmp;
            }
            String novo = value.substring(0, start) + insert + value.substring(end);
            return setTextAndCursor(node, novo, start + insert.length());
        } catch (Throwable t) {
            Log.w(TAG, "Digitacao recusada", t);
            return false;
        }
    }

    /**
     * <p>O texto que o usuario de fato escreveu no campo. Campo vazio relata a dica
     * ("Pesquise no Google ou digite um URL") como texto do no -- o Android faz isso de
     * proposito para leitores de tela --, e usar esse valor gravava a dica dentro do campo
     * junto com o que foi digitado. Nesse caso o valor real e' vazio.</p>
     */
    private static String fieldText(AccessibilityNodeInfo node) {
        CharSequence current = node.getText();
        if (current == null) {
            return "";
        }
        if (Build.VERSION.SDK_INT >= 26) {
            if (node.isShowingHintText()) {
                return "";
            }
            CharSequence hint = node.getHintText();
            if (hint != null && hint.length() > 0 && hint.toString().contentEquals(current)
                    && node.getTextSelectionStart() <= 0 && node.getTextSelectionEnd() <= 0) {
                // Campo customizado que nao marca isShowingHintText, mas relata a dica como
                // texto com o cursor no inicio: e' a dica, nao conteudo.
                return "";
            }
        }
        return current.toString();
    }

    /** Aplica o valor novo e tenta posicionar o cursor. O cursor e' cosmetico: se o campo
     * recusar {@code ACTION_SET_SELECTION} (alguns campos customizados recusam), o texto ja
     * foi escrito, e isso vale mais do que a posicao exata do cursor. */
    private boolean setTextAndCursor(AccessibilityNodeInfo node, String novo, int cursor) {
        Bundle setArgs = new Bundle();
        setArgs.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, novo);
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setArgs)) {
            return false;
        }
        Bundle selArgs = new Bundle();
        selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor);
        selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor);
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs);
        return true;
    }

    /** Apaga o caractere antes do cursor, ou a selecao inteira quando ela nao estiver
     * colapsada -- o mesmo comportamento de backspace em qualquer editor de texto. */
    private boolean backspace(AccessibilityNodeInfo node) {
        try {
            String value = fieldText(node);
            int start = node.getTextSelectionStart();
            int end = node.getTextSelectionEnd();
            if (start < 0 || end < 0 || start > value.length() || end > value.length()) {
                start = value.length();
                end = value.length();
            }
            if (start > end) {
                int tmp = start;
                start = end;
                end = tmp;
            }
            if (start == end) {
                if (start == 0) {
                    // Nada antes do cursor: nao e' erro, so' nao ha o que apagar.
                    return true;
                }
                start = start - 1;
            }
            String novo = value.substring(0, start) + value.substring(end);
            return setTextAndCursor(node, novo, start);
        } catch (Throwable t) {
            Log.w(TAG, "Backspace recusado", t);
            return false;
        }
    }

    /**
     * <p>Confirma o campo. No Android 11+ (API 30) usa {@code ACTION_IME_ENTER}, a acao que
     * o teclado usa para a tecla de confirmar/enviar -- e' o que faz um campo de busca
     * disparar a busca, por exemplo, em vez de so' quebrar linha. Em aparelhos mais antigos,
     * ou quando o campo nao trata essa acao, o recuo e' inserir uma quebra de linha, que ao
     * menos funciona em qualquer campo multi-linha.</p>
     */
    private boolean enter(AccessibilityNodeInfo node) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                // ACTION_IME_ENTER so' existe como AccessibilityAction (objeto), nao como
                // constante inteira solta em AccessibilityNodeInfo; performAction(int)
                // exige o id de dentro dele.
                if (node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId())) {
                    return true;
                }
            } catch (Throwable ignored) {
                // Cai no recuo abaixo.
            }
        }
        return insertAtCursor(node, "\n");
    }

    /**
     * Move o cursor sem alterar o texto. {@code delta} negativo move para a esquerda; a
     * posicao e' presa aos limites do campo, entao pedir para mover alem da borda so' deixa
     * o cursor na borda -- nao e' erro.
     */
    private boolean moveCursor(AccessibilityNodeInfo node, int delta) {
        try {
            int len = fieldText(node).length();
            int start = node.getTextSelectionStart();
            int end = node.getTextSelectionEnd();
            int pos = end >= 0 ? end : (start >= 0 ? start : len);
            pos = Math.max(0, Math.min(len, pos + delta));
            Bundle selArgs = new Bundle();
            selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, pos);
            selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, pos);
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs);
        } catch (Throwable t) {
            Log.w(TAG, "Movimento de cursor recusado", t);
            return false;
        }
    }

    /**
     * <p>Teclas que o painel manda por nome em vez de coordenada.</p>
     *
     * <p>Dois grupos, tratados diferente: {@code back}/{@code home}/{@code recents}/
     * {@code notifications} sao acoes globais do sistema e nao dependem de campo nenhum em
     * foco -- os nomes e o comportamento sao os mesmos de antes desta correcao, porque o
     * painel ja os chama por eles. {@code backspace}/{@code enter}/{@code tab}/{@code left}/
     * {@code right} sao novos, agem sobre o campo em foco e por isso passam pela mesma busca
     * que {@link #type(String)} faz -- sem campo em foco, apagar/mover/confirmar nao tem
     * onde atuar.</p>
     */
    public boolean key(String name) {
        lastFailureReason = null;
        if (name == null) {
            return false;
        }

        Integer globalAction = null;
        switch (name) {
            case "back":     globalAction = GLOBAL_ACTION_BACK; break;
            case "home":     globalAction = GLOBAL_ACTION_HOME; break;
            case "recents":  globalAction = GLOBAL_ACTION_RECENTS; break;
            case "notifications": globalAction = GLOBAL_ACTION_NOTIFICATIONS; break;
            default: break;
        }
        if (globalAction != null) {
            try {
                return performGlobalAction(globalAction);
            } catch (Throwable t) {
                Log.w(TAG, "Tecla recusada", t);
                return false;
            }
        }

        switch (name) {
            case "backspace":
            case "enter":
            case "tab":
            case "left":
            case "right":
                break;
            default:
                Log.w(TAG, "Tecla desconhecida: " + name);
                return false;
        }

        AccessibilityNodeInfo focused = findFocusedEditable();
        if (focused == null) {
            lastFailureReason = "no_focused_field";
            Log.w(TAG, "Tecla '" + name + "' ignorada: nenhum campo de texto em foco");
            return false;
        }
        try {
            switch (name) {
                case "backspace": return backspace(focused);
                case "enter":     return enter(focused);
                // Tab nao tem acao padronizada de "proximo campo" na arvore de
                // acessibilidade fora de conteudo web; inserir o caractere e' o
                // comportamento util que sobra, e e' aceito por editores e navegadores.
                case "tab":       return insertAtCursor(focused, "\t");
                case "left":      return moveCursor(focused, -1);
                case "right":     return moveCursor(focused, 1);
                default:          return false;
            }
        } finally {
            focused.recycle();
        }
    }
}
