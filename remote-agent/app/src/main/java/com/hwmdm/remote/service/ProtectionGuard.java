package com.hwmdm.remote.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import com.hwmdm.remote.mdm.RemoteLog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Impede que as Configuracoes do Android sejam abertas no aparelho, para que ninguem
 * desfaca as permissoes de que o suporte remoto depende.</p>
 *
 * <h3>Por que nao e' modo quiosque</h3>
 *
 * <p>O caminho oficial seria {@code startLockTask}, e ele exige que o aplicativo esteja na
 * lista de {@code setLockTaskPackages} -- chamada que so' o <b>device owner</b> pode fazer.
 * O device owner aqui e' o launcher, e o {@code hmdm-6.36-os.apk} nao expoe isso: o binario
 * nao contem {@code startLockTask}, {@code stopLockTask} nem {@code setLockTaskPackages}, e
 * a classe {@code ProUtils} que decidiria o quiosque devolve false na versao livre
 * ({@code kioskModeRequired()} devolve false). Modo quiosque e' recurso pago do Headwind, e
 * o perfil chamado "Kiosk Total" nunca teve efeito -- e' por isso que os aparelhos sempre
 * reportaram {@code kioskMode: false}.</p>
 *
 * <p>Entao a protecao e' feita de fora: o servico de acessibilidade ja recebe
 * {@code TYPE_WINDOW_STATE_CHANGED}, que carrega o pacote da janela que assumiu a tela. Ao
 * ver as Configuracoes subirem, manda o aparelho de volta para a tela inicial. E' o mesmo
 * mecanismo que aplicativos de quiosque usam quando nao sao device owner.</p>
 *
 * <h3>O que isto garante, e o que nao garante</h3>
 *
 * <p><b>Nao</b> e' uma barreira criptografica: e' uma corrida. A tela das Configuracoes
 * chega a aparecer por um instante antes de ser fechada. O que ela garante e' que ninguem
 * <i>permanece</i> ali o suficiente para navegar ate um interruptor e desliga-lo.</p>
 *
 * <p>Ha uma propriedade util: a protecao depende da acessibilidade, e desligar a
 * acessibilidade exige entrar nas Configuracoes -- que e' justamente o que ela bloqueia.
 * Uma vez ativa, ela se sustenta.</p>
 *
 * <p><b>E por isso ela e' desligavel pelo painel.</b> Uma protecao que se sustenta sozinha e
 * que nao pudesse ser removida a distancia seria um jeito de perder o aparelho por engano.
 * O comando "Liberar configuracoes" existe para isso e e' a razao de o estado ficar
 * gravado em disco: ele precisa sobreviver a um reinicio, senao bastaria reiniciar o tablet
 * para contornar tudo.</p>
 */
public final class ProtectionGuard {

    private static final String PREFS = "hwmdm_protecao";
    private static final String KEY_ACTIVE = "ativa";
    private static final String KEY_AUTO_ENGAGED = "auto_engajada";

    /**
     * Pacotes de Configuracoes. Samsung usa o proprio alem do padrao do Android, e os dois
     * levam as mesmas telas de acessibilidade e sobreposicao.
     */
    private static final Set<String> BLOQUEADOS = new HashSet<>(Arrays.asList(
            "com.android.settings",
            "com.samsung.android.settings",
            "com.android.settings.intelligence",
            "com.google.android.settings.intelligence"
    ));

    private static volatile Boolean cache;

    private ProtectionGuard() {
    }

    public static boolean isActive(Context context) {
        Boolean current = cache;
        if (current != null) {
            return current;
        }
        boolean stored = prefs(context).getBoolean(KEY_ACTIVE, false);
        cache = stored;
        return stored;
    }

