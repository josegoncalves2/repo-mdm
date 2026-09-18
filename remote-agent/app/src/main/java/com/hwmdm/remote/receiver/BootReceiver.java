package com.hwmdm.remote.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hwmdm.remote.service.RemoteAgentService;

/**
 * Sobe o agente no boot.
 *
 * <p>Sem isto o suporte remoto so' funcionaria em aparelho que alguem tivesse aberto o
 * aplicativo desde o ultimo reinicio -- e um tablet em quiosque passa meses sem que
 * ninguem abra nada. O gatilho remoto precisa estar registrado antes de ser necessario,
 * nao depois.</p>
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            RemoteAgentService.start(context);
        }
    }
}
