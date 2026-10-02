package com.hwmdm.webfilter.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PolicySyncJobService extends JobService {
    private static final String TAG = "hwmdm-webfilter";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public boolean onStartJob(JobParameters params) {
        executor.execute(() -> {
            boolean retry = false;
            try {
                BrowserPolicySync.refresh(this);
            } catch (Exception e) {
                Log.w(TAG, "Browser policy sync failed", e);
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
