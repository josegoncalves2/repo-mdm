package com.hwmdm.webfilter.ui;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hwmdm.webfilter.browser.WebFilterAccessibilityService;
import com.hwmdm.webfilter.sync.BrowserPolicySync;
import com.hwmdm.webfilter.sync.PolicyScheduler;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private TextView status;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 40, 32, 32);

        TextView title = new TextView(this);
        title.setText("HWMDM Web Filter v" + com.hwmdm.webfilter.BuildConfig.VERSION_NAME);
        title.setTextSize(24);
        layout.addView(title);

        status = new TextView(this);
        status.setTextSize(14);
        status.setPadding(0, 16, 0, 0);
        layout.addView(status);

        setContentView(layout);

        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        boolean delegated = false;
        if (dpm != null) {
            try {
                delegated = dpm.getDelegatedScopes(null, getPackageName())
                        .contains(DevicePolicyManager.DELEGATION_APP_RESTRICTIONS);
            } catch (Exception ignored) { }
        }
        boolean accessibility = WebFilterAccessibilityService.isAvailable();

        StringBuilder sb = new StringBuilder();
        sb.append("Delegação APP_RESTRICTIONS: ").append(delegated ? "✓ Ativa" : "✗ Aguardando MDM 6.36").append("\n");
        sb.append("Serviço de acessibilidade: ").append(accessibility ? "✓ Ativo" : "✗ Desativado").append("\n\n");

        if (delegated) {
            sb.append("Sincronizando políticas do navegador…");
        } else {
            sb.append("O MDM 6.36 precisa conceder DELEGATION_APP_RESTRICTIONS a este app.");
        }
        status.setText(sb.toString());

        PolicyScheduler.schedule(this);

        Executors.newSingleThreadExecutor().execute(() -> {
            String result;
            try { result = BrowserPolicySync.refresh(this); }
            catch (Exception e) { result = "Erro: " + e.getMessage(); }
            final String show = result;
            runOnUiThread(() -> status.setText(show + "\n\nAtualizado: "
                    + DateFormat.getDateTimeInstance().format(new Date())));
        });
    }
}
