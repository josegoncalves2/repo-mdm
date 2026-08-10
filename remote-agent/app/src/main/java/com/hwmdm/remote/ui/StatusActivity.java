package com.hwmdm.remote.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hwmdm.remote.R;
import com.hwmdm.remote.mdm.MdmLink;
import com.hwmdm.remote.service.InputInjectionService;
import com.hwmdm.remote.service.RemoteAgentService;
import com.hwmdm.remote.service.ScreenStreamService;

/**
 * <p>Tela unica do aplicativo.</p>
 *
 * <p>Ela existe por dois motivos, ambos praticos. Primeiro, o Android exige que a
 * acessibilidade seja habilitada pelo proprio usuario nas Configuracoes -- nem device
 * owner concede isso a outro aplicativo (o launcher tem o codigo para tentar, mas so'
 * funciona em build com privilegio de sistema). Alguem precisa de um lugar que leve ate
 * la. Segundo, quando o suporte remoto nao funciona, o tecnico precisa saber onde parou:
 * se falta vinculo com o MDM, ou se falta a permissao de toque.</p>
 *
 * <p>A interface e' montada em codigo, sem layout XML, porque sao seis elementos e um
 * arquivo de layout a mais seria um arquivo a mais para manter sincronizado.</p>
 */
public class StatusActivity extends Activity {

    private TextView sessionView;
    private TextView mdmView;
    private TextView inputView;
    private TextView overlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.status_title);

        // Garante que o agente esteja de pe: se alguem abriu o aplicativo, e' porque
        // espera que ele esteja pronto para atender.
        RemoteAgentService.start(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 48);

        sessionView = addText(root, "", 20, true);
        addSpace(root, 32);

        mdmView = addText(root, "", 14, false);
        addSpace(root, 32);

        addText(root, getString(R.string.perm_accessibility_title), 16, true);
        inputView = addText(root, "", 14, false);
        addSpace(root, 8);

        Button open = new Button(this);
        open.setText(R.string.perm_accessibility_open);
        open.setOnClickListener(v -> openAccessibilitySettings());
        root.addView(open);

        addSpace(root, 16);
        addText(root, getString(R.string.perm_accessibility_hint), 12, false);

        addSpace(root, 32);

        // A permissao que decide se o atendimento comeca sozinho ou so' depois de alguem
        // abrir este aplicativo. Fica ao lado da acessibilidade porque as duas sao
        // concedidas do mesmo jeito: uma vez por aparelho, pelo usuario, nas Configuracoes.
        addText(root, getString(R.string.perm_overlay_title), 16, true);
        overlayView = addText(root, "", 14, false);
        addSpace(root, 8);

        Button overlay = new Button(this);
        overlay.setText(R.string.perm_overlay_open);
        overlay.setOnClickListener(v -> openOverlaySettings());
        root.addView(overlay);

        addSpace(root, 16);
        addText(root, getString(R.string.perm_overlay_hint), 12, false);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        // A consulta ao MDM e' um bind entre processos; fora da thread principal para nao
        // travar a tela num aparelho lento.
        new Thread(() -> {
            MdmLink.Config config = MdmLink.query(this);
            runOnUiThread(() -> mdmView.setText(config == null
                    ? getString(R.string.mdm_not_linked)
                    : getString(R.string.mdm_linked, config.toString())));
        }, "hwmdm-remote-status").start();
    }

    private void refresh() {
        boolean sharing = ScreenStreamService.isStreaming();
        sessionView.setText(sharing ? R.string.status_sharing : R.string.status_waiting);
        sessionView.setTextColor(sharing ? Color.parseColor("#C62828") : Color.parseColor("#2E7D32"));

        boolean input = InputInjectionService.isAvailable();
        inputView.setText(input ? R.string.perm_granted : R.string.perm_missing);
        inputView.setTextColor(input ? Color.parseColor("#2E7D32") : Color.parseColor("#EF6C00"));

        boolean overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        overlayView.setText(overlay ? R.string.perm_granted : R.string.perm_missing);
        overlayView.setTextColor(overlay ? Color.parseColor("#2E7D32") : Color.parseColor("#EF6C00"));
    }

    private void openOverlaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) {
            // Aparelho sem essa tela de configuracao; nada a fazer daqui.
        }
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) {
            // Aparelho sem essa tela de configuracao; nada a fazer daqui.
        }
    }

    private TextView addText(LinearLayout parent, String text, int sizeSp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setGravity(Gravity.START);
        if (bold) {
            view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        }
        parent.addView(view);
        return view;
    }

    private void addSpace(LinearLayout parent, int height) {
        View space = new View(this);
        space.setMinimumHeight(height);
        parent.addView(space);
    }
}
