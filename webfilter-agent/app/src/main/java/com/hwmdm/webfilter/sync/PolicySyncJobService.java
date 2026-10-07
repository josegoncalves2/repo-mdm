package com.hwmdm.webfilter.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import com.hwmdm.webfilter.BuildConfig;
import com.hwmdm.webfilter.mdm.RemoteLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PolicySyncJobService extends JobService {
    private static final String TAG = "hwmdm-webfilter";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public boolean onStartJob(JobParameters params) {
        executor.execute(() -> {
            boolean retry = false;
            try {
                String status = BrowserPolicySync.refresh(this);
                RemoteLog.i(this, "Web Filter " + BuildConfig.VERSION_NAME + ": " + status);
            } catch (Exception e) {
                Log.w(TAG, "Browser policy sync failed", e);
                RemoteLog.e(this, "Web Filter " + BuildConfig.VERSION_NAME + ": sync falhou: " + e.getMessage());
                retry = true;
            }
            jobFinished(params, retry);
        });
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        return true;
    }
}
