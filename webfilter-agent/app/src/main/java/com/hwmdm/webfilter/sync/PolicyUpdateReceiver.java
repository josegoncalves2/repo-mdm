package com.hwmdm.webfilter.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PolicyUpdateReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent != null && "com.hwmdm.webfilter.action.POLICY_UPDATE".equals(intent.getAction())) {
            PolicyScheduler.scheduleNow(context);
        }
    }
}
