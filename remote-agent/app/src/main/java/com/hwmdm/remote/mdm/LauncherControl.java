package com.hwmdm.remote.mdm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * <p>Traz o launcher do MDM de volta ao primeiro plano.</p>
 *
 * <p>Este e' o handler do push {@code lockKiosk}. O launcher 6.36 oficial trata
 * {@code exitKiosk} -- sair do modo quiosque temporariamente -- mas <b>nao</b> tem a
 * constante do caminho inverso: {@code lockKiosk} nao existe no binario, entao o push cai
 * no repasse para plugins e chegava a lugar nenhum. Era por isso que "Liberar
 * temporariamente" funcionava e "Bloquear (kiosk)" nao surtia efeito: metade do par
 * estava implementada.</p>
 *
 * <p>Voltar ao quiosque, na pratica, e' voltar ao launcher. Ele e' o home do aparelho e
 * reaplica a propria politica de quiosque ao assumir o primeiro plano; nao ha nada aqui
 * decidindo o que o quiosque significa, o que manteria duas definicoes da mesma regra em
 * lugares diferentes. Este codigo so' diz "volte para o launcher".</p>
 */
public final class LauncherControl {

    /** O mesmo pacote declarado em &lt;queries&gt;; sem aquilo, o Android 11+ o esconderia. */
    public static final String LAUNCHER_PACKAGE = "com.hmdm.launcher";

    private LauncherControl() {
    }

    /**
     * @return null em caso de sucesso, ou o motivo da falha - o chamador registra no log do
     *         servidor, que e' onde o operador vai procurar quando o botao nao surtir efeito.
     */
    public static String bringToFront(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(LAUNCHER_PACKAGE);
        if (intent == null) {
            // Sem launcher instalado nao ha quiosque para restaurar. Isso nao deveria
            // acontecer -- e' ele quem nos entregou o push -- mas um aparelho em meio a uma
            // desinstalacao chegaria aqui.
            return "launcher " + LAUNCHER_PACKAGE + " nao encontrado neste aparelho";
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(intent);
            return null;
        } catch (Throwable t) {
            return "falha ao abrir o launcher: " + t;
        }
    }
}