    public static void setActive(Context context, boolean active) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, active).apply();
        cache = active;
    }

    /**
     * <p>Se a protecao ja foi ligada automaticamente neste aparelho, ao fim do primeiro
     * cadastro das permissoes (ver {@link com.hwmdm.remote.ui.StatusActivity}).</p>
     *
     * <p>Existe para a auto-ativacao acontecer <b>uma vez so'</b>. Sem essa marca, toda vez
     * que o tecnico reabrisse o aplicativo com acessibilidade e sobreposicao concedidas a
     * protecao voltaria sozinha -- inclusive logo depois de o painel manda-la desligar de
     * proposito (comando "Liberar configuracoes", usado para manutencao). Isso tornaria o
     * botao de liberar inutil na pratica. A marca fica gravada mesmo quando a protecao e'
     * liberada depois, entao "liberar pelo painel" continua valendo ate alguem religar --
     * pelo painel ou reinstalando o aplicativo.</p>
     */
    public static boolean wasAutoEngaged(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_ENGAGED, false);
    }

    public static void markAutoEngaged(Context context) {
        prefs(context).edit().putBoolean(KEY_AUTO_ENGAGED, true).apply();
    }

    /**
     * @param pacote o pacote da janela que acabou de assumir a tela.
     * @return true quando o aparelho deve ser mandado de volta a tela inicial.
     */
    public static boolean shouldBlock(Context context, CharSequence pacote) {
        if (pacote == null || !isActive(context)) {
            return false;
        }
        if (System.currentTimeMillis() < suspensaAte) {
            return false;
        }
        String p = pacote.toString();
        return BLOQUEADOS.contains(p) || resolvidos(context).contains(p);
    }

    /**
     * <p>Suspende a protecao por um instante.</p>
     *
     * <p>Existe porque a protecao derrubava o proprio pedido de captura de tela: neste
     * aparelho a janela de autorizacao da MediaProjection pertence a
     * {@code com.android.settings}, o mesmo pacote que a protecao fecha. O resultado era o
     * atendimento parar de abrir sozinho assim que a protecao era ligada -- um recurso
     * quebrando o outro, e por um motivo que nao aparece em lugar nenhum ate se cruzar os
     * dois registros.</p>
     *
     * <p>A janela e' curta e tem prazo proprio: se o consentimento nunca vier -- ninguem
     * respondeu, a tela ficou aberta e esquecida -- a protecao volta sozinha, em vez de
     * depender de alguem lembrar de reativa-la.</p>
     */
    public static void suspend(long millis) {
        suspensaAte = System.currentTimeMillis() + Math.max(0, millis);
    }

    public static void resume() {
        suspensaAte = 0L;
    }

    private static volatile long suspensaAte;

    /**
     * <p>Os pacotes que o proprio sistema declara como donos das telas de configuracao.</p>
     *
     * <p>A lista fixa acima cobre os nomes conhecidos, mas depender so' dela e' apostar que
     * eu adivinhei o nome usado por cada fabricante -- e uma tela de permissao que escape da
     * lista e' uma porta aberta com aparencia de porta fechada. Perguntar ao
     * {@link PackageManager} quem atende as intencoes de configuracao devolve o nome real
     * deste aparelho, seja ele qual for.</p>
     *
     * <p>Resolvido uma vez e guardado: sao consultas ao sistema, e isto roda a cada troca de
     * janela.</p>
     */
    private static Set<String> resolvidos(Context context) {
        Set<String> atual = resolvidosCache;
        if (atual != null) {
            return atual;
        }
        Set<String> encontrados = new HashSet<>();
        String[] acoes = {
                android.provider.Settings.ACTION_SETTINGS,
                android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS,
                android.provider.Settings.ACTION_APPLICATION_SETTINGS,
                android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS
        };
        try {
            PackageManager pm = context.getPackageManager();
            for (String acao : acoes) {
                try {
                    android.content.pm.ResolveInfo r =
                            pm.resolveActivity(new android.content.Intent(acao), 0);
                    if (r != null && r.activityInfo != null && r.activityInfo.packageName != null) {
                        encontrados.add(r.activityInfo.packageName);
                    }
                } catch (Throwable ignored) {
                    // Uma acao inexistente neste aparelho nao invalida as demais.
                }
            }
        } catch (Throwable ignored) {
        }
        resolvidosCache = encontrados;
        return encontrados;
    }

    private static volatile Set<String> resolvidosCache;

    /**
     * Registra a tentativa no log do painel, com intervalo minimo entre avisos.
     *
     * Sem o intervalo, uma pessoa insistindo em abrir as Configuracoes geraria dezenas de
     * linhas por minuto e afogaria o log do aparelho -- que e' onde se procura outra coisa
     * quando algo da errado.
     */
    public static void reportBlock(Context context, CharSequence pacote) {
        long agora = System.currentTimeMillis();
        if (agora - ultimoAviso < INTERVALO_AVISO_MS) {
            return;
        }
        ultimoAviso = agora;
        RemoteLog.w(context, "Protecao ativa: acesso as Configuracoes bloqueado (" + pacote + ")");
    }

    private static volatile long ultimoAviso;
    private static final long INTERVALO_AVISO_MS = 60_000L;

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
