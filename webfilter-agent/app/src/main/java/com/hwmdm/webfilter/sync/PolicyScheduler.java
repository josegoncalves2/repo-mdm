package com.hwmdm.webfilter.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public final class PolicyScheduler {
    private static final int JOB_ID = 0x5746;
    private PolicyScheduler() { }

    public static void schedule(Context context) {
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, PolicySyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000L);
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) scheduler.schedule(builder.build());
    }

    public static void scheduleNow(Context context) {
        JobInfo job = new JobInfo.Builder(JOB_ID + 1,
                new ComponentName(context, PolicySyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(0)
                .setOverrideDeadline(60_000)
                .build();
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) scheduler.schedule(job);
    }
}
